/**
 * Lokale Speicherung ausschließlich im Browser (IndexedDB). Es gibt keine serverseitige Partiespeicherung.
 * Jede Schreiboperation meldet zurück, ob sie wirklich erfolgreich war, damit die UI nie eine
 * erfolgreiche Sicherung behauptet, die nicht stattgefunden hat (FR-07).
 */
import { openDB, type DBSchema, type IDBPDatabase } from 'idb';
import type { AnalysisProfile, Color, Difficulty, GameStatus } from '@/models/game';
import type { MoveAnalysis } from '@/models/analysis';
import type { LearningStats } from '@/models/coaching';

export const DB_NAME = 'chess-coach';
export const DB_VERSION = 1;
export const CURRENT_GAME_ID = 'current';

export interface StoredGame {
  id: string;
  gameId: string;
  initialFen: string;
  moves: string[];
  sanMoves: string[];
  fen: string;
  playerColor: Color;
  difficulty: Difficulty;
  analysisProfile: AnalysisProfile;
  status: GameStatus;
  result: string;
  savedAt: string;
  finished: boolean;
}

export interface CachedAnalysis {
  key: string;
  savedAt: string;
  analysis: MoveAnalysis;
}

interface CoachDB extends DBSchema {
  games: { key: string; value: StoredGame; indexes: { savedAt: string } };
  settings: { key: string; value: { id: string; value: unknown } };
  learningStats: { key: string; value: LearningStats };
  analysisCache: { key: string; value: CachedAnalysis };
}

export interface SaveResult {
  ok: boolean;
  error?: string;
}

let dbPromise: Promise<IDBPDatabase<CoachDB>> | null = null;

function db(): Promise<IDBPDatabase<CoachDB>> {
  if (typeof indexedDB === 'undefined') {
    return Promise.reject(new Error('IndexedDB ist in diesem Browser nicht verfügbar.'));
  }
  if (!dbPromise) {
    dbPromise = openDB<CoachDB>(DB_NAME, DB_VERSION, {
      upgrade(database) {
        const games = database.createObjectStore('games', { keyPath: 'id' });
        games.createIndex('savedAt', 'savedAt');
        database.createObjectStore('settings', { keyPath: 'id' });
        database.createObjectStore('learningStats', { keyPath: 'id' });
        database.createObjectStore('analysisCache', { keyPath: 'key' });
      },
    }).catch((e) => {
      dbPromise = null;
      throw e;
    });
  }
  return dbPromise;
}

function failure(e: unknown): SaveResult {
  return { ok: false, error: e instanceof Error ? e.message : 'Unbekannter Speicherfehler' };
}

/** Schlüssel des Analyse-Caches: Stellung, Zug, Engine-Version, Analyseparameter und Regelversion. */
export function analysisCacheKey(fenBefore: string, uci: string, engineVersion: string, profile: AnalysisProfile,
                                 depthLevel: number, rulesVersion: string): string {
  return [fenBefore, uci, engineVersion, profile, depthLevel, rulesVersion].join('|');
}

export const localGameStorage = {
  async isAvailable(): Promise<boolean> {
    try {
      await db();
      return true;
    } catch {
      return false;
    }
  },

  async saveCurrentGame(game: Omit<StoredGame, 'id'>): Promise<SaveResult> {
    try {
      const d = await db();
      await d.put('games', { ...game, id: CURRENT_GAME_ID });
      return { ok: true };
    } catch (e) {
      return failure(e);
    }
  },

  async loadCurrentGame(): Promise<StoredGame | undefined> {
    try {
      return await (await db()).get('games', CURRENT_GAME_ID);
    } catch {
      return undefined;
    }
  },

  async clearCurrentGame(): Promise<SaveResult> {
    try {
      await (await db()).delete('games', CURRENT_GAME_ID);
      return { ok: true };
    } catch (e) {
      return failure(e);
    }
  },

  /** Archiviert eine abgeschlossene Partie unter eigener Kennung. */
  async archiveGame(game: Omit<StoredGame, 'id'>): Promise<SaveResult> {
    try {
      await (await db()).put('games', { ...game, id: `archive-${game.gameId}-${game.savedAt}`, finished: true });
      return { ok: true };
    } catch (e) {
      return failure(e);
    }
  },

  async listArchivedGames(): Promise<StoredGame[]> {
    try {
      const all = await (await db()).getAll('games');
      return all.filter((g) => g.id !== CURRENT_GAME_ID).sort((a, b) => b.savedAt.localeCompare(a.savedAt));
    } catch {
      return [];
    }
  },

  async saveSetting(id: string, value: unknown): Promise<SaveResult> {
    try {
      await (await db()).put('settings', { id, value });
      return { ok: true };
    } catch (e) {
      return failure(e);
    }
  },

  async loadLearningStats(): Promise<LearningStats | undefined> {
    try {
      return await (await db()).get('learningStats', 'stats');
    } catch {
      return undefined;
    }
  },

  async saveLearningStats(stats: LearningStats): Promise<SaveResult> {
    try {
      await (await db()).put('learningStats', stats);
      return { ok: true };
    } catch (e) {
      return failure(e);
    }
  },

  async getCachedAnalysis(key: string): Promise<MoveAnalysis | undefined> {
    try {
      return (await (await db()).get('analysisCache', key))?.analysis;
    } catch {
      return undefined;
    }
  },

  async cacheAnalysis(key: string, analysis: MoveAnalysis): Promise<SaveResult> {
    try {
      await (await db()).put('analysisCache', { key, analysis, savedAt: new Date().toISOString() });
      return { ok: true };
    } catch (e) {
      return failure(e);
    }
  },

  /** Löscht alle lokal gespeicherten Daten der Anwendung (FR-08). */
  async clearAll(): Promise<SaveResult> {
    try {
      const d = await db();
      await Promise.all([d.clear('games'), d.clear('settings'), d.clear('learningStats'), d.clear('analysisCache')]);
      try {
        localStorage.removeItem('chess-coach-settings');
      } catch {
        // localStorage nicht verfügbar – nichts zu löschen.
      }
      return { ok: true };
    } catch (e) {
      return failure(e);
    }
  },

  /** Nur für Tests. */
  resetConnection(): void {
    dbPromise = null;
  },
};
