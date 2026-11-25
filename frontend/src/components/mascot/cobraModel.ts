import * as THREE from 'three';
import type { PoseParams } from './mascotPose';

/**
 * Coby, la cobra de TriviaQuick, modelada por código (sin texturas externas): cuerpo enroscado de
 * sección variable, cabeza redondeada, ojos grandes y una capucha de dos aletas con interior coral
 * que puede taparle los ojos como si fueran patitas.
 */

const COLORS = {
  body: new THREE.Color('#38b6dd'),
  bodyDeep: new THREE.Color('#2a93c4'),
  belly: new THREE.Color('#ffc9b8'),
  coral: new THREE.Color('#ff806f'),
  coralSoft: new THREE.Color('#fff1ec'),
  eyeWhite: new THREE.Color('#ffffff'),
  pupil: new THREE.Color('#071a3b'),
  mouth: new THREE.Color('#164e83'),
};

export interface CobraRig {
  root: THREE.Group;
  apply(pose: PoseParams, t: number, blink: number): void;
  dispose(): void;
}

/** Tubo de radio variable a lo largo de una curva, con color por vértice (vientre claro y anillos). */
function taperedTube(curve: THREE.Curve<THREE.Vector3>, segments: number, radial: number,
                     radiusAt: (u: number) => number): THREE.BufferGeometry {
  const frames = curve.computeFrenetFrames(segments, false);
  const positions: number[] = [];
  const normals: number[] = [];
  const colors: number[] = [];
  const indices: number[] = [];
  const p = new THREE.Vector3();
  const n = new THREE.Vector3();
  const c = new THREE.Color();
  for (let i = 0; i <= segments; i++) {
    const u = i / segments;
    curve.getPointAt(u, p);
    const N = frames.normals[i];
    const B = frames.binormals[i];
    const r = radiusAt(u);
    for (let j = 0; j <= radial; j++) {
      const v = (j / radial) * Math.PI * 2;
      n.copy(N).multiplyScalar(Math.cos(v)).addScaledVector(B, Math.sin(v)).normalize();
      positions.push(p.x + n.x * r, p.y + n.y * r, p.z + n.z * r);
      normals.push(n.x, n.y, n.z);
      // Vientre: la cara que mira al frente en el tramo que se eleva.
      const bellyAmount = THREE.MathUtils.smoothstep(n.z, 0.6, 0.9) * THREE.MathUtils.smoothstep(p.y, 0.3, 0.7) * 0.85;
      c.copy(COLORS.body).lerp(COLORS.bodyDeep, THREE.MathUtils.smoothstep(-n.y, 0.2, 1) * 0.6);
      c.lerp(COLORS.belly, bellyAmount);
      // Anillos de escamas muy suaves en el vientre.
      if (bellyAmount > 0.25 && i % 6 < 1) c.lerp(COLORS.coral, 0.18);
      colors.push(c.r, c.g, c.b);
    }
  }
  for (let i = 0; i < segments; i++) {
    for (let j = 0; j < radial; j++) {
      const a = i * (radial + 1) + j;
      const b = (i + 1) * (radial + 1) + j;
      // Orden antihorario visto desde fuera: las caras exteriores son las visibles.
      indices.push(a, a + 1, b, b, a + 1, b + 1);
    }
  }
  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
  geo.setAttribute('normal', new THREE.Float32BufferAttribute(normals, 3));
  geo.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
  geo.setIndex(indices);
  return geo;
}

function hoodShape(): THREE.Shape {
  const s = new THREE.Shape();
  s.moveTo(0, 0.5);
  s.bezierCurveTo(0.32, 0.72, 0.84, 0.46, 0.82, 0.02);
  s.bezierCurveTo(0.8, -0.44, 0.46, -0.82, 0.04, -0.95);
  s.lineTo(0, -0.95);
  s.lineTo(0, 0.5);
  return s;
}

export function createCobra(): CobraRig {
  const disposables: { dispose(): void }[] = [];
  const track = <T extends { dispose(): void }>(x: T) => {
    disposables.push(x);
    return x;
  };

  const skin = track(new THREE.MeshPhysicalMaterial({
    vertexColors: true, roughness: 0.38, clearcoat: 0.7, clearcoatRoughness: 0.28, sheen: 0.3, sheenColor: COLORS.coralSoft,
  }));
  const headSkin = track(new THREE.MeshPhysicalMaterial({
    color: COLORS.body, roughness: 0.36, clearcoat: 0.75, clearcoatRoughness: 0.25,
  }));
  const coralMat = track(new THREE.MeshPhysicalMaterial({
    color: COLORS.coral, roughness: 0.45, clearcoat: 0.4, side: THREE.DoubleSide,
  }));
  const coralSoftMat = track(new THREE.MeshStandardMaterial({ color: COLORS.coralSoft, roughness: 0.5, side: THREE.DoubleSide }));
  const hoodOuter = track(new THREE.MeshPhysicalMaterial({
    color: COLORS.body, roughness: 0.38, clearcoat: 0.6, side: THREE.DoubleSide,
  }));
  const eyeMat = track(new THREE.MeshPhysicalMaterial({ color: COLORS.eyeWhite, roughness: 0.12, clearcoat: 1 }));
  const pupilMat = track(new THREE.MeshPhysicalMaterial({ color: COLORS.pupil, roughness: 0.08, clearcoat: 1 }));
  const shineMat = track(new THREE.MeshBasicMaterial({ color: 0xffffff }));
  const mouthMat = track(new THREE.MeshStandardMaterial({ color: COLORS.mouth, roughness: 0.6 }));

  const root = new THREE.Group();
  const bodyGroup = new THREE.Group();
  root.add(bodyGroup);

  // ---------------------------------------------------------------- cuerpo enroscado + cuello
  const path = new THREE.CatmullRomCurve3([
    new THREE.Vector3(1.05, 0.08, 0.35), // punta de la cola
    new THREE.Vector3(0.75, 0.12, -0.45),
    new THREE.Vector3(-0.2, 0.16, -0.75),
    new THREE.Vector3(-0.9, 0.18, -0.2),
    new THREE.Vector3(-0.7, 0.2, 0.55),
    new THREE.Vector3(0.05, 0.24, 0.8),
    new THREE.Vector3(0.62, 0.32, 0.35),
    new THREE.Vector3(0.42, 0.85, 0.02),
    new THREE.Vector3(0.12, 1.4, 0.02),
    new THREE.Vector3(0.0, 1.82, 0.1), // base de la cabeza
  ], false, 'catmullrom', 0.5);
  const radiusAt = (u: number) => {
    const tail = THREE.MathUtils.smoothstep(u, 0, 0.32);
    const neck = 1 - THREE.MathUtils.smoothstep(u, 0.72, 1) * 0.12;
    return 0.035 + 0.28 * tail * neck;
  };
  const body = new THREE.Mesh(track(taperedTube(path, 220, 28, radiusAt)), skin);
  bodyGroup.add(body);

  // Punta de la cola: rizo independiente que saluda.
  const tailPivot = new THREE.Group();
  tailPivot.position.copy(path.getPointAt(0));
  const tailCurve = new THREE.CatmullRomCurve3([
    new THREE.Vector3(0, 0, 0), new THREE.Vector3(0.08, 0.14, 0.02), new THREE.Vector3(0.05, 0.3, 0.04),
    new THREE.Vector3(-0.04, 0.4, 0.05),
  ]);
  const tailTip = new THREE.Mesh(track(taperedTube(tailCurve, 30, 12, (u) => 0.036 * (1 - u * 0.75))), skin);
  tailPivot.add(tailTip);
  bodyGroup.add(tailPivot);

  // ---------------------------------------------------------------- cabeza
  const headPivot = new THREE.Group();
  headPivot.position.copy(path.getPointAt(1));
  bodyGroup.add(headPivot);

  const head = new THREE.Group();
  head.position.set(0, 0.3, 0.08);
  head.scale.setScalar(1.32); // cabeza grande y amable: proporciones de personaje
  headPivot.add(head);

  const skull = new THREE.Mesh(track(new THREE.SphereGeometry(0.42, 48, 32)), headSkin);
  skull.scale.set(1.12, 0.94, 1.0);
  head.add(skull);
  const snout = new THREE.Mesh(track(new THREE.SphereGeometry(0.3, 40, 28)), headSkin);
  snout.position.set(0, -0.1, 0.2);
  snout.scale.set(1.12, 0.8, 1.0);
  head.add(snout);
  // Mejillas coral muy sutiles.
  const cheekGeo = track(new THREE.SphereGeometry(0.07, 20, 12));
  const cheekMat = track(new THREE.MeshStandardMaterial({ color: COLORS.coral, transparent: true, opacity: 0.55, roughness: 0.8 }));
  [-1, 1].forEach((side) => {
    const cheek = new THREE.Mesh(cheekGeo, cheekMat);
    cheek.position.set(side * 0.27, -0.12, 0.33);
    cheek.scale.set(1, 0.55, 0.4);
    head.add(cheek);
  });

  // Ojos grandes y amables.
  const eyes = new THREE.Group();
  eyes.position.set(0, 0.06, 0.3);
  head.add(eyes);
  const eyeGeo = track(new THREE.SphereGeometry(0.15, 32, 24));
  const pupilGeo = track(new THREE.SphereGeometry(0.088, 28, 20));
  const shineGeo = track(new THREE.SphereGeometry(0.026, 12, 8));
  const pupils: THREE.Group[] = [];
  const eyeballs: THREE.Group[] = [];
  [-1, 1].forEach((side) => {
    const eyeball = new THREE.Group();
    eyeball.position.set(side * 0.175, 0, 0);
    const white = new THREE.Mesh(eyeGeo, eyeMat);
    white.scale.set(0.95, 1.08, 0.8);
    eyeball.add(white);
    const pupil = new THREE.Group();
    const iris = new THREE.Mesh(pupilGeo, pupilMat);
    iris.scale.set(0.92, 1.1, 0.6);
    pupil.add(iris);
    const shine = new THREE.Mesh(shineGeo, shineMat);
    shine.position.set(0.03, 0.04, 0.05);
    pupil.add(shine);
    pupil.position.set(0, 0, 0.085);
    eyeball.add(pupil);
    eyes.add(eyeball);
    pupils.push(pupil);
    eyeballs.push(eyeball);
  });

  // Boca: un arco que sonríe o se cierra en una "o" de sorpresa.
  const smileMesh = new THREE.Mesh(track(new THREE.TorusGeometry(0.075, 0.016, 8, 24, Math.PI)), mouthMat);
  smileMesh.rotation.z = Math.PI;
  smileMesh.position.set(0, -0.15, 0.47);
  head.add(smileMesh);
  const oMouth = new THREE.Mesh(track(new THREE.TorusGeometry(0.032, 0.013, 8, 20)), mouthMat);
  oMouth.position.set(0, -0.17, 0.465);
  head.add(oMouth);

  // ---------------------------------------------------------------- capucha (dos aletas)
  const shape = hoodShape();
  const outerGeo = track(new THREE.ExtrudeGeometry(shape, {
    depth: 0.04, bevelEnabled: true, bevelThickness: 0.025, bevelSize: 0.03, bevelSegments: 4, curveSegments: 24,
  }));
  const innerGeo = track(new THREE.ShapeGeometry(shape, 24));
  const spotShape = new THREE.Shape();
  spotShape.absellipse(0, 0, 0.12, 0.2, 0, Math.PI * 2, false, 0);
  const spotGeo = track(new THREE.ShapeGeometry(spotShape, 24));

  interface Flap { pivot: THREE.Group; side: number; rest: THREE.Vector3; }
  const flaps: Flap[] = [];
  [-1, 1].forEach((side) => {
    const pivot = new THREE.Group();
    // La capucha nace detrás de la cabeza y se abre hacia los lados enmarcando cabeza y cuello.
    const rest = new THREE.Vector3(side * 0.12, -0.38, -0.22);
    pivot.position.copy(rest);
    const flap = new THREE.Group();
    flap.scale.x = side; // la aleta izquierda es el reflejo de la derecha
    const outer = new THREE.Mesh(outerGeo, hoodOuter);
    outer.position.z = -0.07;
    flap.add(outer);
    const inner = new THREE.Mesh(innerGeo, coralMat);
    inner.scale.set(0.84, 0.86, 1);
    inner.position.set(0.06, -0.04, 0.012);
    flap.add(inner);
    const spot = new THREE.Mesh(spotGeo, coralSoftMat);
    spot.position.set(0.42, -0.18, 0.016);
    spot.rotation.z = -0.35;
    flap.add(spot);
    pivot.add(flap);
    head.add(pivot);
    flaps.push({ pivot, side, rest });
  });

  // ---------------------------------------------------------------- sombra de contacto
  const shadowCanvas = document.createElement('canvas');
  shadowCanvas.width = shadowCanvas.height = 128;
  const sctx = shadowCanvas.getContext('2d');
  if (sctx) {
    const g = sctx.createRadialGradient(64, 64, 4, 64, 64, 62);
    g.addColorStop(0, 'rgba(7,26,59,0.45)');
    g.addColorStop(1, 'rgba(7,26,59,0)');
    sctx.fillStyle = g;
    sctx.fillRect(0, 0, 128, 128);
  }
  const shadowTex = track(new THREE.CanvasTexture(shadowCanvas));
  const shadow = new THREE.Mesh(track(new THREE.PlaneGeometry(2.8, 2.2)),
    track(new THREE.MeshBasicMaterial({ map: shadowTex, transparent: true, depthWrite: false })));
  shadow.rotation.x = -Math.PI / 2;
  shadow.position.set(0.05, 0.0, 0.05);
  root.add(shadow);

  const easeOut = (x: number) => 1 - Math.pow(1 - Math.min(1, Math.max(0, x)), 3);

  function apply(pose: PoseParams, t: number, blink: number) {
    bodyGroup.position.y = pose.bounce;
    bodyGroup.rotation.z = -pose.lean * 0.12;
    bodyGroup.scale.y = 1 + Math.sin(t * 1.6) * 0.008;
    shadow.scale.setScalar(1 - Math.max(0, pose.bounce) * 0.6);

    headPivot.rotation.set(pose.headPitch, pose.headYaw, -pose.headRoll + pose.lean * 0.1, 'YXZ');
    tailPivot.rotation.z = Math.sin(t * 9) * pose.tailWave * 0.55;
    tailPivot.rotation.x = Math.sin(t * 4.5) * pose.tailWave * 0.2;

    const open = Math.max(0.05, pose.eyeOpen * blink);
    eyeballs.forEach((e) => (e.scale.y = Math.min(1.15, open)));
    pupils.forEach((p) => {
      p.position.x = pose.pupilX;
      p.position.y = pose.pupilY;
    });

    const smile = pose.smile;
    smileMesh.visible = smile > 0;
    smileMesh.scale.set(0.85 + smile * 0.25, 0.7 + smile * 0.35, 1);
    oMouth.visible = smile <= 0;
    oMouth.scale.setScalar(0.8 + Math.min(1, -smile) * 0.5);

    flaps.forEach(({ pivot, side, rest }) => {
      const cover = side < 0 ? pose.coverLeft : pose.coverRight;
      // Primero avanza (z) y después se desliza hacia el ojo: nunca atraviesa la cabeza.
      const cz = easeOut(cover * 1.5);
      const cxy = easeOut(cover);
      const spread = pose.hoodSpread;
      pivot.position.set(
        // Cubierta: la aleta (que crece hacia fuera desde su pivote) queda justo delante de su ojo.
        THREE.MathUtils.lerp(rest.x, side * 0.02, cxy),
        THREE.MathUtils.lerp(rest.y, 0.2, cxy),
        THREE.MathUtils.lerp(rest.z, 0.5, cz),
      );
      const s = THREE.MathUtils.lerp((0.78 + spread * 0.22) * 0.72, 0.4, cxy);
      pivot.scale.set(s, THREE.MathUtils.lerp((0.82 + spread * 0.18) * 0.68, 0.42, cxy), 1);
      pivot.rotation.set(0, THREE.MathUtils.lerp(side * -0.32 * spread, 0, cxy), THREE.MathUtils.lerp(side * -0.12, side * 0.22, cxy));
    });
  }

  return {
    root,
    apply,
    dispose: () => disposables.forEach((d) => d.dispose()),
  };
}

/** Escena con iluminación suave de personaje de videojuego. */
export function createStage(width: number, height: number) {
  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(28, width / height, 0.1, 50);
  camera.position.set(0, 1.55, 6.3);
  camera.lookAt(0, 1.3, 0);

  scene.add(new THREE.HemisphereLight(0xd6ecff, 0x0b2a55, 1.1));
  const key = new THREE.DirectionalLight(0xffffff, 2.4);
  key.position.set(2.4, 4.5, 4.2);
  scene.add(key);
  const rim = new THREE.DirectionalLight(0xff9c8c, 1.6);
  rim.position.set(-3.2, 2.4, -2.8);
  scene.add(rim);
  const fill = new THREE.DirectionalLight(0x8bc9ef, 0.8);
  fill.position.set(3.5, 0.8, 1.5);
  scene.add(fill);
  return { scene, camera };
}
