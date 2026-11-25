import { api } from './api';
import type { Ranking } from '../types/api';

export const rankingService = {
  ranking: (sessionId: number, signal?: AbortSignal) =>
    api.get<Ranking>(`/api/sessions/${sessionId}/ranking`, signal),
};
