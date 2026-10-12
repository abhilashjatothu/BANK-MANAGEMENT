import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { bankApi } from '../services/bankApi.js';

const USER_KEY = 'apex_current_user';
const AuthContext = createContext(null);

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);

  const login = useCallback(async (username, password) => {
    // Make sure we know whether the backend is up before choosing live vs offline auth.
    await bankApi.checkHealth();
    const loggedIn = await bankApi.login(username, password);
    localStorage.setItem(USER_KEY, JSON.stringify(loggedIn));
    setUser(loggedIn);
    return loggedIn;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
