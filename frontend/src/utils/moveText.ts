/**
 * Wandelt einen Zug in einen leicht lesbaren deutschen Satz um, z. B. "Bauer von d2 nach d4" oder
 * "Springer von f3 schlägt Bauer auf e5". Benötigt nur die Stellung vor dem Zug, den UCI-Zug und die SAN
 * (für Schach/Matt); Schachregeln werden dabei nicht ausgewertet.
 */
import { parseBoard, pieceName } from './fen';

const PROMOTION: Record<string, string> = { q: 'Dame', r: 'Turm', b: 'Läufer', n: 'Springer' };

export function describeMove(fenBefore: string, uci: string, san?: string | null): string {
  const board = parseBoard(fenBefore);
  const from = uci.slice(0, 2);
  const to = uci.slice(2, 4);
  const piece = board[from];
  if (!piece) return san ?? uci;

  let text: string;
  const fileDistance = Math.abs(from.charCodeAt(0) - to.charCodeAt(0));
  if (piece[1] === 'K' && fileDistance === 2) {
    text = to[0] === 'g' ? `Kurze Rochade (König von ${from} nach ${to})` : `Lange Rochade (König von ${from} nach ${to})`;
  } else {
    const target = board[to];
    const name = pieceName(piece);
    if (target) {
      text = `${name} von ${from} schlägt ${pieceName(target)} auf ${to}`;
    } else if (piece[1] === 'P' && fileDistance === 1) {
      text = `${name} von ${from} schlägt Bauer en passant auf ${to}`;
    } else {
      text = `${name} von ${from} nach ${to}`;
    }
  }
  const promotion = uci[4];
  if (promotion && PROMOTION[promotion]) text += `, Umwandlung in ${PROMOTION[promotion]}`;
  if (san?.endsWith('#')) text += ' – Schachmatt';
  else if (san?.endsWith('+')) text += ' – Schach';
  return text;
}
