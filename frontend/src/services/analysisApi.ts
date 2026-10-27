import { request } from './apiClient';
import { validateAnalysis } from './validate';
import type { MoveAnalysis } from '@/models/analysis';

export const analysisApi = {
  get: (gameId: string, ply: number): Promise<MoveAnalysis> =>
    request('GET', `/games/${gameId}/analysis/${ply}`, undefined, validateAnalysis),
  request: (gameId: string, ply: number, force = false): Promise<MoveAnalysis> =>
    request('POST', `/games/${gameId}/analysis/${ply}${force ? '?force=true' : ''}`, undefined, validateAnalysis),
  deepen: (gameId: string, ply: number): Promise<MoveAnalysis> =>
    request('POST', `/games/${gameId}/analysis/${ply}/variations`, undefined, validateAnalysis),
};
