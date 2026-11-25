import { useEffect, useState } from 'react';

/**
 * Milisegundos restantes hasta un plazo del servidor, corrigiendo la diferencia entre el reloj del
 * servidor y el del dispositivo. Solo es una representación: el servidor valida el plazo real.
 */
export function useServerCountdown(deadline: string | null | undefined, serverTime: string | null | undefined): number {
  const [remaining, setRemaining] = useState(0);
  useEffect(() => {
    if (!deadline) {
      setRemaining(0);
      return;
    }
    const offset = serverTime ? Date.parse(serverTime) - Date.now() : 0;
    const target = Date.parse(deadline);
    const tick = () => setRemaining(Math.max(0, target - (Date.now() + offset)));
    tick();
    const id = window.setInterval(tick, 200);
    return () => window.clearInterval(id);
  }, [deadline, serverTime]);
  return remaining;
}
