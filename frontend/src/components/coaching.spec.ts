import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import CoachingSummary from './CoachingSummary.vue';
import CandidateMoves from './CandidateMoves.vue';
import VariationViewer from './VariationViewer.vue';
import MoveList from './MoveList.vue';
import { completedAnalysis, move } from '@/test/fixtures';

describe('CoachingSummary', () => {
  it('zeigt Klasse, Begründung, beste Alternative und Bewertungsänderung', () => {
    const w = mount(CoachingSummary, { props: { analysis: completedAnalysis() } });
    expect(w.find('[data-testid="classification"]').text()).toContain('Ungenau');
    expect(w.find('[data-testid="played-move"]').text()).toBe('1.e4');
    expect(w.find('[data-testid="played-move-text"]').text()).toBe('Bauer von e2 nach e4');
    expect(w.text()).toContain('Bauer von d2 nach d4');
    expect(w.find('[data-testid="main-reason"]').text()).toContain('Testbegründung');
    expect(w.text()).toContain('1.d4');
    expect(w.find('[data-testid="evaluation"]').text()).toContain('Vor deinem Zug: ausgeglichen');
    expect(w.find('[data-testid="evaluation"]').text()).toContain('Nach deinem Zug: der Gegner steht etwas besser');
    expect(w.text()).toContain('Verschenkt gegenüber dem besten Zug: etwa einen Bauern');
    expect(w.text()).not.toContain('cp');
  });

  it('nennt eine Alternative nur dann „besser“, wenn sie es wirklich ist', () => {
    const heading = (classification: string, classificationLabel: string) => mount(CoachingSummary, {
      props: { analysis: completedAnalysis({ classification: classification as never, classificationLabel }) },
    });
    const best = heading('BEST', 'Bestzug');
    expect(best.find('[data-testid="alternative-heading"]').text()).toBe('Gleich gute Alternative');
    expect(best.text()).toContain('Dein Zug ist genauso stark');
    expect(best.text()).not.toContain('Bessere Alternative');
    expect(heading('EXCELLENT', 'Sehr gut').find('[data-testid="alternative-heading"]').text()).toBe('Etwas genauer wäre gewesen');
    expect(heading('MISTAKE', 'Fehler').find('[data-testid="alternative-heading"]').text()).toBe('Bessere Alternative');
  });

  it('zeigt laufende Analyse ohne Ergebnisfelder vorauszusetzen', () => {
    const pending = completedAnalysis({ status: 'RUNNING', stage: 'Kandidaten', progressPercent: 40, classification: null,
      classificationLabel: null, mainReason: null, scoreAfter: null, candidates: null, sections: null, findings: null });
    const w = mount(CoachingSummary, { props: { analysis: pending } });
    expect(w.text()).toContain('wird analysiert');
    expect(w.find('[data-testid="classification"]').exists()).toBe(false);
  });

  it('kennzeichnet unsichere und veraltete Analysen', () => {
    const w = mount(CoachingSummary, {
      props: { analysis: completedAnalysis({ classificationProvisional: true, outdated: true, analysisConfidence: 'LOW' }) },
    });
    expect(w.text()).toContain('vorläufig');
    expect(w.text()).toContain('veraltet');
    expect(w.text()).toContain('nicht ganz sicher');
  });

  it('zeigt fehlgeschlagene Analysen mit Wiederholung an', async () => {
    const w = mount(CoachingSummary, {
      props: { analysis: completedAnalysis({ status: 'FAILED', errorMessage: 'Die Schach-Engine ist nicht verfügbar.' }) },
    });
    expect(w.text()).toContain('nicht verfügbar');
    await w.find('button').trigger('click');
    expect(w.emitted('retry')).toBeTruthy();
  });
});

describe('Kandidaten und Varianten', () => {
  it('kennzeichnet Varianten als Hauptvarianten und meldet die Auswahl', async () => {
    const a = completedAnalysis();
    const w = mount(CandidateMoves, { props: { candidates: a.candidates!, selectedUci: null, startFen: a.fenBefore } });
    expect(w.text()).toContain('keine erzwungenen Zugfolgen');
    expect(w.text()).toContain('Bauer von d2 nach d4');
    expect(w.text()).toContain('ausgeglichen');
    expect(w.text()).toContain('1.d4 1...d5');
    await w.find('[data-testid="candidate-d2d4"]').trigger('click');
    expect(w.emitted('select')).toEqual([['d2d4']]);
  });

  it('VariationViewer navigiert schrittweise und kann verlassen werden', async () => {
    const c = completedAnalysis().candidates![0];
    const w = mount(VariationViewer, { props: { candidate: c, step: 1, startFen: completedAnalysis().fenBefore } });
    expect(w.text()).toContain('nicht Teil der Partie');
    expect(w.text()).toContain('Weiß: Bauer von d2 nach d4');
    expect(w.text()).toContain('Zug 1 von 2');
    await w.find('[data-testid="variation-forward"]').trigger('click');
    await w.find('[data-testid="variation-exit"]').trigger('click');
    expect(w.emitted('step')).toEqual([[1]]);
    expect(w.emitted('exit')).toBeTruthy();
  });
});

describe('MoveList', () => {
  it('zeigt Züge in SAN und navigiert', async () => {
    const moves = [move(1, 'e2e4', 'e4', true), move(2, 'e7e5', 'e5', false,
      'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1')];
    const w = mount(MoveList, { props: { moves, currentPly: null, analyses: { 1: completedAnalysis() } } });
    expect(w.find('[data-testid="move-1"]').text()).toContain('Weiß (du): Bauer von e2 nach e4');
    expect(w.find('[data-testid="move-1"]').text()).toContain('Ungenau');
    expect(w.find('[data-testid="move-2"]').text()).toContain('Schwarz: Bauer von e7 nach e5');
    expect(w.find('[data-testid="move-2"]').text()).toContain('1…');
    await w.find('[data-testid="move-1"]').trigger('click');
    await w.find('[data-testid="nav-back"]').trigger('click');
    expect(w.emitted('select')).toEqual([[1], [1]]);
  });
});
