export interface UserPreferences {
  defaultFuelType?: string;
  alertsEnabled: boolean;
  theme: 'light' | 'dark';
}

export interface User {
  id: string;
  email: string;
  name: string;
  preferences: UserPreferences;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  userId: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
