import { useEffect, useState } from 'react';
import { CobraSilhouette } from './CobraSilhouette';
import { poseFor, type MascotMood } from './mascotPose';

const cache = new Map<MascotMood, string>();
const pending = new Map<MascotMood, Promise<string | null>>();

/** Renderiza una pose una sola vez a una imagen y libera el contexto WebGL. */
async function snapshot(mood: MascotMood): Promise<string | null> {
  const [THREE, model] = await Promise.all([import('three'), import('./cobraModel')]);
  let renderer: InstanceType<typeof THREE.WebGLRenderer>;
  try {
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
  } catch {
    return null;
  }
  try {
    renderer.setPixelRatio(1);
    renderer.setSize(360, 380, false);
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    const { scene, camera } = model.createStage(360, 380);
    const cobra = model.createCobra();
    scene.add(cobra.root);
    cobra.apply(poseFor(mood, { t: 0.4, sinceMood: 4, typing: 0 }), 0, 1);
    renderer.render(scene, camera);
    const url = renderer.domElement.toDataURL('image/png');
    cobra.dispose();
    return url;
  } finally {
    renderer.dispose();
    renderer.forceContextLoss();
  }
}

/**
 * Coby como ilustración estática (estados vacíos, carga y celebraciones fuera del acceso).
 * Es el mismo modelo 3D en una pose fija: no se mueve ni distrae.
 */
export function CobraStill({ mood = 'idle', className, size = 120 }: { mood?: MascotMood; className?: string; size?: number }) {
  const [src, setSrc] = useState<string | null>(() => cache.get(mood) ?? null);

  useEffect(() => {
    if (cache.has(mood)) {
      setSrc(cache.get(mood)!);
      return;
    }
    let alive = true;
    if (!pending.has(mood)) {
      pending.set(mood, snapshot(mood).then((url) => {
        if (url) cache.set(mood, url);
        return url;
      }).catch(() => null));
    }
    pending.get(mood)!.then((url) => alive && setSrc(url));
    return () => {
      alive = false;
    };
  }, [mood]);

  return (
    <span className={`tq-still${className ? ` ${className}` : ''}`} style={{ width: size, height: size * 1.05 }} aria-hidden="true">
      {src ? <img src={src} alt="" width={size} height={size * 1.05} /> : <CobraSilhouette />}
    </span>
  );
}
