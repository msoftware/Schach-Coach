/** Texte und Darstellung der Bewertungsleiste („wer liegt wie weit vorne“). */
import type { Color, Evaluation } from '@/models/game';

/** Sichtbarer Mindestanteil je Seite, damit die Leiste bei klaren Vorteilen nicht ganz verschwindet. */
const MIN_VISIBLE = 0.03;
/** Unterhalb dieses Vorteils (Centipawns) gilt die Stellung wie im Zug-Coaching als ausgeglichen. */
const BALANCED_BELOW = 50;

/** Position des Schiebers in Prozent (0 = ganz bei Schwarz, 100 = ganz bei Weiß). */
export function whitePercent(evaluation: Evaluation | null): number {
  if (!evaluation) return 50;
  const share = evaluation.finished || evaluation.score?.type === 'MATE'
    ? evaluation.whiteShare
    : Math.min(1 - MIN_VISIBLE, Math.max(MIN_VISIBLE, evaluation.whiteShare));
  return Math.round(share * 1000) / 10;
}

function sideName(white: boolean): string {
  return white ? 'Weiß' : 'Schwarz';
}

/** Kurzer Satz, z. B. „Weiß liegt leicht vorne (+0.45)“ oder „Schwarz setzt in 3 Zügen matt“. */
export function evaluationSentence(evaluation: Evaluation | null): string {
  if (!evaluation) return 'Die Stellung wird bewertet …';
  if (evaluation.finished) {
    if (evaluation.whiteShare === 0.5) return `Partie beendet: Remis (${evaluation.display})`;
    return `Partie beendet: ${sideName(evaluation.whiteShare > 0.5)} hat gewonnen (${evaluation.display})`;
  }
  const score = evaluation.score;
  if (!score) return 'Keine Bewertung verfügbar';
  if (score.type === 'MATE') {
    const n = Math.abs(score.value);
    return `${sideName(score.value > 0)} setzt in ${n} ${n === 1 ? 'Zug' : 'Zügen'} matt`;
  }
  const cp = Math.abs(score.value);
  const shown = `${score.value >= 0 ? '+' : '−'}${(cp / 100).toFixed(2)}`;
  if (cp < BALANCED_BELOW) return `Ausgeglichen (${shown})`;
  // Gleiche Abstufung wie die Coaching-Texte (scoreText.evaluationWords).
  const grade = cp < 120 ? 'leicht vorne' : cp < 250 ? 'klar vorne' : cp < 600 ? 'deutlich vorne' : 'auf Gewinn';
  return grade === 'auf Gewinn'
    ? `${sideName(score.value > 0)} steht auf Gewinn (${shown})`
    : `${sideName(score.value > 0)} liegt ${grade} (${shown})`;
}

/** Ergänzung aus Sicht des Benutzers, z. B. „Du liegst vorne.“ */
export function playerPerspective(evaluation: Evaluation | null, playerColor: Color): string {
  if (!evaluation || evaluation.finished) return '';
  const userShare = playerColor === 'WHITE' ? evaluation.whiteShare : 1 - evaluation.whiteShare;
  if (evaluation.score?.type === 'CP' && Math.abs(evaluation.score.value) < BALANCED_BELOW) return 'Keine Seite hat einen Vorteil.';
  return userShare > 0.5 ? 'Du liegst vorne.' : 'Der Computer liegt vorne.';
}
