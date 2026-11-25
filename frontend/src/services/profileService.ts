import { api } from './api';
import type { Preferences, Profile } from '../types/api';

export const profileService = {
  get: () => api.get<Profile>('/api/profile'),
  rename: (displayName: string) => api.patch<Profile>('/api/profile', { displayName }),
  requestEmailChange: (newEmail: string, currentPassword: string) =>
    api.post<{ pendingEmail: string; emailSent: boolean }>('/api/profile/email', { newEmail, currentPassword }),
  cancelEmailChange: () => api.del<Profile>('/api/profile/email'),
  confirmEmail: (token: string) => api.post<void>('/api/profile/email/confirm', { token }),
  changePassword: (currentPassword: string, newPassword: string, confirmPassword: string) =>
    api.post<Profile>('/api/profile/password', { currentPassword, newPassword, confirmPassword }),
  uploadAvatar: (file: File) => api.upload<Profile>('/api/profile/avatar', file),
  removeAvatar: () => api.del<Profile>('/api/profile/avatar'),
  updatePreferences: (prefs: Preferences) => api.put<Preferences>('/api/profile/preferences', prefs),
  deleteAccount: (currentPassword: string, confirmation: string) =>
    api.del<void>('/api/profile', { currentPassword, confirmation }),
};
