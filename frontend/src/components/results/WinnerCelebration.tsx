import { useEffect, useRef } from 'react';
import { useReducedMotion } from '../../hooks/useReducedMotion';

interface Props {
  active: boolean;
}

interface Piece {
  x: number; y: number; vx: number; vy: number;
  w: number; h: number; rot: number; vr: number;
  color: string; tilt: number;
}

// Metales del podio + azules de la marca, leídos de los tokens de theme.css. Sin morados.
const TOKENS = ['--gold', '--gold-deep', '--silver', '--bronze', '--blue-electric', '--blue-sky', '--white'];
const FALLBACK = ['#f1bf4e', '#c38323', '#cad2db', '#d38b59', '#287bc1', '#8bc9ef', '#fcfeff'];

function confettiColors(): string[] {
  const style = getComputedStyle(document.documentElement);
  return TOKENS.map((t, i) => style.getPropertyValue(t).trim() || FALLBACK[i]);
}

/**
 * Confeti en canvas: dos ráfagas desde los laterales del ganador, con gravedad y rozamiento.
 * Dura unos 3,5 s y se detiene solo. No se dibuja con movimiento reducido ni con la pestaña oculta.
 */
export function WinnerCelebration({ active }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const reduced = useReducedMotion();

  useEffect(() => {
    if (!active || reduced) return;
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;

    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const resize = () => {
      canvas.width = canvas.clientWidth * dpr;
      canvas.height = canvas.clientHeight * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();
    window.addEventListener('resize', resize);

    const W = () => canvas.clientWidth;
    const H = () => canvas.clientHeight;
    const colors = confettiColors();
    const pieces: Piece[] = [];
    const burst = (fromLeft: boolean, n: number) => {
      for (let i = 0; i < n; i++) {
        const angle = (fromLeft ? -105 : -75) + (Math.random() - 0.5) * 36;
        const speed = 7 + Math.random() * 7;
        const rad = (angle * Math.PI) / 180;
        pieces.push({
          // Nacen a los lados del escalón del ganador, por debajo de su avatar, y salen hacia fuera.
          x: W() * (fromLeft ? 0.3 : 0.7),
          y: H() * 0.62,
          vx: Math.cos(rad) * speed,
          vy: Math.sin(rad) * speed,
          w: 5 + Math.random() * 6,
          h: 8 + Math.random() * 8,
          rot: Math.random() * Math.PI,
          vr: (Math.random() - 0.5) * 0.3,
          color: colors[Math.floor(Math.random() * colors.length)],
          tilt: Math.random() * Math.PI,
        });
      }
    };
    burst(true, 70);
    burst(false, 70);
    const second = setTimeout(() => {
      burst(true, 40);
      burst(false, 40);
    }, 450);

    const started = performance.now();
    let raf = 0;
    const draw = (now: number) => {
      const elapsed = now - started;
      ctx.clearRect(0, 0, W(), H());
      for (const p of pieces) {
        p.vy += 0.22;
        p.vx *= 0.985;
        p.vy *= 0.985;
        p.x += p.vx;
        p.y += p.vy;
        p.rot += p.vr;
        p.tilt += 0.08;
        const fade = elapsed > 2600 ? Math.max(0, 1 - (elapsed - 2600) / 900) : 1;
        ctx.save();
        ctx.globalAlpha = fade;
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rot);
        ctx.scale(1, Math.abs(Math.cos(p.tilt)) * 0.8 + 0.2);
        ctx.fillStyle = p.color;
        ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
        ctx.restore();
      }
      if (elapsed < 3500 && !document.hidden) {
        raf = requestAnimationFrame(draw);
      } else {
        ctx.clearRect(0, 0, W(), H());
      }
    };
    raf = requestAnimationFrame(draw);

    return () => {
      cancelAnimationFrame(raf);
      clearTimeout(second);
      window.removeEventListener('resize', resize);
    };
  }, [active, reduced]);

  if (reduced) return null;
  return <canvas ref={canvasRef} className="tq-confetti" aria-hidden="true" />;
}
