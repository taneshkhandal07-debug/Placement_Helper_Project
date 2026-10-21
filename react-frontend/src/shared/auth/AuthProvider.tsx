import React, { useState, useEffect, useCallback, ReactNode } from 'react';
import { AuthContext } from './AuthContext';
import { authApi } from '../api/auth';
import { CurrentUser, LoginRequest, LoginResponse } from '../types/auth.types';

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const refreshUser = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const currentUser = await authApi.getCurrentUser();
      setUser(currentUser);
    } catch {
      // 401 or network error indicates unauthenticated state
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = async (credentials: LoginRequest): Promise<LoginResponse> => {
    setError(null);
    try {
      const response = await authApi.login(credentials);
      // After successful session creation, fetch full user profile
      await refreshUser();
      return response;
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || 'Login failed';
      setError(msg);
      throw new Error(msg);
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        error,
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
