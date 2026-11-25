import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { websocketUrl } from '../services/api';

export interface SessionMessage {
  sessionId: number;
  type:
    | 'PARTICIPANT_JOINED'
    | 'QUESTION_STARTED'
    | 'ANSWER_RECEIVED'
    | 'EVENT_STARTED'
    | 'EVENT_UPDATED'
    | 'EVENT_RESOLVED'
    | 'RANKING_UPDATED'
    | 'SESSION_FINISHED';
  serverTime: string;
  deadline: string | null;
  data: Record<string, unknown>;
}

/**
 * Suscripción de solo lectura a los mensajes oficiales de una sesión. El cliente nunca publica:
 * cualquier SEND sería rechazado por el servidor. Ante cada mensaje, la vista vuelve a pedir su
 * estado autorizado por REST.
 */
export function useSessionSocket(sessionId: number | null, onMessage: (m: SessionMessage) => void): boolean {
  const [connected, setConnected] = useState(false);
  const handler = useRef(onMessage);
  handler.current = onMessage;

  useEffect(() => {
    if (!sessionId) return;
    const client = new Client({
      brokerURL: websocketUrl(),
      reconnectDelay: 3000,
      heartbeatIncoming: 15000,
      heartbeatOutgoing: 15000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/sessions/${sessionId}`, (frame) => {
          try {
            handler.current(JSON.parse(frame.body) as SessionMessage);
          } catch {
            /* mensaje malformado: se ignora */
          }
        });
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    });
    client.activate();
    return () => {
      setConnected(false);
      void client.deactivate();
    };
  }, [sessionId]);

  return connected;
}
