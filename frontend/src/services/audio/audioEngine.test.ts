import { AudioEngine, DEFAULT_AUDIO, type SoundName } from './audioEngine';

function engineWithLog(settings = DEFAULT_AUDIO) {
  const engine = new AudioEngine();
  engine.configure(settings);
  const log: SoundName[] = [];
  engine.subscribe((n) => log.push(n));
  return { engine, log };
}

describe('AudioEngine', () => {
  it('no reproduce nada si el sonido está desactivado', () => {
    const { engine, log } = engineWithLog({ ...DEFAULT_AUDIO, enabled: false });
    expect(engine.play('answer')).toBe(false);
    expect(engine.play('winner')).toBe(false);
    expect(log).toEqual([]);
  });

  it('respeta la preferencia de cuenta regresiva sin silenciar el resto', () => {
    const { engine, log } = engineWithLog({ ...DEFAULT_AUDIO, countdownSounds: false });
    engine.play('tick');
    engine.play('tickUrgent');
    engine.play('shield');
    expect(log).toEqual(['shield']);
  });

  it('el silencio de partida es temporal y no cambia las preferencias', () => {
    const { engine, log } = engineWithLog();
    engine.setSessionMuted(true);
    engine.play('lightning');
    engine.setSessionMuted(false);
    engine.play('lightning');
    expect(log).toEqual(['lightning']);
  });

  it('cada habilidad tiene su propio efecto', () => {
    const { engine, log } = engineWithLog();
    (['lightning', 'multiplier', 'duel', 'shield', 'reward'] as SoundName[]).forEach((n) => engine.play(n));
    expect(new Set(log).size).toBe(5);
  });

  it('sin interacción del usuario no hay contexto de audio: no suena (y no falla)', () => {
    const { engine } = engineWithLog();
    expect(engine.play('celebration')).toBe(false);
  });
});
