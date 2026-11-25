import { Crown } from 'lucide-react';
import type { PodiumPlace } from '../../types/api';
import { AnimatedScore } from './AnimatedScore';
import { Avatar } from './Avatar';
import { Medal } from './Medal';

interface Props {
  place: PodiumPlace;
  /** Columna visual: left (2.º), center (1.º), right (3.º). */
  slot: 'left' | 'center' | 'right';
  revealed: boolean;
  counting: boolean;
  /** Muestra la puntuación final sin conteo (al saltar la animación). */
  instant?: boolean;
}

const TONE: Record<number, 'gold' | 'silver' | 'bronze'> = { 1: 'gold', 2: 'silver', 3: 'bronze' };
const ORDINAL: Record<number, string> = { 1: 'Primer puesto', 2: 'Segundo puesto', 3: 'Tercer puesto' };

/**
 * Un escalón del podio. La altura depende de la posición real (no del orden visual), así que
 * con empates o puestos vacíos el podio sigue siendo honesto.
 */
export function PodiumPosition({ place, slot, revealed, counting, instant = false }: Props) {
  const tone = TONE[place.position] ?? 'bronze';
  const isWinner = place.position === 1;
  const names = place.participants.map((p) => p.displayName).join(', ');
  const self = place.participants.some((p) => p.currentUser);

  return (
    <li
      className={`tq-podium__slot tq-podium__slot--${slot} tq-podium__slot--p${place.position}`}
      data-revealed={revealed}
      aria-label={`${ORDINAL[place.position]}: ${names}, ${place.score} puntos${place.participants.length > 1 ? ' (empate)' : ''}`}
    >
      <div className="tq-podium__people">
        {isWinner && (
          <Crown className="tq-podium__crown" aria-hidden="true" strokeWidth={1.75} />
        )}
        <div className="tq-podium__avatars" data-count={place.participants.length}>
          {place.participants.map((p) => (
            <Avatar
              key={p.participantId}
              initials={p.initials}
              name={p.displayName}
              size={isWinner ? 'xl' : 'lg'}
              tone={tone}
            />
          ))}
        </div>
        <p className="tq-podium__name">
          {place.participants.map((p, i) => (
            <span key={p.participantId} className={p.currentUser ? 'is-self' : undefined}>
              {i > 0 && <span className="tq-podium__amp"> · </span>}
              {p.displayName}
            </span>
          ))}
        </p>
        {place.participants.length > 1 && <p className="tq-podium__tie">Empate</p>}
        {self && <p className="tq-podium__you">Tú</p>}
        <AnimatedScore className="tq-podium__score" value={place.score} run={counting} duration={instant ? 0 : 1400} />
      </div>
      <div className={`tq-podium__step tq-podium__step--${tone}`}>
        <Medal position={place.position} size={isWinner ? 38 : 32} />
        <span className="tq-podium__rank" aria-hidden="true">
          {place.position}
        </span>
      </div>
    </li>
  );
}
