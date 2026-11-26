import type {
  AbilitySettings, EventSettings, GameEventType, QuestionDraft, ScoringSettings, TimelineEventDraft, TriviaDetail, TriviaUpsert,
} from '../../types/api';

/**
 * Modelo del editor: la línea de tiempo es una lista ordenada de preguntas y eventos.
 * Al guardar se convierte en el formato del backend (preguntas en orden + eventos con
 * "después de la pregunta N"). El servidor vuelve a validar todo.
 */
export type QuestionItem = { kind: 'question'; key: string; question: QuestionDraft };
export type EventItem = { kind: 'event'; key: string; event: Omit<TimelineEventDraft, 'afterQuestion'> };
export type TimelineItem = QuestionItem | EventItem;

export interface EditorMeta {
  title: string;
  description: string;
  category: string;
  playMode: 'INDIVIDUAL' | 'GROUP';
  coverImageKey: string | null;
  coverImageUrl: string | null;
  activityMinutes: number | null;
}

export interface EditorState {
  meta: EditorMeta;
  scoring: ScoringSettings;
  events: EventSettings;
  items: TimelineItem[];
}

let seq = 0;
export const newKey = (prefix: string) => `${prefix}-${Date.now().toString(36)}-${(seq++).toString(36)}`;

export const DEFAULT_ABILITIES: AbilitySettings = {
  lightningSeconds: 20,
  lightningReward: 300,
  lightningCategory: 'ANY',
  multiplierFactor: 2,
  multiplierTarget: 'BOTTOM_HALF',
  shieldTarget: 'CORRECT_PREVIOUS',
  duelSeconds: 20,
  duelStake: 250,
  powerUpSeconds: 15,
};

export const DEFAULT_SCORING: ScoringSettings = {
  defaultPoints: 1000,
  speedBonusEnabled: true,
  speedTiers: [
    { maxElapsedPercent: 25, bonusPoints: 100 },
    { maxElapsedPercent: 60, bonusPoints: 50 },
    { maxElapsedPercent: 100, bonusPoints: 10 },
  ],
  streakEnabled: true,
  streakBreaksOnMiss: true,
  streakTiers: [
    { streakLength: 3, bonusPoints: 30 },
    { streakLength: 5, bonusPoints: 60 },
    { streakLength: 10, bonusPoints: 100 },
  ],
  fastestBonusEnabled: true,
  fastestBonusPoints: 100,
};

export const ALL_EVENT_TYPES: GameEventType[] = ['LIGHTNING_CHALLENGE', 'MULTIPLIER', 'SHIELD', 'DUEL'];

export function newQuestion(points = 1000, type: QuestionDraft['type'] = 'MULTIPLE_CHOICE'): QuestionItem {
  const answers = type === 'TRUE_FALSE'
    ? [{ text: 'Verdadero', correct: true }, { text: 'Falso', correct: false }]
    : [{ text: '', correct: true }, { text: '', correct: false }, { text: '', correct: false }, { text: '', correct: false }];
  return {
    kind: 'question',
    key: newKey('q'),
    question: { type, text: '', timeLimitSeconds: 20, basePoints: points, imageKey: null, imageUrl: null, videoKey: null, videoUrl: null, answers },
  };
}

export function newEvent(type: GameEventType): EventItem {
  return {
    kind: 'event',
    key: newKey('e'),
    event: { type, durationSeconds: null, rewardPoints: null, multiplierFactor: null, stake: null, challengeCategory: null, target: null },
  };
}

export function emptyState(): EditorState {
  return {
    meta: { title: '', description: '', category: '', playMode: 'GROUP', coverImageKey: null, coverImageUrl: null, activityMinutes: null },
    scoring: structuredClone(DEFAULT_SCORING),
    events: { mode: 'AUTO', autoFrequency: 2, autoTypes: [...ALL_EVENT_TYPES], abilities: { ...DEFAULT_ABILITIES } },
    items: [newQuestion()],
  };
}

export function fromDetail(d: TriviaDetail): EditorState {
  const items: TimelineItem[] = [];
  d.questions.forEach((q, index) => {
    items.push({
      kind: 'question',
      key: newKey('q'),
      question: { type: q.type, text: q.text, timeLimitSeconds: q.timeLimitSeconds, basePoints: q.basePoints,
        imageKey: q.imageKey, imageUrl: q.imageUrl ?? null, videoKey: q.videoKey ?? null, videoUrl: q.videoUrl ?? null, answers: q.answers.map((a) => ({ text: a.text, correct: a.correct })) },
    });
    const ev = d.events.mode === 'MANUAL' ? d.timeline.find((t) => t.afterQuestion === index) : undefined;
    if (ev) {
      const { afterQuestion: _ignored, ...rest } = ev;
      void _ignored;
      items.push({ kind: 'event', key: newKey('e'), event: rest });
    }
  });
  return {
    meta: {
      title: d.title, description: d.description ?? '', category: d.category ?? '', playMode: d.playMode,
      coverImageKey: d.coverImageKey, coverImageUrl: d.coverImageUrl, activityMinutes: d.activityMinutes,
    },
    scoring: { ...DEFAULT_SCORING, ...d.scoring },
    events: { ...d.events, autoTypes: [...d.events.autoTypes], abilities: { ...DEFAULT_ABILITIES, ...d.events.abilities } },
    items,
  };
}

export function toUpsert(state: EditorState, status: 'DRAFT' | 'PUBLISHED'): TriviaUpsert {
  const questions: QuestionDraft[] = [];
  const timeline: TimelineEventDraft[] = [];
  for (const item of state.items) {
    if (item.kind === 'question') {
      const { imageUrl: _url, videoUrl: _video, ...q } = item.question;
      void _url;
      void _video;
      questions.push({ ...q, text: q.text.trim(), answers: q.answers.map((a) => ({ text: a.text.trim(), correct: a.correct })) });
    } else if (questions.length > 0 && state.events.mode === 'MANUAL') {
      timeline.push({ ...item.event, afterQuestion: questions.length - 1 });
    }
  }
  return {
    title: state.meta.title.trim(),
    description: state.meta.description.trim() || null,
    category: state.meta.category.trim() || null,
    status,
    playMode: state.meta.playMode,
    coverImageKey: state.meta.coverImageKey,
    activityMinutes: state.meta.activityMinutes,
    questions,
    scoring: state.scoring,
    events: state.events,
    timeline,
  };
}

// ------------------------------------------------------------------ operaciones de la línea de tiempo

/** Un evento solo puede ir entre dos preguntas y nunca junto a otro evento. */
export function canInsertEventAt(items: TimelineItem[], index: number): boolean {
  const before = items[index - 1];
  const after = items[index];
  return before?.kind === 'question' && after?.kind === 'question';
}

export function insertAt(items: TimelineItem[], index: number, item: TimelineItem): TimelineItem[] {
  if (item.kind === 'event' && !canInsertEventAt(items, index)) return items;
  const next = [...items];
  next.splice(index, 0, item);
  return next;
}

export function removeItem(items: TimelineItem[], key: string): TimelineItem[] {
  return normalize(items.filter((i) => i.key !== key));
}

/** Mueve un elemento a otra posición y limpia eventos que queden en lugares no válidos. */
export function moveItem(items: TimelineItem[], from: number, to: number): TimelineItem[] {
  if (from === to || from < 0 || from >= items.length) return items;
  const next = [...items];
  const [moved] = next.splice(from, 1);
  next.splice(Math.max(0, Math.min(next.length, to)), 0, moved);
  if (moved.kind === 'event') {
    const idx = next.indexOf(moved);
    if (next[idx - 1]?.kind !== 'question' || next[idx + 1]?.kind !== 'question') return items; // movimiento no válido
  }
  return normalize(next);
}

/** Elimina eventos al principio, al final o duplicados consecutivos (tras borrar o mover preguntas). */
export function normalize(items: TimelineItem[]): TimelineItem[] {
  const out: TimelineItem[] = [];
  for (const item of items) {
    if (item.kind === 'event' && (out.length === 0 || out[out.length - 1].kind === 'event')) continue;
    out.push(item);
  }
  while (out.length && out[out.length - 1].kind === 'event') out.pop();
  return out;
}

export function updateItem(items: TimelineItem[], key: string, patch: (item: TimelineItem) => TimelineItem): TimelineItem[] {
  return items.map((i) => (i.key === key ? patch(i) : i));
}

export function questionNumber(items: TimelineItem[], key: string): number {
  let n = 0;
  for (const i of items) {
    if (i.kind === 'question') n++;
    if (i.key === key) return n;
  }
  return n;
}

/** Comprobaciones para orientar al docente antes de publicar (el servidor es la fuente de verdad). */
export function publishIssues(state: EditorState): string[] {
  const issues: string[] = [];
  if (!state.meta.title.trim()) issues.push('Ponle un título al cuestionario.');
  const questions = state.items.filter((i): i is QuestionItem => i.kind === 'question');
  if (questions.length === 0) issues.push('Añade al menos una pregunta.');
  questions.forEach((q, i) => {
    if (!q.question.text.trim()) issues.push(`La pregunta ${i + 1} no tiene enunciado.`);
    if (q.question.answers.some((a) => !a.text.trim())) issues.push(`La pregunta ${i + 1} tiene opciones vacías.`);
    if (q.question.answers.filter((a) => a.correct).length !== 1) issues.push(`Marca una única respuesta correcta en la pregunta ${i + 1}.`);
  });
  if (state.events.mode === 'AUTO' && state.events.autoTypes.length === 0) issues.push('Habilita al menos un minijuego o desactiva los eventos.');
  return issues;
}
