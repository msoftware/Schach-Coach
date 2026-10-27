import { request } from './apiClient';
import { validateEngineStatus, validateEvaluation, validateGame, validateHint, validateMoveResult } from './validate';
import type { AnalysisProfile, Color, Difficulty, EngineStatus, Evaluation, Game, Hint, MoveResult, SettingsOptions } from '@/models/game';

export interface CreateGameOptions {
  playerColor: Color;
  difficulty: Difficulty;
  analysisProfile: AnalysisProfile;
  initialFen?: string;
  moves?: string[];
}

export const gameApi = {
  create: (options: CreateGameOptions): Promise<Game> => request('POST', '/games', options, validateGame),
  get: (gameId: string): Promise<Game> => request('GET', `/games/${gameId}`, undefined, validateGame),
  move: (gameId: string, uciMove: string, expectedPositionVersion: number): Promise<MoveResult> =>
    request('POST', `/games/${gameId}/moves`, { uciMove, expectedPositionVersion }, validateMoveResult),
  engineMove: (gameId: string, expectedPositionVersion: number): Promise<MoveResult> =>
    request('POST', `/games/${gameId}/engine-move`, { expectedPositionVersion }, validateMoveResult),
  undo: (gameId: string, expectedPositionVersion: number): Promise<Game> =>
    request('POST', `/games/${gameId}/undo`, { expectedPositionVersion }, validateGame),
  redo: (gameId: string, expectedPositionVersion: number): Promise<Game> =>
    request('POST', `/games/${gameId}/redo`, { expectedPositionVersion }, validateGame),
  restart: (gameId: string, expectedPositionVersion: number, playerColor: Color, difficulty: Difficulty): Promise<Game> =>
    request('POST', `/games/${gameId}/restart`, { expectedPositionVersion, playerColor, difficulty }, validateGame),
  resign: (gameId: string, expectedPositionVersion: number): Promise<Game> =>
    request('POST', `/games/${gameId}/resign`, { expectedPositionVersion }, validateGame),
  hint: (gameId: string): Promise<Hint> => request('POST', `/games/${gameId}/hint`, undefined, validateHint),
  evaluate: (gameId: string): Promise<Evaluation> => request('POST', `/games/${gameId}/evaluation`, undefined, validateEvaluation),
  engineStatus: (): Promise<EngineStatus> => request('GET', '/engine/status', undefined, validateEngineStatus),
  settingsOptions: (): Promise<SettingsOptions> => request('GET', '/settings/options'),
  updateSettings: (gameId: string, difficulty: Difficulty, analysisProfile: AnalysisProfile) =>
    request<{ settingsVersion: number; outdatedAnalyses: string[] }>('PUT', '/settings', { gameId, difficulty, analysisProfile }),
};
