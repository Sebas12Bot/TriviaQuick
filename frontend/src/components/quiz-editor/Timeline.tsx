import { Fragment, useState, type DragEvent } from 'react';
import { ArrowDown, ArrowUp, Flag, GripVertical, Plus, Settings2, Trash2, Zap } from 'lucide-react';
import { EVENT_META } from '../game/EventCard';
import type { GameEventType, EventMode } from '../../types/api';
import { canInsertEventAt, type TimelineItem } from './editorModel';

export type Selection = { kind: 'settings' } | { kind: 'item'; key: string };

interface Props {
  items: TimelineItem[];
  selection: Selection;
  eventMode: EventMode;
  autoFrequency: number;
  locked: boolean;
  onSelect: (s: Selection) => void;
  onInsertQuestion: (index: number) => void;
  onInsertEvent: (index: number, type: GameEventType) => void;
  onMove: (from: number, to: number) => void;
  onRemove: (key: string) => void;
}

/**
 * Línea de tiempo vertical: INICIO → preguntas y minijuegos → RESULTADOS.
 * Se reordena arrastrando o con los botones de subir/bajar (accesible por teclado).
 */
export function Timeline({ items, selection, eventMode, autoFrequency, locked, onSelect, onInsertQuestion, onInsertEvent, onMove, onRemove }: Props) {
  const [dragFrom, setDragFrom] = useState<number | null>(null);
  const [dropAt, setDropAt] = useState<number | null>(null);
  const [eventMenuAt, setEventMenuAt] = useState<number | null>(null);
  let questionCount = 0;

  const onDrop = (e: DragEvent, index: number) => {
    e.preventDefault();
    if (dragFrom !== null) onMove(dragFrom, dragFrom < index ? index - 1 : index);
    setDragFrom(null);
    setDropAt(null);
  };

  const gap = (index: number) => {
    const eventAllowed = eventMode === 'MANUAL' && canInsertEventAt(items, index) && !locked;
    // Vista previa del modo automático: dónde puede aparecer un minijuego.
    const qBefore = items.slice(0, index).filter((i) => i.kind === 'question').length;
    const autoSlot = eventMode === 'AUTO' && canInsertEventAt(items, index) && qBefore % autoFrequency === 0;
    return (
      <li
        className="tq-tl__gap"
        data-drop={dropAt === index}
        onDragOver={(e) => {
          if (dragFrom === null) return;
          e.preventDefault();
          setDropAt(index);
        }}
        onDragLeave={() => setDropAt((d) => (d === index ? null : d))}
        onDrop={(e) => onDrop(e, index)}
      >
        <span className="tq-tl__line" aria-hidden="true" />
        {autoSlot && <span className="tq-tl__auto"><Zap size={13} aria-hidden="true" /> Minijuego automático posible</span>}
        {!locked && (
          <span className="tq-tl__adders">
            <button type="button" className="tq-tl__add" onClick={() => onInsertQuestion(index)} aria-label={`Insertar pregunta en la posición ${index + 1}`}>
              <Plus size={14} aria-hidden="true" /> Pregunta
            </button>
            {eventAllowed && (
              <span className="tq-tl__event-adder">
                <button type="button" className="tq-tl__add tq-tl__add--event" aria-expanded={eventMenuAt === index}
                  onClick={() => setEventMenuAt((v) => (v === index ? null : index))}>
                  <Plus size={14} aria-hidden="true" /> Minijuego
                </button>
                {eventMenuAt === index && (
                  <span className="tq-tl__menu" role="menu">
                    {(Object.keys(EVENT_META) as GameEventType[]).map((t) => {
                      const M = EVENT_META[t];
                      return (
                        <button key={t} type="button" role="menuitem" onClick={() => { onInsertEvent(index, t); setEventMenuAt(null); }}>
                          <M.Icon size={16} aria-hidden="true" /> {M.title}
                        </button>
                      );
                    })}
                  </span>
                )}
              </span>
            )}
          </span>
        )}
      </li>
    );
  };

  return (
    <ol className="tq-tl" aria-label="Línea de tiempo del cuestionario">
      <li>
        <button type="button" className="tq-tl__node tq-tl__node--start" aria-pressed={selection.kind === 'settings'}
          onClick={() => onSelect({ kind: 'settings' })}>
          <Settings2 size={18} aria-hidden="true" />
          <span><strong>Inicio</strong><small>Ajustes, puntuación y minijuegos</small></span>
        </button>
      </li>
      {gap(0)}
      {items.map((item, index) => {
        const selected = selection.kind === 'item' && selection.key === item.key;
        const isQ = item.kind === 'question';
        if (isQ) questionCount++;
        const meta = !isQ ? EVENT_META[item.event.type] : null;
        const label = isQ ? `Pregunta ${questionCount}` : meta!.title;
        const answersOk = isQ && item.question.answers.filter((a) => a.correct).length === 1 && item.question.text.trim();
        return (
          <Fragment key={item.key}>
          <li
            className={`tq-tl__item${isQ ? '' : ' tq-tl__item--event'}`}
            data-dragging={dragFrom === index}
            draggable={!locked}
            onDragStart={(e) => { setDragFrom(index); e.dataTransfer.effectAllowed = 'move'; }}
            onDragEnd={() => { setDragFrom(null); setDropAt(null); }}>
            <div className={`tq-tl__node${selected ? ' is-selected' : ''}`}>
              {!locked && <GripVertical className="tq-tl__grip" size={16} aria-hidden="true" />}
              <button type="button" className="tq-tl__select" aria-pressed={selected} onClick={() => onSelect({ kind: 'item', key: item.key })}>
                {isQ ? (
                  <span className="tq-tl__badge tabular" aria-hidden="true">{questionCount}</span>
                ) : (
                  <span className="tq-tl__badge tq-tl__badge--event" aria-hidden="true">{meta && <meta.Icon size={15} />}</span>
                )}
                <span className="tq-tl__text">
                  <strong>{label}</strong>
                  <small>
                    {isQ
                      ? (item.question.text.trim() || 'Sin enunciado todavía') + ` · ${item.question.basePoints} pts · ${item.question.timeLimitSeconds} s`
                      : 'Minijuego · se configura al seleccionarlo'}
                  </small>
                </span>
                {isQ && !answersOk && <span className="tq-tl__warn" title="Pregunta incompleta">Incompleta</span>}
              </button>
              {!locked && (
                <span className="tq-tl__actions">
                  <button type="button" onClick={() => onMove(index, index - 1)} disabled={index === 0} aria-label={`Subir ${label}`}>
                    <ArrowUp size={15} aria-hidden="true" />
                  </button>
                  <button type="button" onClick={() => onMove(index, index + 1)} disabled={index === items.length - 1} aria-label={`Bajar ${label}`}>
                    <ArrowDown size={15} aria-hidden="true" />
                  </button>
                  <button type="button" className="tq-tl__del" onClick={() => onRemove(item.key)} aria-label={`Eliminar ${label}`}>
                    <Trash2 size={15} aria-hidden="true" />
                  </button>
                </span>
              )}
            </div>
          </li>
          {gap(index + 1)}
          </Fragment>
        );
      })}
      <li>
        <div className="tq-tl__node tq-tl__node--end">
          <Flag size={18} aria-hidden="true" />
          <span><strong>Resultados</strong><small>Podio y clasificación final</small></span>
        </div>
      </li>
    </ol>
  );
}
