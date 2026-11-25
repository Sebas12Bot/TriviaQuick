import { useEffect, useRef } from 'react';
import { useServerCountdown } from '../../hooks/useServerCountdown';
import { useAudio } from '../../context/AudioContext';

interface Props {
  deadline: string | null | undefined;
  serverTime: string | null | undefined;
  totalSeconds: number;
  /** Reproduce un tic por segundo (más agudo en los últimos 5). */
  sound?: boolean;
}

/** Barra de tiempo sincronizada con el reloj del servidor. */
export function TimerBar({ deadline, serverTime, totalSeconds, sound = false }: Props) {
  const remaining = useServerCountdown(deadline, serverTime);
  const seconds = Math.ceil(remaining / 1000);
  const fraction = totalSeconds > 0 ? Math.min(1, remaining / (totalSeconds * 1000)) : 0;
  const urgent = seconds <= 5;
  const { play } = useAudio();
  const lastSecond = useRef<number | null>(null);

  useEffect(() => {
    if (!sound || !deadline) return;
    if (lastSecond.current !== null && seconds < lastSecond.current && seconds > 0) {
      play(seconds <= 5 ? 'tickUrgent' : 'tick');
    }
    lastSecond.current = seconds;
  }, [seconds, sound, deadline, play]);

  return (
    <div className={`tq-timer${urgent ? ' tq-timer--urgent' : ''}`}>
      <div
        className="tq-timer__track"
        role="progressbar"
        aria-label="Tiempo restante"
        aria-valuemin={0}
        aria-valuemax={totalSeconds}
        aria-valuenow={seconds}
      >
        <div className="tq-timer__fill" style={{ transform: `scaleX(${fraction})` }} />
      </div>
      <span className="tq-timer__num tabular" aria-hidden="true">
        {seconds}
      </span>
    </div>
  );
}
