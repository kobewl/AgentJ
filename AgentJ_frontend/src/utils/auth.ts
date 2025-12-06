const TOKEN_KEY = 'agentj_token';
const USER_KEY = 'agentj_user';

export interface User {
  id: number;
  username: string;
  email: string;
  displayName: string;
  status: string;
  createdAt: string;
  lastLogin: string;
  preferences?: string[];
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function getUser(): User | null {
  const userStr = localStorage.getItem(USER_KEY);
  if (!userStr) return null;
  try {
    return JSON.parse(userStr);
  } catch {
    return null;
  }
}

export function setUser(user: User) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearUser() {
  localStorage.removeItem(USER_KEY);
}

export function clearAuth() {
  clearToken();
  clearUser();
}

export function isAuthenticated(): boolean {
  return !!getToken();
}
