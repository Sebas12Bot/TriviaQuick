import type { ReactNode } from 'react';
import { BrandMark } from '../common/BrandMark';
import { CobraMascot } from '../mascot/CobraMascot';
import { useMediaQuery } from '../../hooks/useMediaQuery';
import type { MascotMood } from '../mascot/mascotPose';

interface Props {
  mood: MascotMood;
  typing: number;
  variant: 'login' | 'register';
  children: ReactNode;
}

/**
 * Distribución de acceso: panel azul noche con Coby como protagonista y el formulario claro a la
 * derecha. En pantallas pequeñas, Coby se reduce a una franja sobre el formulario.
 */
export function AuthLayout({ mood, typing, variant, children }: Props) {
  // Una sola instancia 3D según el tamaño de pantalla (un único contexto WebGL).
  const compact = useMediaQuery('(max-width: 820px)');
  return (
    <div className={`tq-auth tq-auth--${variant}`}>
      <aside className="tq-auth__side">
        <BrandMark inverse />
        <div className="tq-auth__stage">
          {!compact && <CobraMascot mood={mood} typing={typing} className="tq-auth__mascot" />}
        </div>
        <div className="tq-auth__copy">
          <p className="tq-auth__claim">
            {variant === 'login' ? 'Preguntas en vivo, minijuegos y un podio que nadie puede trucar.' : 'Crea tu cuenta y únete a la próxima partida.'}
          </p>
          <p className="tq-auth__hint">Coby te acompaña: mientras escribes tu contraseña, no mira.</p>
        </div>
      </aside>

      <main className="tq-auth__main">
        <div className="tq-auth__card">
          <div className="tq-auth__mobile-top">
            <BrandMark />
            {compact && <CobraMascot mood={mood} typing={typing} className="tq-auth__mascot-mini" />}
          </div>
          {children}
        </div>
      </main>
    </div>
  );
}
