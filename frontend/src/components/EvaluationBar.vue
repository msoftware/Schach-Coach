<script setup lang="ts">
import { computed } from 'vue';
import type { Color, Evaluation } from '@/models/game';
import { evaluationSentence, playerPerspective, whitePercent } from '@/utils/evaluationText';

const props = defineProps<{
  evaluation: Evaluation | null;
  loading?: boolean;
  failed?: boolean;
  playerColor: Color;
  /** Mobile Darstellung: schmale Leiste mit Beschriftung in einer Zeile, direkt unter dem Brett. */
  compact?: boolean;
}>();

const percent = computed(() => whitePercent(props.evaluation));
const sentence = computed(() => evaluationSentence(props.evaluation));
const perspective = computed(() => playerPerspective(props.evaluation, props.playerColor));
const valueText = computed(() => [sentence.value, perspective.value].filter(Boolean).join(' '));
</script>

<template>
  <div v-if="compact" class="evaluation-bar compact" data-testid="evaluation-bar">
    <div class="row items-center no-wrap q-gutter-x-sm">
      <span class="side-label">Weiß<span v-if="playerColor === 'WHITE'"> (du)</span></span>
      <div class="track col" role="meter" aria-label="Bewertung: Anteil von Weiß" aria-valuemin="0" aria-valuemax="100"
           :aria-valuenow="percent" :aria-valuetext="valueText" data-testid="evaluation-track">
        <div class="white-part" :style="{ width: `${percent}%` }" data-testid="evaluation-white" />
        <div class="center-mark" aria-hidden="true" />
        <div class="thumb" :style="{ left: `${percent}%` }" aria-hidden="true" />
      </div>
      <span class="side-label">Schwarz<span v-if="playerColor === 'BLACK'"> (du)</span></span>
    </div>
    <div class="row items-center no-wrap q-mt-xs compact-text">
      <span class="ellipsis"><span data-testid="evaluation-text">{{ sentence }}</span><span v-if="perspective" class="text-grey-7"> · {{ perspective }}</span></span>
      <q-spinner v-if="loading" size="12px" color="grey-7" class="q-ml-xs" aria-label="Bewertung wird aktualisiert" />
      <span v-if="failed" class="text-warning q-ml-xs">· nicht aktuell</span>
    </div>
  </div>

  <q-card v-else flat bordered class="evaluation-bar" data-testid="evaluation-bar">
    <q-card-section class="q-py-sm">
      <div class="row items-center justify-between text-caption text-grey-8">
        <span>Weiß<span v-if="playerColor === 'WHITE'"> (du)</span></span>
        <span class="row items-center q-gutter-xs">
          <span>Wer liegt vorne?</span>
          <q-spinner v-if="loading" size="12px" color="grey-7" aria-label="Bewertung wird aktualisiert" />
        </span>
        <span>Schwarz<span v-if="playerColor === 'BLACK'"> (du)</span></span>
      </div>
      <div class="track q-my-xs" role="meter" aria-label="Bewertung: Anteil von Weiß" aria-valuemin="0" aria-valuemax="100"
           :aria-valuenow="percent" :aria-valuetext="valueText" data-testid="evaluation-track">
        <div class="white-part" :style="{ width: `${percent}%` }" data-testid="evaluation-white" />
        <div class="center-mark" aria-hidden="true" />
        <div class="thumb" :style="{ left: `${percent}%` }" aria-hidden="true" />
      </div>
      <div class="text-body2" data-testid="evaluation-text">{{ sentence }}</div>
      <div v-if="perspective" class="text-caption text-grey-8">{{ perspective }}</div>
      <div v-if="failed" class="text-caption text-warning">Die Bewertung konnte nicht aktualisiert werden.</div>
    </q-card-section>
  </q-card>
</template>

<style scoped>
.track {
  position: relative;
  height: 16px;
  border-radius: 8px;
  background: #2b2b2b;
  border: 1px solid #9e9e9e;
  overflow: visible;
}
.white-part {
  height: 100%;
  border-radius: 8px 0 0 8px;
  background: #f5f5f5;
  transition: width 0.6s ease;
}
.center-mark {
  position: absolute;
  top: -3px;
  bottom: -3px;
  left: 50%;
  width: 2px;
  background: #e53935;
  opacity: 0.7;
}
.thumb {
  position: absolute;
  top: 50%;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #1976d2;
  border: 2px solid #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
  transform: translate(-50%, -50%);
  transition: left 0.6s ease;
}
.compact .track { height: 12px; }
.compact .thumb { width: 18px; height: 18px; }
.compact .side-label { font-size: 12px; color: #4b5563; white-space: nowrap; }
.compact-text { font-size: 13px; color: #1f2937; min-width: 0; }
@media (prefers-reduced-motion: reduce) {
  .white-part, .thumb { transition: none; }
}
</style>
