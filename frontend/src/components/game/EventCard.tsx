import { useState } from 'react';
import { Shield, Swords, Zap, X as Times } from 'lucide-react';
import type { GameEvent, GameEventType } from '../../types/api';
import { TimerBar } from './TimerBar';

export const EVENT_META: Record<GameEventType, { title: string; blurb: string; Icon: typeof Zap }> = {
  LIGHTNING_CHALLENGE: { title: 'Reto relámpago', blurb: 'Acierta antes de que acabe el tiempo y gana puntos extra.', Icon: Zap },
  MULTIPLIER: { title: 'Multiplicador de puntos', blurb: 'Duplica los puntos de tu próxima respuesta.', Icon: Times },
  DUEL: { title: 'Duelo de puntos', blurb: 'Dos participantes, un reto. Quien gane se lleva puntos del rival.', Icon: Swords },
  SHIELD: { title: 'Escudo protector', blurb: 'Bloquea la próxima transferencia de puntos en tu contra.', Icon: Shield },
};

interface Props {
  event: GameEvent;
  serverTime: string;
  /** Solo participantes elegibles responden; el anfitrión ve el reto sin opciones activas. */
  onRespond?: (optionIndex: number) => Promise<string | null>;
}

/** Representa un minijuego. El resultado lo decide el servidor; aquí solo se elige una opción. */
export function EventCard({ event, serverTime, onRespond }: Props) {
  const meta = EVENT_META[event.type];
  const [chosen, setChosen] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const total = Math.max(1, Math.round((Date.parse(event.expiresAt) - Date.parse(event.startedAt)) / 1000));
  const canAnswer = !!onRespond && event.eligible && !event.responded && chosen === null && event.status === 'ACTIVE';

  async function pick(i: number) {
    if (!onRespond || !canAnswer) return;
    setChosen(i);
    setBusy(true);
    setMessage(await onRespond(i));
    setBusy(false);
  }

  return (
    <section className={`tq-event tq-event--${event.type.toLowerCase()}`} aria-labelledby="event-title">
      <div className="tq-event__head">
        <span className="tq-event__icon" aria-hidden="true">
          <meta.Icon size={22} strokeWidth={2} />
        </span>
        <div>
          <h2 id="event-title">{meta.title}</h2>
          <p className="tq-event__blurb">{meta.blurb}</p>
        </div>
      </div>
      {event.status === 'ACTIVE' && <TimerBar deadline={event.expiresAt} serverTime={serverTime} totalSeconds={total} sound={canAnswer} />}

      {event.type === 'DUEL' && event.duelists.length === 2 && (
        <p className="tq-event__duel">
          <strong>{event.duelists[0]}</strong>
          <span aria-hidden="true">vs</span>
          <span className="visually-hidden">contra</span>
          <strong>{event.duelists[1]}</strong>
        </p>
      )}

      {event.challenge && (
        <div className="tq-event__challenge">
          <p className="tq-event__category">{event.challenge.category}</p>
          <p className="tq-event__prompt">{event.challenge.prompt}</p>
          <div className="tq-options tq-options--compact">
            {event.challenge.options.map((opt, i) => (
              <button
                key={i}
                type="button"
                className="tq-option"
                data-chosen={chosen === i}
                disabled={!canAnswer || busy}
                onClick={() => void pick(i)}
              >
                <span className="tq-option__key" aria-hidden="true">{String.fromCharCode(65 + i)}</span>
                {opt}
              </button>
            ))}
          </div>
        </div>
      )}

      {(message || (event.responded && event.myResponseCorrect !== null)) && (
        <p className="tq-event__result" role="status">
          {message ?? (event.myResponseCorrect ? 'Respondiste correctamente.' : 'Tu respuesta no fue correcta.')}
        </p>
      )}
      {onRespond && event.challenge && !event.eligible && event.status === 'ACTIVE' && (
        <p className="tq-muted-on-dark">Este reto no es para ti: observa cómo termina.</p>
      )}
      {event.outcome && <p className="tq-event__outcome">{event.outcome}</p>}
    </section>
  );
}
