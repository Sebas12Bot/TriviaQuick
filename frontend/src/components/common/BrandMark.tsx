/** Logotipo: tres barras de podio, la central rematada en oro. */
export function BrandMark({ inverse = false, compact = false }: { inverse?: boolean; compact?: boolean }) {
  return (
    <span className={`tq-brand${inverse ? ' tq-brand--inverse' : ''}`}>
      <svg width="28" height="28" viewBox="0 0 64 64" aria-hidden="true">
        <path d="M10 50h13V30H10zm16 0h13V14H26zm16 0h13V36H42z" className="tq-brand__bars" />
        <path d="M26 14h13v5H26z" className="tq-brand__cap" />
      </svg>
      {!compact && (
        <span className="tq-brand__word">
          Trivia<span>Quick</span>
        </span>
      )}
    </span>
  );
}
