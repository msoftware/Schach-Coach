<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import type { MoveRecord } from '@/models/game';
import type { MoveAnalysis } from '@/models/analysis';
import { CLASSIFICATION_COLORS, CLASSIFICATION_ICONS } from '@/models/analysis';
import { describeMove } from '@/utils/moveText';

const props = defineProps<{
  moves: MoveRecord[];
  currentPly: number | null;
  analyses: Record<number, MoveAnalysis>;
}>();
const emit = defineEmits<{ select: [ply: number | null] }>();

const listEl = ref<HTMLElement | null>(null);

/** Eine Zeile pro Halbzug: Zugnummer, Farbe, Zug in Worten und Kurznotation. */
const items = computed(() => props.moves.map((m) => {
  const number = Number(m.fenBefore.split(' ')[5] ?? '1');
  return {
    move: m,
    number: m.color === 'WHITE' ? `${number}.` : `${number}…`,
    side: m.color === 'WHITE' ? 'Weiß' : 'Schwarz',
    text: describeMove(m.fenBefore, m.uci, m.san),
  };
}));

const activePly = computed(() => props.currentPly ?? props.moves.length);

watch(() => props.moves.length, async () => {
  await nextTick();
  listEl.value?.scrollTo({ top: listEl.value.scrollHeight });
});

function badge(m: MoveRecord) {
  const a = props.analyses[m.ply];
  if (!m.byUser) return null;
  if (a?.status === 'COMPLETED' && a.classification) {
    return { icon: CLASSIFICATION_ICONS[a.classification], color: CLASSIFICATION_COLORS[a.classification], label: a.classificationLabel ?? '' };
  }
  if (m.analysisStatus === 'PENDING' || m.analysisStatus === 'RUNNING' || a?.status === 'PENDING' || a?.status === 'RUNNING') {
    return { icon: 'hourglass_empty', color: 'grey-6', label: 'Analyse läuft' };
  }
  if (a?.status === 'FAILED') return { icon: 'warning', color: 'warning', label: 'Analyse fehlgeschlagen' };
  return null;
}
</script>

<template>
  <section class="move-list" aria-label="Zugliste">
    <div class="row items-center q-mb-xs nav-row">
      <div class="text-subtitle2 col">Züge</div>
      <q-btn flat dense round icon="first_page" aria-label="Zum Anfang" :disable="moves.length === 0" @click="emit('select', 0)" />
      <q-btn flat dense round icon="chevron_left" aria-label="Einen Zug zurück" :disable="activePly === 0"
             data-testid="nav-back" @click="emit('select', activePly - 1)" />
      <q-btn flat dense round icon="chevron_right" aria-label="Einen Zug vor" :disable="currentPly === null"
             data-testid="nav-forward" @click="emit('select', activePly + 1)" />
      <q-btn flat dense round icon="last_page" aria-label="Zur aktuellen Stellung" :disable="currentPly === null"
             @click="emit('select', null)" />
    </div>
    <div ref="listEl" class="moves-scroll" data-testid="move-list">
      <p v-if="moves.length === 0" class="text-grey-7 q-ma-sm">Noch keine Züge.</p>
      <ol v-else class="moves">
        <li v-for="item in items" :key="item.move.moveId" :class="{ black: item.move.color === 'BLACK' }">
          <button
            type="button"
            class="move-btn"
            :class="{ active: activePly === item.move.ply }"
            :aria-current="activePly === item.move.ply ? 'step' : undefined"
            :data-testid="`move-${item.move.ply}`"
            @click="emit('select', item.move.ply)"
          >
            <span class="num">{{ item.number }}</span>
            <span class="side" :class="item.move.color === 'WHITE' ? 'white' : 'black'">
              {{ item.side }}{{ item.move.byUser ? ' (du)' : '' }}:
            </span>
            <span class="text">
              {{ item.text }}
              <span class="san">({{ item.move.san }})</span>
            </span>
            <span v-if="badge(item.move)" class="badge">
              <q-icon :name="badge(item.move)!.icon" :color="badge(item.move)!.color" size="16px" />
              <span :class="`text-${badge(item.move)!.color}`">{{ badge(item.move)!.label }}</span>
            </span>
            <q-icon v-if="item.move.analysisOutdated" name="update" color="grey-7" size="14px">
              <q-tooltip>Analyse mit älteren Einstellungen erstellt</q-tooltip>
            </q-icon>
          </button>
        </li>
      </ol>
    </div>
  </section>
</template>

<style scoped>
.moves-scroll {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
}
.moves { list-style: none; margin: 0; padding: 0; }
.moves li { border-bottom: 1px solid #f1f5f9; }
.moves li.black { background: #f8fafc; }
.move-btn {
  width: 100%;
  border: none;
  background: transparent;
  padding: 5px 8px;
  cursor: pointer;
  font: inherit;
  text-align: left;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 2px 8px;
  color: #111827;
}
.num { min-width: 2.4em; color: #475569; font-weight: 600; }
.side { font-weight: 600; min-width: 5.2em; }
.text { flex: 1 1 12em; }
.san { color: #64748b; font-size: 0.85em; }
.badge { display: inline-flex; align-items: center; gap: 3px; font-size: 0.85em; font-weight: 600; }
.move-btn.active { background: #1d4ed8; color: #fff; }
.move-btn.active .num, .move-btn.active .san, .move-btn.active .badge span { color: #fff !important; }
.move-btn:focus-visible { outline: 3px solid #f59e0b; outline-offset: -3px; }
/* Handy: ausreichend große Tippflächen (mind. 44 px). */
@media (max-width: 1023px) {
  .move-btn { padding: 10px; min-height: 44px; -webkit-tap-highlight-color: transparent; }
  .nav-row :deep(.q-btn) { min-width: 44px; min-height: 44px; }
}
</style>
