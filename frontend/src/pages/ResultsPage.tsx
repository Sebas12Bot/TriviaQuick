import { useCallback, useEffect } from 'react';
import { useAudio } from '../context/AudioContext';
import type { RevealPhase } from '../hooks/useRevealSequence';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, RotateCcw, SkipForward, Trophy } from 'lucide-react';
import { useResults } from '../hooks/useResults';
import { useReducedMotion } from '../hooks/useReducedMotion';
import { phaseReached, useRevealSequence } from '../hooks/useRevealSequence';
import { useSessionSocket } from '../hooks/useSessionSocket';
import { WinnerPodium } from '../components/results/WinnerPodium';
import { ParticipantRanking } from '../components/results/ParticipantRanking';
import { WinnerCelebration } from '../components/results/WinnerCelebration';
import { GameSummary } from '../components/results/GameSummary';
import { Countdown } from '../components/results/Countdown';
import { BrandMark } from '../components/common/BrandMark';
import type { GameResult } from '../types/api';
import '../styles/results.css';

/**
 * Pantalla de premiación. Solo representa los resultados oficiales que entrega el backend:
 * no ordena, no desempata y no calcula puntos.
 */
export function ResultsPage() {
  const { sessionId } = useParams();
  const id = Number(sessionId);
  const { state, retry } = useResults(id);

  // Si la partida aún no terminó, espera el aviso oficial de cierre y recarga.
  const onMessage = useCallback(
    (m: { type: string }) => {
      if (m.type === 'SESSION_FINISHED') retry();
    },
    [retry],
  );
  useSessionSocket(state.status === 'pending' ? id : null, onMessage);

  return (
    <div className="tq-stage">
      <nav className="tq-stage__bar" aria-label="Navegación">
        <Link to="/" className="tq-stage__back">
          <ArrowLeft aria-hidden="true" size={18} />
          <span>Volver al panel</span>
        </Link>
        <BrandMark inverse />
      </nav>

      {state.status === 'loading' && <ResultsSkeleton />}

      {state.status === 'pending' && (
        <StageMessage
          title="La partida sigue en juego"
          body="Los resultados aparecerán aquí automáticamente en cuanto el docente cierre la partida."
          live
        />
      )}

      {state.status === 'error' && (
        <StageMessage
          title={state.code === 'FORBIDDEN' ? 'No puedes ver estos resultados' : 'No se pudieron cargar los resultados'}
          body={state.message}
          action={state.code === 'FORBIDDEN' ? undefined : { label: 'Reintentar', onClick: retry }}
        />
      )}

      {state.status === 'ready' &&
        (state.data.ranking.length === 0 ? (
          <StageMessage
            title="Nadie participó en esta partida"
            body="La partida terminó sin participantes, así que no hay podio ni clasificación que mostrar."
          />
        ) : (
          <Ceremony result={state.data} />
        ))}
    </div>
  );
}

function Ceremony({ result }: { result: GameResult }) {
  const reduced = useReducedMotion();
  const { phase, count, skip, replay, skipped } = useRevealSequence(true, reduced);
  const winners = result.podium.places[0]?.participants ?? [];
  const sequenceRunning = phase !== 'complete';
  usePodiumSounds(phase, count, skipped);

  return (
    <main className="tq-ceremony" data-phase={phase} data-skipped={skipped}>
      <GameSummary result={result} />

      <div className="tq-ceremony__grid">
        <section className="tq-ceremony__stage" aria-label="Podio">
          {phase === 'countdown' && <Countdown value={count} />}
          {phase === 'intro' && (
            <div className="tq-ceremony__intro" aria-hidden="true">
              <Trophy size={40} strokeWidth={1.5} />
            </div>
          )}
          <WinnerPodium podium={result.podium} phase={phase} instant={skipped} />
          <WinnerCelebration active={!skipped && phaseReached(phase, 'celebrate')} />

          <p className="visually-hidden" role="status" aria-live="polite">
            {phaseReached(phase, 'winner')
              ? `${winners.length > 1 ? 'Ganadores empatados' : 'Ganador'}: ${winners.map((w) => w.displayName).join(' y ')}`
              : ''}
          </p>

          <div className="tq-ceremony__controls">
            {sequenceRunning ? (
              <button type="button" className="tq-ghost-btn" onClick={skip}>
                <SkipForward size={16} aria-hidden="true" />
                Saltar animación
              </button>
            ) : (
              !reduced && (
                <button type="button" className="tq-ghost-btn" onClick={replay}>
                  <RotateCcw size={16} aria-hidden="true" />
                  Repetir ceremonia
                </button>
              )
            )}
          </div>
        </section>

        <ParticipantRanking entries={result.ranking} tieBreakRule={result.tieBreakRule} visible={phase === 'complete'} />
      </div>
    </main>
  );
}

/**
 * Sonidos de la ceremonia: cuenta atrás, revelación discreta del 2.º y 3.º, efecto especial del
 * ganador (el momento más destacado) y celebración junto al confeti. Saltar la animación los omite.
 */
function usePodiumSounds(phase: RevealPhase, count: number, skipped: boolean) {
  const { play } = useAudio();
  useEffect(() => {
    if (skipped) return;
    if (phase === 'runners') play('podiumReveal');
    if (phase === 'winner') play('winner');
    if (phase === 'celebrate') play('celebration');
  }, [phase, skipped, play]);
  useEffect(() => {
    if (!skipped && phase === 'countdown') play('countdownBeat');
  }, [phase, count, skipped, play]);
}

function StageMessage({
  title,
  body,
  action,
  live,
}: {
  title: string;
  body: string;
  action?: { label: string; onClick: () => void };
  live?: boolean;
}) {
  return (
    <main className="tq-stage-message" role={live ? 'status' : undefined}>
      <div className="tq-stage-message__mark" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
      <h1>{title}</h1>
      <p>{body}</p>
      {action && (
        <button type="button" className="tq-primary-btn" onClick={action.onClick}>
          {action.label}
        </button>
      )}
    </main>
  );
}

function ResultsSkeleton() {
  return (
    <main className="tq-ceremony tq-ceremony--loading" aria-busy="true" aria-label="Cargando resultados">
      <div className="tq-skel tq-skel--title" />
      <div className="tq-ceremony__grid">
        <div className="tq-skel-podium" aria-hidden="true">
          <span className="tq-skel tq-skel--step2" />
          <span className="tq-skel tq-skel--step1" />
          <span className="tq-skel tq-skel--step3" />
        </div>
        <div className="tq-skel tq-skel--panel" aria-hidden="true" />
      </div>
      <p className="visually-hidden" role="status">
        Cargando resultados…
      </p>
    </main>
  );
}
