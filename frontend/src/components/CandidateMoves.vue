<script setup lang="ts">
import type { Candidate } from '@/models/analysis';

defineProps<{ candidates: Candidate[]; selectedUci: string | null }>();
const emit = defineEmits<{ select: [uci: string] }>();
</script>

<template>
  <section aria-label="Kandidatenzüge">
    <div class="text-subtitle2 q-mb-xs">Kandidatenzüge</div>
    <p class="text-caption text-grey-8 q-mb-sm">
      Varianten sind Hauptvarianten der Engine-Suche, keine erzwungenen Zugfolgen. Klicke auf einen Kandidaten,
      um die Variante auf dem Brett durchzuspielen.
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
          <q-item-label>
            <strong>{{ c.score.display }}</strong>
            <span class="text-grey-8"> · Tiefe {{ c.depth }}</span>
            <span class="text-grey-8"> · {{ c.comparison }}</span>
          </q-item-label>
          <q-item-label caption class="variation-line">
            {{ c.line.map((m) => m.numbered).join(' ') }}<span v-if="c.lineTruncated"> (gekürzt)</span>
          </q-item-label>
          <q-item-label v-if="c.motifs.length" caption>Taktische Motive: {{ c.motifs.join(', ') }}</q-item-label>
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
