import { api } from './api';
import type {
  AnswerResult,
  EventResponseResult,
  GameEvent,
  PowerUp,
  SessionState,
  SessionSummary,
  TriviaSummary,
} from '../types/api';

export const sessionService = {
  mine: () => api.get<SessionSummary[]>('/api/sessions'),
  trivias: () => api.get<TriviaSummary[]>('/api/trivias'),
  create: (triviaId: number, eventsEnabled: boolean, eventFrequency: number) =>
    api.post<SessionSummary>('/api/sessions', { triviaId, eventsEnabled, eventFrequency }),
  join: (code: string) => api.post<SessionSummary>('/api/sessions/join', { code }),
  state: (id: number, signal?: AbortSignal) => api.get<SessionState>(`/api/sessions/${id}/state`, signal),
  start: (id: number) => api.post<SessionState>(`/api/sessions/${id}/start`),
  advance: (id: number) => api.post<SessionState>(`/api/sessions/${id}/advance`),
  finish: (id: number) => api.post<SessionState>(`/api/sessions/${id}/finish`),
  // Solo se envían la pregunta y la opción: el servidor decide si es correcta y cuántos puntos vale.
  answer: (id: number, questionId: number, answerId: number) =>
    api.post<AnswerResult>(`/api/sessions/${id}/answers`, { questionId, answerId }),
  currentEvent: (id: number) => api.get<GameEvent>(`/api/sessions/${id}/events/current`),
  respondEvent: (id: number, eventId: number, optionIndex: number) =>
    api.post<EventResponseResult>(`/api/sessions/${id}/events/${eventId}/responses`, { optionIndex }),
  activatePowerUp: (id: number, powerUpId: number) =>
    api.post<PowerUp>(`/api/sessions/${id}/power-ups/${powerUpId}/activate`),
};
