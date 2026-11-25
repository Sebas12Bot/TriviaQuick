import { useEffect, useRef, useState } from 'react';
import { useReducedMotion } from '../../hooks/useReducedMotion';
import { CobraSilhouette } from './CobraSilhouette';
import { approach, INITIAL_POSE, poseFor, type MascotMood, type PoseParams } from './mascotPose';

interface Props {
  mood: MascotMood;
  /** 0..1: cuánto se ha escrito en el campo enfocado (Coby sigue el texto con la mirada). */
  typing?: number;
  className?: string;
}

type Status = 'loading' | 'ready' | 'unsupported';

/**
 * Coby animada en 3D (three.js). El modelo se carga en un fragmento aparte después de pintar el
 * formulario, de modo que nunca retrasa el inicio de sesión. Se pausa fuera de pantalla y con la
 * pestaña oculta; con movimiento reducido muestra cada pose sin animar.
 */
export function CobraMascot({ mood, typing = 0, className }: Props) {
  const hostRef = useRef<HTMLDivElement>(null);
  const moodRef = useRef(mood);
  const typingRef = useRef(typing);
  const moodChangedAt = useRef(0);
  const redrawRef = useRef<() => void>(() => undefined);
  const reduced = useReducedMotion();
  const [status, setStatus] = useState<Status>('loading');

  useEffect(() => {
    moodRef.current = mood;
    moodChangedAt.current = performance.now();
    redrawRef.current();
  }, [mood]);

  useEffect(() => {
    typingRef.current = typing;
  }, [typing]);

  useEffect(() => {
    const host = hostRef.current;
    if (!host) return;
    let disposed = false;
    let cleanup: () => void = () => undefined;

    (async () => {
      const [THREE, model] = await Promise.all([import('three'), import('./cobraModel')]);
      if (disposed) return;
      let renderer: InstanceType<typeof THREE.WebGLRenderer>;
      try {
        renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'low-power' });
      } catch {
        setStatus('unsupported');
        return;
      }
      renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
      renderer.toneMapping = THREE.ACESFilmicToneMapping;
      renderer.toneMappingExposure = 1.05;
      renderer.outputColorSpace = THREE.SRGBColorSpace;
      renderer.domElement.setAttribute('aria-hidden', 'true');
      renderer.domElement.className = 'tq-mascot__canvas';
      host.appendChild(renderer.domElement);

      const { scene, camera } = model.createStage(1, 1);
      const cobra = model.createCobra();
      scene.add(cobra.root);

      const resize = () => {
        const w = Math.max(1, host.clientWidth);
        const h = Math.max(1, host.clientHeight);
        renderer.setSize(w, h, false);
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
        render(performance.now());
      };

      let pose: PoseParams = reduced ? poseFor(moodRef.current, { t: 0, sinceMood: 5, typing: 0 }) : { ...INITIAL_POSE };
      let last = performance.now();
      const start = last;
      let nextBlink = start + 2500;
      let blinkUntil = 0;
      let visible = true;
      let raf = 0;

      function render(now: number) {
        const t = (now - start) / 1000;
        const sinceMood = (now - moodChangedAt.current) / 1000;
        const target = poseFor(moodRef.current, { t: reduced ? 0 : t, sinceMood: reduced ? 5 : sinceMood, typing: typingRef.current });
        const dt = Math.min(0.05, (now - last) / 1000);
        last = now;
        pose = reduced ? target : approach(pose, target, dt, moodRef.current === 'shy' ? 10 : 7);
        if (!reduced && now > nextBlink) {
          blinkUntil = now + 130;
          nextBlink = now + 2600 + Math.random() * 2600;
        }
        cobra.apply(pose, reduced ? 0 : t, now < blinkUntil ? 0.08 : 1);
        renderer.render(scene, camera);
      }

      const loop = (now: number) => {
        render(now);
        if (visible && !document.hidden) raf = requestAnimationFrame(loop);
        else raf = 0;
      };
      const wake = () => {
        if (!reduced && !raf && visible && !document.hidden) {
          last = performance.now();
          raf = requestAnimationFrame(loop);
        }
      };

      const ro = new ResizeObserver(resize);
      ro.observe(host);
      const io = new IntersectionObserver((entries) => {
        visible = entries.some((e) => e.isIntersecting);
        wake();
      });
      io.observe(host);
      document.addEventListener('visibilitychange', wake);
      redrawRef.current = () => (reduced ? render(performance.now()) : wake());

      resize();
      setStatus('ready');
      if (!reduced) raf = requestAnimationFrame(loop);

      cleanup = () => {
        cancelAnimationFrame(raf);
        ro.disconnect();
        io.disconnect();
        document.removeEventListener('visibilitychange', wake);
        redrawRef.current = () => undefined;
        cobra.dispose();
        renderer.dispose();
        renderer.forceContextLoss();
        renderer.domElement.remove();
      };
    })().catch(() => setStatus('unsupported'));

    return () => {
      disposed = true;
      cleanup();
    };
  }, [reduced]);

  return (
    <div ref={hostRef} className={`tq-mascot${className ? ` ${className}` : ''}`} data-status={status} data-mood={mood}>
      {status !== 'ready' && <CobraSilhouette className="tq-mascot__fallback" />}
    </div>
  );
}
