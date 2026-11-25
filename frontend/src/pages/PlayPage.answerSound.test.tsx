import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { PlayPage } from './PlayPage';
import { sessionService } from '../services/sessionService';
import { audioEngine, type SoundName } from '../services/audio/audioEngine';
import type { AnswerResult, SessionState } from '../types/api';

// El socket nunca publica: el componente solo escucha. Se sustituye para aislar la prueba.
const socketSpy = vi.fn();
vi.mock('../hooks/useSessionSocket', () => ({
  useSessionSocket: (...args: unknown[]) => {
    socketSpy(...args);
    return true;
  },
}));

const state = (answered: SessionState['myAnswer'] = null): SessionState => ({
  id: 1, triviaTitle: 'T', status: 'QUESTION', role: 'PARTICIPANT', joinCode: null, serverTime: new Date().toISOString(),
  questionCount: 2, answersReceived: null, currentEvent: null, myPowerUps: [], participants: [], lastQuestion: false,
  me: { participantId: 1, displayName: 'Ana', score: 0, correctAnswers: 0, answeredCount: 0, position: 1, currentStreak: 0, bestStreak: 0 },
  myAnswer: answered,
  currentQuestion: {
    id: 7, index: 0, total: 2, text: '¿Cuál?', type: 'MULTIPLE_CHOICE', imageUrl: null, timeLimitSeconds: 20, basePoints: 1000,
    startedAt: new Date().toISOString(), deadline: new Date(Date.now() + 20000).toISOString(),
    options: [{ id: 70, text: 'Buena' }, { id: 71, text: 'Mala' }],
  },
});

const result = (correct: boolean): AnswerResult => ({
  correct, pointsAwarded: correct ? 1100 : 0, multiplierApplied: false, totalScore: correct ? 1100 : 0, correctAnswerId: 70,
  basePoints: correct ? 1000 : 0, speedBonus: correct ? 100 : 0, multiplierFactor: 1, streak: correct ? 1 : 0, streakBonus: 0,
});

async function answerAndRecord(correct: boolean): Promise<SoundName[]> {
  const played: SoundName[] = [];
  const unsubscribe = audioEngine.subscribe((n) => played.push(n));
  vi.spyOn(sessionService, 'state')
    .mockResolvedValueOnce(state())
    .mockResolvedValue(state({ answerId: correct ? 70 : 71, correct, pointsAwarded: correct ? 1100 : 0, multiplierApplied: false,
      speedBonus: correct ? 100 : 0, multiplierFactor: 1 }));
  vi.spyOn(sessionService, 'answer').mockResolvedValue(result(correct));
  const view = render(
    <MemoryRouter initialEntries={['/play/1']}>
      <Routes><Route path="/play/:sessionId" element={<PlayPage />} /></Routes>
    </MemoryRouter>,
  );
  await userEvent.click(await screen.findByRole('button', { name: correct ? /Buena/ : /Mala/ }));
  await waitFor(() => expect(sessionService.answer).toHaveBeenCalled());
  await screen.findByRole('status', { name: '' }).catch(() => undefined);
  await new Promise((r) => setTimeout(r, 120));
  unsubscribe();
  view.unmount();
  return played.filter((n) => n !== 'tick' && n !== 'tickUrgent');
}

describe('Sonido de respuesta', () => {
  it('es exactamente el mismo para una respuesta correcta y una incorrecta', async () => {
    const correct = await answerAndRecord(true);
    vi.restoreAllMocks();
    await new Promise((r) => setTimeout(r, 80)); // evita el filtro anti-ráfaga entre pruebas
    const wrong = await answerAndRecord(false);
    expect(correct).toEqual(['answer']);
    expect(wrong).toEqual(['answer']);
  });

  it('se reproduce localmente: no se envía nada por WebSocket', async () => {
    socketSpy.mockClear();
    await answerAndRecord(true);
    // El hook de socket solo recibe el id de sesión y un manejador de mensajes entrantes.
    socketSpy.mock.calls.forEach((call) => {
      expect(call).toHaveLength(2);
      expect(typeof call[1]).toBe('function');
    });
  });
});
