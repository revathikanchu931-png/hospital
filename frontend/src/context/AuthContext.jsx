import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import api from '../services/api';
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); const [loading, setLoading] = useState(true);
  useEffect(() => { const clear=()=>setUser(null);window.addEventListener('carevia:session-expired',clear);api.get('/auth/csrf').then(()=>api.get('/auth/me')).then(({ data }) => setUser(data)).catch(() => setUser(null)).finally(() => setLoading(false));return()=>window.removeEventListener('carevia:session-expired',clear); }, []);
  const login = useCallback(async (credentials) => { const { data } = await api.post('/auth/login', credentials); setUser(data); return data; }, []);
  const logout = useCallback(async () => { try { await api.post('/auth/logout'); } finally { setUser(null); } }, []);
  const refresh = useCallback(async () => { const { data } = await api.get('/auth/me'); setUser(data); return data; }, []);
  return <AuthContext.Provider value={{ user, loading, login, logout, refresh }}>{children}</AuthContext.Provider>;
}
export const useAuth = () => useContext(AuthContext);
