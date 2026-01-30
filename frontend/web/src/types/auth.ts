export interface User {
  id: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  phone?: string;
  defaultCurrency: string;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  phone?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  userId: string;
  email: string;
  displayName: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}
