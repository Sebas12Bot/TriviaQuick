interface Props {
  value: number;
}

/** Cuenta 3 · 2 · 1 a escala de marcador, con un anillo que se dibuja en cada segundo. */
export function Countdown({ value }: Props) {
  return (
    <div className="tq-countdown" role="status" aria-live="assertive">
      <svg className="tq-countdown__ring" viewBox="0 0 120 120" aria-hidden="true" key={`ring-${value}`}>
        <circle cx="60" cy="60" r="54" className="tq-countdown__track" />
        <circle cx="60" cy="60" r="54" className="tq-countdown__arc" />
      </svg>
      <span className="tq-countdown__num tabular" key={value}>
        {value}
      </span>
      <span className="visually-hidden">Revelando resultados en {value}</span>
    </div>
  );
}
