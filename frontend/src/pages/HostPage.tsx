import { useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { ArrowLeft, Flag, Play, SkipForward } from 'lucide-react';
import { useLiveSession } from '../hooks/useLiveSession';
import { ApiError } from '../services/api';
import { sessionService } from '../services/sessionService';
import { rankingService } from '../services/rankingService';
import { BrandMark } from '../components/common/BrandMark';
import { TimerBar } from '../components/game/TimerBar';
import { EventCard } from '../components/game/EventCard';
import { Avatar } from '../components/results/Avatar';
import type { Ranking } from '../types/api';
import { useEventStartSound } from '../hooks/useGameSounds';
import { SoundToggle } from '../components/audio/SoundToggle';
import '../styles/results.css';
import '../styles/game.css';

const format = new Intl.NumberFormat('es');

/** Consola del docente: muestra el estado y envía órdenes de control; no calcula nada. */
export function HostPage() {
  const id = Number(useParams().sessionId);
  const { state, error, setState } = useLiveSession(id);
  const [ranking, setRanking] = useState<Ranking | null>(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  useEventStartSound(state?.status === 'EVENT' ? state.currentEvent : null);

  // Cada nuevo estado (avisado por el servidor) refresca la clasificación provisional oficial.
  useEffect(() => {
    if (!state || state.status === 'LOBBY') return;
    rankingService.ranking(id).then(setRanking).catch(() => undefined);
  }, [id, state]);

  if (state?.status === 'FINISHED') return <Navigate to={`/sessions/${id}/results`} replace />;

  async function run(action: () => Promise<typeof state>) {
    setBusy(true);
    setActionError(null);
    try {
      const next = await action();
      if (next) setState(next);
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'No se pudo completar la acción.');
    } finally {
      setBusy(false);
    }
  }

  const q = state?.currentQuestion;
  const advanceLabel =
    state?.status === 'EVENT' ? (state.lastQuestion ? 'Ver resultados' : 'Siguiente pregunta')
      : state?.lastQuestion ? 'Terminar y ver resultados' : 'Continuar';

  return (
    <div className="tq-stage">
      <nav className="tq-stage__bar" aria-label="Navegación">
        <Link to="/" className="tq-stage__back">
          <ArrowLeft aria-hidden="true" size={18} />
          <span>Panel</span>
        </Link>
        <div className="tq-stage__tools">
          <SoundToggle />
          <BrandMark inverse />
        </div>
      </nav>

      <main className="tq-host">
        {error && !state && (
          <div className="tq-stage-message">
            <h1>No se pudo abrir la partida</h1>
            <p>{error.message}</p>
          </div>
        )}
        {state && (
          <>
            <header className="tq-host__head">
              <div>
                <h1>{state.triviaTitle}</h1>
                <p className="tq-muted-on-dark">
                  {state.status === 'LOBBY' ? 'Sala de espera' : q ? `Pregunta ${q.index + 1} de ${q.total}` : 'Minijuego en curso'}
                </p>
              </div>
              {state.joinCode && (
                <div className="tq-host__code" aria-label={`Código de acceso ${state.joinCode.split('').join(' ')}`}>
                  <span className="tq-host__code-label">Código</span>
                  <span className="tq-host__code-value tabular">{state.joinCode}</span>
                </div>
              )}
            </header>

            <div className="tq-host__grid">
              <section className="tq-host__main">
                {state.status === 'LOBBY' && (
                  <div className="tq-lobby tq-lobby--host">
                    <p className="tq-lobby__count tabular">{state.participants.length} participantes conectados</p>
                    <ul className="tq-lobby__people">
                      {state.participants.map((p) => (
                        <li key={p.participantId}>
                          <Avatar initials={p.initials} name={p.displayName} size="md" />
                          <span>{p.displayName}</span>
                        </li>
                      ))}
                    </ul>
                    {state.participants.length === 0 && (
                      <p className="tq-muted-on-dark">Comparte el código para que los estudiantes se unan.</p>
                    )}
                  </div>
                )}
                {state.status === 'QUESTION' && q && (
                  <div className="tq-question tq-question--host">
                    <TimerBar deadline={q.deadline} serverTime={state.serverTime} totalSeconds={q.timeLimitSeconds} sound />
                    <h2 className="tq-question__text">{q.text}</h2>
                    {q.imageUrl && <img className="tq-question__image" src={q.imageUrl} alt="Imagen de la pregunta" />}
                    {q.videoUrl && <video className="tq-question__video" src={q.videoUrl} controls playsInline preload="metadata" aria-label="Video de la pregunta" />}
                    <ol className="tq-options tq-options--static">
                      {q.options.map((o, i) => (
                        <li key={o.id} className="tq-option">
                          <span className="tq-option__key" aria-hidden="true">{String.fromCharCode(65 + i)}</span>
                          {o.text}
                        </li>
                      ))}
                    </ol>
                    <p className="tq-host__answers tabular" aria-live="polite">
                      {state.answersReceived ?? 0} de {state.participants.length} han respondido
                    </p>
                  </div>
                )}
                {state.status === 'EVENT' && state.currentEvent && (
                  <EventCard key={state.currentEvent.id} event={state.currentEvent} serverTime={state.serverTime} />
                )}

                <div className="tq-host__actions">
                  {state.status === 'LOBBY' ? (
                    <button type="button" className="tq-primary-btn" disabled={busy || state.participants.length === 0}
                      onClick={() => void run(() => sessionService.start(id))}>
                      <Play size={18} aria-hidden="true" /> Iniciar partida
                    </button>
                  ) : (
                    <button type="button" className="tq-primary-btn" disabled={busy}
                      onClick={() => void run(() => sessionService.advance(id))}>
                      <SkipForward size={18} aria-hidden="true" /> {advanceLabel}
                    </button>
                  )}
                  {state.status !== 'LOBBY' && (
                    <button type="button" className="tq-ghost-btn" disabled={busy}
                      onClick={() => {
                        if (window.confirm('¿Cerrar la partida ahora? Las puntuaciones quedarán fijadas.')) {
                          void run(() => sessionService.finish(id));
                        }
                      }}>
                      <Flag size={16} aria-hidden="true" /> Cerrar partida
                    </button>
                  )}
                </div>
                {actionError && <p className="tq-form__error tq-form__error--dark" role="alert">{actionError}</p>}
              </section>

              {ranking && ranking.entries.length > 0 && (
                <aside className="tq-ranking tq-ranking--live" data-visible="true" aria-labelledby="live-ranking">
                  <header className="tq-ranking__head">
                    <h2 id="live-ranking">Clasificación provisional</h2>
                  </header>
                  <ol className="tq-ranking__list">
                    {ranking.entries.map((e) => (
                      <li key={e.participantId} className="tq-rank-item tq-rank-item--compact" style={{ ['--i' as string]: 0 }}>
                        <span className="tq-rank-item__pos tabular">{e.position}</span>
                        <Avatar initials={e.initials} name={e.displayName} size="sm" />
                        <span className="tq-rank-item__name">{e.displayName}</span>
                        <span className="tq-rank-item__score tabular">{format.format(e.score)}</span>
                      </li>
                    ))}
                  </ol>
                </aside>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
