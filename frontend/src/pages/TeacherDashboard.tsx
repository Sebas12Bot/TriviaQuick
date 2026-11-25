import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Copy, Pencil, Play, Plus, Radio, Trash2, Trophy } from 'lucide-react';
import { AppHeader } from '../components/common/AppHeader';
import { CobraStill } from '../components/mascot/CobraStill';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../services/api';
import { sessionService } from '../services/sessionService';
import { triviaService } from '../services/triviaService';
import type { SessionSummary, TeacherDashboard as Dashboard, TriviaListItem } from '../types/api';
import '../styles/teacher.css';

const dateFormat = new Intl.DateTimeFormat('es', { dateStyle: 'medium' });
const dateTime = new Intl.DateTimeFormat('es', { dateStyle: 'medium', timeStyle: 'short' });
const fmt = new Intl.NumberFormat('es');

/** Panel docente: cifras reales del backend y accesos rápidos. */
export function TeacherDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [dash, setDash] = useState<Dashboard | null>(null);
  const [quizzes, setQuizzes] = useState<TriviaListItem[] | null>(null);
  const [live, setLive] = useState<SessionSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(() => {
    Promise.all([triviaService.dashboard(), triviaService.list(), sessionService.mine()])
      .then(([d, q, s]) => {
        setDash(d);
        setQuizzes(q);
        setLive(s.filter((x) => x.role === 'HOST' && x.status !== 'FINISHED'));
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : 'No se pudo cargar el panel.'));
  }, []);
  useEffect(load, [load]);

  async function act(id: number, fn: () => Promise<unknown>) {
    setBusyId(id);
    setError(null);
    try {
      await fn();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo completar la acción.');
    } finally {
      setBusyId(null);
    }
  }

  const launch = (q: TriviaListItem) => act(q.id, async () => {
    const s = await sessionService.create(q.id, true, 2);
    navigate(`/host/${s.id}`);
  });

  const stats = dash ? [
    { label: 'Cuestionarios', value: dash.totalQuizzes },
    { label: 'Publicados', value: dash.publishedQuizzes },
    { label: 'Borradores', value: dash.draftQuizzes, accent: dash.draftQuizzes > 0 },
    { label: 'Partidas realizadas', value: dash.finishedSessions },
  ] : [];

  return (
    <div className="tq-app">
      <AppHeader />
      <main className="tq-teacher">
        <div className="tq-teacher__head">
          <div>
            <h1>Hola, {user?.displayName.split(' ')[0]}</h1>
            <p className="tq-muted">Crea cuestionarios, lánzalos en clase y revisa cómo les fue.</p>
          </div>
          <Link to="/teacher/quizzes/new" className="tq-btn tq-btn--primary"><Plus size={18} aria-hidden="true" /> Crear cuestionario</Link>
        </div>

        {error && <p className="tq-form__error" role="alert">{error}</p>}

        <dl className="tq-stats" aria-busy={!dash}>
          {dash ? stats.map((s) => (
            <div key={s.label} className={`tq-stat${s.accent ? ' tq-stat--accent' : ''}`}>
              <dt>{s.label}</dt>
              <dd className="tabular">{fmt.format(s.value)}</dd>
            </div>
          )) : [0, 1, 2, 3].map((i) => <div key={i} className="tq-stat tq-stat--skeleton" aria-hidden="true" />)}
        </dl>

        {live.length > 0 && (
          <section className="tq-panel tq-live" aria-labelledby="live-title">
            <h2 id="live-title">Partidas en curso</h2>
            <ul className="tq-session-list">
              {live.map((s) => (
                <li key={s.id} className="tq-session">
                  <div className="tq-session__info">
                    <p className="tq-session__title">{s.triviaTitle}</p>
                    <p className="tq-muted tq-session__meta">
                      <span className="tq-pill tq-pill--question">{s.status === 'LOBBY' ? 'En sala de espera' : 'En juego'}</span>
                      <span>{s.participantCount} participantes</span>
                      {s.joinCode && <span className="tq-code">{s.joinCode}</span>}
                    </p>
                  </div>
                  <Link className="tq-btn tq-btn--ghost" to={`/host/${s.id}`}><Radio size={16} aria-hidden="true" /> Controlar</Link>
                </li>
              ))}
            </ul>
          </section>
        )}

        <div className="tq-teacher__grid">
          <section className="tq-panel" aria-labelledby="quizzes-title">
            <div className="tq-panel__head">
              <h2 id="quizzes-title">Tus cuestionarios</h2>
            </div>
            {!quizzes && !error && <p className="tq-muted">Cargando cuestionarios…</p>}
            {quizzes && quizzes.length === 0 && (
              <div className="tq-empty">
                <CobraStill mood="idle" size={110} />
                <p><strong>Aún no tienes cuestionarios.</strong> Crea el primero: preguntas, puntos y minijuegos a tu medida.</p>
                <Link to="/teacher/quizzes/new" className="tq-btn tq-btn--primary"><Plus size={16} aria-hidden="true" /> Crear cuestionario</Link>
              </div>
            )}
            {quizzes && quizzes.length > 0 && (
              <ul className="tq-quiz-list">
                {quizzes.map((q) => (
                  <li key={q.id} className="tq-quiz">
                    <div className="tq-quiz__cover" aria-hidden="true">
                      {q.coverImageUrl ? <img src={q.coverImageUrl} alt="" /> : <span>{q.title.trim().charAt(0).toUpperCase() || '?'}</span>}
                    </div>
                    <div className="tq-quiz__info">
                      <p className="tq-quiz__title">
                        <Link to={`/teacher/quizzes/${q.id}`}>{q.title}</Link>
                        <span className={`tq-status tq-status--${q.status.toLowerCase()}`}>{q.status === 'PUBLISHED' ? 'Publicado' : 'Borrador'}</span>
                      </p>
                      <p className="tq-muted tq-quiz__meta">
                        {q.category && <span>{q.category}</span>}
                        <span>{q.questionCount} {q.questionCount === 1 ? 'pregunta' : 'preguntas'}</span>
                        <span>{q.sessionsPlayed} {q.sessionsPlayed === 1 ? 'partida' : 'partidas'}</span>
                        <span>Editado {dateFormat.format(new Date(q.updatedAt))}</span>
                      </p>
                    </div>
                    <div className="tq-quiz__actions">
                      {q.status === 'PUBLISHED' && (
                        <button type="button" className="tq-btn tq-btn--primary tq-btn--sm" disabled={busyId === q.id} onClick={() => void launch(q)}>
                          <Play size={15} aria-hidden="true" /> Lanzar
                        </button>
                      )}
                      <Link to={`/teacher/quizzes/${q.id}`} className="tq-icon-btn" aria-label={`Editar ${q.title}`} title="Editar"><Pencil size={16} aria-hidden="true" /></Link>
                      <button type="button" className="tq-icon-btn" aria-label={`Duplicar ${q.title}`} title="Duplicar" disabled={busyId === q.id}
                        onClick={() => void act(q.id, async () => { const c = await triviaService.duplicate(q.id); navigate(`/teacher/quizzes/${c.id}`); })}>
                        <Copy size={16} aria-hidden="true" />
                      </button>
                      {q.sessionsPlayed === 0 && (
                        <button type="button" className="tq-icon-btn tq-icon-btn--danger" aria-label={`Eliminar ${q.title}`} title="Eliminar" disabled={busyId === q.id}
                          onClick={() => {
                            if (window.confirm(`¿Eliminar «${q.title}»? Esta acción no se puede deshacer.`)) {
                              void act(q.id, async () => { await triviaService.remove(q.id); load(); });
                            }
                          }}>
                          <Trash2 size={16} aria-hidden="true" />
                        </button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="tq-panel" aria-labelledby="recent-title">
            <h2 id="recent-title">Resultados recientes</h2>
            {dash && dash.recentResults.length === 0 && <p className="tq-muted">Cuando termines una partida, sus resultados aparecerán aquí.</p>}
            {dash && dash.recentResults.length > 0 && (
              <ul className="tq-recent">
                {dash.recentResults.map((r) => (
                  <li key={r.sessionId}>
                    <Link to={`/sessions/${r.sessionId}/results`} className="tq-recent__row">
                      <Trophy size={18} aria-hidden="true" className="tq-recent__icon" />
                      <span className="tq-recent__text">
                        <strong>{r.triviaTitle}</strong>
                        <small>
                          {dateTime.format(new Date(r.finishedAt))} · {r.participantCount} participantes
                          {r.winners.length > 0 && ` · Ganó ${r.winners.join(' y ')}`}
                        </small>
                      </span>
                      {r.topScore !== null && <span className="tq-recent__score tabular">{fmt.format(r.topScore)}</span>}
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
