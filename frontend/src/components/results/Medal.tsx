const LABEL: Record<number, string> = { 1: 'Medalla de oro', 2: 'Medalla de plata', 3: 'Medalla de bronce' };
const TONE: Record<number, string> = { 1: 'gold', 2: 'silver', 3: 'bronze' };

/** Medalla dibujada en SVG: disco metálico con la posición grabada y su cinta. */
export function Medal({ position, size = 34 }: { position: number; size?: number }) {
  if (position < 1 || position > 3) return null;
  const tone = TONE[position];
  return (
    <svg
      className={`tq-medal tq-medal--${tone}`}
      width={size}
      height={size * 1.25}
      viewBox="0 0 32 40"
      role="img"
      aria-label={LABEL[position]}
    >
      <path d="M9 0h6l3 13h-6z" className="tq-medal__ribbon-a" />
      <path d="M17 0h6l-3 13h-6z" className="tq-medal__ribbon-b" />
      <circle cx="16" cy="25" r="13" className="tq-medal__disc" />
      <circle cx="16" cy="25" r="9.5" className="tq-medal__ring" />
      <text x="16" y="29.5" textAnchor="middle" className="tq-medal__num">
        {position}
      </text>
    </svg>
  );
}
