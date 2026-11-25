import type { GameResult, ParticipantRanking } from '../types/api';

/** Construye resultados de prueba con la misma forma que devuelve el backend. */
export function row(
  participantId: number,
  position: number,
  displayName: string,
  score: number,
  extra: Partial<ParticipantRanking> = {},
): ParticipantRanking {
  const initials = displayName
    .split(' ')
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
  return { participantId, position, displayName, initials, score, correctAnswers: 0, currentUser: false, tied: false, ...extra };
}

export function result(ranking: ParticipantRanking[]): GameResult {
  const places: GameResult['podium']['places'] = [];
  for (const r of ranking) {
    if (r.position > 3) break;
    const last = places[places.length - 1];
    if (last && last.position === r.position) last.participants.push(r);
    else places.push({ position: r.position, score: r.score, participants: [r] });
  }
  return {
    sessionId: 1,
    triviaTitle: 'Ciencia y mundo',
    finishedAt: '2026-11-19T10:00:00Z',
    participantCount: ranking.length,
    questionCount: 9,
    myResult: ranking.find((r) => r.currentUser) ?? null,
    podium: { sessionId: 1, places },
    ranking,
    tieBreakRule: 'Orden por puntuación total descendente; desempate por respuestas correctas.',
  };
}
