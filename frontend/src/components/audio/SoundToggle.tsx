import { Volume2, VolumeX } from 'lucide-react';
import { useAudio } from '../../context/AudioContext';

/** Silencia o reactiva los efectos durante la partida sin cambiar las preferencias guardadas. */
export function SoundToggle({ className }: { className?: string }) {
  const { sessionMuted, toggleSessionMute, settings } = useAudio();
  const off = sessionMuted || !settings.enabled;
  if (!settings.enabled) return null;
  return (
    <button type="button" className={`tq-sound-toggle${className ? ` ${className}` : ''}`} onClick={toggleSessionMute}
      aria-pressed={sessionMuted} aria-label={off ? 'Activar sonido de la partida' : 'Silenciar sonido de la partida'}
      title={off ? 'Activar sonido' : 'Silenciar'}>
      {off ? <VolumeX size={18} aria-hidden="true" /> : <Volume2 size={18} aria-hidden="true" />}
    </button>
  );
}
