import {
  createContext, useCallback, useContext, useEffect, useMemo, useState, type Dispatch, type ReactNode, type SetStateAction,
} from 'react';
import { authService } from '../services/authService';
import { profileService } from '../services/profileService';
import type { Profile, Role, User } from '../types/api';

interface AuthContextValue {
  user: User | null;
  /** Perfil completo (foto y preferencias); se carga tras identificar la sesión. */
  profile: Profile | null;
  setProfile: Dispatch<SetStateAction<Profile | null>>;
  refreshProfile: () => Promise<void>;
  loading: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (data: { email: string; displayName: string; password: string; role: Role; teacherCode?: string }) => Promise<User>;
  logout: () => Promise<void>;
  /** Limpia la sesión en el cliente (p. ej. tras eliminar la cuenta). */
  clear: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** La identidad se consulta al servidor (cookie httpOnly); el cliente solo guarda el perfil en memoria. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshProfile = useCallback(async () => {
    try {
      const p = await profileService.get();
      setProfile(p);
      setUser((u) => (u ? { ...u, displayName: p.displayName, email: p.email } : u));
    } catch {
      setProfile(null);
    }
  }, []);

  useEffect(() => {
    authService
      .me()
      .then((u) => {
        setUser(u);
        return refreshProfile();
      })
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, [refreshProfile]);

  const login = useCallback(async (email: string, password: string) => {
    const u = await authService.login(email, password);
    setUser(u);
    void refreshProfile();
    return u;
  }, [refreshProfile]);

  const register = useCallback(async (data: Parameters<AuthContextValue['register']>[0]) => {
    const u = await authService.register(data);
    setUser(u);
    void refreshProfile();
    return u;
  }, [refreshProfile]);

  const clear = useCallback(() => {
    setUser(null);
    setProfile(null);
  }, []);

  const logout = useCallback(async () => {
    await authService.logout().catch(() => undefined);
    clear();
  }, [clear]);

  const value = useMemo(
    () => ({ user, profile, setProfile, refreshProfile, loading, login, register, logout, clear }),
    [user, profile, refreshProfile, loading, login, register, logout, clear],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}
