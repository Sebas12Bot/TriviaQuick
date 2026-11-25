import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../services/api';
import { AuthLayout } from '../components/auth/AuthLayout';
import { PasswordField } from '../components/auth/PasswordField';
import { TextField } from '../components/auth/TextField';
import { useAuthMascot } from '../components/auth/useAuthMascot';

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const mascot = useAuthMascot('login');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user && mascot.status !== 'success') return <Navigate to="/" replace />;

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    setBusy(true);
    setError(null);
    try {
      await login(String(form.get('email')), String(form.get('password')));
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && !from.startsWith('/login') ? from : '/', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo iniciar sesión.');
      mascot.flagError();
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout mood={mascot.mood} typing={mascot.typing} variant="login">
      <h1>Entra a tu cuenta</h1>
      <p className="tq-auth__lead">Hola de nuevo. Tus partidas y resultados te esperan.</p>
      <nav className="tq-segmented" aria-label="Acceso">
        <span aria-current="page">Iniciar sesión</span>
        <Link to="/register">Registrarse</Link>
      </nav>
      <form onSubmit={onSubmit} className="tq-form">
        <TextField id="login-email" name="email" type="email" label="Correo" autoComplete="email" handlers={mascot.bind('email')} />
        <PasswordField id="login-password" name="password" label="Contraseña" autoComplete="current-password"
          visible={mascot.showPassword} onToggle={() => mascot.setShowPassword((v) => !v)} handlers={mascot.bind('password')} />
        {error && <p className="tq-form__error" role="alert">{error}</p>}
        <button type="submit" className="tq-btn tq-btn--primary tq-btn--block" disabled={busy}>
          {busy ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
      <p className="tq-auth__switch">
        ¿Aún no tienes cuenta? <Link to="/register">Regístrate</Link>
      </p>
    </AuthLayout>
  );
}
