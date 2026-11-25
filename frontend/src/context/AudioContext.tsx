import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { audioEngine, DEFAULT_AUDIO, type AudioSettings, type SoundName } from '../services/audio/audioEngine';
import { profileService } from '../services/profileService';
import { useAuth } from './AuthContext';

interface AudioContextValue {
  settings: AudioSettings;
  /** Guarda las preferencias en el perfil del usuario. */
  save: (next: AudioSettings) => Promise<void>;
  play: (name: SoundName) => void;
  sessionMuted: boolean;
  toggleSessionMute: () => void;
}

const Ctx = createContext<AudioContextValue | null>(null);

/**
 * Preferencias y reproducción de audio para toda la app. Las preferencias vienen del perfil
 * (persisten por usuario en el backend) y el motor se desbloquea con la primera interacción.
 */
export function AudioProvider({ children }: { children: ReactNode }) {
  const { profile, setProfile } = useAuth();
  const [settings, setSettings] = useState<AudioSettings>(DEFAULT_AUDIO);
  const [sessionMuted, setSessionMuted] = useState(audioEngine.isSessionMuted());

  useEffect(() => {
    const p = profile?.preferences;
    const next = p
      ? { enabled: p.soundEnabled, masterVolume: p.masterVolume, effectsVolume: p.effectsVolume, countdownSounds: p.countdownSounds }
      : DEFAULT_AUDIO;
    setSettings(next);
    audioEngine.configure(next);
  }, [profile?.preferences]);

  useEffect(() => {
    const unlock = () => audioEngine.unlock();
    window.addEventListener('pointerdown', unlock, { passive: true });
    window.addEventListener('keydown', unlock);
    return () => {
      window.removeEventListener('pointerdown', unlock);
      window.removeEventListener('keydown', unlock);
    };
  }, []);

  const save = useCallback(async (next: AudioSettings) => {
    setSettings(next);
    audioEngine.configure(next);
    const saved = await profileService.updatePreferences({
      soundEnabled: next.enabled,
      masterVolume: next.masterVolume,
      effectsVolume: next.effectsVolume,
      countdownSounds: next.countdownSounds,
    });
    setProfile((prev) => (prev ? { ...prev, preferences: saved } : prev));
  }, [setProfile]);

  const play = useCallback((name: SoundName) => {
    audioEngine.play(name);
  }, []);

  const toggleSessionMute = useCallback(() => {
    const next = !audioEngine.isSessionMuted();
    audioEngine.setSessionMuted(next);
    setSessionMuted(next);
  }, []);

  const value = useMemo(() => ({ settings, save, play, sessionMuted, toggleSessionMute }),
    [settings, save, play, sessionMuted, toggleSessionMute]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAudio(): AudioContextValue {
  const ctx = useContext(Ctx);
  if (!ctx) {
    // Fuera del proveedor (pruebas aisladas): sin sonido.
    return {
      settings: DEFAULT_AUDIO,
      save: async () => undefined,
      play: (name: SoundName) => {
        audioEngine.play(name);
      },
      sessionMuted: false,
      toggleSessionMute: () => undefined,
    };
  }
  return ctx;
}
