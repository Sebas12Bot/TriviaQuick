import { Plus, Trash2 } from 'lucide-react';
import { EVENT_META } from '../game/EventCard';
import type { ChallengeCategory, EventMode, EventSettings, GameEventType, PowerUpTarget, ScoringSettings } from '../../types/api';
import { ImagePicker } from './ImagePicker';
import { ALL_EVENT_TYPES, type EditorMeta } from './editorModel';

interface Props {
  meta: EditorMeta;
  scoring: ScoringSettings;
  events: EventSettings;
  onMeta: (m: EditorMeta) => void;
  onScoring: (s: ScoringSettings) => void;
  onEvents: (e: EventSettings) => void;
}

/** Ajustes generales, sistema de puntuación y modo de minijuegos del cuestionario. */
export function QuizSettingsPanel({ meta, scoring, events, onMeta, onScoring, onEvents }: Props) {
  const ab = events.abilities;
  const setAb = (patch: Partial<typeof ab>) => onEvents({ ...events, abilities: { ...ab, ...patch } });

  return (
    <section className="tq-inspector" aria-labelledby="inspector-title">
      <header className="tq-inspector__head">
        <h2 id="inspector-title">Ajustes del cuestionario</h2>
      </header>

      <fieldset className="tq-section">
        <legend>General</legend>
        <div className="tq-field">
          <label htmlFor="qz-title">Título</label>
          <input id="qz-title" maxLength={140} value={meta.title} placeholder="Ej.: Repaso de ciencias, unidad 3"
            onChange={(e) => onMeta({ ...meta, title: e.target.value })} />
        </div>
        <div className="tq-field">
          <label htmlFor="qz-desc">Descripción</label>
          <textarea id="qz-desc" rows={2} maxLength={500} value={meta.description}
            onChange={(e) => onMeta({ ...meta, description: e.target.value })} />
        </div>
        <div className="tq-grid-2">
          <div className="tq-field">
            <label htmlFor="qz-cat">Categoría o tema</label>
            <input id="qz-cat" maxLength={60} value={meta.category} placeholder="Ciencias, Historia…"
              onChange={(e) => onMeta({ ...meta, category: e.target.value })} />
          </div>
          <div className="tq-field">
            <label htmlFor="qz-mode">Modalidad</label>
            <select id="qz-mode" value={meta.playMode} onChange={(e) => onMeta({ ...meta, playMode: e.target.value as EditorMeta['playMode'] })}>
              <option value="GROUP">Grupal en vivo</option>
              <option value="INDIVIDUAL">Individual</option>
            </select>
          </div>
        </div>
        <div className="tq-field">
          <label htmlFor="qz-minutes">Tiempo general de la actividad (min, opcional)</label>
          <input id="qz-minutes" type="number" min={1} max={240} value={meta.activityMinutes ?? ''}
            onChange={(e) => onMeta({ ...meta, activityMinutes: e.target.value === '' ? null : Number(e.target.value) })} />
          <p className="tq-field__hint">Referencia para planificar la clase; en las partidas en vivo el ritmo lo marca el docente.</p>
        </div>
        <ImagePicker label="Imagen de portada (opcional)" imageUrl={meta.coverImageUrl}
          onChange={(img) => onMeta({ ...meta, coverImageKey: img?.key ?? null, coverImageUrl: img?.url ?? null })} />
      </fieldset>

      <fieldset className="tq-section">
        <legend>Puntuación</legend>
        <div className="tq-field">
          <label htmlFor="qz-points">Puntaje predeterminado de las preguntas nuevas</label>
          <input id="qz-points" type="number" min={10} max={5000} step={10} value={scoring.defaultPoints}
            onChange={(e) => onScoring({ ...scoring, defaultPoints: Number(e.target.value) })} />
        </div>

        <label className="tq-switch">
          <input type="checkbox" checked={scoring.speedBonusEnabled} onChange={(e) => onScoring({ ...scoring, speedBonusEnabled: e.target.checked })} />
          <span>Bonificación por velocidad</span>
        </label>
        {scoring.speedBonusEnabled && (
          <TierTable
            caption="Respuestas correctas dentro del % de tiempo indicado"
            firstLabel="Hasta el % del tiempo"
            rows={scoring.speedTiers.map((t) => [t.maxElapsedPercent, t.bonusPoints])}
            firstMin={1} firstMax={100}
            onChange={(rows) => onScoring({ ...scoring, speedTiers: rows.map(([a, b]) => ({ maxElapsedPercent: a, bonusPoints: b })) })}
            newRow={[100, 10]}
            hint="La bonificación debe disminuir con el tiempo. Con el tiempo agotado no hay bonificación."
          />
        )}

        <label className="tq-switch">
          <input type="checkbox" checked={scoring.fastestBonusEnabled}
            onChange={(e) => onScoring({ ...scoring, fastestBonusEnabled: e.target.checked })} />
          <span>Premio a la respuesta correcta más rápida</span>
        </label>
        {scoring.fastestBonusEnabled && (
          <div className="tq-field">
            <label htmlFor="qz-fastest">Puntos extra para quien acierte primero cada pregunta</label>
            <input id="qz-fastest" type="number" min={1} max={2000} step={10} value={scoring.fastestBonusPoints}
              onChange={(e) => onScoring({ ...scoring, fastestBonusPoints: Number(e.target.value) })} />
            <p className="tq-field__hint">Se suma al cerrar la pregunta, así nadie sabe durante el juego quién acertó. A igual tiempo, gana quien respondió antes.</p>
          </div>
        )}

        <label className="tq-switch">
          <input type="checkbox" checked={scoring.streakEnabled} onChange={(e) => onScoring({ ...scoring, streakEnabled: e.target.checked })} />
          <span>Bonificación por racha</span>
        </label>
        {scoring.streakEnabled && (
          <>
            <TierTable
              caption="Aciertos consecutivos y puntos extra al alcanzarlos"
              firstLabel="Aciertos seguidos"
              rows={scoring.streakTiers.map((t) => [t.streakLength, t.bonusPoints])}
              firstMin={2} firstMax={50}
              onChange={(rows) => onScoring({ ...scoring, streakTiers: rows.map(([a, b]) => ({ streakLength: a, bonusPoints: b })) })}
              newRow={[4, 40]}
            />
            <label className="tq-check">
              <input type="checkbox" checked={scoring.streakBreaksOnMiss}
                onChange={(e) => onScoring({ ...scoring, streakBreaksOnMiss: e.target.checked })} />
              Una respuesta incorrecta o sin responder rompe la racha
            </label>
          </>
        )}
      </fieldset>

      <fieldset className="tq-section">
        <legend>Minijuegos</legend>
        <div className="tq-chipset" role="radiogroup" aria-label="Modo de minijuegos">
          {([['AUTO', 'Automático'], ['MANUAL', 'Manual (línea de tiempo)'], ['OFF', 'Desactivados']] as [EventMode, string][]).map(([m, label]) => (
            <button key={m} type="button" role="radio" aria-checked={events.mode === m} className="tq-chip"
              onClick={() => onEvents({ ...events, mode: m })}>{label}</button>
          ))}
        </div>
        <p className="tq-field__hint">
          {events.mode === 'MANUAL' && 'Coloca cada minijuego entre dos preguntas desde la línea de tiempo. Solo se ejecutan los que añadas.'}
          {events.mode === 'AUTO' && 'TriviaQuick elige un minijuego habilitado cada cierto número de preguntas, sin repetir el mismo dos veces seguidas.'}
          {events.mode === 'OFF' && 'La partida solo tendrá preguntas.'}
        </p>
        {events.mode === 'AUTO' && (
          <>
            <div className="tq-field">
              <label htmlFor="qz-freq">Un minijuego cada</label>
              <select id="qz-freq" value={events.autoFrequency} onChange={(e) => onEvents({ ...events, autoFrequency: Number(e.target.value) })}>
                {[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n === 1 ? '1 pregunta' : `${n} preguntas`}</option>)}
              </select>
            </div>
            <div className="tq-checks" role="group" aria-label="Minijuegos habilitados">
              {ALL_EVENT_TYPES.map((t: GameEventType) => {
                const M = EVENT_META[t];
                const on = events.autoTypes.includes(t);
                return (
                  <label key={t} className={`tq-check-card${on ? ' is-on' : ''}`}>
                    <input type="checkbox" checked={on}
                      onChange={() => onEvents({ ...events, autoTypes: on ? events.autoTypes.filter((x) => x !== t) : [...events.autoTypes, t] })} />
                    <M.Icon size={16} aria-hidden="true" /> {M.title}
                  </label>
                );
              })}
            </div>
          </>
        )}
        {events.mode !== 'OFF' && (
          <details className="tq-details">
            <summary>Valores por defecto de cada habilidad</summary>
            <div className="tq-grid-2">
              <NumField id="ab-ls" label="Reto relámpago: duración (s)" value={ab.lightningSeconds} min={5} max={60} onChange={(v) => setAb({ lightningSeconds: v })} />
              <NumField id="ab-lr" label="Reto relámpago: recompensa" value={ab.lightningReward} min={10} max={2000} step={10} onChange={(v) => setAb({ lightningReward: v })} />
              <div className="tq-field">
                <label htmlFor="ab-lc">Reto relámpago: tipo de desafío</label>
                <select id="ab-lc" value={ab.lightningCategory} onChange={(e) => setAb({ lightningCategory: e.target.value as ChallengeCategory })}>
                  <option value="ANY">Cualquiera</option><option value="LOGIC">Lógica</option><option value="MATH">Cálculo mental</option>
                  <option value="MEMORY">Memoria</option><option value="KNOWLEDGE">Conocimiento</option>
                </select>
              </div>
              <div className="tq-field">
                <label htmlFor="ab-mf">Multiplicador: factor</label>
                <select id="ab-mf" value={ab.multiplierFactor} onChange={(e) => setAb({ multiplierFactor: Number(e.target.value) })}>
                  <option value={2}>x2</option><option value={3}>x3</option>
                </select>
              </div>
              <div className="tq-field">
                <label htmlFor="ab-mt">Multiplicador: lo recibe</label>
                <select id="ab-mt" value={ab.multiplierTarget} onChange={(e) => setAb({ multiplierTarget: e.target.value as PowerUpTarget })}>
                  <option value="BOTTOM_HALF">Mitad inferior</option><option value="ALL">Todos</option>
                </select>
              </div>
              <div className="tq-field">
                <label htmlFor="ab-st">Escudo: lo recibe</label>
                <select id="ab-st" value={ab.shieldTarget} onChange={(e) => setAb({ shieldTarget: e.target.value as PowerUpTarget })}>
                  <option value="CORRECT_PREVIOUS">Quien acertó la anterior</option><option value="ALL">Todos</option>
                </select>
              </div>
              <NumField id="ab-ds" label="Duelo: duración (s)" value={ab.duelSeconds} min={5} max={60} onChange={(v) => setAb({ duelSeconds: v })} />
              <NumField id="ab-dk" label="Duelo: puntos máximos" value={ab.duelStake} min={10} max={2000} step={10} onChange={(v) => setAb({ duelStake: v })} />
              <NumField id="ab-ps" label="Habilidades: tiempo para activarlas (s)" value={ab.powerUpSeconds} min={5} max={60} onChange={(v) => setAb({ powerUpSeconds: v })} />
            </div>
          </details>
        )}
      </fieldset>
    </section>
  );
}

function NumField({ id, label, value, min, max, step = 1, onChange }: {
  id: string; label: string; value: number; min: number; max: number; step?: number; onChange: (v: number) => void;
}) {
  return (
    <div className="tq-field">
      <label htmlFor={id}>{label}</label>
      <input id={id} type="number" min={min} max={max} step={step} value={value} onChange={(e) => onChange(Number(e.target.value))} />
    </div>
  );
}

function TierTable({ caption, firstLabel, rows, firstMin, firstMax, onChange, newRow, hint }: {
  caption: string; firstLabel: string; rows: [number, number][]; firstMin: number; firstMax: number;
  onChange: (rows: [number, number][]) => void; newRow: [number, number]; hint?: string;
}) {
  return (
    <div className="tq-tiers">
      <table>
        <caption>{caption}</caption>
        <thead><tr><th scope="col">{firstLabel}</th><th scope="col">Puntos extra</th><th><span className="visually-hidden">Acciones</span></th></tr></thead>
        <tbody>
          {rows.map(([a, b], i) => (
            <tr key={i}>
              <td><input type="number" min={firstMin} max={firstMax} value={a} aria-label={`${firstLabel}, tramo ${i + 1}`}
                onChange={(e) => onChange(rows.map((r, j) => (j === i ? [Number(e.target.value), r[1]] : r)))} /></td>
              <td><input type="number" min={0} max={2000} step={5} value={b} aria-label={`Puntos extra, tramo ${i + 1}`}
                onChange={(e) => onChange(rows.map((r, j) => (j === i ? [r[0], Number(e.target.value)] : r)))} /></td>
              <td><button type="button" className="tq-icon-btn" aria-label={`Eliminar tramo ${i + 1}`}
                onClick={() => onChange(rows.filter((_, j) => j !== i))}><Trash2 size={15} aria-hidden="true" /></button></td>
            </tr>
          ))}
        </tbody>
      </table>
      {rows.length < 6 && (
        <button type="button" className="tq-btn tq-btn--quiet tq-btn--small-quiet" onClick={() => onChange([...rows, newRow])}>
          <Plus size={15} aria-hidden="true" /> Añadir tramo
        </button>
      )}
      {hint && <p className="tq-field__hint">{hint}</p>}
    </div>
  );
}
