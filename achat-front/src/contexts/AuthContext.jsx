import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api } from '../lib/api.js';

const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState(null);
  const reloadUser = useCallback(async () => {
    if (!api.readSession()) { setUser(null); setStatus('ready'); setError(null); return null; }
    try { const next = await api.request('/auth/me'); if (api.readSession()) setUser(next); setStatus('ready'); setError(null); return next; }
    catch (error) { if (!api.readSession()) { setUser(null); setStatus('ready'); } else { setError(error); setStatus('error'); } return null; }
  }, []);
  useEffect(() => { reloadUser(); return api.subscribe((session) => { if (!session) { setUser(null); setStatus('ready'); } }); }, [reloadUser]);
  const login = async (email, password) => { const session = await api.request('/auth/login', { method: 'POST', body: { email, password }, auth: false }); api.setSession(session); const next = await reloadUser(); if (!next) throw new Error('사용자 정보를 불러오지 못했습니다. 다시 시도해주세요.'); return next; };
  const logout = async () => { try { await api.logout(); } catch (error) { setError(error); } };
  return <AuthContext.Provider value={{ user, status, error, login, reloadUser, logout }}>{children}</AuthContext.Provider>;
}
export const useAuth = () => useContext(AuthContext);
