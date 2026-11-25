import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ResultsPage } from './ResultsPage';
import { resultService } from '../services/resultService';
import { ApiError } from '../services/api';
import { mockReducedMotion } from '../test/setup';
import { result, row } from '../test/fixtures';

vi.mock('../hooks/useSessionSocket', () => ({ useSessionSocket: () => false }));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/sessions/1/results']}>
      <Routes>
        <Route path="/sessions/:sessionId/results" element={<ResultsPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

const sample = result([
  row(1, 1, 'Ana Ruiz', 2000, { correctAnswers: 2 }),
  row(2, 2, 'Beto Gil', 1900, { correctAnswers: 2, currentUser: true }),
  row(3, 3, 'Cris Paz', 900, { correctAnswers: 1 }),
  row(4, 4, 'Dani Sol', 0),
]);

describe('ResultsPage', () => {
  afterEach(() => {
    vi.useRealTimers();
    mockReducedMotion(false);
  });

  it('muestra el estado de carga mientras espera al servidor', () => {
    vi.spyOn(resultService, 'results').mockReturnValue(new Promise(() => undefined));
    renderPage();
    expect(screen.getByLabelText('Cargando resultados')).toHaveAttribute('aria-busy', 'true');
  });

  it('muestra un error recuperable y reintenta', async () => {
    const spy = vi
      .spyOn(resultService, 'results')
      .mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'Ocurrió un error inesperado.'))
      .mockResolvedValueOnce(sample);
    mockReducedMotion(true);
    renderPage();
    expect(await screen.findByText('No se pudieron cargar los resultados')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('heading', { name: 'Clasificación de participantes' })).toBeInTheDocument();
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('sin permiso no ofrece reintentar', async () => {
    vi.spyOn(resultService, 'results').mockRejectedValue(new ApiError(403, 'FORBIDDEN', 'No participas en esta sesión.'));
    renderPage();
    expect(await screen.findByText('No puedes ver estos resultados')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });

  it('indica cuando la partida aún no ha terminado', async () => {
    vi.spyOn(resultService, 'results').mockRejectedValue(new ApiError(409, 'RESULTS_NOT_READY', 'Aún no'));
    renderPage();
    expect(await screen.findByText('La partida sigue en juego')).toBeInTheDocument();
  });

  it('muestra un estado vacío sin participantes ficticios', async () => {
    vi.spyOn(resultService, 'results').mockResolvedValue(result([]));
    renderPage();
    expect(await screen.findByText('Nadie participó en esta partida')).toBeInTheDocument();
    expect(document.querySelector('.tq-podium')).toBeNull();
  });

  it('con movimiento reducido presenta directamente podio y clasificación', async () => {
    mockReducedMotion(true);
    vi.spyOn(resultService, 'results').mockResolvedValue(sample);
    renderPage();
    const panel = (await screen.findByRole('heading', { name: 'Clasificación de participantes' })).closest('section')!;
    expect(panel).toHaveAttribute('data-visible', 'true');
    expect(screen.queryByRole('button', { name: /Saltar animación/ })).not.toBeInTheDocument();
    expect(document.querySelector('.tq-confetti')).toBeNull();
    expect(screen.getByText('Ciencia y mundo')).toBeInTheDocument();
    expect(screen.getByText('Tu puesto')).toBeInTheDocument();
  });

  it('recorre la secuencia (cuenta atrás → podio → clasificación) y permite saltarla', async () => {
    vi.useFakeTimers();
    vi.spyOn(resultService, 'results').mockResolvedValue(sample);
    renderPage();
    // Resuelve la petición simulada sin dejar correr los temporizadores.
    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText('Ciencia y mundo')).toBeInTheDocument();
    const panel = screen.getByRole('heading', { name: 'Clasificación de participantes' }).closest('section')!;
    expect(panel).toHaveAttribute('data-visible', 'false');

    await act(async () => {
      vi.advanceTimersByTime(750); // fin de la intro
    });
    expect(screen.getByText('Revelando resultados en 3')).toBeInTheDocument();
    await act(async () => {
      vi.advanceTimersByTime(950);
    });
    expect(screen.getByText('Revelando resultados en 2')).toBeInTheDocument();

    // Cada fase programa la siguiente al renderizar: se avanza fase a fase.
    for (let i = 0; i < 8; i++) {
      await act(async () => {
        vi.advanceTimersByTime(3000);
      });
    }
    expect(panel).toHaveAttribute('data-visible', 'true');
    expect(screen.getByText(/Ganador: Ana Ruiz/)).toBeInTheDocument();
  });

  it('el botón Saltar animación muestra el resultado final al instante', async () => {
    vi.spyOn(resultService, 'results').mockResolvedValue(sample);
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: /Saltar animación/ }));
    const panel = screen.getByRole('heading', { name: 'Clasificación de participantes' }).closest('section')!;
    expect(panel).toHaveAttribute('data-visible', 'true');
    expect(screen.getByRole('button', { name: /Repetir ceremonia/ })).toBeInTheDocument();
  });
});
