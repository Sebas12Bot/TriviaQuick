import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { moodForAuth, type AuthMascotInput } from '../mascot/mascotPose';

type Field = AuthMascotInput['field'];

/**
 * Estado de la mascota en los formularios de acceso: campo enfocado, contraseña visible,
 * error o éxito. Expone manejadores para conectar a los inputs.
 */
export function useAuthMascot(variant: 'login' | 'register') {
  const [entering, setEntering] = useState(true);
  const [field, setField] = useState<Field>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [status, setStatus] = useState<'idle' | 'error' | 'success'>('idle');
  const [typing, setTyping] = useState(0);
  const errorTimer = useRef<number | null>(null);

  useEffect(() => {
    const id = setTimeout(() => setEntering(false), 2600);
    return () => clearTimeout(id);
  }, []);

  useEffect(() => () => {
    if (errorTimer.current) clearTimeout(errorTimer.current);
  }, []);

  const bind = useCallback(
    (name: Exclude<Field, null>) => ({
      onFocus: () => {
        setField(name);
        setEntering(false);
      },
      onBlur: () => setField((f) => (f === name ? null : f)),
      onInput: (e: FormEvent<HTMLInputElement>) => {
        setTyping(Math.min(1, e.currentTarget.value.length / 28));
        setStatus((s) => (s === 'error' ? 'idle' : s));
      },
    }),
    [],
  );

  const flagError = useCallback(() => {
    setStatus('error');
    if (errorTimer.current) clearTimeout(errorTimer.current);
    errorTimer.current = window.setTimeout(() => setStatus((s) => (s === 'error' ? 'idle' : s)), 1800);
  }, []);

  const mood = moodForAuth({ entering, field, showPassword, status, variant });
  return { mood, typing, bind, showPassword, setShowPassword, flagError, setSuccess: () => setStatus('success'), status };
}
