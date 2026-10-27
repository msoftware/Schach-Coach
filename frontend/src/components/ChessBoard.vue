<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import ChessPiece from './ChessPiece.vue';
import type { LegalMove } from '@/models/game';
import { FILES, isLightSquare, orderedSquares, parseBoard, pieceLabel, type PieceCode } from '@/utils/fen';

/**
 * Interaktives Schachbrett. Es kennt keine Schachregeln: Ziehbare Figuren und Zielfelder ergeben sich
 * ausschließlich aus den legalen Zügen, die das Backend liefert.
 */
const props = withDefaults(defineProps<{
  fen: string;
  orientation?: 'WHITE' | 'BLACK';
  legalMoves?: LegalMove[];
  interactive?: boolean;
  lastMove?: { from: string; to: string } | null;
  checkSquare?: string | null;
  markedSquares?: string[];
  showCoordinates?: boolean;
}>(), {
  orientation: 'WHITE',
  legalMoves: () => [],
  interactive: false,
  lastMove: null,
  checkSquare: null,
  markedSquares: () => [],
  showCoordinates: true,
});

const emit = defineEmits<{ move: [uci: string] }>();

const boardEl = ref<HTMLElement | null>(null);
const selected = ref<string | null>(null);
const focused = ref<string | null>(null);
const promotion = ref<{ from: string; to: string; options: LegalMove[] } | null>(null);
const drag = ref<{ from: string; piece: PieceCode; x: number; y: number; startX: number; startY: number; moved: boolean; touch: boolean } | null>(null);

const board = computed(() => parseBoard(props.fen));
const squares = computed(() => orderedSquares(props.orientation));
const movesFrom = computed(() => {
  const map: Record<string, LegalMove[]> = {};
  for (const m of props.legalMoves) (map[m.from] ??= []).push(m);
  return map;
});
const targets = computed(() => new Set((selected.value ? movesFrom.value[selected.value] ?? [] : []).map((m) => m.to)));

watch(() => props.fen, () => {
  selected.value = null;
  promotion.value = null;
});

function canSelect(square: string): boolean {
  return props.interactive && (movesFrom.value[square]?.length ?? 0) > 0;
}

/** Kurzes haptisches Feedback auf Geräten mit Vibrationsmotor (z. B. Android-Handys). */
function haptic() {
  if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') navigator.vibrate(12);
}

function tryMove(from: string, to: string): boolean {
  const candidates = (movesFrom.value[from] ?? []).filter((m) => m.to === to);
  if (candidates.length === 0) return false;
  if (candidates.length > 1 || candidates[0].promotion) {
    promotion.value = { from, to, options: candidates };
    return true;
  }
  selected.value = null;
  haptic();
  emit('move', candidates[0].uci);
  return true;
}

function choosePromotion(move: LegalMove) {
  promotion.value = null;
  selected.value = null;
  haptic();
  emit('move', move.uci);
}

function onSquareActivate(square: string) {
  if (!props.interactive || promotion.value) return;
  if (selected.value && selected.value !== square && tryMove(selected.value, square)) return;
  if (selected.value === square) {
    selected.value = null;
  } else if (canSelect(square)) {
    selected.value = square;
  } else {
    selected.value = null;
  }
}

function squareFromPoint(x: number, y: number): string | null {
  const el = boardEl.value;
  if (!el) return null;
  const rect = el.getBoundingClientRect();
  const col = Math.floor(((x - rect.left) / rect.width) * 8);
  const row = Math.floor(((y - rect.top) / rect.height) * 8);
  if (col < 0 || col > 7 || row < 0 || row > 7) return null;
  return squares.value[row * 8 + col] ?? null;
}

function onPointerDown(event: PointerEvent, square: string) {
  if (!props.interactive || promotion.value) return;
  const piece = board.value[square];
  if (!piece || !canSelect(square)) {
    return;
  }
  // Nur bei der Maus (verhindert Textauswahl). Bei Touch unterdrückt Safari/iOS sonst den folgenden Klick, sodass
  // Antippen einer Figur nichts auswählt; Scrollen verhindert dort bereits touch-action: none.
  if (event.pointerType === 'mouse') event.preventDefault();
  drag.value = {
    from: square, piece, x: event.clientX, y: event.clientY, startX: event.clientX, startY: event.clientY, moved: false,
    touch: event.pointerType === 'touch',
  };
  window.addEventListener('pointermove', onPointerMove);
  window.addEventListener('pointerup', onPointerUp, { once: true });
}

function onPointerMove(event: PointerEvent) {
  if (!drag.value) return;
  const moved = drag.value.moved || Math.hypot(event.clientX - drag.value.startX, event.clientY - drag.value.startY) > 6;
  drag.value = { ...drag.value, x: event.clientX, y: event.clientY, moved };
  if (moved) selected.value = drag.value.from;
}

let suppressClick = false;

function onPointerUp(event: PointerEvent) {
  window.removeEventListener('pointermove', onPointerMove);
  const d = drag.value;
  drag.value = null;
  if (!d || !d.moved) return; // Ohne Bewegung übernimmt der Klick-Handler (Auswahl/Zielfeld).
  suppressClick = true;
  const target = squareFromPoint(event.clientX, event.clientY);
  if (!target || target === d.from || !tryMove(d.from, target)) {
    selected.value = d.from;
  }
}

function onClick(square: string) {
  if (suppressClick) {
    suppressClick = false;
    return;
  }
  onSquareActivate(square);
}

onBeforeUnmount(() => window.removeEventListener('pointermove', onPointerMove));

/** Tastaturbedienung: Pfeiltasten bewegen den Fokus, Enter/Leertaste wählt bzw. zieht. */
function onKeydown(event: KeyboardEvent, square: string) {
  const index = squares.value.indexOf(square);
  const moves: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -8, ArrowDown: 8 };
  if (event.key in moves) {
    event.preventDefault();
    const next = index + moves[event.key];
    if (next >= 0 && next < 64) {
      focused.value = squares.value[next];
      (boardEl.value?.querySelector(`[data-square="${squares.value[next]}"]`) as HTMLElement | null)?.focus();
    }
  } else if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    onSquareActivate(square);
  } else if (event.key === 'Escape') {
    selected.value = null;
    promotion.value = null;
  }
}

function squareLabel(square: string): string {
  const piece = board.value[square];
  const parts = [square, piece ? pieceLabel(piece) : 'leer'];
  if (selected.value === square) parts.push('ausgewählt');
  if (targets.value.has(square)) parts.push('mögliches Zielfeld');
  if (props.lastMove && (props.lastMove.from === square || props.lastMove.to === square)) parts.push('letzter Zug');
  if (props.checkSquare === square) parts.push('steht im Schach');
  return parts.join(', ');
}

const ghostStyle = computed(() => {
  if (!drag.value || !drag.value.moved || !boardEl.value) return null;
  const square = boardEl.value.getBoundingClientRect().width / 8;
  // Mit dem Finger: Figur vergrößert und oberhalb des Fingers zeigen, damit sie nicht verdeckt ist.
  const size = drag.value.touch ? square * 1.5 : square;
  const lift = drag.value.touch ? size * 0.9 : size / 2;
  return { width: `${size}px`, height: `${size}px`, left: `${drag.value.x - size / 2}px`, top: `${drag.value.y - lift}px` };
});

const promotionColor = computed(() => (props.fen.split(' ')[1] === 'b' ? 'b' : 'w'));
const promotionPieces = computed(() => {
  const order: Array<'q' | 'r' | 'b' | 'n'> = ['q', 'r', 'b', 'n'];
  const names = { q: 'Dame', r: 'Turm', b: 'Läufer', n: 'Springer' };
  return order
    .map((p) => ({ move: promotion.value?.options.find((m) => m.promotion === p), code: `${promotionColor.value}${p.toUpperCase()}` as PieceCode, name: names[p] }))
    .filter((p) => p.move);
});

function fileLabel(square: string, index: number): string | null {
  return props.showCoordinates && index >= 56 ? square[0] : null;
}

function rankLabel(square: string, index: number): string | null {
  return props.showCoordinates && index % 8 === 0 ? square[1] : null;
}

defineExpose({ select: onSquareActivate, FILES });
</script>

<template>
  <div class="board-wrapper">
    <div
      ref="boardEl"
      class="chess-board"
      role="grid"
      aria-label="Schachbrett"
      :class="{ interactive }"
      data-testid="chess-board"
    >
      <div
        v-for="(square, index) in squares"
        :key="square"
        class="square"
        role="gridcell"
        :tabindex="interactive ? (focused === square || (!focused && index === 56) ? 0 : -1) : -1"
        :data-square="square"
        :aria-label="squareLabel(square)"
        :aria-selected="selected === square"
        :class="{
          light: isLightSquare(square),
          dark: !isLightSquare(square),
          selected: selected === square,
          'last-move': lastMove && (lastMove.from === square || lastMove.to === square),
          check: checkSquare === square,
          marked: markedSquares.includes(square),
          movable: canSelect(square),
        }"
        @pointerdown="onPointerDown($event, square)"
        @click="onClick(square)"
        @keydown="onKeydown($event, square)"
        @focus="focused = square"
      >
        <ChessPiece v-if="board[square] && !(drag?.moved && drag.from === square)" :piece="board[square]" />
        <span v-if="targets.has(square)" class="target" :class="{ capture: !!board[square] }" aria-hidden="true" />
        <span v-if="fileLabel(square, index)" class="coord file" aria-hidden="true">{{ fileLabel(square, index) }}</span>
        <span v-if="rankLabel(square, index)" class="coord rank" aria-hidden="true">{{ rankLabel(square, index) }}</span>
      </div>

      <div v-if="promotion" class="promotion" role="dialog" aria-label="Umwandlungsfigur wählen" data-testid="promotion-dialog">
        <div class="promotion-title">Umwandlung in:</div>
        <div class="promotion-options">
          <button
            v-for="p in promotionPieces"
            :key="p.code"
            type="button"
            class="promotion-option"
            :aria-label="p.name"
            :data-testid="`promote-${p.move!.promotion}`"
            @click="choosePromotion(p.move!)"
          >
            <ChessPiece :piece="p.code" decorative />
            <span>{{ p.name }}</span>
          </button>
        </div>
        <button type="button" class="promotion-cancel" @click="promotion = null">Abbrechen</button>
      </div>
    </div>
    <div v-if="ghostStyle && drag" class="drag-ghost" :style="ghostStyle" aria-hidden="true">
      <ChessPiece :piece="drag.piece" decorative />
    </div>
  </div>
</template>

<style scoped>
.board-wrapper {
  width: 100%;
  /* Die Seite kann die Brettgröße über --board-max an das Layout (z. B. Handy hoch/quer) anpassen. */
  max-width: var(--board-max, min(92vw, 640px, calc(100vh - 170px)));
  min-width: 260px;
  margin: 0 auto;
}
.chess-board {
  position: relative;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  grid-template-rows: repeat(8, 1fr);
  aspect-ratio: 1 / 1;
  width: 100%;
  border: 3px solid #5d4632;
  border-radius: 4px;
  /* Scrollen über das Brett bleibt möglich; nur auf ziehbaren Figuren wird die Wischgeste zum Ziehen genutzt. */
  touch-action: manipulation;
  user-select: none;
  -webkit-user-select: none;
  -webkit-tap-highlight-color: transparent;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}
/* Kein Kontextmenü „Bild sichern“ beim langen Drücken auf eine Figur (iOS). */
.chess-board :deep(img) {
  -webkit-touch-callout: none;
  -webkit-user-drag: none;
  pointer-events: none;
}
.square {
  position: relative;
  outline: none;
}
.chess-board.interactive .square.movable { touch-action: none; }
.square.light { background: #f0d9b5; }
.square.dark { background: #b58863; }
.square.movable { cursor: grab; }
.square.last-move.light { background: #f6ec8f; }
.square.last-move.dark { background: #d4c056; }
/* Zusätzliche Kennzeichnung durch Rahmen, nicht nur durch Farbe. */
.square.last-move::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 3px solid rgba(120, 100, 0, 0.55);
  pointer-events: none;
}
.square.selected { box-shadow: inset 0 0 0 4px #1d4ed8; }
.square.check { background: radial-gradient(circle, #ff3b3b 0%, #e11d1d 45%, rgba(169, 0, 0, 0) 80%) !important; }
.square.check::before {
  content: '!';
  position: absolute;
  top: 1px;
  right: 4px;
  font-weight: 800;
  color: #fff;
  text-shadow: 0 0 2px #000;
  z-index: 2;
}
.square.marked { box-shadow: inset 0 0 0 4px #0f766e; }
.square:focus-visible { box-shadow: inset 0 0 0 4px #111827, inset 0 0 0 7px #fbbf24; z-index: 1; }
.target {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 30%;
  height: 30%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: rgba(20, 83, 45, 0.55);
  border: 2px solid rgba(255, 255, 255, 0.7);
  pointer-events: none;
}
.target.capture {
  width: 92%;
  height: 92%;
  background: transparent;
  border: 5px solid rgba(20, 83, 45, 0.7);
}
.coord {
  position: absolute;
  font-size: clamp(9px, 1.6vw, 13px);
  font-weight: 700;
  pointer-events: none;
}
.square.light .coord { color: #946f51; }
.square.dark .coord { color: #f0d9b5; }
.coord.file { right: 3px; bottom: 1px; }
.coord.rank { left: 3px; top: 1px; }
.drag-ghost {
  position: fixed;
  pointer-events: none;
  z-index: 9999;
  filter: drop-shadow(0 6px 6px rgba(0, 0, 0, 0.35));
}
.promotion {
  position: absolute;
  inset: 22% 4%;
  background: #fff;
  color: #111;
  border-radius: 8px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  z-index: 10;
  padding: 8px;
}
.promotion-title { font-weight: 700; }
.promotion-options { display: flex; gap: 6px; justify-content: center; width: 100%; }
.promotion-option {
  flex: 1 1 0;
  max-width: 72px;
  min-width: 0;
  min-height: 72px;
  border: 2px solid #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
  cursor: pointer;
  padding: 4px;
}
.promotion-option:hover, .promotion-option:focus-visible { border-color: #1d4ed8; outline: 2px solid #1d4ed8; }
.promotion-option :deep(img) { width: 100%; max-width: 56px; height: auto; aspect-ratio: 1; }
.promotion-cancel { background: none; border: none; text-decoration: underline; cursor: pointer; color: #1f2937; min-height: 40px; padding: 0 12px; }
</style>
