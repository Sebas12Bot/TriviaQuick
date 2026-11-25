import { useEffect, useRef, useState } from 'react';
import { useReducedMotion } from '../../hooks/useReducedMotion';

interface Props {
  value: number;
  /** Cuando es true el marcador cuenta desde 0 hasta el valor. Antes muestra 0. */
  run: boolean;
  duration?: number;
  className?: string;
}

const format = new Intl.NumberFormat('es');

/** Marcador que cuenta hacia arriba. El valor viene siempre del servidor; aquí solo se anima. */
export function AnimatedScore({ value, run, duration = 1400, className }: Props) {
  const reduced = useReducedMotion();
  const [shown, setShown] = useState(reduced ? value : 0);
  const frame = useRef<number | null>(null);

  useEffect(() => {
    if (reduced || (run && duration <= 0)) {
      setShown(value);
      return;
    }
    if (!run) {
      setShown(0);
      return;
    }
    const start = performance.now();
    const step = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 4);
      setShown(Math.round(value * eased));
      if (t < 1) frame.current = requestAnimationFrame(step);
    };
    frame.current = requestAnimationFrame(step);
    return () => {
      if (frame.current !== null) cancelAnimationFrame(frame.current);
    };
  }, [value, run, duration, reduced]);

  return (
    <span className={className} data-armed={run || reduced}>
      <span aria-hidden="true" className="tabular">
        {format.format(shown)}
      </span>
      <span className="visually-hidden">{format.format(value)} puntos</span>
    </span>
  );
}
