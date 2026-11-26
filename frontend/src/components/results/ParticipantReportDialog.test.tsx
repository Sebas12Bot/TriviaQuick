import { fireEvent, render, screen, within } from '@testing-library/react';
import { vi } from 'vitest';
import { ParticipantReportDialog } from './ParticipantReportDialog';
import { ParticipantRanking } from './ParticipantRanking';
import { resultService } from '../../services/resultService';
import { row } from '../../test/fixtures';
import type { ParticipantReport } from '../../types/api';

const ana = row(1, 1, 'Ana Ruiz', 1300, { correctAnswers: 1 });

const report: ParticipantReport = {
  participantId: 1, displayName: 'Ana Ruiz', initials: 'AR', position: 1, score: 1300,
  correctAnswers: 1, wrongAnswers: 1, unanswered: 1, bestStreak: 1, averageResponseMs: 3500,
  questions: [
    { index: 0, text: '¿Capital de Francia?', type: 'MULTIPLE_CHOICE', outcome: 'ANSWERED_CORRECT', chosenAnswer: 'París',
      correctAnswer: 'París', pointsAwarded: 1200, speedBonus: 200, multiplierFactor: 1, responseTimeMs: 2000, fastest: true },
    { index: 1, text: '¿2 + 2?', type: 'MULTIPLE_CHOICE', outcome: 'ANSWERED_WRONG', chosenAnswer: '5',
      correctAnswer: '4', pointsAwarded: 0, speedBonus: 0, multiplierFactor: 1, responseTimeMs: 5000, fastest: false },
    { index: 2, text: '¿Color del cielo?', type: 'MULTIPLE_CHOICE', outcome: 'UNANSWERED', chosenAnswer: null,
      correctAnswer: 'Azul', pointsAwarded: 0, speedBonus: 0, multiplierFactor: 1, responseTimeMs: null, fastest: false },
    { index: 3, text: 'No llegó', type: 'MULTIPLE_CHOICE', outcome: 'NOT_ASKED', chosenAnswer: null,
      correctAnswer: 'X', pointsAwarded: 0, speedBonus: 0, multiplierFactor: 1, responseTimeMs: null, fastest: false },
  ],
  bonuses: [{ reason: 'FASTEST_ANSWER', delta: 100, description: 'Respuesta correcta más rápida de la pregunta 1 (2 s)' }],
};

describe('Reporte del participante', () => {
  afterEach(() => vi.restoreAllMocks());

  it('el menú ⋯ solo aparece cuando se puede abrir el reporte', () => {
    const { rerender } = render(<ParticipantRanking entries={[ana]} tieBreakRule="r" visible />);
    expect(screen.queryByRole('button', { name: 'Ver reporte de Ana Ruiz' })).toBeNull();
    const onReport = vi.fn();
    rerender(<ParticipantRanking entries={[ana]} tieBreakRule="r" visible onReport={onReport} />);
    fireEvent.click(screen.getByRole('button', { name: 'Ver reporte de Ana Ruiz' }));
    expect(onReport).toHaveBeenCalledWith(ana);
  });

  it('muestra aciertos, fallos y bonificaciones, y se cierra con Escape', async () => {
    vi.spyOn(resultService, 'report').mockResolvedValue(report);
    const onClose = vi.fn();
    render(<ParticipantReportDialog sessionId={7} participant={ana} onClose={onClose} />);

    expect(resultService.report).toHaveBeenCalledWith(7, 1, expect.any(AbortSignal));
    const dialog = screen.getByRole('dialog', { name: 'Ana Ruiz' });
    const items = await within(dialog).findAllByRole('listitem');
    const questions = items.filter((li) => li.classList.contains('tq-report__q'));
    expect(questions).toHaveLength(3); // la no planteada se omite
    expect(questions[0]).toHaveTextContent('Correcta');
    expect(questions[0]).toHaveTextContent('La más rápida');
    expect(questions[1]).toHaveTextContent('Incorrecta');
    expect(questions[1]).toHaveTextContent('Correcta: 4');
    expect(questions[2]).toHaveTextContent('No respondió');
    expect(dialog).toHaveTextContent('Respuesta correcta más rápida de la pregunta 1');

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });
});
