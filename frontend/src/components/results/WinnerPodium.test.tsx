import { render, screen, within } from '@testing-library/react';
import { WinnerPodium } from './WinnerPodium';
import { result, row } from '../../test/fixtures';

const complete = 'complete' as const;

describe('WinnerPodium', () => {
  it('con un solo participante muestra únicamente el puesto central', () => {
    const r = result([row(1, 1, 'Ana Ruiz', 900)]);
    const { container } = render(<WinnerPodium podium={r.podium} phase={complete} />);
    const slots = container.querySelectorAll('.tq-podium__slot');
    expect(slots).toHaveLength(1);
    expect(slots[0]).toHaveClass('tq-podium__slot--center');
    expect(screen.getByLabelText(/Primer puesto: Ana Ruiz/)).toBeInTheDocument();
  });

  it('con dos participantes coloca el segundo a la izquierda', () => {
    const r = result([row(1, 1, 'Ana', 900), row(2, 2, 'Beto', 500)]);
    const { container } = render(<WinnerPodium podium={r.podium} phase={complete} />);
    expect(container.querySelector('.tq-podium__slot--left')).toHaveAccessibleName(/Segundo puesto: Beto/);
    expect(container.querySelector('.tq-podium__slot--right')).toBeNull();
  });

  it('con tres o más participantes reparte 2 · 1 · 3 y deja fuera al cuarto', () => {
    const r = result([row(1, 1, 'Ana', 900), row(2, 2, 'Beto', 700), row(3, 3, 'Cris', 400), row(4, 4, 'Dani', 100)]);
    const { container } = render(<WinnerPodium podium={r.podium} phase={complete} />);
    expect(container.querySelector('.tq-podium__slot--center')).toHaveAccessibleName(/Ana/);
    expect(container.querySelector('.tq-podium__slot--left')).toHaveAccessibleName(/Beto/);
    expect(container.querySelector('.tq-podium__slot--right')).toHaveAccessibleName(/Cris/);
    expect(screen.queryByText('Dani')).not.toBeInTheDocument();
  });

  it('solo el ganador lleva corona y las medallas corresponden a su puesto', () => {
    const r = result([row(1, 1, 'Ana', 900), row(2, 2, 'Beto', 700), row(3, 3, 'Cris', 400)]);
    const { container } = render(<WinnerPodium podium={r.podium} phase={complete} />);
    expect(container.querySelectorAll('.tq-podium__crown')).toHaveLength(1);
    expect(within(container.querySelector('.tq-podium__slot--center') as HTMLElement).getByLabelText('Medalla de oro')).toBeInTheDocument();
    expect(screen.getByLabelText('Medalla de plata')).toBeInTheDocument();
    expect(screen.getByLabelText('Medalla de bronce')).toBeInTheDocument();
  });

  it('un empate en el primer puesto comparte escalón y el siguiente puesto es el 3.º', () => {
    const r = result([
      row(1, 1, 'Ana', 900, { tied: true }),
      row(2, 1, 'Beto', 900, { tied: true }),
      row(3, 3, 'Cris', 400),
    ]);
    const { container } = render(<WinnerPodium podium={r.podium} phase={complete} />);
    const center = container.querySelector('.tq-podium__slot--center') as HTMLElement;
    expect(center).toHaveAccessibleName(/Ana, Beto, 900 puntos \(empate\)/);
    expect(within(center).getByText('Empate')).toBeInTheDocument();
    // El grupo del puesto 3 ocupa la columna izquierda, pero su escalón es de bronce (altura del 3.º).
    const left = container.querySelector('.tq-podium__slot--left') as HTMLElement;
    expect(left).toHaveClass('tq-podium__slot--p3');
    expect(left.querySelector('.tq-podium__step--bronze')).not.toBeNull();
  });

  it('marca al usuario actual en el podio', () => {
    const r = result([row(1, 1, 'Ana', 900), row(2, 2, 'Beto', 700, { currentUser: true })]);
    render(<WinnerPodium podium={r.podium} phase={complete} />);
    expect(screen.getByText('Tú')).toBeInTheDocument();
  });

  it('sigue la secuencia: ganador oculto mientras se revelan 2.º y 3.º', () => {
    const r = result([row(1, 1, 'Ana', 900), row(2, 2, 'Beto', 700), row(3, 3, 'Cris', 400)]);
    const { container, rerender } = render(<WinnerPodium podium={r.podium} phase="runners" />);
    expect(container.querySelector('.tq-podium__slot--center')).toHaveAttribute('data-revealed', 'false');
    expect(container.querySelector('.tq-podium__slot--left')).toHaveAttribute('data-revealed', 'true');
    rerender(<WinnerPodium podium={r.podium} phase="winner" />);
    expect(container.querySelector('.tq-podium__slot--center')).toHaveAttribute('data-revealed', 'true');
  });
});
