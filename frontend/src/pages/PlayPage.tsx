import { useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { ArrowLeft, Check, X } from 'lucide-react';
import { useLiveSession } from '../hooks/useLiveSession';
import { ApiError } from '../services/api';
import { sessionService } from '../services/sessionService';
import { BrandMark } from '../components/common/BrandMark';
import { TimerBar } from '../components/game/TimerBar';
import { EventCard } from '../components/game/EventCard';
import { PowerUpTray } from '../components/game/PowerUpTray';
import type { AnswerResult } from '../types/api';
import { useAudio } from '../context/AudioContext';
import { useEventStartSound, useRewardSound } from '../hooks/useGameSounds';
import { SoundToggle } from '../components/audio/SoundToggle';
import '../styles/results.css';
import '../styles/game.css';

const format = new Intl.NumberFormat('es');

/** Pantalla del participante. Envía elecciones; muestra lo que el servidor decide. */
export function PlayPage() {
  const id = Number(useParams().sessionId);
  const { state, error, refresh } = useLiveSession(id);
  const [lastResult, setLastResult] = useState<AnswerResult | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [pending, setPending] = useState<number | null>(null);
  const { play } = useAudio();
  useEventStartSound(state?.status === 'EVENT' ? state.currentEvent : null);
  useRewardSound(state?.myPowerUps);

  const questionId = state?.currentQuestion?.id;
  useEffect(() => {
    setLastResult(null);
    setActionError(null);
    setPending(null);
  }, [questionId]);

  if (state?.status === 'FINISHED') return <Navigate to={`/sessions/${id}/results`} replace />;

  async function answer(answerId: number) {
    if (!state?.currentQuestion) return;
    // Mismo sonido para cualquier opción, antes de conocer el resultado y solo en este dispositivo.
    play('answer');
    setPending(answerId);
    setActionError(null);
    try {
      setLastResult(await sessionService.answer(id, state.currentQuestion.id, answerId));
      await refresh();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'No se pudo enviar la respuesta.');
      setPending(null);
      await refresh();
    }
  }

  async function respondEvent(optionIndex: number): Promise<string | null> {
    if (!state?.currentEvent) return null;
    play('answer');
    try {
      const r = await sessionService.respondEvent(id, state.currentEvent.id, optionIndex);
      await refresh();
      return r.message;
    } catch (err) {
      return err instanceof ApiError ? err.message : 'No se pudo enviar la respuesta.';
    }
  }

  async function activate(powerUpId: number) {
    try {
      await sessionService.activatePowerUp(id, powerUpId);
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'No se pudo activar la habilidad.');
    }
    await refresh();
  }

  const q = state?.currentQuestion;
  const answered = state?.myAnswer ?? null;
  const chosenId = answered?.answerId ?? pending;

  return (
    <div className="tq-stage">
      <nav className="tq-stage__bar" aria-label="Navegación">
        <Link to="/" className="tq-stage__back">
          <ArrowLeft aria-hidden="true" size={18} />
          <span>Salir</span>
        </Link>
        <BrandMark inverse compact />
        <SoundToggle />
        {state?.me && state.status !== 'LOBBY' && (
          <p className="tq-hud" aria-live="polite">
            <span className="tq-hud__pos">{state.me.position}.º</span>
            <span className="tq-hud__score tabular">{format.format(state.me.score)}</span>
            <span className="visually-hidden">puntos</span>
          </p>
        )}
      </nav>

      <main className="tq-play">
        {error && !state && (
          <div className="tq-stage-message">
            <h1>No se pudo abrir la partida</h1>
            <p>{error.message}</p>
            <Link to="/" className="tq-primary-btn">Volver al panel</Link>
          </div>
        )}
        {!state && !error && <p className="tq-muted-on-dark" role="status">Conectando con la partida…</p>}

        {state?.status === 'LOBBY' && (
          <div className="tq-lobby">
            <h1>{state.triviaTitle}</h1>
            <p className="tq-muted-on-dark">Ya estás dentro. La partida empezará cuando el docente la inicie.</p>
            <p className="tq-lobby__count tabular">{state.participants.length} conectados</p>
          </div>
        )}

        {state?.status === 'QUESTION' && q && (
          <section className="tq-question" aria-labelledby="q-text">
            <p className="tq-question__index tabular">
              Pregunta {q.index + 1} de {q.total}
            </p>
            <TimerBar deadline={q.deadline} serverTime={state.serverTime} totalSeconds={q.timeLimitSeconds} sound={!answered} />
            <h1 id="q-text" className="tq-question__text">{q.text}</h1>
            {q.imageUrl && <img className="tq-question__image" src={q.imageUrl} alt="Imagen de la pregunta" />}
            {q.videoUrl && <video className="tq-question__video" src={q.videoUrl} controls playsInline preload="metadata" aria-label="Video de la pregunta" />}
            <div className="tq-options">
              {q.options.map((o, i) => {
                const isChosen = chosenId === o.id;
                const verdict = isChosen && answered ? (answered.correct ? 'correct' : 'wrong') : undefined;
                return (
                  <button
                    key={o.id}
                    type="button"
                    className="tq-option"
                    data-chosen={isChosen}
                    data-verdict={verdict}
                    disabled={chosenId !== null}
                    onClick={() => void answer(o.id)}
                  >
                    <span className="tq-option__key" aria-hidden="true">{String.fromCharCode(65 + i)}</span>
                    <span className="tq-option__text">{o.text}</span>
                    {verdict === 'correct' && <Check size={20} aria-label="Correcta" />}
                    {verdict === 'wrong' && <X size={20} aria-label="Incorrecta" />}
                  </button>
                );
              })}
            </div>
            {answered && (
              <p className={`tq-feedback tq-feedback--${answered.correct ? 'ok' : 'ko'}`} role="status">
                {answered.correct
                  ? `¡Correcto! +${format.format(answered.pointsAwarded)} puntos${answered.multiplierApplied ? ' (x2)' : ''}`
                  : 'Respuesta incorrecta. ¡A por la siguiente!'}
              </p>
            )}
            {!answered && lastResult === null && pending === null && (
              <p className="tq-muted-on-dark tq-question__hint">Cuanto antes aciertes, más puntos sumas.</p>
            )}
          </section>
        )}

        {state?.status === 'EVENT' && state.currentEvent && (
          <EventCard key={state.currentEvent.id} event={state.currentEvent} serverTime={state.serverTime} onRespond={respondEvent} />
        )}

        {actionError && <p className="tq-form__error tq-form__error--dark" role="alert">{actionError}</p>}
        {state && <PowerUpTray powerUps={state.myPowerUps} onActivate={activate} />}
      </main>
    </div>
  );
}
