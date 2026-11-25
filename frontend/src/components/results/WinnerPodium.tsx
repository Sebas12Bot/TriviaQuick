import type { Podium } from '../../types/api';
import { phaseReached, type RevealPhase } from '../../hooks/useRevealSequence';
import { PodiumPosition } from './PodiumPosition';

interface Props {
  podium: Podium;
  phase: RevealPhase;
  instant?: boolean;
}

/**
 * Podio 2 · 1 · 3. Los puestos llegan del servidor ya agrupados (con empates puede haber
 * varios participantes en un puesto y faltar el siguiente, p. ej. 1, 1, 3). El primer grupo
 * va al centro, el segundo a la izquierda y el tercero a la derecha.
 */
export function WinnerPodium({ podium, phase, instant = false }: Props) {
  const [first, second, third] = podium.places;
  const counting = phaseReached(phase, 'counting');
  const winnerShown = phaseReached(phase, 'winner');
  const runnersShown = phaseReached(phase, 'runners');

  if (!first) return null;

  return (
    <div className="tq-podium" data-places={podium.places.length} data-phase={phase}>
      <div className="tq-podium__beam" data-on={winnerShown} aria-hidden="true" />
      <ol className="tq-podium__row" aria-label="Podio de ganadores">
        {/* Orden en el DOM = orden de lectura (1.º, 2.º, 3.º); el CSS los coloca en 2 · 1 · 3. */}
        <PodiumPosition place={first} slot="center" revealed={winnerShown} counting={counting} instant={instant} />
        {second && <PodiumPosition place={second} slot="left" revealed={runnersShown} counting={counting} instant={instant} />}
        {third && <PodiumPosition place={third} slot="right" revealed={runnersShown} counting={counting} instant={instant} />}
      </ol>
      <div className="tq-podium__floor" aria-hidden="true" />
    </div>
  );
}
