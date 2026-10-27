/** Typen der Analyse-API (Vertrag: backend dto.response.AnalysisResponse, API-Version 1). */
import type { AnalysisStatus } from './game';
import type { CoachingFinding, StrategicPlan, ExplanationSection, CoachingTip } from './coaching';

export interface Score {
  type: 'CP' | 'MATE';
  value: number;
  display: string;
  perspective: 'USER';
}

export interface VariationMove {
  uci: string;
  san: string;
  numbered: string;
  fenAfter: string;
}

export interface Candidate {
  variationId: string;
  uci: string;
  san: string;
  score: Score;
  depth: number;
  nodes: number;
  line: VariationMove[];
  lineTruncated: boolean;
  played: boolean;
  best: boolean;
  deltaToPlayed: number | null;
  motifs: string[];
  strategicPros: string[];
  strategicCons: string[];
  findings: CoachingFinding[];
  comparison: string;
  searchLine: boolean;
}

export type Classification =
  | 'BEST'
  | 'EXCELLENT'
  | 'GOOD'
  | 'INACCURACY'
  | 'MISTAKE'
  | 'BLUNDER'
  | 'FORCED'
  | 'UNCERTAIN';

/** Ergebnisfelder sind nur bei status === 'COMPLETED' gesetzt und werden sonst als null geliefert. */
export interface MoveAnalysis {
  apiVersion: string;
  gameId: string;
  ply: number;
  moveId: string;
  playedMove: string;
  playedMoveUci: string;
  fenBefore: string;
  fenAfter: string;
  positionVersion: number;
  currentPositionVersion: number;
  status: AnalysisStatus;
  stage: string | null;
  progressPercent: number;
  errorMessage: string | null;
  outdated: boolean;
  settingsVersion: number;
  depthLevel: number;
  classification: Classification | null;
  classificationLabel: string | null;
  classificationProvisional: boolean | null;
  classificationReasons: string[] | null;
  explanationType: string | null;
  explanationTypeLabel: string | null;
  headline: string | null;
  mainReason: string | null;
  mainReasonSources: string[] | null;
  scoreBefore: Score | null;
  scoreAfter: Score | null;
  bestScore: Score | null;
  evaluationLoss: { centipawns: number | null; winProbabilityLoss: number; reliable: boolean } | null;
  bestMove: string | null;
  bestMoveUci: string | null;
  bestMoveExplanation: string | null;
  principalVariation: string[] | null;
  candidates: Candidate[] | null;
  findings: CoachingFinding[] | null;
  strategicPlans: StrategicPlan[] | null;
  sections: ExplanationSection[] | null;
  tip: CoachingTip | null;
  learningTags: string[] | null;
  material: { before: number; after: number; delta: number } | null;
  analysisConfidence: 'HIGH' | 'MEDIUM' | 'LOW' | null;
  confidenceReasons: string[] | null;
  critical: boolean | null;
  engine: { name: string; version: string; searchLimits: string; nodes: number; elapsedMillis: number; rootDepth: number } | null;
  rulesVersion: string | null;
}

export const CLASSIFICATION_COLORS: Record<Classification, string> = {
  BEST: 'positive',
  EXCELLENT: 'positive',
  GOOD: 'teal-8',
  INACCURACY: 'orange-9',
  MISTAKE: 'deep-orange-10',
  BLUNDER: 'negative',
  FORCED: 'blue-grey-7',
  UNCERTAIN: 'grey-7',
};

export const CLASSIFICATION_ICONS: Record<Classification, string> = {
  BEST: 'star',
  EXCELLENT: 'thumb_up',
  GOOD: 'check',
  INACCURACY: 'help',
  MISTAKE: 'error_outline',
  BLUNDER: 'report',
  FORCED: 'lock',
  UNCERTAIN: 'question_mark',
};

export function isFinalStatus(status: AnalysisStatus): boolean {
  return status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED';
}
