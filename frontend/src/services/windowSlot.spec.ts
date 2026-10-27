import { afterEach, describe, expect, it } from 'vitest';
import { createWindowSlot, LEGACY_SLOT, SLOT_PREFIX, type SlotCandidate } from './windowSlot';

class MemoryStorage implements Storage {
  private data = new Map<string, string>();
  get length() { return this.data.size; }
  clear() { this.data.clear(); }
  getItem(key: string) { return this.data.get(key) ?? null; }
  key(index: number) { return [...this.data.keys()][index] ?? null; }
  removeItem(key: string) { this.data.delete(key); }
  setItem(key: string, value: string) { this.data.set(key, value); }
}

const open: { close(): void }[] = [];
let channelCounter = 0;

/** Simuliert mehrere Fenster desselben Browsers: gemeinsame Slot-Liste und gemeinsamer Kanal, eigenes sessionStorage. */
function browser(slots: SlotCandidate[] = []) {
  const channelName = `test-slots-${channelCounter++}`;
  const deleted: string[] = [];
  return {
    slots,
    deleted,
    window(session: Storage = new MemoryStorage()) {
      const slot = createWindowSlot({
        session,
        channelName,
        timeoutMs: 30,
        listSlots: async () => slots,
        deleteSlots: async (ids) => {
          deleted.push(...ids);
        },
      });
      open.push(slot);
      return { slot, session };
    },
  };
}

afterEach(() => {
  open.splice(0).forEach((s) => s.close());
});

describe('windowSlot', () => {
  it('gibt zwei gleichzeitig offenen Fenstern verschiedene Slots', async () => {
    const b = browser();
    const first = await b.window().slot.current();
    const second = await b.window().slot.current();
    expect(first.startsWith(SLOT_PREFIX)).toBe(true);
    expect(second).not.toBe(first);
  });

  it('behält den Slot beim Neuladen desselben Fensters', async () => {
    const b = browser();
    const w = b.window();
    const before = await w.slot.current();
    w.slot.close();
    expect(await b.window(w.session).slot.current()).toBe(before);
  });

  it('wählt für einen duplizierten Tab (kopiertes sessionStorage) einen neuen Slot', async () => {
    const b = browser();
    const original = b.window();
    const slot = await original.slot.current();
    const copy = new MemoryStorage();
    copy.setItem('chess-coach-window-slot', slot);
    expect(await b.window(copy).slot.current()).not.toBe(slot);
  });

  it('übernimmt in einem neuen Fenster die jüngste verwaiste Partie, aber nie die eines offenen Fensters', async () => {
    const b = browser([
      { id: LEGACY_SLOT, savedAt: '2026-10-01T10:00:00Z', finished: false },
      { id: `${SLOT_PREFIX}b`, savedAt: '2026-10-02T10:00:00Z', finished: false },
    ]);
    const busy = new MemoryStorage();
    busy.setItem('chess-coach-window-slot', `${SLOT_PREFIX}b`);
    expect(await b.window(busy).slot.current()).toBe(`${SLOT_PREFIX}b`);
    expect(await b.window().slot.current()).toBe(LEGACY_SLOT);
  });

  it('gibt vielen gleichzeitig startenden Fenstern lauter verschiedene Slots', async () => {
    const b = browser();
    const slots = await Promise.all(Array.from({ length: 8 }, () => b.window().slot.current()));
    expect(new Set(slots).size).toBe(8);
  });

  it('behält beim Neuladen eines Fensters alle Slots der übrigen offenen Fenster bei', async () => {
    const b = browser();
    const windows = Array.from({ length: 4 }, () => b.window());
    const before = await Promise.all(windows.map((w) => w.slot.current()));
    windows[1].slot.close();
    const reloaded = await b.window(windows[1].session).slot.current();
    expect(reloaded).toBe(before[1]);
    const late = await b.window().slot.current();
    expect(before).not.toContain(late);
  });

  it('räumt beendete verwaiste Slots auf', async () => {
    const b = browser([{ id: `${SLOT_PREFIX}done`, savedAt: new Date().toISOString(), finished: true }]);
    await b.window().slot.current();
    expect(b.deleted).toEqual([`${SLOT_PREFIX}done`]);
  });
});
