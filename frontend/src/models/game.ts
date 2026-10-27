/** Typen der Partie-API (Vertrag: backend dto.response.GameResponse, API-Version 1). */

export type Color = 'WHITE' | 'BLACK';
export type Difficulty = 'BEGINNER' | 'EASY' | 'MEDIUM' | 'STRONG' | 'MAXIMUM';
export type AnalysisProfile = 'FAST' | 'NORMAL' | 'DEEP';
export type GameStatus =
  | 'IN_PROGRESS'
  | 'CHECKMATE'
  | 'STALEMATE'
  | 'DRAW_INSUFFICIENT_MATERIAL'
  | 'DRAW_THREEFOLD_REPETITION'
  | 'DRAW_FIFTY_MOVE_RULE'
  | 'RESIGNED';
export type AnalysisStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface LegalMove {
  uci: string;
  san: string;
  from: string;
  to: string;
  promotion: 'q' | 'r' | 'b' | 'n' | null;
}

export interface MoveRecord {
  ply: number;
  moveId: string;
  uci: string;
  san: string;
  from: string;
  to: string;
  color: Color;
  byUser: boolean;
  fenBefore: string;
  fenAfter: string;
  positionVersion: number;
  analysisStatus: AnalysisStatus | null;
  analysisOutdated: boolean;
  timestamp: string;
}

export interface Game {
  apiVersion: string;
  gameId: string;
  status: GameStatus;
  statusText: string;
  result: string;
  winner: Color | null;
  playerColor: Color;
  sideToMove: Color;
  fen: string;
  initialFen: string;
  positionVersion: number;
  difficulty: Difficulty;
  analysisProfile: AnalysisProfile;
  settingsVersion: number;
  inCheck: boolean;
  checkSquare: string | null;
  lastMove: { from: string; to: string } | null;
  moves: MoveRecord[];
  legalMoves: LegalMove[];
  canUndo: boolean;
  canRedo: boolean;
  engineThinking: boolean;
  engineStatus: 'READY' | 'UNAVAILABLE';
}

export interface MoveResult {
  gameId: string;
  positionVersion: number;
  ply: number;
  playedMove: string;
  san: string;
  fen: string;
  status: GameStatus;
  analysisStatus: AnalysisStatus | null;
  replayed: boolean;
  game: Game;
}

export interface EngineInstance {
  role: string;
  name: string;
  author: string;
  version: string;
  state: string;
  message: string;
  supportsMultiPv: boolean;
}

export interface EngineStatus {
  available: boolean;
  engines: EngineInstance[];
  analysisQueue: number;
}

/** Bewertung der aktuellen Stellung für die Bewertungsleiste, immer aus Sicht von Weiß. */
export interface Evaluation {
  gameId: string;
  positionVersion: number;
  fen: string;
  /** null bei beendeter Partie */
  score: { type: 'CP' | 'MATE'; value: number; display: string; perspective: 'WHITE' } | null;
  /** 0 = Schwarz gewinnt, 0.5 = ausgeglichen, 1 = Weiß gewinnt */
  whiteShare: number;
  display: string;
  depth: number;
  finished: boolean;
}

export interface Hint {
  uci: string;
  san: string;
  from: string;
  pieceName: string;
  score: string | null;
  depth: number;
  source: string;
}

export interface DifficultyOption {
  id: Difficulty;
  label: string;
  nodes: number | null;
  depth: number | null;
  movetimeMillis: number | null;
  sampleFraction: number | null;
  description: string;
}

export interface SettingsOptions {
  difficulties: DifficultyOption[];
  analysisProfiles: { id: AnalysisProfile; label: string; factor: number }[];
  rulesVersion: string;
}

export const DIFFICULTY_LABELS: Record<Difficulty, string> = {
  BEGINNER: 'Anfänger',
  EASY: 'Leicht',
  MEDIUM: 'Mittel',
  STRONG: 'Stark',
  MAXIMUM: 'Maximal',
};

export const PROFILE_LABELS: Record<AnalysisProfile, string> = {
  FAST: 'Schnell',
  NORMAL: 'Normal',
  DEEP: 'Gründlich',
};

export function isFinished(status: GameStatus): boolean {
  return status !== 'IN_PROGRESS';
}
