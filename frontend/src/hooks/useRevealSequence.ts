import { useCallback, useEffect, useState } from 'react';

/**
 * Secuencia de la ceremonia:
 * intro → countdown (3, 2, 1) → runners (2.º y 3.º) → winner (1.º) → celebrate (confeti)
 * → counting (marcadores) → complete (clasificación completa).
 * Con movimiento reducido se salta directamente al estado final.
 */
export type RevealPhase = 'intro' | 'countdown' | 'runners' | 'winner' | 'celebrate' | 'counting' | 'complete';

const ORDER: RevealPhase[] = ['intro', 'countdown', 'runners', 'winner', 'celebrate', 'counting', 'complete'];

const DURATIONS: Record<Exclude<RevealPhase, 'complete'>, number> = {
  intro: 700,
  countdown: 2700, // 3 × 900 ms
  runners: 1100,
  winner: 1200,
  celebrate: 500,
  counting: 1500,
};

export function phaseReached(current: RevealPhase, target: RevealPhase): boolean {
  return ORDER.indexOf(current) >= ORDER.indexOf(target);
}

export function useRevealSequence(enabled: boolean, reducedMotion: boolean) {
  const [phase, setPhase] = useState<RevealPhase>(reducedMotion ? 'complete' : 'intro');
  const [count, setCount] = useState(3);

  useEffect(() => {
    if (reducedMotion) setPhase('complete');
  }, [reducedMotion]);

  useEffect(() => {
    if (!enabled || phase === 'complete') return;
    const timer = setTimeout(() => {
      setPhase((p) => ORDER[Math.min(ORDER.indexOf(p) + 1, ORDER.length - 1)]);
    }, DURATIONS[phase]);
    return () => clearTimeout(timer);
  }, [enabled, phase]);

  useEffect(() => {
    if (phase !== 'countdown') return;
    setCount(3);
    const a = setTimeout(() => setCount(2), 900);
    const b = setTimeout(() => setCount(1), 1800);
    return () => {
      clearTimeout(a);
      clearTimeout(b);
    };
  }, [phase]);

  // Saltar es instantáneo: la vista no reproduce las entradas restantes.
  const [skipped, setSkipped] = useState(false);
  const skip = useCallback(() => {
    setSkipped(true);
    setPhase('complete');
  }, []);
  const replay = useCallback(() => {
    setSkipped(false);
    setPhase(reducedMotion ? 'complete' : 'intro');
  }, [reducedMotion]);

  return { phase, count, skip, replay, skipped };
}
