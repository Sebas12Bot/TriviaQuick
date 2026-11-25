import { useState } from 'react';
import { Shield, X as Times } from 'lucide-react';
import type { PowerUp } from '../../types/api';

const LABEL: Record<PowerUp['type'], string> = { MULTIPLIER: 'Multiplicador x2', SHIELD: 'Escudo' };
const STATE: Record<PowerUp['status'], string> = { AVAILABLE: 'Disponible', ACTIVE: 'Activo', CONSUMED: 'Usado' };

interface Props {
  powerUps: PowerUp[];
  onActivate: (id: number) => Promise<void>;
}

/** Habilidades del jugador. Activarlas es una petición: el servidor valida si es posible. */
export function PowerUpTray({ powerUps, onActivate }: Props) {
  const [busy, setBusy] = useState<number | null>(null);
  const visible = powerUps.filter((p) => p.status !== 'CONSUMED');
  if (visible.length === 0) return null;
  return (
    <section className="tq-powerups" aria-label="Tus habilidades">
      {visible.map((p) => {
        const Icon = p.type === 'SHIELD' ? Shield : Times;
        return (
          <div key={p.id} className={`tq-powerup tq-powerup--${p.status.toLowerCase()}`}>
            <Icon size={18} aria-hidden="true" />
            <span className="tq-powerup__name">{LABEL[p.type]}</span>
            <span className="tq-powerup__state">{STATE[p.status]}</span>
            {p.status === 'AVAILABLE' && (
              <button
                type="button"
                className="tq-btn tq-btn--small"
                disabled={busy === p.id}
                onClick={async () => {
                  setBusy(p.id);
                  try {
                    await onActivate(p.id);
                  } finally {
                    setBusy(null);
                  }
                }}
              >
                Activar
              </button>
            )}
          </div>
        );
      })}
    </section>
  );
}
