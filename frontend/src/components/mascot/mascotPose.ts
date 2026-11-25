/**
 * Poses de Coby, la cobra mascota de TriviaQuick. Lógica pura (sin three.js) para poder probarla:
 * cada estado de ánimo define una pose objetivo y el renderizador interpola suavemente hacia ella.
 */
export type MascotMood =
  | 'idle'
  | 'greet' // saludo de entrada
  | 'watch' // mira el formulario mientras se escribe
  | 'shy' // se tapa los ojos (contraseña oculta)
  | 'peek' // espía con un ojo (contraseña visible)
  | 'error' // confusión breve
  | 'celebrate' // registro o acción completada
  | 'register'; // pose propia del registro

export interface PoseParams {
  headYaw: number; // giro a izquierda/derecha (rad)
  headPitch: number; // cabeceo (rad, + hacia abajo)
  headRoll: number; // inclinación lateral (rad)
  lean: number; // inclinación del cuerpo hacia el formulario (-1..1)
  bounce: number; // altura de salto
  hoodSpread: number; // apertura de la capucha (0.6..1.3)
  coverLeft: number; // 0 = capucha en reposo, 1 = tapa el ojo izquierdo
  coverRight: number;
  eyeOpen: number; // 0 cerrado, 1 normal, >1 sorprendido
  pupilX: number;
  pupilY: number;
  smile: number; // >0 sonrisa, <0 boca en "o"
  tailWave: number; // amplitud del saludo con la cola
}

export interface PoseContext {
  /** Segundos desde el inicio. */
  t: number;
  /** Segundos desde que cambió el estado de ánimo. */
  sinceMood: number;
  /** 0..1: progreso de lo escrito en el campo enfocado (desplaza la mirada). */
  typing: number;
}

const BASE: PoseParams = {
  headYaw: 0,
  headPitch: 0,
  headRoll: 0,
  lean: 0,
  bounce: 0,
  hoodSpread: 1,
  coverLeft: 0,
  coverRight: 0,
  eyeOpen: 1,
  pupilX: 0,
  pupilY: 0,
  smile: 1,
  tailWave: 0,
};

export function poseFor(mood: MascotMood, ctx: PoseContext): PoseParams {
  const { t, sinceMood, typing } = ctx;
  const breathe = Math.sin(t * 1.6) * 0.015;
  switch (mood) {
    case 'greet': {
      // Se eleva, ladea la cabeza al ritmo del saludo y agita la punta de la cola.
      const fade = Math.max(0, 1 - Math.max(0, sinceMood - 1.6) / 0.8);
      return {
        ...BASE,
        bounce: 0.12 * Math.sin(Math.min(sinceMood, 1) * Math.PI * 0.5) + breathe,
        headRoll: Math.sin(sinceMood * 6) * 0.14 * fade,
        headPitch: -0.08,
        hoodSpread: 1.15,
        tailWave: 0.75 * fade,
        smile: 1.25,
        eyeOpen: 1.05,
      };
    }
    case 'watch':
      return {
        ...BASE,
        headYaw: 0.42,
        headPitch: 0.12,
        lean: 0.35,
        pupilX: -0.02 + typing * 0.06,
        pupilY: -0.02,
        bounce: breathe,
        smile: 1.05,
      };
    case 'shy':
      return {
        ...BASE,
        headPitch: 0.16,
        headYaw: 0.08,
        coverLeft: 1,
        coverRight: 1,
        eyeOpen: 0.15,
        hoodSpread: 0.9,
        smile: 0.7,
        bounce: breathe,
      };
    case 'peek':
      return {
        ...BASE,
        headRoll: 0.2,
        headYaw: 0.3,
        coverLeft: 0.3,
        coverRight: 1,
        eyeOpen: 1.15,
        pupilX: 0.05,
        smile: 0.9,
        bounce: breathe,
      };
    case 'error': {
      // Sacudida corta que se amortigua: confusión, sin dramatismo.
      const decay = Math.exp(-sinceMood * 3);
      return {
        ...BASE,
        headYaw: Math.sin(sinceMood * 16) * 0.22 * decay,
        headRoll: 0.16,
        eyeOpen: 0.75,
        smile: -0.6,
        hoodSpread: 0.75,
        pupilY: 0.02,
        bounce: breathe,
      };
    }
    case 'celebrate':
      return {
        ...BASE,
        bounce: Math.abs(Math.sin(sinceMood * 5.5)) * 0.22,
        headRoll: Math.sin(sinceMood * 5.5) * 0.12,
        headPitch: -0.12,
        hoodSpread: 1.25 + Math.sin(sinceMood * 11) * 0.05,
        tailWave: 0.9,
        smile: 1.35,
        eyeOpen: 0.55, // ojos sonrientes
      };
    case 'register':
      return {
        ...BASE,
        headYaw: 0.3,
        headRoll: -0.14,
        lean: 0.5,
        hoodSpread: 1.1,
        pupilX: 0.02 + typing * 0.04,
        bounce: breathe,
        smile: 1.1,
      };
    case 'idle':
    default:
      return {
        ...BASE,
        headYaw: Math.sin(t * 0.5) * 0.12,
        headRoll: Math.sin(t * 0.37) * 0.05,
        bounce: breathe,
      };
  }
}

/** Interpolación exponencial independiente de los FPS. */
export function approach(current: PoseParams, target: PoseParams, dt: number, speed = 7): PoseParams {
  const k = 1 - Math.exp(-speed * dt);
  const out = { ...current };
  (Object.keys(target) as (keyof PoseParams)[]).forEach((key) => {
    out[key] = current[key] + (target[key] - current[key]) * k;
  });
  return out;
}

export interface AuthMascotInput {
  entering: boolean;
  field: 'email' | 'password' | 'name' | 'confirm' | 'code' | null;
  showPassword: boolean;
  status: 'idle' | 'error' | 'success';
  variant: 'login' | 'register';
}

/** Traduce el estado del formulario de acceso a un estado de ánimo de la mascota. */
export function moodForAuth({ entering, field, showPassword, status, variant }: AuthMascotInput): MascotMood {
  if (status === 'success') return 'celebrate';
  if (status === 'error') return 'error';
  if (field === 'password' || field === 'confirm') return showPassword ? 'peek' : 'shy';
  if (field) return 'watch';
  if (entering) return 'greet';
  return variant === 'register' ? 'register' : 'idle';
}

export const INITIAL_POSE: PoseParams = { ...BASE, bounce: -0.6, hoodSpread: 0.6, eyeOpen: 0.2 };
