import { useEffect, useMemo, useState, type MouseEvent as ReactMouseEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Copy, Lock, Save, Send } from 'lucide-react';
import { AppHeader } from '../components/common/AppHeader';
import { Timeline, type Selection } from '../components/quiz-editor/Timeline';
import { QuestionInspector } from '../components/quiz-editor/QuestionInspector';
import { EventInspector } from '../components/quiz-editor/EventInspector';
import { QuizSettingsPanel } from '../components/quiz-editor/QuizSettingsPanel';
import {
  emptyState, fromDetail, insertAt, moveItem, newEvent, newQuestion, publishIssues, questionNumber, removeItem, toUpsert,
  updateItem, type EditorState,
} from '../components/quiz-editor/editorModel';
import { ApiError } from '../services/api';
import { triviaService } from '../services/triviaService';
import type { TriviaStatus } from '../types/api';
import '../styles/editor.css';

/** Editor de cuestionarios del docente con línea de tiempo y panel de configuración. */
export function QuizEditorPage() {
  const { quizId } = useParams();
  const navigate = useNavigate();
  const isNew = !quizId;
  const [state, setState] = useState<EditorState | null>(isNew ? emptyState() : null);
  const [status, setStatus] = useState<TriviaStatus>('DRAFT');
  const [locked, setLocked] = useState(false);
  const [inUse, setInUse] = useState(false);
  const [selection, setSelection] = useState<Selection>({ kind: 'settings' });
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ tone: 'ok' | 'error'; text: string } | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (isNew) return;
    triviaService.get(Number(quizId))
      .then((d) => {
        setState(fromDetail(d));
        setStatus(d.status);
        setLocked(d.questionsLocked);
        setInUse(d.inUse);
      })
      .catch((e) => setLoadError(e instanceof ApiError ? e.message : 'No se pudo cargar el cuestionario.'));
  }, [isNew, quizId]);

  // Avisa antes de cerrar o recargar la pestaña con cambios sin guardar.
  useEffect(() => {
    if (!dirty) return;
    const warn = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [dirty]);

  const leave = (e: ReactMouseEvent) => {
    if (dirty && !window.confirm('Tienes cambios sin guardar. ¿Salir de todos modos?')) e.preventDefault();
  };

  const change = (fn: (s: EditorState) => EditorState) => {
    setState((s) => (s ? fn(s) : s));
    setDirty(true);
    setMessage(null);
  };

  const issues = useMemo(() => (state ? publishIssues(state) : []), [state]);

  async function save(target: TriviaStatus) {
    if (!state) return;
    if (target === 'PUBLISHED' && issues.length) {
      setMessage({ tone: 'error', text: issues[0] });
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      const payload = toUpsert(state, target);
      const saved = isNew ? await triviaService.create(payload) : await triviaService.update(Number(quizId), payload);
      setDirty(false);
      setStatus(saved.status);
      setLocked(saved.questionsLocked);
      setMessage({ tone: 'ok', text: target === 'PUBLISHED' ? 'Cuestionario publicado. Ya puedes lanzar una partida.' : 'Borrador guardado.' });
      if (isNew) navigate(`/teacher/quizzes/${saved.id}`, { replace: true });
    } catch (err) {
      setMessage({ tone: 'error', text: err instanceof ApiError ? err.message : 'No se pudo guardar.' });
    } finally {
      setSaving(false);
    }
  }

  async function duplicate() {
    try {
      const copy = await triviaService.duplicate(Number(quizId));
      navigate(`/teacher/quizzes/${copy.id}`);
    } catch (err) {
      setMessage({ tone: 'error', text: err instanceof ApiError ? err.message : 'No se pudo duplicar.' });
    }
  }

  if (loadError) {
    return (
      <div className="tq-app">
        <AppHeader />
        <main className="tq-editor-empty"><h1>No se pudo abrir el cuestionario</h1><p>{loadError}</p>
          <Link className="tq-btn tq-btn--ghost" to="/">Volver al panel</Link></main>
      </div>
    );
  }
  if (!state) {
    return <div className="tq-app"><AppHeader /><p className="tq-boot" role="status">Cargando cuestionario…</p></div>;
  }

  const selectedItem = selection.kind === 'item' ? state.items.find((i) => i.key === selection.key) : undefined;

  return (
    <div className="tq-app tq-app--editor">
      <AppHeader />
      <div className="tq-editor-bar">
        <Link to="/" className="tq-btn tq-btn--quiet" onClick={leave}><ArrowLeft size={16} aria-hidden="true" /> Panel</Link>
        <div className="tq-editor-bar__title">
          <h1>{state.meta.title.trim() || 'Cuestionario sin título'}</h1>
          <span className={`tq-status tq-status--${status.toLowerCase()}`}>{status === 'PUBLISHED' ? 'Publicado' : 'Borrador'}</span>
          {dirty && <span className="tq-editor-bar__dirty">Cambios sin guardar</span>}
        </div>
        <div className="tq-editor-bar__actions">
          {!isNew && locked && (
            <button type="button" className="tq-btn tq-btn--ghost" onClick={duplicate}><Copy size={16} aria-hidden="true" /> Duplicar para editar preguntas</button>
          )}
          <button type="button" className="tq-btn tq-btn--coral" disabled={saving || inUse} onClick={() => void save('DRAFT')}>
            <Save size={16} aria-hidden="true" /> {status === 'PUBLISHED' ? 'Pasar a borrador' : 'Guardar borrador'}
          </button>
          <button type="button" className="tq-btn tq-btn--primary" disabled={saving || inUse} onClick={() => void save('PUBLISHED')}>
            <Send size={16} aria-hidden="true" /> {saving ? 'Guardando…' : status === 'PUBLISHED' ? 'Guardar y publicar' : 'Publicar'}
          </button>
        </div>
      </div>

      {(inUse || locked) && (
        <p className="tq-banner" role="status">
          <Lock size={16} aria-hidden="true" />
          {inUse
            ? 'Hay una partida en curso con este cuestionario: no se puede editar hasta que termine.'
            : 'Este cuestionario ya se jugó: sus preguntas están bloqueadas para conservar el historial. Puedes cambiar ajustes y puntuación, o duplicarlo.'}
        </p>
      )}
      {message && <p className={`tq-banner tq-banner--${message.tone}`} role={message.tone === 'error' ? 'alert' : 'status'}>{message.text}</p>}

      <div className="tq-editor">
        <aside className="tq-editor__timeline" aria-label="Secuencia">
          <Timeline
            items={state.items}
            selection={selection}
            eventMode={state.events.mode}
            autoFrequency={state.events.autoFrequency}
            locked={locked || inUse}
            onSelect={setSelection}
            onInsertQuestion={(index) => {
              const q = newQuestion(state.scoring.defaultPoints);
              change((s) => ({ ...s, items: insertAt(s.items, index, q) }));
              setSelection({ kind: 'item', key: q.key });
            }}
            onInsertEvent={(index, type) => {
              const ev = newEvent(type);
              change((s) => ({ ...s, items: insertAt(s.items, index, ev) }));
              setSelection({ kind: 'item', key: ev.key });
            }}
            onMove={(from, to) => change((s) => ({ ...s, items: moveItem(s.items, from, to) }))}
            onRemove={(key) => {
              change((s) => ({ ...s, items: removeItem(s.items, key) }));
              if (selection.kind === 'item' && selection.key === key) setSelection({ kind: 'settings' });
            }}
          />
        </aside>

        <div className="tq-editor__inspector">
          {selection.kind === 'settings' || !selectedItem ? (
            <QuizSettingsPanel
              meta={state.meta} scoring={state.scoring} events={state.events}
              onMeta={(meta) => change((s) => ({ ...s, meta }))}
              onScoring={(scoring) => change((s) => ({ ...s, scoring }))}
              onEvents={(events) => change((s) => ({ ...s, events }))}
            />
          ) : selectedItem.kind === 'question' ? (
            <QuestionInspector
              number={questionNumber(state.items, selectedItem.key)}
              question={selectedItem.question}
              locked={locked || inUse}
              onChange={(question) => change((s) => ({
                ...s, items: updateItem(s.items, selectedItem.key, (i) => (i.kind === 'question' ? { ...i, question } : i)),
              }))}
            />
          ) : (
            <EventInspector
              event={selectedItem.event}
              defaults={state.events.abilities}
              onChange={(event) => change((s) => ({
                ...s, items: updateItem(s.items, selectedItem.key, (i) => (i.kind === 'event' ? { ...i, event } : i)),
              }))}
            />
          )}
          {issues.length > 0 && (
            <details className="tq-issues">
              <summary>{issues.length === 1 ? '1 detalle pendiente para publicar' : `${issues.length} detalles pendientes para publicar`}</summary>
              <ul>{issues.map((i) => <li key={i}>{i}</li>)}</ul>
            </details>
          )}
        </div>
      </div>
    </div>
  );
}
