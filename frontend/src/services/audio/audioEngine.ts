/**
 * Motor de audio de TriviaQuick. Los efectos se sintetizan con Web Audio (sin archivos externos,
 * sin licencias, sin descargas): cortos, discretos y con volumen bajo por defecto.
 *
 * Reglas:
 * - Nada suena hasta que el usuario interactúa (restricción de reproducción automática).
 * - Si el sonido está desactivado, play() no hace nada.
 * - El sonido de respuesta es UNO SOLO ('answer'): no recibe ningún parámetro sobre si fue
 *   correcta, así que no puede revelar el resultado. Se reproduce solo en este dispositivo.
 */
export type SoundName =
  | 'tick'
  | 'tickUrgent'
  | 'answer'
  | 'lightning'
  | 'multiplier'
  | 'duel'
  | 'shield'
  | 'reward'
  | 'countdownBeat'
  | 'podiumReveal'
  | 'winner'
  | 'celebration'
  | 'ui';

export interface AudioSettings {
  enabled: boolean;
  masterVolume: number; // 0..100
  effectsVolume: number; // 0..100
  countdownSounds: boolean;
}

export const DEFAULT_AUDIO: AudioSettings = { enabled: true, masterVolume: 70, effectsVolume: 80, countdownSounds: true };

type Listener = (name: SoundName) => void;

export class AudioEngine {
  private ctx: AudioContext | null = null;
  private out: GainNode | null = null;
  private settings: AudioSettings = { ...DEFAULT_AUDIO };
  private sessionMuted = false;
  private listeners = new Set<Listener>();
  private lastAt = new Map<SoundName, number>();

  configure(settings: AudioSettings) {
    this.settings = { ...settings };
    this.applyVolume();
  }

  /** Silencio temporal durante una partida (no cambia las preferencias guardadas). */
  setSessionMuted(muted: boolean) {
    this.sessionMuted = muted;
  }

  isSessionMuted() {
    return this.sessionMuted;
  }

  /** Debe llamarse desde un gesto del usuario (clic, tecla, toque). */
  unlock() {
    const Ctor = typeof window !== 'undefined'
      ? (window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext)
      : undefined;
    if (!Ctor) return;
    if (!this.ctx) {
      this.ctx = new Ctor();
      this.out = this.ctx.createGain();
      this.out.connect(this.ctx.destination);
      this.applyVolume();
    }
    if (this.ctx.state === 'suspended') void this.ctx.resume();
  }

  /** Permite observar qué se reproduce (útil para pruebas y depuración). */
  subscribe(listener: Listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  /** Reproduce un efecto. Devuelve false si no sonó (desactivado, silenciado o sin desbloquear). */
  play(name: SoundName): boolean {
    if (!this.settings.enabled || this.sessionMuted) return false;
    if ((name === 'tick' || name === 'tickUrgent') && !this.settings.countdownSounds) return false;
    const now = Date.now();
    if (now - (this.lastAt.get(name) ?? 0) < 60) return false; // evita ráfagas duplicadas
    this.lastAt.set(name, now);
    this.listeners.forEach((l) => l(name));
    if (!this.ctx || !this.out || this.ctx.state !== 'running') return false;
    SYNTHS[name](this.ctx, this.out, this.ctx.currentTime);
    return true;
  }

  private applyVolume() {
    if (!this.out || !this.ctx) return;
    const v = (this.settings.masterVolume / 100) * (this.settings.effectsVolume / 100);
    // Curva perceptual y techo bajo: los efectos nunca son estridentes.
    this.out.gain.setTargetAtTime(Math.pow(v, 1.6) * 0.55, this.ctx.currentTime, 0.02);
  }
}

// ------------------------------------------------------------------ síntesis

function tone(ctx: AudioContext, out: AudioNode, t: number, freq: number, dur: number, opts: {
  type?: OscillatorType; gain?: number; attack?: number; to?: number; detune?: number;
} = {}) {
  const osc = ctx.createOscillator();
  const g = ctx.createGain();
  osc.type = opts.type ?? 'sine';
  osc.frequency.setValueAtTime(freq, t);
  if (opts.to) osc.frequency.exponentialRampToValueAtTime(opts.to, t + dur);
  if (opts.detune) osc.detune.value = opts.detune;
  const peak = opts.gain ?? 0.2;
  const attack = opts.attack ?? 0.005;
  g.gain.setValueAtTime(0.0001, t);
  g.gain.exponentialRampToValueAtTime(peak, t + attack);
  g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
  osc.connect(g).connect(out);
  osc.start(t);
  osc.stop(t + dur + 0.02);
}

function noise(ctx: AudioContext, out: AudioNode, t: number, dur: number, gain: number, freq: number) {
  const buffer = ctx.createBuffer(1, Math.max(1, Math.floor(ctx.sampleRate * dur)), ctx.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < data.length; i++) data[i] = (Math.random() * 2 - 1) * (1 - i / data.length);
  const src = ctx.createBufferSource();
  src.buffer = buffer;
  const filter = ctx.createBiquadFilter();
  filter.type = 'bandpass';
  filter.frequency.value = freq;
  const g = ctx.createGain();
  g.gain.value = gain;
  src.connect(filter).connect(g).connect(out);
  src.start(t);
}

const SYNTHS: Record<SoundName, (ctx: AudioContext, out: AudioNode, t: number) => void> = {
  tick: (c, o, t) => tone(c, o, t, 880, 0.05, { gain: 0.09 }),
  tickUrgent: (c, o, t) => tone(c, o, t, 1175, 0.07, { type: 'triangle', gain: 0.13 }),
  // Selección neutra: idéntica para cualquier respuesta.
  answer: (c, o, t) => {
    tone(c, o, t, 520, 0.08, { type: 'triangle', gain: 0.16 });
    tone(c, o, t + 0.06, 700, 0.1, { type: 'sine', gain: 0.12 });
  },
  lightning: (c, o, t) => {
    tone(c, o, t, 420, 0.14, { type: 'sawtooth', gain: 0.07, to: 1800 });
    noise(c, o, t, 0.12, 0.12, 3200);
    tone(c, o, t + 0.12, 1568, 0.12, { gain: 0.12 });
  },
  multiplier: (c, o, t) => [523, 659, 784, 1047].forEach((f, i) => tone(c, o, t + i * 0.07, f, 0.16, { type: 'triangle', gain: 0.13 })),
  duel: (c, o, t) => {
    noise(c, o, t, 0.1, 0.25, 900);
    tone(c, o, t, 196, 0.22, { type: 'square', gain: 0.06, to: 150 });
    noise(c, o, t + 0.16, 0.1, 0.22, 1200);
    tone(c, o, t + 0.16, 247, 0.24, { type: 'square', gain: 0.06, to: 185 });
  },
  shield: (c, o, t) => {
    tone(c, o, t, 660, 0.55, { gain: 0.08, attack: 0.18, detune: -8 });
    tone(c, o, t, 990, 0.55, { gain: 0.06, attack: 0.2, detune: 7 });
    tone(c, o, t + 0.05, 1320, 0.4, { gain: 0.03, attack: 0.15 });
  },
  reward: (c, o, t) => {
    tone(c, o, t, 880, 0.12, { gain: 0.12 });
    tone(c, o, t + 0.09, 1319, 0.22, { gain: 0.12 });
  },
  countdownBeat: (c, o, t) => {
    tone(c, o, t, 220, 0.16, { gain: 0.22, to: 160 });
    noise(c, o, t, 0.05, 0.08, 400);
  },
  podiumReveal: (c, o, t) => {
    noise(c, o, t, 0.35, 0.08, 1800);
    tone(c, o, t + 0.18, 587, 0.3, { type: 'triangle', gain: 0.12 });
  },
  // Momento sonoro principal: acorde ascendente que se sostiene.
  winner: (c, o, t) => {
    noise(c, o, t, 0.45, 0.1, 2400);
    [523, 659, 784].forEach((f, i) => tone(c, o, t + 0.12 + i * 0.09, f, 0.9, { type: 'triangle', gain: 0.12, attack: 0.02 }));
    tone(c, o, t + 0.45, 1047, 1.1, { type: 'sine', gain: 0.14, attack: 0.04 });
    tone(c, o, t + 0.45, 1568, 0.9, { type: 'sine', gain: 0.05, attack: 0.08 });
  },
  celebration: (c, o, t) => {
    for (let i = 0; i < 9; i++) {
      tone(c, o, t + i * 0.09 + Math.random() * 0.04, 1200 + Math.random() * 1400, 0.12, { gain: 0.05 });
    }
  },
  ui: (c, o, t) => tone(c, o, t, 660, 0.04, { gain: 0.06 }),
};

/** Instancia única compartida por toda la aplicación. */
export const audioEngine = new AudioEngine();
