<script setup lang="ts">
import { computed } from 'vue';
import type { CoachingFinding } from '@/models/coaching';

const props = defineProps<{ findings: CoachingFinding[]; title: string; emptyText?: string }>();
const emit = defineEmits<{ highlight: [squares: string[]] }>();

const sorted = computed(() => [...props.findings].sort((a, b) => Math.abs(b.score) - Math.abs(a.score)));

function icon(f: CoachingFinding) {
  if (f.polarity === 'POSITIVE') return { name: 'add_circle', color: 'positive', label: 'vorteilhaft' };
  if (f.polarity === 'NEGATIVE') return { name: 'remove_circle', color: 'negative', label: 'nachteilig' };
  return { name: 'info', color: 'grey-7', label: 'neutral' };
}
</script>

<template>
  <section :aria-label="title">
    <div class="text-subtitle2 q-mb-xs">{{ title }}</div>
    <p v-if="sorted.length === 0" class="text-grey-7 text-body2">{{ emptyText ?? 'Keine Befunde.' }}</p>
    <q-list v-else dense separator bordered class="rounded-borders">
      <q-item v-for="(f, i) in sorted" :key="f.ruleId + i" clickable :class="{ 'overridden': f.overriddenByTactics }"
              @mouseenter="emit('highlight', f.affectedSquares)" @mouseleave="emit('highlight', [])"
              @focus="emit('highlight', f.affectedSquares)" @click="emit('highlight', f.affectedSquares)">
        <q-item-section avatar>
          <q-icon :name="icon(f).name" :color="icon(f).color" :aria-label="icon(f).label" />
        </q-item-section>
        <q-item-section>
          <q-item-label class="text-weight-medium">
            {{ f.title }}
            <q-badge outline color="grey-8" class="q-ml-xs">{{ f.ruleId }}</q-badge>
            <q-badge v-if="f.certainty === 'HEURISTIC'" outline color="blue-grey-6" class="q-ml-xs">Faustregel</q-badge>
            <q-badge v-else outline color="teal-8" class="q-ml-xs">nachgerechnet</q-badge>
            <q-badge v-if="f.source === 'ENGINE'" outline color="indigo-7" class="q-ml-xs">Computer</q-badge>
          </q-item-label>
          <q-item-label class="text-body2">{{ f.explanation }}</q-item-label>
          <q-item-label v-if="f.overriddenByTactics" caption class="text-orange-10">
            Gilt hier nicht: Die taktische Folge des Zuges ist wichtiger.
          </q-item-label>
          <q-item-label v-if="f.evidence?.line?.length" caption>
            Berechnete Züge: {{ f.evidence.triggerMove }} {{ f.evidence.line.join(' ') }}
          </q-item-label>
        </q-item-section>
      </q-item>
    </q-list>
  </section>
</template>

<style scoped>
.overridden { opacity: 0.7; }
</style>
