import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import GameControls from './GameControls.vue';

const props = { canUndo: true, canRedo: false, canHint: true, canResign: true, busy: false };

describe('GameControls', () => {
  it('zeigt auf dem Handy eine Aktionsleiste mit großen Tippflächen im Daumenbereich', async () => {
    const w = mount(GameControls, { props: { ...props, compact: true } });
    const bar = w.find('[data-testid="bottom-bar"]');
    expect(bar.exists()).toBe(true);
    expect(bar.text()).toContain('Neu');
    expect(bar.text()).toContain('Zurück');
    expect(bar.text()).toContain('Tipp');
    expect(bar.text()).toContain('Mehr');
    // Seltene Aktionen liegen im Menü „Mehr“, nicht direkt in der Leiste.
    expect(bar.text()).not.toContain('Aufgeben');
    expect(w.find('[data-testid="btn-redo"]').attributes('disabled')).toBeDefined();

    await w.find('[data-testid="btn-undo"]').trigger('click');
    await w.find('[data-testid="btn-hint"]').trigger('click');
    expect(w.emitted('undo')).toHaveLength(1);
    expect(w.emitted('hint')).toHaveLength(1);
  });

  it('sperrt Aktionen, solange gerechnet wird', () => {
    const w = mount(GameControls, { props: { ...props, compact: true, busy: true } });
    expect(w.find('[data-testid="btn-undo"]').attributes('disabled')).toBeDefined();
    expect(w.find('[data-testid="btn-hint"]').attributes('disabled')).toBeDefined();
    expect(w.find('[data-testid="btn-new"]').attributes('disabled')).toBeUndefined();
  });

  it('behält auf dem Desktop die bisherige Werkzeugleiste', () => {
    const w = mount(GameControls, { props });
    expect(w.find('[data-testid="bottom-bar"]').exists()).toBe(false);
    expect(w.find('[data-testid="btn-resign"]').exists()).toBe(true);
  });
});
