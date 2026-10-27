import type { Game, LegalMove, MoveRecord } from '@/models/game';
import type { MoveAnalysis } from '@/models/analysis';

export const START = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
export const AFTER_E4 = 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1';

export function legal(uci: string, san: string, promotion: LegalMove['promotion'] = null): LegalMove {
  return { uci, san, from: uci.slice(0, 2), to: uci.slice(2, 4), promotion };
}

export function move(ply: number, uci: string, san: string, byUser: boolean, fenBefore = START, fenAfter = AFTER_E4): MoveRecord {
  return {
    ply, moveId: `m${ply}`, uci, san, from: uci.slice(0, 2), to: uci.slice(2, 4), color: ply % 2 === 1 ? 'WHITE' : 'BLACK',
    byUser, fenBefore, fenAfter, positionVersion: ply + 1, analysisStatus: byUser ? 'PENDING' : null, analysisOutdated: false,
    timestamp: '2026-01-01T00:00:00Z',
  };
}

export function game(overrides: Partial<Game> = {}): Game {
  return {
    apiVersion: '1', gameId: 'a'.repeat(32), status: 'IN_PROGRESS', statusText: 'Weiß ist am Zug.', result: '*', winner: null,
    playerColor: 'WHITE', sideToMove: 'WHITE', fen: START, initialFen: START, positionVersion: 1, difficulty: 'MEDIUM',
    analysisProfile: 'NORMAL', settingsVersion: 0, inCheck: false, checkSquare: null, lastMove: null, moves: [],
    legalMoves: [legal('e2e4', 'e4'), legal('d2d4', 'd4'), legal('g1f3', 'Nf3')], canUndo: false, canRedo: false,
    engineThinking: false, engineStatus: 'READY', ...overrides,
  };
}

export function completedAnalysis(overrides: Partial<MoveAnalysis> = {}): MoveAnalysis {
  return {
    apiVersion: '1', gameId: 'a'.repeat(32), ply: 1, moveId: 'm1', playedMove: 'e4', playedMoveUci: 'e2e4', fenBefore: START,
    fenAfter: AFTER_E4, positionVersion: 2, currentPositionVersion: 2, status: 'COMPLETED', stage: 'Abgeschlossen',
    progressPercent: 100, errorMessage: null, outdated: false, settingsVersion: 0, depthLevel: 1,
    classification: 'INACCURACY', classificationLabel: 'Ungenau', classificationProvisional: false, classificationReasons: ['Testdaten'],
    explanationType: 'STRATEGIC_INACCURACY', explanationTypeLabel: 'Strategische Ungenauigkeit', headline: 'Ungenau: 1.e4',
    mainReason: 'Testbegründung aus Testdaten.', mainReasonSources: ['STRAT-002'],
    scoreBefore: { type: 'CP', value: 30, display: '+0.30', perspective: 'USER' },
    scoreAfter: { type: 'CP', value: -50, display: '-0.50', perspective: 'USER' },
    bestScore: { type: 'CP', value: 30, display: '+0.30', perspective: 'USER' },
    evaluationLoss: { centipawns: 80, winProbabilityLoss: 0.1, reliable: true },
    bestMove: 'd4', bestMoveUci: 'd2d4', bestMoveExplanation: null, principalVariation: ['d4', 'd5'],
    candidates: [
      {
        variationId: 'v-d2d4', uci: 'd2d4', san: 'd4', score: { type: 'CP', value: 30, display: '+0.30', perspective: 'USER' }, depth: 12,
        nodes: 1000, line: [
          { uci: 'd2d4', san: 'd4', numbered: '1.d4', fenAfter: 'rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1' },
          { uci: 'd7d5', san: 'd5', numbered: '1...d5', fenAfter: 'rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2' },
        ], lineTruncated: false, played: false, best: true, deltaToPlayed: 80, motifs: [], strategicPros: ['Zentrumskontrolle'],
        strategicCons: [], findings: [], comparison: 'Um 80 Centipawns besser als dein Zug.', searchLine: true,
      },
    ],
    findings: [], strategicPlans: [], sections: [
      { key: 'assessment', question: 'Was war gut, was war schlecht?', statements: [{ text: 'Testaussage', sources: ['ENGINE'] }] },
    ],
    tip: null, learningTags: ['STRAT-002'], material: { before: 0, after: 0, delta: 0 }, analysisConfidence: 'HIGH',
    confidenceReasons: [], critical: false,
    engine: { name: 'Calvin', version: '6.1.1', searchLimits: 'Test', nodes: 1, elapsedMillis: 1, rootDepth: 10 },
    rulesVersion: '1.0.0', ...overrides,
  };
}
