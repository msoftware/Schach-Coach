/**
 * Liest nur die Figurenstellung aus einer FEN für die Darstellung. Das Frontend implementiert bewusst keine
 * Schachregeln: Legale Züge, Schach, Matt und Remis liefert ausschließlich das Backend.
 */

export type PieceCode = 'wK' | 'wQ' | 'wR' | 'wB' | 'wN' | 'wP' | 'bK' | 'bQ' | 'bR' | 'bB' | 'bN' | 'bP';

export const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'] as const;

const NAMES: Record<string, string> = { K: 'König', Q: 'Dame', R: 'Turm', B: 'Läufer', N: 'Springer', P: 'Bauer' };

/** Abbildung Feldname → Figur, z. B. { e1: 'wK' }. */
export function parseBoard(fen: string): Record<string, PieceCode> {
  const placement = fen.split(' ')[0] ?? '';
  const rows = placement.split('/');
  const board: Record<string, PieceCode> = {};
  rows.forEach((row, rowIndex) => {
    const rank = 8 - rowIndex;
    let file = 0;
    for (const ch of row) {
      if (/\d/.test(ch)) {
        file += Number(ch);
      } else if (file < 8) {
        const color = ch === ch.toUpperCase() ? 'w' : 'b';
        board[`${FILES[file]}${rank}`] = `${color}${ch.toUpperCase()}` as PieceCode;
        file++;
      }
    }
  });
  return board;
}

export function sideToMove(fen: string): 'WHITE' | 'BLACK' {
  return fen.split(' ')[1] === 'b' ? 'BLACK' : 'WHITE';
}

/** Deutsche Beschriftung, z. B. "weißer Springer" oder "schwarze Dame". */
export function pieceLabel(code: PieceCode): string {
  const white = code[0] === 'w';
  const kind = code[1];
  const feminine = kind === 'Q';
  const adjective = white ? 'weiß' : 'schwarz';
  return `${adjective}${feminine ? 'e' : 'er'} ${NAMES[kind]}`;
}

/** Nur die Figurenart, z. B. "Springer". */
export function pieceName(code: PieceCode): string {
  return NAMES[code[1]];
}

/** Felder in Darstellungsreihenfolge (oben links zuerst). */
export function orderedSquares(orientation: 'WHITE' | 'BLACK'): string[] {
  const squares: string[] = [];
  const ranks = orientation === 'WHITE' ? [8, 7, 6, 5, 4, 3, 2, 1] : [1, 2, 3, 4, 5, 6, 7, 8];
  const files = orientation === 'WHITE' ? [...FILES] : [...FILES].reverse();
  for (const r of ranks) {
    for (const f of files) {
      squares.push(`${f}${r}`);
    }
  }
  return squares;
}

/** a1 ist dunkel, h1 hell. */
export function isLightSquare(square: string): boolean {
  const file = FILES.indexOf(square[0] as (typeof FILES)[number]);
  const rank = Number(square[1]) - 1;
  return (file + rank) % 2 === 1;
}
