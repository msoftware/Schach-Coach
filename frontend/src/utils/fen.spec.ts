import { describe, expect, it } from 'vitest';
import { isLightSquare, orderedSquares, parseBoard, pieceLabel, sideToMove } from './fen';
import { START } from '@/test/fixtures';

describe('fen', () => {
  it('liest die Figurenstellung', () => {
    const board = parseBoard(START);
    expect(board.e1).toBe('wK');
    expect(board.d8).toBe('bQ');
    expect(board.e4).toBeUndefined();
    expect(Object.keys(board)).toHaveLength(32);
    expect(sideToMove(START)).toBe('WHITE');
  });

  it('h1 ist hell und a1 dunkel', () => {
    expect(isLightSquare('h1')).toBe(true);
    expect(isLightSquare('a1')).toBe(false);
  });

  it('Weiß steht in der Standardorientierung unten', () => {
    const white = orderedSquares('WHITE');
    expect(white[0]).toBe('a8');
    expect(white[63]).toBe('h1');
    const black = orderedSquares('BLACK');
    expect(black[0]).toBe('h1');
    expect(black[63]).toBe('a8');
  });

  it('beschriftet Figuren auf Deutsch', () => {
    expect(pieceLabel('wN')).toBe('weißer Springer');
    expect(pieceLabel('bQ')).toBe('schwarze Dame');
  });
});
