import { useEffect, useRef, useState } from 'react';
import { Check, Minus, Timer, X, Zap } from 'lucide-react';
import { resultService } from '../../services/resultService';
import { ApiError } from '../../services/api';
import type { ParticipantRanking, ParticipantReport, QuestionOutcome } from '../../types/api';
import { Avatar } from './Avatar';

interface Props {
  sessionId: number;
  participant: ParticipantRanking;
  onClose: () => void;
}

const fmt = new Intl.NumberFormat('es');
const seconds = (ms: number | null) => (ms === null ? '—' : `${(ms / 1000).toLocaleString('es', { maximumFractionDigits: 1 })} s`);

const OUTCOME: Record<QuestionOutcome, { label: string; Icon: typeof Check }> = {
  ANSWERED_CORRECT: { label: 'Correcta', Icon: Check },
  ANSWERED_WRONG: { label: 'Incorrecta', Icon: X },
  UNANSWERED: { label: 'Sin responder', Icon: Minus },
  NOT_ASKED: { label: 'No planteada', Icon: Minus },
};

const BONUS_LABEL: Record<string, string> = {
  FASTEST_ANSWER: 'Más rápida',
  STREAK_BONUS: 'Racha',
  LIGHTNING_BONUS: 'Reto relámpago',
  DUEL_WIN: 'Duelo ganado',
  DUEL_LOSS: 'Duelo perdido',
  SHIELD_BLOCK: 'Escudo',
};

/**
 * Reporte de un participante para el docente: pregunta por pregunta, qué respondió, si acertó,
 * cuántos puntos sumó y qué bonificaciones recibió. Los datos los calcula el servidor.
 */
export function ParticipantReportDialog({ sessionId, participant, onClose }: Props) {
  const [report, setReport] = useState<ParticipantReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const opener = useRef<Element | null>(null);

  useEffect(() => {
    opener.current = document.activeElement;
    closeRef.current?.focus();
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = overflow;
      (opener.current as HTMLElement | null)?.focus?.();
    };
  }, [onClose]);

  useEffect(() => {
    const controller = new AbortController();
    resultService.report(sessionId, participant.participantId, controller.signal)
      .then(setReport)
      .catch((e) => {
        if ((e as Error).name !== 'AbortError') setError(e instanceof ApiError ? e.message : 'No se pudo cargar el reporte.');
      });
    return () => controller.abort();
  }, [sessionId, participant.participantId]);

  return (
    <div className="tq-report-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="tq-report" role="dialog" aria-modal="true" aria-labelledby="report-title">
        <header className="tq-report__head">
          <Avatar initials={participant.initials} name={participant.displayName} size="md" />
          <div className="tq-report__who">
            <h2 id="report-title">{participant.displayName}</h2>
            <p>
              {participant.position}.º puesto · <span className="tabular">{fmt.format(participant.score)}</span> puntos
            </p>
          </div>
          <button ref={closeRef} type="button" className="tq-report__close" onClick={onClose} aria-label="Cerrar reporte">
            <X size={18} aria-hidden="true" />
          </button>
        </header>

        {!report && !error && <p className="tq-report__state" role="status">Cargando reporte…</p>}
        {error && <p className="tq-report__state" role="alert">{error}</p>}

        {report && (
          <div className="tq-report__body">
            <dl className="tq-report__stats">
              <div className="is-ok"><dt>Correctas</dt><dd className="tabular">{report.correctAnswers}</dd></div>
              <div className="is-ko"><dt>Incorrectas</dt><dd className="tabular">{report.wrongAnswers}</dd></div>
              <div><dt>Sin responder</dt><dd className="tabular">{report.unanswered}</dd></div>
              <div><dt>Mejor racha</dt><dd className="tabular">{report.bestStreak}</dd></div>
              <div><dt>Tiempo medio</dt><dd className="tabular">{seconds(report.averageResponseMs)}</dd></div>
            </dl>

            <h3 className="tq-report__subtitle">Respuestas</h3>
            <ol className="tq-report__questions">
              {report.questions.filter((q) => q.outcome !== 'NOT_ASKED').map((q) => {
                const meta = OUTCOME[q.outcome];
                return (
                  <li key={q.index} className={`tq-report__q tq-report__q--${q.outcome.toLowerCase()}`}>
                    <span className="tq-report__qnum tabular" aria-hidden="true">{q.index + 1}</span>
                    <div className="tq-report__qbody">
                      <p className="tq-report__qtext">{q.text}</p>
                      <p className="tq-report__qanswer">
                        {q.chosenAnswer !== null ? <>Respondió: <strong>{q.chosenAnswer}</strong></> : 'No respondió'}
                        {q.outcome !== 'ANSWERED_CORRECT' && q.correctAnswer && <> · Correcta: <strong>{q.correctAnswer}</strong></>}
                      </p>
                      {q.outcome === 'ANSWERED_CORRECT' && (
                        <p className="tq-report__qmeta">
                          <span className="tabular">+{fmt.format(q.pointsAwarded)} pts</span>
                          {q.speedBonus > 0 && <span>incluye +{q.speedBonus} por velocidad</span>}
                          {q.multiplierFactor > 1 && <span>x{q.multiplierFactor}</span>}
                          {q.fastest && <span className="tq-report__fastest"><Zap size={13} aria-hidden="true" /> La más rápida</span>}
                        </p>
                      )}
                    </div>
                    <div className="tq-report__qside">
                      <span className="tq-report__chip"><meta.Icon size={14} aria-hidden="true" /> {meta.label}</span>
                      {q.responseTimeMs !== null && (
                        <span className="tq-report__time tabular"><Timer size={13} aria-hidden="true" /> {seconds(q.responseTimeMs)}</span>
                      )}
                    </div>
                  </li>
                );
              })}
            </ol>

            {report.bonuses.length > 0 && (
              <>
                <h3 className="tq-report__subtitle">Bonificaciones y minijuegos</h3>
                <ul className="tq-report__bonuses">
                  {report.bonuses.map((b, i) => (
                    <li key={i}>
                      <span className="tq-report__bonus-tag">{BONUS_LABEL[b.reason] ?? b.reason}</span>
                      <span className="tq-report__bonus-desc">{b.description}</span>
                      <span className={`tq-report__bonus-delta tabular${b.delta < 0 ? ' is-neg' : ''}`}>
                        {b.delta > 0 ? '+' : ''}{fmt.format(b.delta)}
                      </span>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
