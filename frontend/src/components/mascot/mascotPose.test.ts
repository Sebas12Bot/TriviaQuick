import { approach, INITIAL_POSE, moodForAuth, poseFor, type AuthMascotInput } from './mascotPose';

const base: AuthMascotInput = { entering: false, field: null, showPassword: false, status: 'idle', variant: 'login' };
const ctx = { t: 1, sinceMood: 0.5, typing: 0 };

describe('moodForAuth (reacciones de Coby en el acceso)', () => {
  it('saluda al entrar', () => {
    expect(moodForAuth({ ...base, entering: true })).toBe('greet');
  });

  it('mira el formulario mientras se escribe el correo o el nombre', () => {
    expect(moodForAuth({ ...base, field: 'email' })).toBe('watch');
    expect(moodForAuth({ ...base, field: 'name', variant: 'register' })).toBe('watch');
  });

  it('se tapa los ojos con la contraseña oculta y espía si se muestra', () => {
    expect(moodForAuth({ ...base, field: 'password' })).toBe('shy');
    expect(moodForAuth({ ...base, field: 'confirm' })).toBe('shy');
    expect(moodForAuth({ ...base, field: 'password', showPassword: true })).toBe('peek');
  });

  it('el error y el éxito tienen prioridad sobre el campo enfocado', () => {
    expect(moodForAuth({ ...base, field: 'password', status: 'error' })).toBe('error');
    expect(moodForAuth({ ...base, field: 'email', status: 'success' })).toBe('celebrate');
  });

  it('el registro tiene una pose propia en reposo', () => {
    expect(moodForAuth({ ...base, variant: 'register' })).toBe('register');
    expect(moodForAuth(base)).toBe('idle');
  });
});

describe('poseFor', () => {
  it('shy cubre los dos ojos y casi los cierra', () => {
    const p = poseFor('shy', ctx);
    expect(p.coverLeft).toBe(1);
    expect(p.coverRight).toBe(1);
    expect(p.eyeOpen).toBeLessThan(0.3);
  });

  it('peek deja un ojo a la vista con expresión curiosa', () => {
    const p = poseFor('peek', ctx);
    expect(p.coverRight).toBe(1);
    expect(p.coverLeft).toBeLessThan(0.5);
    expect(p.eyeOpen).toBeGreaterThan(1);
  });

  it('watch gira la cabeza hacia el formulario y sigue el texto', () => {
    const a = poseFor('watch', { ...ctx, typing: 0 });
    const b = poseFor('watch', { ...ctx, typing: 1 });
    expect(a.headYaw).toBeGreaterThan(0.3);
    expect(b.pupilX).toBeGreaterThan(a.pupilX);
  });

  it('error muestra confusión (boca en o) y la sacudida se amortigua', () => {
    const early = poseFor('error', { ...ctx, sinceMood: 0.1 });
    const late = poseFor('error', { ...ctx, sinceMood: 3 });
    expect(early.smile).toBeLessThan(0);
    expect(Math.abs(late.headYaw)).toBeLessThan(0.01);
  });

  it('greet agita la cola al principio y la detiene después', () => {
    expect(poseFor('greet', { ...ctx, sinceMood: 0.5 }).tailWave).toBeGreaterThan(0.5);
    expect(poseFor('greet', { ...ctx, sinceMood: 3 }).tailWave).toBe(0);
  });

  it('approach interpola hacia el objetivo sin pasarse', () => {
    const target = poseFor('shy', ctx);
    const step = approach(INITIAL_POSE, target, 0.016);
    expect(step.coverLeft).toBeGreaterThan(0);
    expect(step.coverLeft).toBeLessThan(1);
    let p = INITIAL_POSE;
    for (let i = 0; i < 200; i++) p = approach(p, target, 0.016);
    expect(p.coverLeft).toBeCloseTo(1, 2);
  });
});
