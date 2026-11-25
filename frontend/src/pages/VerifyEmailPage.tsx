import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { BrandMark } from '../components/common/BrandMark';
import { CobraStill } from '../components/mascot/CobraStill';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../services/api';
import { profileService } from '../services/profileService';

/** Confirma el cambio de correo con el token del enlace recibido. */
export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const { user, refreshProfile } = useAuth();
  const [state, setState] = useState<'working' | 'ok' | 'error'>('working');
  const [message, setMessage] = useState('');
  const sent = useRef(false);

  useEffect(() => {
    const token = params.get('token');
    if (!token) {
      setState('error');
      setMessage('El enlace no es válido.');
      return;
    }
    if (sent.current) return;
    sent.current = true;
    profileService.confirmEmail(token)
      .then(() => {
        setState('ok');
        if (user) void refreshProfile();
      })
      .catch((e) => {
        setState('error');
        setMessage(e instanceof ApiError ? e.message : 'No se pudo verificar el correo.');
      });
  }, [params, user, refreshProfile]);

  return (
    <main className="tq-verify">
      <BrandMark />
      <CobraStill mood={state === 'ok' ? 'celebrate' : state === 'error' ? 'error' : 'idle'} size={140} />
      {state === 'working' && <p role="status">Verificando tu nuevo correo…</p>}
      {state === 'ok' && (
        <>
          <h1>Correo confirmado</h1>
          <p>Desde ahora iniciarás sesión con tu nuevo correo.</p>
        </>
      )}
      {state === 'error' && (
        <>
          <h1>No pudimos confirmar el correo</h1>
          <p role="alert">{message}</p>
        </>
      )}
      <Link to={user ? '/profile' : '/login'} className="tq-btn tq-btn--primary">{user ? 'Ir a mi perfil' : 'Iniciar sesión'}</Link>
    </main>
  );
}
