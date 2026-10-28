/**
 * Zentraler HTTP-Client. Fehlerantworten im Format application/problem+json werden in {@link ApiError}
 * übersetzt; Netzwerkfehler werden als "Backend nicht erreichbar" gekennzeichnet.
 */

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  positionVersion?: number;
  currentState?: unknown;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly problem: ProblemDetail;
  readonly offline: boolean;

  constructor(status: number, problem: ProblemDetail, offline = false) {
    super(problem.detail ?? problem.title ?? 'Unbekannter Fehler');
    this.status = status;
    this.code = problem.code ?? (offline ? 'OFFLINE' : 'UNKNOWN');
    this.problem = problem;
    this.offline = offline;
  }
}

const BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? '/api';
/**
 * Obergrenze für eine Anfrage. Länger als jede reguläre Engine-Suche (max. 20 s) und gleich dem
 * proxy_read_timeout von nginx; danach gilt das Backend als nicht erreichbar, statt dass die UI endlos wartet.
 */
export const REQUEST_TIMEOUT_MS = 60_000;

export async function request<T>(method: string, path: string, body?: unknown,
                                 validate?: (value: unknown) => T): Promise<T> {
  let response: Response;
  let text: string;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    response = await fetch(BASE + path, {
      method,
      headers: body === undefined ? { Accept: 'application/json' } : { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });
    text = await response.text();
  } catch {
    throw controller.signal.aborted
      ? new ApiError(0, { title: 'Backend nicht erreichbar', detail: 'Der Server antwortet nicht (Zeitüberschreitung). Bitte versuche es erneut.' }, true)
      : new ApiError(0, { title: 'Backend nicht erreichbar', detail: 'Der Server ist nicht erreichbar. Bitte prüfe, ob das Backend läuft.' }, true);
  } finally {
    clearTimeout(timer);
  }
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }
  if (!response.ok) {
    const problem = (data && typeof data === 'object' ? data : {}) as ProblemDetail;
    if (response.status >= 500 && !problem.code) {
      // Proxy- oder Gateway-Fehler ohne fachliche Fehlerbeschreibung: Backend nicht erreichbar.
      throw new ApiError(response.status, { title: 'Backend nicht erreichbar', detail: 'Der Server antwortet nicht.' }, true);
    }
    throw new ApiError(response.status, problem);
  }
  return validate ? validate(data) : (data as T);
}
