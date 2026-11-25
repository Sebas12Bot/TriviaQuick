import { api } from './api';
import type { GameResult, Podium } from '../types/api';

export const resultService = {
  results: (sessionId: number, signal?: AbortSignal) =>
    api.get<GameResult>(`/api/sessions/${sessionId}/results`, signal),
  podium: (sessionId: number, signal?: AbortSignal) => api.get<Podium>(`/api/sessions/${sessionId}/podium`, signal),
};
