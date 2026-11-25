import { api } from './api';
import type { Role, User } from '../types/api';

export const authService = {
  me: () => api.get<User>('/api/auth/me'),
  login: (email: string, password: string) => api.post<User>('/api/auth/login', { email, password }),
  register: (data: { email: string; displayName: string; password: string; role: Role; teacherCode?: string }) =>
    api.post<User>('/api/auth/register', data),
  logout: () => api.post<void>('/api/auth/logout'),
};
