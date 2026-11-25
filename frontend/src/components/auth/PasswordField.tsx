import { Eye, EyeOff } from 'lucide-react';
import type { FormEvent } from 'react';

interface Props {
  id: string;
  name: string;
  label: string;
  autoComplete: string;
  visible: boolean;
  onToggle: () => void;
  error?: string;
  hint?: string;
  minLength?: number;
  handlers: { onFocus: () => void; onBlur: () => void; onInput: (e: FormEvent<HTMLInputElement>) => void };
}

/** Campo de contraseña con botón para mostrarla (la mascota reacciona a ambos estados). */
export function PasswordField({ id, name, label, autoComplete, visible, onToggle, error, hint, minLength, handlers }: Props) {
  const describedBy = error ? `${id}-err` : hint ? `${id}-hint` : undefined;
  return (
    <div className="tq-field">
      <label htmlFor={id}>{label}</label>
      <div className="tq-field__affix">
        <input id={id} name={name} type={visible ? 'text' : 'password'} required autoComplete={autoComplete}
          minLength={minLength} aria-invalid={!!error} aria-describedby={describedBy} {...handlers} />
        <button type="button" className="tq-field__toggle" onClick={onToggle} onMouseDown={(e) => e.preventDefault()}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-pressed={visible}>
          {visible ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
        </button>
      </div>
      {error ? <p id={`${id}-err`} className="tq-field__error">{error}</p>
        : hint ? <p id={`${id}-hint`} className="tq-field__hint">{hint}</p> : null}
    </div>
  );
}
