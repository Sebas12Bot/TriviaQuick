import { EVENT_META } from '../game/EventCard';
import type { AbilitySettings, ChallengeCategory, PowerUpTarget, TimelineEventDraft } from '../../types/api';

type EventDraft = Omit<TimelineEventDraft, 'afterQuestion'>;

interface Props {
  event: EventDraft;
  defaults: AbilitySettings;
  onChange: (e: EventDraft) => void;
}

const CATEGORIES: Record<ChallengeCategory, string> = {
  ANY: 'Cualquiera', LOGIC: 'Lógica', MATH: 'Cálculo mental', MEMORY: 'Memoria', KNOWLEDGE: 'Conocimiento',
};

/** Configuración de un minijuego colocado en la línea de tiempo. Vacío = valor por defecto del cuestionario. */
export function EventInspector({ event, defaults, onChange }: Props) {
  const meta = EVENT_META[event.type];
  const set = (patch: Partial<EventDraft>) => onChange({ ...event, ...patch });
  const num = (v: string) => (v === '' ? null : Number(v));
  const defaultSeconds = event.type === 'LIGHTNING_CHALLENGE' ? defaults.lightningSeconds
    : event.type === 'DUEL' ? defaults.duelSeconds : defaults.powerUpSeconds;

  return (
    <section className="tq-inspector" aria-labelledby="inspector-title">
      <header className="tq-inspector__head">
        <span className={`tq-inspector__icon tq-inspector__icon--${event.type.toLowerCase()}`} aria-hidden="true">
          <meta.Icon size={20} />
        </span>
        <div>
          <h2 id="inspector-title">{meta.title}</h2>
          <p className="tq-muted">{meta.blurb}</p>
        </div>
      </header>

      <div className="tq-grid-2">
        <div className="tq-field">
          <label htmlFor="ev-seconds">Duración (s)</label>
          <input id="ev-seconds" type="number" min={5} max={60} placeholder={`${defaultSeconds} (por defecto)`}
            value={event.durationSeconds ?? ''} onChange={(e) => set({ durationSeconds: num(e.target.value) })} />
        </div>

        {event.type === 'LIGHTNING_CHALLENGE' && (
          <div className="tq-field">
            <label htmlFor="ev-reward">Puntos de recompensa</label>
            <input id="ev-reward" type="number" min={10} max={2000} step={10} placeholder={`${defaults.lightningReward} (por defecto)`}
              value={event.rewardPoints ?? ''} onChange={(e) => set({ rewardPoints: num(e.target.value) })} />
          </div>
        )}
        {event.type === 'MULTIPLIER' && (
          <div className="tq-field">
            <label htmlFor="ev-factor">Factor de multiplicación</label>
            <select id="ev-factor" value={event.multiplierFactor ?? ''} onChange={(e) => set({ multiplierFactor: num(e.target.value) })}>
              <option value="">x{defaults.multiplierFactor} (por defecto)</option>
              <option value="2">x2</option>
              <option value="3">x3</option>
            </select>
          </div>
        )}
        {event.type === 'DUEL' && (
          <div className="tq-field">
            <label htmlFor="ev-stake">Puntos máximos a transferir</label>
            <input id="ev-stake" type="number" min={10} max={2000} step={10} placeholder={`${defaults.duelStake} (por defecto)`}
              value={event.stake ?? ''} onChange={(e) => set({ stake: num(e.target.value) })} />
          </div>
        )}
      </div>

      {event.type === 'LIGHTNING_CHALLENGE' && (
        <div className="tq-field">
          <label htmlFor="ev-cat">Tipo de desafío</label>
          <select id="ev-cat" value={event.challengeCategory ?? ''}
            onChange={(e) => set({ challengeCategory: (e.target.value || null) as ChallengeCategory | null })}>
            <option value="">{CATEGORIES[defaults.lightningCategory]} (por defecto)</option>
            {(Object.keys(CATEGORIES) as ChallengeCategory[]).map((c) => <option key={c} value={c}>{CATEGORIES[c]}</option>)}
          </select>
        </div>
      )}
      {(event.type === 'MULTIPLIER' || event.type === 'SHIELD') && (
        <div className="tq-field">
          <label htmlFor="ev-target">¿Quién lo recibe?</label>
          <select id="ev-target" value={event.target ?? ''} onChange={(e) => set({ target: (e.target.value || null) as PowerUpTarget | null })}>
            <option value="">Por defecto del cuestionario</option>
            {event.type === 'MULTIPLIER' && <option value="BOTTOM_HALF">Mitad inferior de la clasificación (remontada)</option>}
            {event.type === 'SHIELD' && <option value="CORRECT_PREVIOUS">Quienes acertaron la pregunta anterior</option>}
            <option value="ALL">Todos los participantes</option>
          </select>
        </div>
      )}

      <p className="tq-inspector__note">
        {event.type === 'DUEL' && 'Se enfrentan los dos participantes con menos diferencia de puntos. Gana quien acierte; si aciertan ambos, el más rápido. Nadie pierde más puntos de los que tiene y un escudo activo bloquea la transferencia.'}
        {event.type === 'MULTIPLIER' && 'Quien lo activa multiplica los puntos de su siguiente respuesta. Se consume con esa respuesta.'}
        {event.type === 'SHIELD' && 'Quien lo activa bloquea la siguiente transferencia de puntos en su contra. Se consume al usarse.'}
        {event.type === 'LIGHTNING_CHALLENGE' && 'Un reto breve para todos. Solo cuenta la primera respuesta de cada participante.'}
      </p>
    </section>
  );
}
