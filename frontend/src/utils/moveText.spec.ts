import { describe, expect, it } from 'vitest';
import { describeMove } from './moveText';
import { evaluationWords, pawnsWords } from './scoreText';
import { START } from '@/test/fixtures';

describe('describeMove', () => {
  it('beschreibt einfache Züge', () => {
    expect(describeMove(START, 'd2d4', 'd4')).toBe('Bauer von d2 nach d4');
    expect(describeMove(START, 'g1f3', 'Nf3')).toBe('Springer von g1 nach f3');
  });

  it('beschreibt Schlagzüge mit geschlagener Figur', () => {
    const fen = 'r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3';
    expect(describeMove(fen, 'f3e5', 'Nxe5')).toBe('Springer von f3 schlägt Bauer auf e5');
  });

  it('beschreibt Rochade, En passant, Umwandlung, Schach und Matt', () => {
    expect(describeMove('r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1', 'e1g1', 'O-O')).toBe('Kurze Rochade (König von e1 nach g1)');
    expect(describeMove('r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1', 'e8c8', 'O-O-O')).toBe('Lange Rochade (König von e8 nach c8)');
    expect(describeMove('4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1', 'e5d6', 'exd6')).toBe('Bauer von e5 schlägt Bauer en passant auf d6');
    expect(describeMove('8/4P3/8/8/8/k7/8/4K3 w - - 0 1', 'e7e8n', 'e8=N')).toBe('Bauer von e7 nach e8, Umwandlung in Springer');
    expect(describeMove('r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4', 'h5f7', 'Qxf7#'))
      .toBe('Dame von h5 schlägt Bauer auf f7 – Schachmatt');
    expect(describeMove('4k3/8/8/8/8/8/8/R3K3 w Q - 0 1', 'a1a8', 'Ra8+')).toBe('Turm von a1 nach a8 – Schach');
  });
});

describe('scoreText', () => {
  it('beschreibt Bewertungen in Worten', () => {
    expect(evaluationWords({ type: 'CP', value: 20, display: '+0.20', perspective: 'USER' })).toBe('ausgeglichen');
    expect(evaluationWords({ type: 'CP', value: -229, display: '-2.29', perspective: 'USER' })).toBe('der Gegner steht klar besser');
    expect(evaluationWords({ type: 'MATE', value: 2, display: 'M2', perspective: 'USER' })).toBe('du kannst in 2 Zügen mattsetzen');
    expect(pawnsWords(250)).toBe('etwa 2½ Bauern');
    expect(pawnsWords(10)).toBe('fast nichts');
  });
});
