export type UserRole = 'ROLE_STUDENT' | 'ROLE_ADMIN' | 'ROLE_ALUMNI' | 'ROLE_FACULTY';

export interface CurrentUser {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  branch: string;
  graduationYear: number;
  emailVerified: boolean;
  isMentor: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  redirectUrl: string;
}

export interface AuthState {
  user: CurrentUser | null;
  loading: boolean;
  error: string | null;
}
