import { afterEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import EvaluationBar from './EvaluationBar.vue';
import type { Evaluation } from '@/models/game';
import { evaluationSentence, playerPerspective, whitePercent } from '@/utils/evaluationText';
import { useEvaluationStore } from '@/stores/evaluationStore';

function evaluation(overrides: Partial<Evaluation> = {}): Evaluation {
  return {
    gameId: 'g1', positionVersion: 1, fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    score: { type: 'CP', value: 20, display: '+0.20', perspective: 'WHITE' }, whiteShare: 0.53, display: '+0.20',
    depth: 12, finished: false, ...overrides,
  };
}

const cp = (value: number, whiteShare: number) => evaluation({
  score: { type: 'CP', value, display: '', perspective: 'WHITE' }, whiteShare,
});

describe('evaluationText', () => {
  it('beschreibt Vorteile in Worten, aus Sicht von Weiß und Schwarz', () => {
    expect(evaluationSentence(cp(33, 0.55))).toBe('Ausgeglichen (+0.33)');
    expect(evaluationSentence(cp(80, 0.61))).toBe('Weiß liegt leicht vorne (+0.80)');
    expect(evaluationSentence(cp(-150, 0.3))).toBe('Schwarz liegt klar vorne (−1.50)');
    expect(evaluationSentence(cp(400, 0.91))).toBe('Weiß liegt deutlich vorne (+4.00)');
    expect(evaluationSentence(cp(900, 0.99))).toBe('Weiß steht auf Gewinn (+9.00)');
  });

  it('beschreibt Matt und beendete Partien', () => {
    expect(evaluationSentence(evaluation({ score: { type: 'MATE', value: -3, display: '-M3', perspective: 'WHITE' }, whiteShare: 0 })))
      .toBe('Schwarz setzt in 3 Zügen matt');
    expect(evaluationSentence(evaluation({ score: null, finished: true, whiteShare: 1, display: '1-0' })))
      .toBe('Partie beendet: Weiß hat gewonnen (1-0)');
    expect(evaluationSentence(evaluation({ score: null, finished: true, whiteShare: 0.5, display: '½-½' })))
      .toBe('Partie beendet: Remis (½-½)');
  });

  it('ergänzt die Sicht des Benutzers', () => {
    expect(playerPerspective(cp(200, 0.76), 'WHITE')).toBe('Du liegst vorne.');
    expect(playerPerspective(cp(200, 0.76), 'BLACK')).toBe('Der Computer liegt vorne.');
    expect(playerPerspective(cp(5, 0.5), 'BLACK')).toBe('Keine Seite hat einen Vorteil.');
  });

  it('hält den Schieber sichtbar, außer bei Matt oder Partieende', () => {
    expect(whitePercent(null)).toBe(50);
    expect(whitePercent(cp(2000, 0.9999))).toBe(97);
    expect(whitePercent(cp(-2000, 0.0001))).toBe(3);
    expect(whitePercent(evaluation({ score: { type: 'MATE', value: 2, display: 'M2', perspective: 'WHITE' }, whiteShare: 1 }))).toBe(100);
    expect(whitePercent(evaluation({ score: null, finished: true, whiteShare: 0, display: '0-1' }))).toBe(0);
  });
});

describe('EvaluationBar', () => {
  it('verschiebt den Schieber entsprechend der Bewertung und beschriftet ihn barrierefrei', async () => {
    const w = mount(EvaluationBar, { props: { evaluation: cp(150, 0.7), playerColor: 'WHITE' } });
    const track = w.find('[data-testid="evaluation-track"]');
    expect(track.attributes('aria-valuenow')).toBe('70');
    expect(track.attributes('aria-valuetext')).toContain('Weiß liegt klar vorne (+1.50)');
    expect(w.find('[data-testid="evaluation-white"]').attributes('style')).toContain('width: 70%');
    expect(w.text()).toContain('Weiß (du)');
    expect(w.text()).toContain('Du liegst vorne.');

    await w.setProps({ evaluation: cp(-150, 0.3) });
    expect(w.find('[data-testid="evaluation-white"]').attributes('style')).toContain('width: 30%');
    expect(w.find('[data-testid="evaluation-text"]').text()).toBe('Schwarz liegt klar vorne (−1.50)');
  });

  it('zeigt ohne Bewertung die Mitte', () => {
    const w = mount(EvaluationBar, { props: { evaluation: null, playerColor: 'BLACK' } });
    expect(w.find('[data-testid="evaluation-track"]').attributes('aria-valuenow')).toBe('50');
    expect(w.text()).toContain('Schwarz (du)');
  });
});

describe('evaluationStore', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('verwirft eine verspätete Antwort zu einer älteren Stellung', async () => {
    setActivePinia(createPinia());
    const resolvers: ((body: unknown) => void)[] = [];
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>((resolve) => {
      resolvers.push((body) => resolve(new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })));
    })));
    const store = useEvaluationStore();
    const older = store.refresh('g1', 1);
    const newer = store.refresh('g1', 2);
    resolvers[1](evaluation({ positionVersion: 2, whiteShare: 0.8 }));
    await newer;
    resolvers[0](evaluation({ positionVersion: 1, whiteShare: 0.2 }));
    await older;
    expect(store.evaluation?.positionVersion).toBe(2);
    expect(store.evaluation?.whiteShare).toBe(0.8);
    expect(store.loading).toBe(false);
  });

  it('behält bei einem Fehler den letzten Stand und meldet das', async () => {
    setActivePinia(createPinia());
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify(evaluation()), { status: 200 })));
    const store = useEvaluationStore();
    await store.refresh('g1', 1);
    vi.stubGlobal('fetch', vi.fn(async () => {
      throw new TypeError('Failed to fetch');
    }));
    await store.refresh('g1', 2);
    expect(store.evaluation?.positionVersion).toBe(1);
    expect(store.failed).toBe(true);
  });
});
