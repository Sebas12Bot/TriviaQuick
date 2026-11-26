import type { CSSProperties } from 'react';
import { MoreHorizontal } from 'lucide-react';
import type { ParticipantRanking } from '../../types/api';
import { Avatar } from './Avatar';

const TONE: Record<number, 'gold' | 'silver' | 'bronze'> = { 1: 'gold', 2: 'silver', 3: 'bronze' };
const format = new Intl.NumberFormat('es');

interface Props {
  entry: ParticipantRanking;
  index: number;
  /** Solo el docente: abre el reporte del participante. */
  onReport?: (entry: ParticipantRanking) => void;
}

export function RankingItem({ entry, index, onReport }: Props) {
  const tone = TONE[entry.position];
  return (
    <li
      className={`tq-rank-item${tone ? ` tq-rank-item--${tone}` : ''}${entry.currentUser ? ' tq-rank-item--self' : ''}${onReport ? ' tq-rank-item--actions' : ''}`}
      style={{ '--i': Math.min(index, 14) } as CSSProperties}
      aria-current={entry.currentUser ? 'true' : undefined}
    >
      <span className="tq-rank-item__pos tabular" aria-label={`Posición ${entry.position}${entry.tied ? ', empatado' : ''}`}>
        {entry.position}
        {entry.tied && <span className="tq-rank-item__eq" aria-hidden="true">=</span>}
      </span>
      <Avatar initials={entry.initials} name={entry.displayName} size="sm" tone={entry.currentUser ? 'self' : tone ?? 'plain'} />
      <span className="tq-rank-item__name">
        {entry.displayName}
        {entry.currentUser && <span className="tq-rank-item__you">Tú</span>}
      </span>
      <span className="tq-rank-item__correct tabular" title="Respuestas correctas">
        {entry.correctAnswers}
        <span className="visually-hidden"> respuestas correctas</span>
      </span>
      <span className="tq-rank-item__score tabular">
        {format.format(entry.score)}
        <span className="visually-hidden"> puntos</span>
      </span>
      {onReport && (
        <button type="button" className="tq-rank-item__more" onClick={() => onReport(entry)}
          aria-label={`Ver reporte de ${entry.displayName}`} title="Ver reporte">
          <MoreHorizontal size={18} aria-hidden="true" />
        </button>
      )}
    </li>
  );
}
