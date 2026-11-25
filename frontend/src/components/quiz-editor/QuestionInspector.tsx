import { Plus, Trash2 } from 'lucide-react';
import type { QuestionDraft, QuestionType } from '../../types/api';
import { ImagePicker } from './ImagePicker';

interface Props {
  number: number;
  question: QuestionDraft;
  locked: boolean;
  onChange: (q: QuestionDraft) => void;
}

/** Configuración de una pregunta: enunciado, tipo, opciones, respuesta correcta, puntos, tiempo e imagen. */
export function QuestionInspector({ number, question, locked, onChange }: Props) {
  const set = (patch: Partial<QuestionDraft>) => onChange({ ...question, ...patch });

  const changeType = (type: QuestionType) => {
    if (type === question.type) return;
    set({
      type,
      answers: type === 'TRUE_FALSE'
        ? [{ text: 'Verdadero', correct: true }, { text: 'Falso', correct: false }]
        : [...question.answers.map((a) => ({ ...a })), { text: '', correct: false }, { text: '', correct: false }].slice(0, 4),
    });
  };

  return (
    <section className="tq-inspector" aria-labelledby="inspector-title">
      <header className="tq-inspector__head">
        <h2 id="inspector-title">Pregunta {number}</h2>
        <div className="tq-chipset" role="radiogroup" aria-label="Tipo de pregunta">
          {(['MULTIPLE_CHOICE', 'TRUE_FALSE'] as QuestionType[]).map((t) => (
            <button key={t} type="button" role="radio" aria-checked={question.type === t} disabled={locked}
              className="tq-chip" onClick={() => changeType(t)}>
              {t === 'MULTIPLE_CHOICE' ? 'Selección múltiple' : 'Verdadero / Falso'}
            </button>
          ))}
        </div>
      </header>

      <div className="tq-field">
        <label htmlFor="q-text">Enunciado</label>
        <textarea id="q-text" rows={3} maxLength={500} value={question.text} disabled={locked}
          placeholder="Escribe la pregunta…" onChange={(e) => set({ text: e.target.value })} />
      </div>

      <fieldset className="tq-answers" disabled={locked}>
        <legend>Opciones <span className="tq-muted">· marca la correcta</span></legend>
        {question.answers.map((a, i) => (
          <div key={i} className={`tq-answer${a.correct ? ' is-correct' : ''}`}>
            <input type="radio" name="correct" id={`a-correct-${i}`} checked={a.correct}
              aria-label={`Marcar la opción ${String.fromCharCode(65 + i)} como correcta`}
              onChange={() => set({ answers: question.answers.map((x, j) => ({ ...x, correct: j === i })) })} />
            <span className="tq-answer__key" aria-hidden="true">{String.fromCharCode(65 + i)}</span>
            <input type="text" className="tq-answer__text" value={a.text} maxLength={200}
              readOnly={question.type === 'TRUE_FALSE'} aria-label={`Texto de la opción ${String.fromCharCode(65 + i)}`}
              placeholder={`Opción ${String.fromCharCode(65 + i)}`}
              onChange={(e) => set({ answers: question.answers.map((x, j) => (j === i ? { ...x, text: e.target.value } : x)) })} />
            {question.type === 'MULTIPLE_CHOICE' && question.answers.length > 2 && (
              <button type="button" className="tq-icon-btn" aria-label={`Eliminar la opción ${String.fromCharCode(65 + i)}`}
                onClick={() => {
                  const next = question.answers.filter((_, j) => j !== i);
                  if (!next.some((x) => x.correct)) next[0] = { ...next[0], correct: true };
                  set({ answers: next });
                }}>
                <Trash2 size={15} aria-hidden="true" />
              </button>
            )}
          </div>
        ))}
        {question.type === 'MULTIPLE_CHOICE' && question.answers.length < 6 && (
          <button type="button" className="tq-btn tq-btn--quiet tq-btn--small-quiet"
            onClick={() => set({ answers: [...question.answers, { text: '', correct: false }] })}>
            <Plus size={15} aria-hidden="true" /> Añadir opción
          </button>
        )}
      </fieldset>

      <div className="tq-grid-2">
        <div className="tq-field">
          <label htmlFor="q-points">Puntaje base</label>
          <input id="q-points" type="number" min={10} max={5000} step={10} value={question.basePoints} disabled={locked}
            onChange={(e) => set({ basePoints: Number(e.target.value) })} />
        </div>
        <div className="tq-field">
          <label htmlFor="q-time">Tiempo para responder (s)</label>
          <input id="q-time" type="number" min={5} max={120} value={question.timeLimitSeconds} disabled={locked}
            onChange={(e) => set({ timeLimitSeconds: Number(e.target.value) })} />
        </div>
      </div>

      <ImagePicker label="Imagen de la pregunta (opcional)" imageUrl={question.imageUrl ?? null} disabled={locked}
        onChange={(img) => set({ imageKey: img?.key ?? null, imageUrl: img?.url ?? null })} />
    </section>
  );
}
