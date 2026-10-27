/** Typen der Coaching-Befunde und Erklärungen. */

export interface Evidence {
  startFen: string | null;
  triggerMove: string | null;
  line: string[];
  materialBefore: number | null;
  materialAfter: number | null;
  outcome: string | null;
  certainty: 'VERIFIED' | 'HEURISTIC';
  confidence: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface CoachingFinding {
  ruleId: string;
  category: 'TACTICAL' | 'STRATEGIC' | 'ENGINE';
  /** ENGINE: von der Schach-Engine berechnet; COACHING_RULE: von der eigenen Coaching-Logik abgeleitet. */
  source: 'ENGINE' | 'COACHING_RULE';
  severity: 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  polarity: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  certainty: 'VERIFIED' | 'HEURISTIC';
  title: string;
  explanation: string;
  affectedSquares: string[];
  affectedPieces: string[];
  evidence: Evidence | null;
  relatedVariationIds: string[];
  learningPrinciple: string | null;
  overriddenByTactics: boolean;
  score: number;
}

export interface StrategicPlan {
  side: 'USER' | 'OPPONENT';
  title: string;
  explanation: string;
  squares: string[];
  basis: string;
  source: 'ENGINE' | 'COACHING_RULE';
}

export interface ExplanationStatement {
  text: string;
  sources: string[];
}

export interface ExplanationSection {
  key: string;
  question: string;
  statements: ExplanationStatement[];
}

export interface CoachingTip {
  title: string;
  observation: string;
  why: string;
  betterAlternative: string;
  expectedConsequence: string;
  learningPrinciple: string;
  confidence: string;
}

/** Lokale Lernstatistik (nur im Browser gespeichert). */
export interface LearningStats {
  id: 'stats';
  analysedMoves: number;
  classifications: Record<string, number>;
  ruleCounts: Record<string, { title: string; count: number; lastSeen: string }>;
  updatedAt: string;
}

export function sourceLabel(source: string): string {
  if (source === 'ENGINE') return 'Engine';
  if (source.startsWith('TACT')) return `Taktikregel ${source}`;
  if (source.startsWith('STRAT')) return `Strategieregel ${source}`;
  return source;
}
