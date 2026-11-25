import type { ApiErrorBody } from '../types/api';

/**
 * Cliente HTTP. La sesión viaja en una cookie httpOnly que el navegador gestiona: el token nunca
 * se guarda en localStorage ni es accesible desde JavaScript. Las peticiones que modifican estado
 * llevan la cabecera anti-CSRF que exige el backend.
 */
const API_BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

async function request<T>(method: string, path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  let response: Response;
  const isForm = typeof FormData !== 'undefined' && body instanceof FormData;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      credentials: 'include',
      signal,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined && !isForm ? { 'Content-Type': 'application/json' } : {}),
        ...(method !== 'GET' ? { 'X-Requested-With': 'TriviaQuick' } : {}),
      },
      body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
    });
  } catch (err) {
    if ((err as Error).name === 'AbortError') throw err;
    throw new ApiError(0, 'NETWORK', 'No se pudo conectar con el servidor. Revisa tu conexión.');
  }
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  const data = text ? safeJson(text) : undefined;
  if (!response.ok) {
    const err = data as Partial<ApiErrorBody> | undefined;
    throw new ApiError(
      response.status,
      err?.code ?? 'HTTP_' + response.status,
      err?.message ?? 'Ocurrió un error inesperado.',
      err?.fieldErrors ?? {},
    );
  }
  return data as T;
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

export const api = {
  get: <T>(path: string, signal?: AbortSignal) => request<T>('GET', path, undefined, signal),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string, body: unknown) => request<T>('PATCH', path, body),
  del: <T>(path: string, body?: unknown) => request<T>('DELETE', path, body),
  /** Sube un archivo como multipart/form-data (campo "file"). */
  upload: <T>(path: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<T>('POST', path, form);
  },
};

/** URL del WebSocket: mismo origen en desarrollo (proxy de Vite) o derivada de VITE_API_URL. */
export function websocketUrl(): string {
  const base = API_BASE || window.location.origin;
  return base.replace(/^http/, 'ws') + '/ws';
}
