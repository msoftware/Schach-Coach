/** Bewertungen in einfacher Sprache (gleiche Abstufung wie im Backend, GermanText.evaluationWords). */
import type { Score } from '@/models/analysis';

export function evaluationWords(score: Score | null | undefined): string {
  if (!score) return 'unklar';
  if (score.type === 'MATE') {
    if (score.value > 0) return `du kannst in ${score.value} ${score.value === 1 ? 'Zug' : 'Zügen'} mattsetzen`;
    if (score.value === 0) return 'du bist matt';
    const n = -score.value;
    return `der Gegner kann dich in ${n} ${n === 1 ? 'Zug' : 'Zügen'} mattsetzen`;
  }
  const cp = score.value;
  const abs = Math.abs(cp);
  const you = cp > 0;
  if (abs < 50) return 'ausgeglichen';
  if (abs < 120) return you ? 'du stehst etwas besser' : 'der Gegner steht etwas besser';
  if (abs < 250) return you ? 'du stehst klar besser' : 'der Gegner steht klar besser';
  if (abs < 600) return you ? 'du stehst deutlich besser' : 'der Gegner steht deutlich besser';
  return you ? 'du stehst auf Gewinn' : 'der Gegner steht auf Gewinn';
}

/** Farbe passend zur Bewertung aus Sicht des Benutzers. */
export function evaluationColor(score: Score | null | undefined): string {
  if (!score) return 'text-grey-8';
  const v = score.type === 'MATE' ? (score.value > 0 ? 1000 : -1000) : score.value;
  if (v >= 50) return 'text-positive';
  if (v <= -50) return 'text-negative';
  return 'text-grey-9';
}

/** Centipawns als ungefährer Bauernwert, z. B. 250 → "etwa 2½ Bauern". */
export function pawnsWords(centipawns: number): string {
  const halves = Math.round(Math.abs(centipawns) / 50);
  if (halves <= 0) return 'fast nichts';
  if (halves === 1) return 'etwa einen halben Bauern';
  const whole = Math.floor(halves / 2);
  const half = halves % 2 === 1;
  if (whole === 1 && !half) return 'etwa einen Bauern';
  return `etwa ${whole}${half ? '½' : ''} Bauern`;
}
