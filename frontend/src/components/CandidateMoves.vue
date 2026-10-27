<script setup lang="ts">
import type { Candidate } from '@/models/analysis';
import { describeMove } from '@/utils/moveText';
import { evaluationColor, evaluationWords } from '@/utils/scoreText';

const props = defineProps<{ candidates: Candidate[]; selectedUci: string | null; startFen?: string }>();

function describe(c: Candidate): string {
  return props.startFen ? describeMove(props.startFen, c.uci, c.line[0]?.san ?? c.san) : c.san;
}
const emit = defineEmits<{ select: [uci: string] }>();
</script>

<template>
  <section aria-label="Kandidatenzüge">
    <div class="text-subtitle2 q-mb-xs">Diese Züge hat der Computer auch geprüft</div>
    <p class="text-caption text-grey-8 q-mb-sm">
      Klicke auf einen Zug, um auf dem Brett zu sehen, wie es weitergehen könnte. Das ist ein Vorschlag des Computers –
      keine erzwungenen Zugfolgen, der Gegner kann auch anders spielen.
    </p>
    <q-list bordered separator class="rounded-borders">
      <q-item v-for="c in candidates" :key="c.uci" clickable :active="selectedUci === c.uci" active-class="bg-blue-1"
              :data-testid="`candidate-${c.uci}`" @click="emit('select', c.uci)">
        <q-item-section avatar class="text-weight-bold" style="min-width: 70px">
          {{ c.san }}
          <div class="row q-gutter-xs">
            <q-badge v-if="c.played" color="primary">gespielt</q-badge>
            <q-badge v-if="c.best" color="positive">bester</q-badge>
          </div>
        </q-item-section>
        <q-item-section>
          <q-item-label class="text-weight-medium">{{ describe(c) }}</q-item-label>
          <q-item-label>
            <span :class="evaluationColor(c.score)">{{ evaluationWords(c.score) }}</span>
            <span class="text-grey-8"> · {{ c.comparison }}</span>
          </q-item-label>
          <q-item-label caption class="variation-line">
            {{ c.line.map((m) => m.numbered).join(' ') }}<span v-if="c.lineTruncated"> (gekürzt)</span>
          </q-item-label>
          <q-item-label v-if="c.motifs.length" caption>Taktik: {{ c.motifs.join(', ') }}</q-item-label>
          <q-item-label v-if="c.strategicPros.length" caption class="text-positive">Vorteile: {{ c.strategicPros.join(', ') }}</q-item-label>
          <q-item-label v-if="c.strategicCons.length" caption class="text-negative">Nachteile: {{ c.strategicCons.join(', ') }}</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-icon name="play_circle" color="primary" aria-label="Variante anzeigen" />
        </q-item-section>
      </q-item>
    </q-list>
  </section>
</template>

<style scoped>
.variation-line { font-family: ui-monospace, Consolas, monospace; }
</style>
