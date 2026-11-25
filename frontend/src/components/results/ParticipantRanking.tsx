import { useEffect, useRef } from 'react';
import type { ParticipantRanking as Entry } from '../../types/api';
import { RankingItem } from './RankingItem';
import { useReducedMotion } from '../../hooks/useReducedMotion';

interface Props {
  entries: Entry[];
  tieBreakRule: string;
  visible: boolean;
}

/** Panel lateral con la clasificación oficial, en el orden que entrega el servidor. */
export function ParticipantRanking({ entries, tieBreakRule, visible }: Props) {
  const listRef = useRef<HTMLOListElement>(null);
  const reduced = useReducedMotion();

  // Lleva a la vista la fila propia cuando la lista es larga.
  useEffect(() => {
    if (!visible) return;
    const self = listRef.current?.querySelector<HTMLElement>('[aria-current="true"]');
    if (self && listRef.current && self.offsetTop > listRef.current.clientHeight - 48) {
      listRef.current.scrollTo({
        top: self.offsetTop - listRef.current.clientHeight / 2,
        behavior: reduced ? 'auto' : 'smooth',
      });
    }
  }, [visible, reduced]);

  return (
    <section className="tq-ranking" data-visible={visible} aria-labelledby="ranking-title">
      <header className="tq-ranking__head">
        <h2 id="ranking-title">Clasificación de participantes</h2>
        <p className="tq-ranking__count tabular">{entries.length}</p>
      </header>
      <div className="tq-ranking__cols" aria-hidden="true">
        <span>Pos.</span>
        <span>Participante</span>
        <span title="Respuestas correctas">Aciertos</span>
        <span>Puntos</span>
      </div>
      <ol ref={listRef} className="tq-ranking__list" tabIndex={0} aria-label="Clasificación completa">
        {entries.map((entry, i) => (
          <RankingItem key={entry.participantId} entry={entry} index={i} />
        ))}
      </ol>
      <p className="tq-ranking__rule">{tieBreakRule}</p>
    </section>
  );
}
