import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../services/api';
import { resultService } from '../services/resultService';
import type { GameResult } from '../types/api';

export type ResultsState =
  | { status: 'loading' }
  | { status: 'pending' } // la partida aún no ha terminado
  | { status: 'error'; message: string; code?: string }
  | { status: 'ready'; data: GameResult };

/** Carga los resultados definitivos calculados por el servidor. */
export function useResults(sessionId: number) {
  const [state, setState] = useState<ResultsState>({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setState({ status: 'loading' });
    resultService
      .results(sessionId, controller.signal)
      .then((data) => setState({ status: 'ready', data }))
      .catch((err: unknown) => {
        if ((err as Error).name === 'AbortError') return;
        if (err instanceof ApiError && err.code === 'RESULTS_NOT_READY') {
          setState({ status: 'pending' });
        } else if (err instanceof ApiError) {
          setState({ status: 'error', message: err.message, code: err.code });
        } else {
          setState({ status: 'error', message: 'No se pudieron cargar los resultados.' });
        }
      });
    return () => controller.abort();
  }, [sessionId, attempt]);

  const retry = useCallback(() => setAttempt((a) => a + 1), []);
  return { state, retry };
}
