import { api } from './api';
import type { TeacherDashboard, TriviaDetail, TriviaListItem, TriviaUpsert } from '../types/api';

export const triviaService = {
  list: () => api.get<TriviaListItem[]>('/api/trivias'),
  get: (id: number) => api.get<TriviaDetail>(`/api/trivias/${id}`),
  create: (data: TriviaUpsert) => api.post<TriviaDetail>('/api/trivias', data),
  update: (id: number, data: TriviaUpsert) => api.put<TriviaDetail>(`/api/trivias/${id}`, data),
  duplicate: (id: number) => api.post<TriviaDetail>(`/api/trivias/${id}/duplicate`),
  remove: (id: number) => api.del<void>(`/api/trivias/${id}`),
  dashboard: () => api.get<TeacherDashboard>('/api/teacher/dashboard'),
  uploadImage: (file: File) => api.upload<{ key: string; url: string }>('/api/media', file),
};
