import type { GameResult } from '../../types/api';

const dateFormat = new Intl.DateTimeFormat('es', { dateStyle: 'medium', timeStyle: 'short' });

/** Franja superior: qué partida es y su resumen, a tamaño de rótulo de retransmisión. */
export function GameSummary({ result }: { result: GameResult }) {
  const mine = result.myResult;
  return (
    <header className="tq-summary">
      <div className="tq-summary__title">
        <h1>{result.triviaTitle}</h1>
        <p className="tq-summary__meta">
          <span>Resultados finales</span>
          <span aria-hidden="true">·</span>
          <time dateTime={result.finishedAt}>{dateFormat.format(new Date(result.finishedAt))}</time>
        </p>
      </div>
      <dl className="tq-summary__stats">
        <div>
          <dt>Participantes</dt>
          <dd className="tabular">{result.participantCount}</dd>
        </div>
        <div>
          <dt>Preguntas</dt>
          <dd className="tabular">{result.questionCount}</dd>
        </div>
        {mine && (
          <div className="tq-summary__mine">
            <dt>Tu puesto</dt>
            <dd className="tabular">
              {mine.position}.º{mine.tied && <span className="tq-summary__tie"> (empate)</span>}
            </dd>
          </div>
        )}
      </dl>
    </header>
  );
}
