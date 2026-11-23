import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import { api } from '../../lib/api/client';
import type { User } from '../../lib/api/types';
import { clearToken, getToken, saveToken } from '../../lib/auth/token';

type AuthContextValue = { user: User | null; loading: boolean; signIn: (email: string, password: string) => Promise<void>; signOut: () => void; refreshUser: () => Promise<void> };
const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = async () => {
    const token = getToken();
    if (!token) { setUser(null); return; }
    const profile = await api.me();
    setUser(profile);
  };

  useEffect(() => {
    refreshUser().catch(() => { clearToken(); setUser(null); }).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const expireSession = () => { clearToken(); setUser(null); };
    window.addEventListener('fintrack:session-expired', expireSession);
    return () => window.removeEventListener('fintrack:session-expired', expireSession);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user, loading,
    async signIn(email, password) {
      const response = await api.login(email, password);
      saveToken(response.accessToken);
      try { await refreshUser(); } catch (error) { clearToken(); throw error; }
    },
    signOut() { clearToken(); setUser(null); },
    refreshUser,
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth AuthProvider daxilində istifadə olunmalıdır');
  return context;
}
