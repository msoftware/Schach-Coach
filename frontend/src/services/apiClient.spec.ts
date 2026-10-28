import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, REQUEST_TIMEOUT_MS, request } from './apiClient';

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe('apiClient', () => {
  it('bricht eine hängende Anfrage nach der Zeitgrenze ab, statt endlos zu warten', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('fetch', vi.fn((_url: string, init?: RequestInit) => new Promise<Response>((_resolve, reject) => {
      init?.signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
    })));
    const pending = request('POST', '/games', {}).catch((e: unknown) => e);
    await vi.advanceTimersByTimeAsync(REQUEST_TIMEOUT_MS + 1);
    const error = await pending;
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).offline).toBe(true);
    expect((error as ApiError).message).toContain('Zeitüberschreitung');
  });

  it('lässt normale Antworten unberührt', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ ok: 1 }), { status: 200 })));
    expect(await request<{ ok: number }>('GET', '/health')).toEqual({ ok: 1 });
  });
});
