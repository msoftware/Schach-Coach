import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useGameStore } from './gameStore';
import { useAnalysisStore } from './analysisStore';
import { useSettingsStore } from './settingsStore';
import { localGameStorage } from '@/services/localGameStorage';
import { AFTER_E4, START, completedAnalysis, game, move } from '@/test/fixtures';

type Handler = (method: string, url: string, body: unknown) => { status: number; body: unknown };

function mockFetch(handler: Handler) {
  const calls: { method: string; url: string; body: unknown }[] = [];
  vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
    const body = init?.body ? JSON.parse(String(init.body)) : undefined;
    calls.push({ method: init?.method ?? 'GET', url, body });
    const r = handler(init?.method ?? 'GET', url, body);
    return new Response(JSON.stringify(r.body), { status: r.status, headers: { 'Content-Type': 'application/json' } });
  }));
  return calls;
}

beforeEach(async () => {
  setActivePinia(createPinia());
  localGameStorage.resetConnection();
  await localGameStorage.clearAll();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('gameStore', () => {
  it('startet eine neue Partie und speichert sie lokal', async () => {
    mockFetch(() => ({ status: 201, body: game() }));
    const store = useGameStore();
    await store.newGame();
    expect(store.game?.fen).toBe(START);
    expect(store.canMove).toBe(true);
    const stored = await localGameStorage.loadCurrentGame();
    expect(stored?.gameId).toBe(game().gameId);
    expect(store.lastSaved).not.toBeNull();
  });

  it('führt Benutzerzug aus, verfolgt die Analyse und fordert den Computerzug an', async () => {
    const settings = useSettingsStore();
    settings.waitForCoaching = false;
    const afterUser = game({ fen: AFTER_E4, sideToMove: 'BLACK', positionVersion: 2, moves: [move(1, 'e2e4', 'e4', true)], legalMoves: [] });
    const afterEngine = game({ positionVersion: 3, moves: [move(1, 'e2e4', 'e4', true), move(2, 'e7e5', 'e5', false, AFTER_E4)] });
    const calls = mockFetch((_method, url) => {
      if (url.endsWith('/moves')) return { status: 200, body: { gameId: 'x', positionVersion: 2, ply: 1, playedMove: 'e2e4', san: 'e4', fen: AFTER_E4, status: 'IN_PROGRESS', analysisStatus: 'PENDING', replayed: false, game: afterUser } };
      if (url.endsWith('/engine-move')) return { status: 200, body: { gameId: 'x', positionVersion: 3, ply: 2, playedMove: 'e7e5', san: 'e5', fen: START, status: 'IN_PROGRESS', analysisStatus: null, replayed: false, game: afterEngine } };
      if (url.includes('/analysis/1')) return { status: 200, body: completedAnalysis() };
      return { status: 201, body: game() };
    });
    const store = useGameStore();
    await store.newGame();
    await store.playMove('e2e4');
    expect(calls.find((c) => c.url.endsWith('/moves'))?.body).toEqual({ uciMove: 'e2e4', expectedPositionVersion: 1 });
    expect(calls.some((c) => c.url.endsWith('/engine-move'))).toBe(true);
    expect(store.game?.moves).toHaveLength(2);
    const analysis = useAnalysisStore();
    await vi.waitFor(() => expect(analysis.byPly[1]?.status).toBe('COMPLETED'));
    expect(analysis.selectedPly).toBe(1);
  });

  it('übernimmt bei Versionskonflikt den aktuellen Serverzustand', async () => {
    const current = game({ positionVersion: 7, fen: AFTER_E4, sideToMove: 'BLACK', legalMoves: [] });
    mockFetch((_method, url) => url.endsWith('/moves')
      ? { status: 409, body: { status: 409, code: 'POSITION_VERSION_CONFLICT', detail: 'veraltet', currentState: current } }
      : { status: 201, body: game() });
    const store = useGameStore();
    await store.newGame();
    await store.playMove('e2e4');
    expect(store.game?.positionVersion).toBe(7);
    expect(store.canMove).toBe(false);
  });

  it('zeigt an, wenn das Backend nicht erreichbar ist', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => {
      throw new TypeError('Failed to fetch');
    }));
    const store = useGameStore();
    await store.newGame();
    expect(store.offline).toBe(true);
    expect(store.error).toContain('nicht erreichbar');
  });

  it('stellt eine gespeicherte Partie aus der Zughistorie wieder her und vergleicht die FEN', async () => {
    await localGameStorage.saveCurrentGame({
      gameId: 'old', initialFen: START, moves: ['e2e4', 'e7e5'], sanMoves: ['e4', 'e5'], fen: 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2',
      playerColor: 'WHITE', difficulty: 'EASY', analysisProfile: 'NORMAL', status: 'IN_PROGRESS', result: '*', savedAt: '2026-01-01', finished: false,
    });
    const restoredGame = game({ fen: 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2', moves: [] });
    const calls = mockFetch(() => ({ status: 201, body: restoredGame }));
    const store = useGameStore();
    expect(await store.restoreFromStorage()).toBe('restored');
    expect(calls[0].body).toMatchObject({ moves: ['e2e4', 'e7e5'], difficulty: 'EASY' });

    mockFetch(() => ({ status: 201, body: game({ fen: START }) }));
    expect(await store.restoreFromStorage()).toBe('inconsistent');
  });

  it('verwirft Analysen nach Undo', async () => {
    const analysis = useAnalysisStore();
    analysis.byPly = { 1: completedAnalysis(), 3: completedAnalysis({ ply: 3 }) };
    analysis.discardFrom(3);
    expect(Object.keys(analysis.byPly)).toEqual(['1']);
  });
});

describe('analysisStore Varianten', () => {
  it('zeigt Varianten ohne die Partie zu verändern', () => {
    const analysis = useAnalysisStore();
    analysis.byPly = { 1: completedAnalysis() };
    analysis.select(1);
    analysis.showVariation(1, 'd2d4');
    expect(analysis.variationFen).toContain('3P4');
    analysis.stepVariation(1);
    expect(analysis.variationFen).toContain('3p4');
    analysis.stepVariation(-5);
    expect(analysis.variationFen).toBe(START);
    analysis.exitVariation();
    expect(analysis.variationFen).toBeNull();
  });
});

describe('localGameStorage', () => {
  it('meldet fehlgeschlagene Speicherung ehrlich', async () => {
    const original = globalThis.indexedDB;
    // @ts-expect-error – IndexedDB simuliert nicht verfügbar
    delete globalThis.indexedDB;
    localGameStorage.resetConnection();
    const result = await localGameStorage.saveCurrentGame({
      gameId: 'x', initialFen: START, moves: [], sanMoves: [], fen: START, playerColor: 'WHITE', difficulty: 'MEDIUM',
      analysisProfile: 'NORMAL', status: 'IN_PROGRESS', result: '*', savedAt: 'now', finished: false,
    });
    expect(result.ok).toBe(false);
    expect(await localGameStorage.isAvailable()).toBe(false);
    globalThis.indexedDB = original;
    localGameStorage.resetConnection();
  });

  it('speichert Lernstatistik und Analyse-Cache', async () => {
    await localGameStorage.cacheAnalysis('k', completedAnalysis());
    expect((await localGameStorage.getCachedAnalysis('k'))?.classification).toBe('INACCURACY');
    expect((await localGameStorage.getCachedAnalysis('other'))).toBeUndefined();
  });
});
