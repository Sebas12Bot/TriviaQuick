import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '../services/api';
import { sessionService } from '../services/sessionService';
import { useSessionSocket } from './useSessionSocket';
import type { SessionState } from '../types/api';

/**
 * Estado de una partida en vivo: lo pide por REST (proyección autorizada para quien consulta)
 * y lo vuelve a pedir ante cada aviso oficial del servidor por WebSocket.
 * Si el socket no está disponible, consulta cada pocos segundos como respaldo.
 */
export function useLiveSession(sessionId: number) {
  const [state, setState] = useState<SessionState | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const inFlight = useRef<AbortController | null>(null);

  const refresh = useCallback(async () => {
    inFlight.current?.abort();
    const controller = new AbortController();
    inFlight.current = controller;
    try {
      const s = await sessionService.state(sessionId, controller.signal);
      setState(s);
      setError(null);
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setError(err instanceof ApiError ? err : new ApiError(0, 'UNKNOWN', 'No se pudo cargar la partida.'));
    }
  }, [sessionId]);

  useEffect(() => {
    void refresh();
    return () => inFlight.current?.abort();
  }, [refresh]);

  const connected = useSessionSocket(sessionId, () => void refresh());

  useEffect(() => {
    if (connected || state?.status === 'FINISHED') return;
    const id = window.setInterval(() => void refresh(), 4000);
    return () => window.clearInterval(id);
  }, [connected, refresh, state?.status]);

  return { state, setState, error, refresh, connected };
}
