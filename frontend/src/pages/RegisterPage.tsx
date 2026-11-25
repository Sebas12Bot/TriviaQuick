import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../services/api';
import { AuthLayout } from '../components/auth/AuthLayout';
import { PasswordField } from '../components/auth/PasswordField';
import { TextField } from '../components/auth/TextField';
import { useAuthMascot } from '../components/auth/useAuthMascot';
import type { Role } from '../types/api';

export function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const mascot = useAuthMascot('register');
  const [role, setRole] = useState<Role>('STUDENT');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  if (user && !done) return <Navigate to="/" replace />;

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    if (form.get('password') !== form.get('confirm')) {
      setFieldErrors({ confirm: 'Las contraseñas no coinciden.' });
      mascot.flagError();
      return;
    }
    setBusy(true);
    setError(null);
    setFieldErrors({});
    try {
      await register({
        email: String(form.get('email')),
        displayName: String(form.get('displayName')),
        password: String(form.get('password')),
        role,
        teacherCode: role === 'TEACHER' ? String(form.get('teacherCode') ?? '') : undefined,
      });
      // Celebración breve antes de entrar.
      setDone(true);
      mascot.setSuccess();
      setTimeout(() => navigate('/', { replace: true }), 1600);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
        setFieldErrors(err.fieldErrors);
      } else {
        setError('No se pudo crear la cuenta.');
      }
      mascot.flagError();
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout mood={mascot.mood} typing={mascot.typing} variant="register">
      <h1>{done ? '¡Bienvenido a TriviaQuick!' : 'Crea tu cuenta'}</h1>
      <p className="tq-auth__lead" role={done ? 'status' : undefined}>
        {done ? 'Tu cuenta está lista. Entrando…' : 'Solo te llevará un minuto.'}
      </p>
      <nav className="tq-segmented" aria-label="Acceso">
        <Link to="/login">Iniciar sesión</Link>
        <span aria-current="page">Registrarse</span>
      </nav>
      <form onSubmit={onSubmit} className="tq-form" aria-hidden={done}>
        <TextField id="reg-name" name="displayName" label="Nombre visible" autoComplete="nickname" minLength={2} maxLength={60}
          error={fieldErrors.displayName} handlers={mascot.bind('name')} />
        <TextField id="reg-email" name="email" type="email" label="Correo" autoComplete="email" error={fieldErrors.email}
          handlers={mascot.bind('email')} />
        <PasswordField id="reg-password" name="password" label="Contraseña" autoComplete="new-password" minLength={8}
          hint="Mínimo 8 caracteres." error={fieldErrors.password} visible={mascot.showPassword}
          onToggle={() => mascot.setShowPassword((v) => !v)} handlers={mascot.bind('password')} />
        <PasswordField id="reg-confirm" name="confirm" label="Confirmar contraseña" autoComplete="new-password"
          error={fieldErrors.confirm} visible={mascot.showPassword} onToggle={() => mascot.setShowPassword((v) => !v)}
          handlers={mascot.bind('confirm')} />
        <fieldset className="tq-roles">
          <legend>Soy</legend>
          <label>
            <input type="radio" name="role" checked={role === 'STUDENT'} onChange={() => setRole('STUDENT')} />
            Estudiante
          </label>
          <label>
            <input type="radio" name="role" checked={role === 'TEACHER'} onChange={() => setRole('TEACHER')} />
            Docente
          </label>
        </fieldset>
        {role === 'TEACHER' && (
          <TextField id="reg-code" name="teacherCode" label="Código de docente" autoComplete="off"
            hint="Lo proporciona la administración de tu centro." handlers={mascot.bind('code')} />
        )}
        {error && <p className="tq-form__error" role="alert">{error}</p>}
        <button type="submit" className="tq-btn tq-btn--primary tq-btn--block" disabled={busy || done}>
          {busy ? 'Creando…' : 'Crear cuenta'}
        </button>
      </form>
      <p className="tq-auth__switch">
        ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
      </p>
    </AuthLayout>
  );
}
