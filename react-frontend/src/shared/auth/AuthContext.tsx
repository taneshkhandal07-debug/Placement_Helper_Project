import { createContext } from 'react';
import { CurrentUser, LoginRequest, LoginResponse } from '../types/auth.types';

export interface AuthContextType {
  user: CurrentUser | null;
  loading: boolean;
  error: string | null;
  login: (credentials: LoginRequest) => Promise<LoginResponse>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
