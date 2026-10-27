import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import ChessBoard from './ChessBoard.vue';
import { legal, START } from '@/test/fixtures';

const PROMO_FEN = '8/4P3/8/8/8/k7/8/4K3 w - - 0 1';

function mountBoard(props: Record<string, unknown> = {}) {
  return mount(ChessBoard, {
    props: { fen: START, interactive: true, legalMoves: [legal('e2e4', 'e4'), legal('e2e3', 'e3'), legal('g1f3', 'Nf3')], ...props },
    attachTo: document.body,
  });
}

describe('ChessBoard', () => {
  it('zeigt 64 Felder und 32 SVG-Figuren mit Beschriftung', () => {
    const w = mountBoard();
    expect(w.findAll('[data-square]')).toHaveLength(64);
    const pieces = w.findAll('img.chess-piece');
    expect(pieces).toHaveLength(32);
    expect(w.find('[data-square="e1"] img').attributes('alt')).toBe('weißer König');
  });

  it('hebt nach Auswahl die legalen Zielfelder hervor und zieht per Klick', async () => {
    const w = mountBoard();
    await w.find('[data-square="e2"]').trigger('click');
    expect(w.find('[data-square="e2"]').classes()).toContain('selected');
    expect(w.findAll('.target')).toHaveLength(2);
    expect(w.find('[data-square="e4"] .target').exists()).toBe(true);
    await w.find('[data-square="e4"]').trigger('click');
    expect(w.emitted('move')).toEqual([['e2e4']]);
  });

  it('verhindert ungültige Züge', async () => {
    const w = mountBoard();
    await w.find('[data-square="e2"]').trigger('click');
    await w.find('[data-square="e5"]').trigger('click');
    expect(w.emitted('move')).toBeUndefined();
    await w.find('[data-square="a2"]').trigger('click');
    expect(w.find('[data-square="a2"]').classes()).not.toContain('selected');
  });

  it('nimmt im nicht interaktiven Zustand keine Züge an', async () => {
    const w = mountBoard({ interactive: false });
    await w.find('[data-square="e2"]').trigger('click');
    await w.find('[data-square="e4"]').trigger('click');
    expect(w.emitted('move')).toBeUndefined();
  });

  it('bietet bei der Umwandlung Dame, Turm, Läufer und Springer an', async () => {
    const w = mountBoard({
      fen: PROMO_FEN,
      legalMoves: ['q', 'r', 'b', 'n'].map((p) => legal(`e7e8${p}`, `e8=${p.toUpperCase()}`, p as 'q')),
    });
    await w.find('[data-square="e7"]').trigger('click');
    await w.find('[data-square="e8"]').trigger('click');
    expect(w.find('[data-testid="promotion-dialog"]').exists()).toBe(true);
    expect(w.findAll('.promotion-option')).toHaveLength(4);
    await w.find('[data-testid="promote-n"]').trigger('click');
    expect(w.emitted('move')).toEqual([['e7e8n']]);
  });

  it('markiert letzten Zug und Schach nicht nur farblich', () => {
    const w = mountBoard({ lastMove: { from: 'e2', to: 'e4' }, checkSquare: 'e1' });
    expect(w.find('[data-square="e4"]').classes()).toContain('last-move');
    expect(w.find('[data-square="e1"]').classes()).toContain('check');
    expect(w.find('[data-square="e1"]').attributes('aria-label')).toContain('steht im Schach');
    expect(w.find('[data-square="e4"]').attributes('aria-label')).toContain('letzter Zug');
  });

  it('ist per Tastatur bedienbar', async () => {
    const w = mountBoard();
    await w.find('[data-square="e2"]').trigger('keydown', { key: 'Enter' });
    await w.find('[data-square="e4"]').trigger('keydown', { key: ' ' });
    expect(w.emitted('move')).toEqual([['e2e4']]);
  });

  it('dreht das Brett für Schwarz', () => {
    const w = mountBoard({ orientation: 'BLACK' });
    expect(w.findAll('[data-square]')[0].attributes('data-square')).toBe('h1');
  });
});
