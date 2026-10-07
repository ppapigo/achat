import { UI_PREVIEW } from '../preview/mode.js';

export class ApiError extends Error {
  constructor(message, status = 0) { super(message); this.name = 'ApiError'; this.status = status; }
}

export function unwrapResponse(payload) {
  if (payload?.success === false) throw new ApiError(payload.error || '요청을 처리하지 못했습니다.');
  return payload && Object.hasOwn(payload, 'data') ? payload.data : payload;
}

export function createApiClient({ baseUrl = '/backend', storage, sessionKey = 'achat.session', fetchImpl = (...args) => fetch(...args) } = {}) {
  const key = sessionKey;
  const listeners = new Set();
  let refreshing = null;
  const readSession = () => {
    try { return JSON.parse(storage?.getItem(key) || 'null'); } catch { return null; }
  };
  function setSession(session) {
    if (session) storage?.setItem(key, JSON.stringify(session)); else storage?.removeItem(key);
    listeners.forEach((listener) => listener(session));
  }
  async function parse(response) {
    const text = await response.text();
    let payload;
    try { payload = text ? JSON.parse(text) : null; } catch { throw new ApiError('서버 응답을 읽을 수 없습니다.', response.status); }
    if (!response.ok) throw new ApiError(payload?.error || payload?.message || (response.status === 403 ? '로그인이 필요하거나 접근 권한이 없습니다.' : '요청을 처리하지 못했습니다.'), response.status);
    return unwrapResponse(payload);
  }
  async function refreshSession() {
    if (refreshing) return refreshing;
    const previous = readSession();
    if (!previous?.refreshToken) throw new ApiError('다시 로그인해주세요.', 401);
    refreshing = (async () => {
      try {
        const response = await fetchImpl(`${baseUrl.replace(/\/$/, '')}/auth/refresh`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: previous.refreshToken }) });
        const next = await parse(response);
        if (!next?.accessToken || !next?.refreshToken) throw new ApiError('다시 로그인해주세요.', 401);
        if (readSession()?.refreshToken !== previous.refreshToken) throw new ApiError('로그인 상태가 변경되었습니다.', 401);
        setSession(next);
        return next;
      } catch (error) {
        if ([400, 401, 403].includes(error.status) && readSession()?.refreshToken === previous.refreshToken) setSession(null);
        throw error;
      } finally { refreshing = null; }
    })();
    return refreshing;
  }
  async function request(path, { method = 'GET', body, auth = true, signal, retry = true } = {}) {
    const session = readSession();
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (auth && session?.accessToken) headers.Authorization = `Bearer ${session.accessToken}`;
    let response;
    try { response = await fetchImpl(`${baseUrl.replace(/\/$/, '')}${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal }); }
    catch (error) { if (error.name === 'AbortError') throw error; throw new ApiError('서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.'); }
    if (auth && retry && session?.refreshToken && [401, 403].includes(response.status)) {
      // A later 401 may belong to the token another request has already refreshed.
      if (readSession()?.accessToken === session.accessToken) await refreshSession();
      if (!readSession()?.accessToken) throw new ApiError('다시 로그인해주세요.', 401);
      return request(path, { method, body, auth, signal, retry: false });
    }
    return parse(response);
  }
  async function ensureAccessToken() {
    const session = readSession();
    if (!session?.accessToken) throw new ApiError('로그인이 필요합니다.', 401);
    let expiresAt;
    try { expiresAt = JSON.parse(atob(session.accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).exp; } catch { /* The server remains the authority for token validity. */ }
    if (expiresAt && expiresAt * 1000 < Date.now() + 30000) return (await refreshSession()).accessToken;
    return session.accessToken;
  }
  return { request, readSession, setSession, refreshSession, ensureAccessToken, subscribe(listener) { listeners.add(listener); return () => listeners.delete(listener); } };
}

export const API_BASE = import.meta.env.VITE_API_BASE_URL || '/backend';
export const api = createApiClient({ baseUrl: API_BASE, storage: typeof window === 'undefined' ? null : window.sessionStorage, sessionKey: UI_PREVIEW ? 'achat.ui-preview.session' : 'achat.session', fetchImpl: import.meta.env.DEV && import.meta.env.MODE === 'ui-preview' ? async (...args) => (await import('../preview/data.js')).previewFetch(...args) : undefined });
export const query = (values) => { const entries = Object.entries(values).filter(([, value]) => value !== '' && value !== undefined && value !== null); const result = new URLSearchParams(entries).toString(); return result ? `?${result}` : ''; };
