import { describe, expect, it } from 'vitest';
import { ValidationError, validateAnalysis, validateGame } from './validate';
import { completedAnalysis, game } from '@/test/fixtures';

describe('validate', () => {
  it('akzeptiert gültige Partien und ergänzt fehlende Listen', () => {
    const raw = { ...game() } as Record<string, unknown>;
    delete raw.legalMoves;
    delete raw.moves;
    const g = validateGame(raw);
    expect(g.legalMoves).toEqual([]);
    expect(g.moves).toEqual([]);
  });

  it('lehnt unvollständige oder ungültige Antworten ab', () => {
    expect(() => validateGame(null)).toThrow(ValidationError);
    expect(() => validateGame({ ...game(), fen: 'kaputt' })).toThrow(ValidationError);
    expect(() => validateGame({ ...game(), playerColor: 'GREEN' })).toThrow(ValidationError);
  });

  it('setzt bei unvollständigen Analysen keine Ergebnisfelder voraus', () => {
    const a = validateAnalysis({ ply: 1, status: 'PENDING', fenBefore: 'x', moveId: 'm1' });
    expect(a.candidates).toBeNull();
    expect(a.progressPercent).toBe(0);
    expect(() => validateAnalysis({ ...completedAnalysis(), classification: undefined })).toThrow(ValidationError);
  });
});
