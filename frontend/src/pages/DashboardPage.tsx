import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Radio, Trophy } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../services/api';
import { sessionService } from '../services/sessionService';
import { AppHeader } from '../components/common/AppHeader';
import { CobraStill } from '../components/mascot/CobraStill';
import { TeacherDashboard } from './TeacherDashboard';
import type { SessionSummary } from '../types/api';

const STATUS_LABEL: Record<SessionSummary['status'], string> = {
  LOBBY: 'En sala de espera',
  QUESTION: 'En juego',
  EVENT: 'En juego',
  FINISHED: 'Finalizada',
};

const dateFormat = new Intl.DateTimeFormat('es', { dateStyle: 'medium', timeStyle: 'short' });

export function DashboardPage() {
  const { user } = useAuth();
  if (user?.role === 'TEACHER') return <TeacherDashboard />;
  return <StudentDashboard />;
}

/** Panel del estudiante: unirse con código y sus partidas. */
function StudentDashboard() {
  const { user } = useAuth();
  const [sessions, setSessions] = useState<SessionSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    sessionService.mine().then(setSessions).catch((e: ApiError) => setError(e.message));
  }, []);

  if (!user) return null;

  return (
    <div className="tq-app">
      <AppHeader />

      <main className="tq-dashboard">
        <h1 className="tq-dashboard__title">Hola, {user.displayName.split(' ')[0]}</h1>
        <div className="tq-dashboard__grid">
          <JoinPanel />
          <section className="tq-panel" aria-labelledby="sessions-title">
            <h2 id="sessions-title">Tus partidas</h2>
            {error && <p className="tq-form__error" role="alert">{error}</p>}
            {!sessions && !error && <p className="tq-muted">Cargando partidas…</p>}
            {sessions && sessions.length === 0 && (
              <div className="tq-empty">
                <CobraStill mood="idle" size={96} />
                <p>Aún no te has unido a ninguna partida. Pide el código a tu docente.</p>
              </div>
            )}
            {sessions && sessions.length > 0 && (
              <ul className="tq-session-list">
                {sessions.map((s) => (
                  <li key={s.id} className="tq-session">
                    <div className="tq-session__info">
                      <p className="tq-session__title">{s.triviaTitle}</p>
                      <p className="tq-muted tq-session__meta">
                        <span className={`tq-pill tq-pill--${s.status.toLowerCase()}`}>{STATUS_LABEL[s.status]}</span>
                        <span>{dateFormat.format(new Date(s.createdAt))}</span>
                        <span>{s.participantCount} participantes</span>
                        {s.joinCode && <span className="tq-code">{s.joinCode}</span>}
                      </p>
                    </div>
                    {s.status === 'FINISHED' ? (
                      <Link className="tq-btn tq-btn--ghost" to={`/sessions/${s.id}/results`}>
                        <Trophy size={16} aria-hidden="true" />
                        Resultados
                      </Link>
                    ) : (
                      <Link className="tq-btn tq-btn--ghost" to={s.role === 'HOST' ? `/host/${s.id}` : `/play/${s.id}`}>
                        <Radio size={16} aria-hidden="true" />
                        {s.role === 'HOST' ? 'Controlar' : 'Entrar'}
                      </Link>
                    )}
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

function JoinPanel() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const code = String(new FormData(e.currentTarget).get('code') ?? '').trim().toUpperCase();
    setBusy(true);
    setError(null);
    try {
      const s = await sessionService.join(code);
      navigate(`/play/${s.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo unir a la partida.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="tq-panel tq-panel--feature" aria-labelledby="join-title">
      <h2 id="join-title">Unirse a una partida</h2>
      <p className="tq-muted">Escribe el código de 6 caracteres que ves en la pantalla del docente.</p>
      <form className="tq-join" onSubmit={onSubmit}>
        <label htmlFor="join-code" className="visually-hidden">Código de la partida</label>
        <input
          id="join-code"
          name="code"
          className="tq-join__input"
          inputMode="text"
          autoComplete="off"
          autoCapitalize="characters"
          spellCheck={false}
          maxLength={6}
          minLength={6}
          pattern="[A-Za-z0-9]{6}"
          required
          placeholder="ABC123"
          aria-describedby={error ? 'join-error' : undefined}
        />
        <button type="submit" className="tq-btn tq-btn--primary" disabled={busy}>
          {busy ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
      {error && <p id="join-error" className="tq-form__error" role="alert">{error}</p>}
    </section>
  );
}
