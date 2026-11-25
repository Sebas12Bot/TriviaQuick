import type { FormEvent } from 'react';

interface Props {
  id: string;
  name: string;
  label: string;
  type?: string;
  autoComplete?: string;
  minLength?: number;
  maxLength?: number;
  error?: string;
  hint?: string;
  required?: boolean;
  defaultValue?: string;
  handlers?: { onFocus: () => void; onBlur: () => void; onInput: (e: FormEvent<HTMLInputElement>) => void };
}

export function TextField({ id, name, label, type = 'text', error, hint, required = true, handlers, ...rest }: Props) {
  const describedBy = error ? `${id}-err` : hint ? `${id}-hint` : undefined;
  return (
    <div className="tq-field">
      <label htmlFor={id}>{label}</label>
      <input id={id} name={name} type={type} required={required} aria-invalid={!!error} aria-describedby={describedBy}
        {...handlers} {...rest} />
      {error ? <p id={`${id}-err`} className="tq-field__error">{error}</p>
        : hint ? <p id={`${id}-hint`} className="tq-field__hint">{hint}</p> : null}
    </div>
  );
}
