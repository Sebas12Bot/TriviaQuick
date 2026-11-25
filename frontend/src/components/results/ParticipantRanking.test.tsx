import { render, screen, within } from '@testing-library/react';
import { ParticipantRanking } from './ParticipantRanking';
import { row } from '../../test/fixtures';

describe('ParticipantRanking', () => {
  const entries = [
    row(1, 1, 'Ana', 2000, { correctAnswers: 2 }),
    row(2, 2, 'Beto', 1900, { tied: true, correctAnswers: 2, currentUser: true }),
    row(3, 2, 'Cris', 1900, { tied: true, correctAnswers: 2 }),
    row(4, 4, 'Dani', 0),
  ];

  it('muestra las filas en el orden del servidor, sin reordenar', () => {
    render(<ParticipantRanking entries={entries} tieBreakRule="regla" visible />);
    const items = within(screen.getByRole('list', { name: 'Clasificación completa' })).getAllByRole('listitem');
    expect(items.map((li) => li.querySelector('.tq-rank-item__name')?.textContent?.replace('Tú', ''))).toEqual([
      'Ana',
      'Beto',
      'Cris',
      'Dani',
    ]);
  });

  it('distingue los tres primeros puestos y resalta al usuario actual', () => {
    render(<ParticipantRanking entries={entries} tieBreakRule="regla" visible />);
    const items = within(screen.getByRole('list', { name: 'Clasificación completa' })).getAllByRole('listitem');
    expect(items[0]).toHaveClass('tq-rank-item--gold');
    expect(items[1]).toHaveClass('tq-rank-item--silver', 'tq-rank-item--self');
    expect(items[1]).toHaveAttribute('aria-current', 'true');
    expect(items[3].className).not.toMatch(/gold|silver|bronze/);
  });

  it('indica los empates con la misma posición', () => {
    render(<ParticipantRanking entries={entries} tieBreakRule="regla" visible />);
    expect(screen.getAllByLabelText('Posición 2, empatado')).toHaveLength(2);
    expect(screen.getByLabelText('Posición 4')).toBeInTheDocument();
  });

  it('permite desplazamiento interno con teclado y muestra la regla de desempate', () => {
    render(<ParticipantRanking entries={entries} tieBreakRule="Regla documentada" visible />);
    expect(screen.getByRole('list', { name: 'Clasificación completa' })).toHaveAttribute('tabindex', '0');
    expect(screen.getByText('Regla documentada')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Clasificación de participantes' })).toBeInTheDocument();
  });
});
