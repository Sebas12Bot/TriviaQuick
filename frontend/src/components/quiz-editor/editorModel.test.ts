import {
  canInsertEventAt, emptyState, fromDetail, insertAt, moveItem, newEvent, newQuestion, normalize, publishIssues, removeItem, toUpsert,
  type EditorState, type TimelineItem,
} from './editorModel';
import type { TriviaDetail } from '../../types/api';

const q = (text: string) => {
  const item = newQuestion(100);
  item.question.text = text;
  item.question.answers = [{ text: 'A', correct: true }, { text: 'B', correct: false }];
  return item;
};
const kinds = (items: TimelineItem[]) => items.map((i) => (i.kind === 'question' ? `Q:${i.question.text}` : `E:${i.event.type}`));

describe('editorModel · línea de tiempo', () => {
  it('solo permite insertar eventos entre dos preguntas', () => {
    const items = [q('1'), q('2')];
    expect(canInsertEventAt(items, 0)).toBe(false);
    expect(canInsertEventAt(items, 1)).toBe(true);
    expect(canInsertEventAt(items, 2)).toBe(false);
    expect(kinds(insertAt(items, 0, newEvent('DUEL')))).toEqual(['Q:1', 'Q:2']);
    expect(kinds(insertAt(items, 1, newEvent('DUEL')))).toEqual(['Q:1', 'E:DUEL', 'Q:2']);
  });

  it('no permite dos eventos seguidos', () => {
    const items = insertAt([q('1'), q('2')], 1, newEvent('SHIELD'));
    expect(kinds(insertAt(items, 1, newEvent('DUEL')))).toEqual(['Q:1', 'E:SHIELD', 'Q:2']);
    expect(kinds(insertAt(items, 2, newEvent('DUEL')))).toEqual(['Q:1', 'E:SHIELD', 'Q:2']);
  });

  it('reordena preguntas y descarta movimientos de eventos a posiciones no válidas', () => {
    const items = insertAt([q('1'), q('2'), q('3')], 1, newEvent('MULTIPLIER'));
    expect(kinds(moveItem(items, 3, 0))).toEqual(['Q:3', 'Q:1', 'E:MULTIPLIER', 'Q:2']);
    // Mover el evento al principio no es válido: se ignora.
    expect(kinds(moveItem(items, 1, 0))).toEqual(kinds(items));
    // Mover el evento entre la 2 y la 3 sí.
    expect(kinds(moveItem(items, 1, 2))).toEqual(['Q:1', 'Q:2', 'E:MULTIPLIER', 'Q:3']);
  });

  it('al borrar una pregunta elimina eventos que queden sueltos', () => {
    const second = q('2');
    const items = insertAt([q('1'), second], 1, newEvent('DUEL'));
    expect(kinds(removeItem(items, second.key))).toEqual(['Q:1']);
    expect(kinds(normalize([newEvent('DUEL'), q('1'), newEvent('SHIELD')]))).toEqual(['Q:1']);
  });
});

describe('editorModel · conversión', () => {
  it('convierte la secuencia en preguntas ordenadas y eventos "después de la pregunta N"', () => {
    const s: EditorState = { ...emptyState(), items: [] };
    s.events.mode = 'MANUAL';
    s.meta.title = 'Mi trivia';
    s.items = insertAt(insertAt([q('1'), q('2'), q('3')], 2, newEvent('DUEL')), 1, newEvent('LIGHTNING_CHALLENGE'));
    const dto = toUpsert(s, 'DRAFT');
    expect(dto.questions.map((x) => x.text)).toEqual(['1', '2', '3']);
    expect(dto.timeline.map((t) => [t.afterQuestion, t.type])).toEqual([[0, 'LIGHTNING_CHALLENGE'], [1, 'DUEL']]);
    expect(dto.status).toBe('DRAFT');
  });

  it('en modo automático no envía eventos manuales', () => {
    const s: EditorState = { ...emptyState(), items: insertAt([q('1'), q('2')], 1, newEvent('DUEL')) };
    s.events.mode = 'AUTO';
    expect(toUpsert(s, 'PUBLISHED').timeline).toEqual([]);
  });

  it('reconstruye la línea de tiempo desde el detalle del servidor', () => {
    const detail = {
      id: 1, title: 'T', description: null, category: null, status: 'DRAFT', playMode: 'GROUP', coverImageKey: null,
      coverImageUrl: null, activityMinutes: null, createdAt: '', updatedAt: '', questionsLocked: false, inUse: false,
      questions: [0, 1, 2].map((i) => ({ id: i + 10, position: i, type: 'MULTIPLE_CHOICE', text: `P${i}`, timeLimitSeconds: 20,
        basePoints: 100, imageKey: null, imageUrl: null, answers: [{ text: 'A', correct: true }, { text: 'B', correct: false }] })),
      scoring: emptyState().scoring,
      events: { ...emptyState().events, mode: 'MANUAL' },
      timeline: [{ afterQuestion: 1, type: 'SHIELD', durationSeconds: null, rewardPoints: null, multiplierFactor: null, stake: null,
        challengeCategory: null, target: 'ALL' }],
    } as unknown as TriviaDetail;
    const state = fromDetail(detail);
    expect(kinds(state.items)).toEqual(['Q:P0', 'Q:P1', 'E:SHIELD', 'Q:P2']);
    expect(toUpsert(state, 'DRAFT').timeline[0]).toMatchObject({ afterQuestion: 1, type: 'SHIELD', target: 'ALL' });
  });

  it('señala lo que falta antes de publicar', () => {
    const s = emptyState();
    const issues = publishIssues(s);
    expect(issues.some((i) => i.includes('título'))).toBe(true);
    expect(issues.some((i) => i.includes('enunciado'))).toBe(true);
  });
});
