import { useEffect, useRef } from 'react';
import { useAudio } from '../context/AudioContext';
import type { SoundName } from '../services/audio/audioEngine';
import type { GameEventType, PowerUp } from '../types/api';

export const EVENT_SOUND: Record<GameEventType, SoundName> = {
  LIGHTNING_CHALLENGE: 'lightning',
  MULTIPLIER: 'multiplier',
  DUEL: 'duel',
  SHIELD: 'shield',
};

/** Efecto propio de cada minijuego cuando empieza (una vez por evento). */
export function useEventStartSound(event: { id: number; type: GameEventType } | null | undefined) {
  const { play } = useAudio();
  const lastId = useRef<number | null>(null);
  useEffect(() => {
    if (!event || event.id === lastId.current) return;
    lastId.current = event.id;
    play(EVENT_SOUND[event.type]);
  }, [event, play]);
}

/** Sonido de logro cuando el jugador recibe una habilidad nueva (solo en su dispositivo). */
export function useRewardSound(powerUps: PowerUp[] | undefined) {
  const { play } = useAudio();
  const known = useRef<Set<number> | null>(null);
  useEffect(() => {
    if (!powerUps) return;
    const ids = new Set(powerUps.map((p) => p.id));
    if (known.current && powerUps.some((p) => !known.current!.has(p.id))) play('reward');
    known.current = ids;
  }, [powerUps, play]);
}
