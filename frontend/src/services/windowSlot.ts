/**
 * Jedes Browserfenster (bzw. jeder Tab) spielt seine eigene Partie und speichert sie unter einem eigenen
 * Speicherplatz („Slot“) in IndexedDB. Die Slot-Kennung liegt im sessionStorage des Fensters und übersteht damit
 * ein Neuladen. Über einen BroadcastChannel melden offene Fenster, welche Slots sie belegen; so übernimmt ein
 * neues Fenster nie die Partie eines noch offenen Fensters, wohl aber eine verwaiste Partie eines geschlossenen.
 * Ein duplizierter Tab (kopiertes sessionStorage) erkennt, dass sein Slot bereits belegt ist, und wählt einen neuen.
 */

export const LEGACY_SLOT = 'current';
export const SLOT_PREFIX = 'current-';
const SESSION_KEY = 'chess-coach-window-slot';
const CHANNEL_NAME = 'chess-coach-window-slots';
/** Verwaiste Slots, die über diese Anzahl hinausgehen oder älter als MAX_AGE_MS sind, werden gelöscht. */
const MAX_ORPHANED_SLOTS = 10;
const MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000;

export interface SlotCandidate {
  id: string;
  savedAt: string;
  finished: boolean;
}

export interface WindowSlotOptions {
  session: Storage | null;
  listSlots: () => Promise<SlotCandidate[]>;
  deleteSlots: (ids: string[]) => Promise<void>;
  channelName?: string;
  /** Wartezeit auf Antworten anderer Fenster beim Start. */
  timeoutMs?: number;
}

type SlotMessage = { type: 'who' } | { type: 'held'; slot: string };

export function isSlotId(id: string): boolean {
  return id === LEGACY_SLOT || id.startsWith(SLOT_PREFIX);
}

function newSlotId(): string {
  const random = typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
  return SLOT_PREFIX + random;
}

export function createWindowSlot(options: WindowSlotOptions) {
  const timeoutMs = options.timeoutMs ?? 150;
  const channel = typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel(options.channelName ?? CHANNEL_NAME);
  let held: string | null = null;
  let claim: Promise<string> | null = null;

  channel?.addEventListener('message', (event: MessageEvent<SlotMessage>) => {
    if (event.data?.type === 'who' && held) channel.postMessage({ type: 'held', slot: held } satisfies SlotMessage);
  });

  function readSession(): string | null {
    try {
      return options.session?.getItem(SESSION_KEY) ?? null;
    } catch {
      return null;
    }
  }

  function writeSession(slot: string) {
    try {
      options.session?.setItem(SESSION_KEY, slot);
    } catch {
      // sessionStorage nicht verfügbar: Der Slot gilt dann nur bis zum Neuladen.
    }
  }

  /** Fragt alle anderen offenen Fenster nach ihren belegten Slots. */
  async function slotsHeldElsewhere(): Promise<Set<string>> {
    const result = new Set<string>();
    if (!channel) return result;
    const onMessage = (event: MessageEvent<SlotMessage>) => {
      if (event.data?.type === 'held') result.add(event.data.slot);
    };
    channel.addEventListener('message', onMessage);
    channel.postMessage({ type: 'who' } satisfies SlotMessage);
    await new Promise((resolve) => setTimeout(resolve, timeoutMs));
    channel.removeEventListener('message', onMessage);
    return result;
  }

  async function doClaim(): Promise<string> {
    const own = readSession();
    const [others, slots] = await Promise.all([slotsHeldElsewhere(), options.listSlots().catch(() => [])]);
    let slot: string;
    if (own && !others.has(own)) {
      slot = own;
    } else {
      const orphan = slots
        .filter((s) => !s.finished && !others.has(s.id))
        .sort((a, b) => b.savedAt.localeCompare(a.savedAt))[0];
      slot = orphan?.id ?? newSlotId();
    }
    held = slot;
    writeSession(slot);

    const now = Date.now();
    const orphaned = slots
      .filter((s) => s.id !== slot && !others.has(s.id))
      .sort((a, b) => b.savedAt.localeCompare(a.savedAt));
    const stale = orphaned.filter((s, i) => s.finished || i >= MAX_ORPHANED_SLOTS || now - Date.parse(s.savedAt) > MAX_AGE_MS);
    if (stale.length > 0) await options.deleteSlots(stale.map((s) => s.id)).catch(() => undefined);
    return slot;
  }

  return {
    /** Slot dieses Fensters; wird beim ersten Aufruf ermittelt und danach beibehalten. */
    current(): Promise<string> {
      claim ??= doClaim().catch((e: unknown) => {
        claim = null;
        throw e;
      });
      return claim;
    },
    /** Gibt den Slot frei (nur für Tests). */
    close(): void {
      held = null;
      claim = null;
      channel?.close();
    },
  };
}
