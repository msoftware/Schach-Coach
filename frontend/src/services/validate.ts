/**
 * Zentrale Validierung der API-Antworten (Requirements 10.2). Pflichtfelder werden geprüft; optionale
 * Felder werden auf sichere Standardwerte normalisiert, damit die UI keine fehlenden Felder voraussetzt.
 */
import type { Game, MoveResult, EngineStatus, Hint } from '@/models/game';
import type { MoveAnalysis } from '@/models/analysis';

export class ValidationError extends Error {
  constructor(message: string) {
    super(`Ungültige Serverantwort: ${message}`);
  }
}

type Obj = Record<string, unknown>;

function obj(value: unknown, what: string): Obj {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new ValidationError(`${what} ist kein Objekt`);
  }
  return value as Obj;
}

function str(o: Obj, key: string): string {
  const v = o[key];
  if (typeof v !== 'string') throw new ValidationError(`Feld "${key}" fehlt`);
  return v;
}

function num(o: Obj, key: string): number {
  const v = o[key];
  if (typeof v !== 'number' || Number.isNaN(v)) throw new ValidationError(`Feld "${key}" ist keine Zahl`);
  return v;
}

function arr<T>(o: Obj, key: string): T[] {
  const v = o[key];
  return Array.isArray(v) ? (v as T[]) : [];
}

const FEN = /^[pnbrqkPNBRQK1-8/]+ [wb] (-|[KQkq]+) (-|[a-h][36]) \d+ \d+$/;

export function validateGame(value: unknown): Game {
  const o = obj(value, 'Partie');
  const fen = str(o, 'fen');
  if (!FEN.test(fen)) throw new ValidationError('FEN der Partie ist ungültig');
  str(o, 'gameId');
  str(o, 'status');
  num(o, 'positionVersion');
  const color = str(o, 'playerColor');
  if (color !== 'WHITE' && color !== 'BLACK') throw new ValidationError('Spielerfarbe ist ungültig');
  return {
    ...(o as unknown as Game),
    moves: arr(o, 'moves'),
    legalMoves: arr(o, 'legalMoves'),
    lastMove: (o.lastMove as Game['lastMove']) ?? null,
    checkSquare: (o.checkSquare as string | null) ?? null,
    winner: (o.winner as Game['winner']) ?? null,
    canUndo: Boolean(o.canUndo),
    canRedo: Boolean(o.canRedo),
    engineThinking: Boolean(o.engineThinking),
  };
}

export function validateMoveResult(value: unknown): MoveResult {
  const o = obj(value, 'Zugergebnis');
  num(o, 'ply');
  str(o, 'san');
  return { ...(o as unknown as MoveResult), replayed: Boolean(o.replayed), game: validateGame(o.game) };
}

export function validateAnalysis(value: unknown): MoveAnalysis {
  const o = obj(value, 'Analyse');
  num(o, 'ply');
  const status = str(o, 'status');
  str(o, 'fenBefore');
  const completed = status === 'COMPLETED';
  if (completed) {
    str(o, 'classification');
    obj(o.scoreAfter, 'Bewertung nach dem Zug');
  }
  return {
    ...(o as unknown as MoveAnalysis),
    candidates: completed ? arr(o, 'candidates') : null,
    findings: completed ? arr(o, 'findings') : null,
    sections: completed ? arr(o, 'sections') : null,
    strategicPlans: completed ? arr(o, 'strategicPlans') : null,
    learningTags: completed ? arr(o, 'learningTags') : null,
    confidenceReasons: completed ? arr(o, 'confidenceReasons') : null,
    classificationReasons: completed ? arr(o, 'classificationReasons') : null,
    progressPercent: typeof o.progressPercent === 'number' ? o.progressPercent : 0,
    outdated: Boolean(o.outdated),
  };
}

export function validateEngineStatus(value: unknown): EngineStatus {
  const o = obj(value, 'Engine-Status');
  return { available: Boolean(o.available), engines: arr(o, 'engines'), analysisQueue: typeof o.analysisQueue === 'number' ? o.analysisQueue : 0 };
}

export function validateHint(value: unknown): Hint {
  const o = obj(value, 'Tipp');
  str(o, 'uci');
  str(o, 'san');
  return o as unknown as Hint;
}
