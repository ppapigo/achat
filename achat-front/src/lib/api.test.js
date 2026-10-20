import { describe, expect, it, vi } from 'vitest';
import { createApiClient, unwrapResponse } from './api.js';

function storage() { const values = new Map(); return { getItem: (key) => values.get(key), setItem: (key, value) => values.set(key, value), removeItem: (key) => values.delete(key) }; }
const json = (data, status = 200) => new Response(JSON.stringify(data), { status });
const tokens = { accessToken: 'old', refreshToken: 'refresh-old' };
const next = { accessToken: 'new', refreshToken: 'refresh-new' };

describe('API contracts and authentication recovery', () => {
  it('does not retry forbidden requests with a refresh token', async () => {
    const fetchImpl = vi.fn(async () => json({ error: '접근 권한이 없습니다' }, 403));
    const api = createApiClient({ storage: storage(), fetchImpl }); api.setSession(tokens);
    await expect(api.request('/api/admin/reports')).rejects.toMatchObject({ status: 403 });
    expect(fetchImpl).toHaveBeenCalledTimes(1);
    expect(api.readSession()).toEqual(tokens);
  });
  it('revokes the server session on logout and clears local credentials', async () => {
    const fetchImpl = vi.fn(async () => json({ data: null }));
    const api = createApiClient({ storage: storage(), fetchImpl }); api.setSession(tokens);
    await api.logout();
    expect(fetchImpl.mock.calls[0][0]).toBe('/backend/auth/logout');
    expect(JSON.parse(fetchImpl.mock.calls[0][1].body)).toEqual({ refreshToken: tokens.refreshToken });
    expect(api.readSession()).toBeNull();
  });
  it('finishes a refresh before revoking its replacement on logout', async () => {
    let release; const gate = new Promise((resolve) => { release = resolve; });
    const fetchImpl = vi.fn(async (url) => {
      if (url.endsWith('/auth/refresh')) { await gate; return json({ data: next }); }
      return json({ data: null });
    });
    const api = createApiClient({ storage: storage(), fetchImpl }); api.setSession(tokens);
    const refresh = api.refreshSession(); const logout = api.logout(); release();
    await Promise.all([refresh, logout]);
    expect(JSON.parse(fetchImpl.mock.calls.find(([url]) => url.endsWith('/auth/logout'))[1].body).refreshToken).toBe(next.refreshToken);
    expect(api.readSession()).toBeNull();
  });
  it('reads both backend response formats and surfaces business errors returned with HTTP 200', async () => {
    expect(unwrapResponse({ success: true, data: { id: 1 } })).toEqual({ id: 1 });
    expect(unwrapResponse({ data: { posts: [] } })).toEqual({ posts: [] });
    expect(unwrapResponse({ seasonId: 2 })).toEqual({ seasonId: 2 });
    const api = createApiClient({ fetchImpl: async () => json({ success: false, error: '코인이 부족합니다' }) });
    await expect(api.request('/purchase')).rejects.toThrow('코인이 부족합니다');
  });

  it('shares one refresh when concurrent requests receive expired-token errors', async () => {
    let release; const gate = new Promise((resolve) => { release = resolve; });
    const fetchImpl = vi.fn(async (url, options) => {
      if (url.endsWith('/auth/refresh')) { await gate; return json({ success: true, data: next }); }
      return options.headers.Authorization === 'Bearer old' ? json({}, 401) : json({ success: true, data: url });
    });
    const api = createApiClient({ storage: storage(), fetchImpl }); api.setSession(tokens);
    const first = api.request('/first'); const second = api.request('/second');
    await vi.waitFor(() => expect(fetchImpl.mock.calls.filter(([url]) => url.endsWith('/auth/refresh'))).toHaveLength(1));
    release();
    await expect(Promise.all([first, second])).resolves.toEqual(['/backend/first', '/backend/second']);
    expect(api.readSession()).toEqual(next);
  });

  it('does not refresh again when a delayed 401 used a token already replaced by another request', async () => {
    let release; const gate = new Promise((resolve) => { release = resolve; });
    const fetchImpl = vi.fn(async (url, options) => {
      if (url.endsWith('/auth/refresh')) return json({ data: next });
      if (options.headers.Authorization === 'Bearer old') { if (url.endsWith('/late')) await gate; return json({}, 401); }
      return json({ data: 'ok' });
    });
    const api = createApiClient({ storage: storage(), fetchImpl }); api.setSession(tokens);
    const late = api.request('/late'); await api.request('/first'); release(); await late;
    expect(fetchImpl.mock.calls.filter(([url]) => url.endsWith('/auth/refresh'))).toHaveLength(1);
  });

  it('does not restore credentials after logout during a refresh', async () => {
    let release; const gate = new Promise((resolve) => { release = resolve; });
    const api = createApiClient({ storage: storage(), fetchImpl: async () => { await gate; return json({ data: next }); } }); api.setSession(tokens);
    const refreshing = api.refreshSession(); api.setSession(null); release();
    await expect(refreshing).rejects.toMatchObject({ status: 401 });
    expect(api.readSession()).toBeNull();
  });

  it('clears expired credentials but retains them when the server is unreachable', async () => {
    const api = createApiClient({ storage: storage(), fetchImpl: async () => json({ error: '만료된 토큰' }, 401) }); api.setSession(tokens);
    await expect(api.refreshSession()).rejects.toMatchObject({ status: 401 }); expect(api.readSession()).toBeNull();
    const offline = createApiClient({ storage: storage(), fetchImpl: async () => { throw new TypeError('offline'); } }); offline.setSession(tokens);
    await expect(offline.refreshSession()).rejects.toThrow('offline'); expect(offline.readSession()).toEqual(tokens);
    const unavailable = createApiClient({ storage: storage(), fetchImpl: async () => json({}, 503) }); unavailable.setSession(tokens);
    await expect(unavailable.refreshSession()).rejects.toMatchObject({ status: 503 }); expect(unavailable.readSession()).toEqual(tokens);
  });

  it('preserves cancellation so stale page requests cannot become displayed network errors', async () => {
    const api = createApiClient({ fetchImpl: async () => { throw new DOMException('cancelled', 'AbortError'); } });
    await expect(api.request('/rooms')).rejects.toMatchObject({ name: 'AbortError' });
  });
});
