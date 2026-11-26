import { api } from './api';
import type { GameResult, ParticipantReport, Podium } from '../types/api';

export const resultService = {
  results: (sessionId: number, signal?: AbortSignal) =>
    api.get<GameResult>(`/api/sessions/${sessionId}/results`, signal),
  podium: (sessionId: number, signal?: AbortSignal) => api.get<Podium>(`/api/sessions/${sessionId}/podium`, signal),
  /** Reporte de un participante (docente anfitrión o el propio participante). */
  report: (sessionId: number, participantId: number, signal?: AbortSignal) =>
    api.get<ParticipantReport>(`/api/sessions/${sessionId}/participants/${participantId}/report`, signal),
};
