import { act, render, screen } from '@testing-library/react';
import { AnimatedScore } from './AnimatedScore';
import { mockReducedMotion } from '../../test/setup';

describe('AnimatedScore', () => {
  afterEach(() => mockReducedMotion(false));

  it('muestra 0 hasta que empieza el conteo y siempre anuncia el valor real', () => {
    const { container } = render(<AnimatedScore value={1850} run={false} />);
    expect(container.querySelector('[aria-hidden="true"]')).toHaveTextContent('0');
    expect(screen.getByText(/1850 puntos|1\.850 puntos/)).toBeInTheDocument();
  });

  it('cuenta hasta el valor exacto del servidor', async () => {
    vi.useFakeTimers({ toFake: ['requestAnimationFrame', 'cancelAnimationFrame', 'performance'] });
    const { container } = render(<AnimatedScore value={1850} run duration={400} />);
    await act(async () => {
      vi.advanceTimersByTime(250);
    });
    const mid = Number(container.querySelector('[aria-hidden="true"]')!.textContent!.replace(/\D/g, ''));
    expect(mid).toBeGreaterThan(0);
    expect(mid).toBeLessThan(1850);
    await act(async () => {
      vi.advanceTimersByTime(600);
    });
    expect(container.querySelector('[aria-hidden="true"]')!.textContent!.replace(/\D/g, '')).toBe('1850');
    vi.useRealTimers();
  });

  it('con movimiento reducido muestra el valor final sin animar', () => {
    mockReducedMotion(true);
    const { container } = render(<AnimatedScore value={720} run={false} />);
    expect(container.querySelector('[aria-hidden="true"]')).toHaveTextContent('720');
  });
});
