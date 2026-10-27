<script setup lang="ts">
import { computed } from 'vue';
import type { Candidate } from '@/models/analysis';
import { describeMove } from '@/utils/moveText';

const props = defineProps<{ candidate: Candidate; step: number; startFen: string }>();
const emit = defineEmits<{ step: [delta: number]; exit: [] }>();

const total = computed(() => props.candidate.line.length);
/** Zug des aktuellen Schritts in Worten (Stellung davor: Ausgangsstellung bzw. Stellung nach dem vorigen Zug). */
const currentText = computed(() => {
  const m = props.candidate.line[props.step - 1];
  if (!m) return '';
  const before = props.step === 1 ? props.startFen : props.candidate.line[props.step - 2].fenAfter;
  const side = before.split(' ')[1] === 'w' ? 'Weiß' : 'Schwarz';
  return `${side}: ${describeMove(before, m.uci, m.san)}`;
});
</script>

<template>
  <q-banner class="variation-viewer bg-blue-1 text-black" rounded dense role="region" aria-label="Variantenanzeige"
            data-testid="variation-viewer">
    <template #avatar><q-icon name="alt_route" color="primary" /></template>
    <div class="text-weight-medium">Variante {{ candidate.san }} – nicht Teil der Partie</div>
    <div class="text-body2" aria-live="polite">
      <template v-if="step === 0">Ausgangsstellung</template>
      <template v-else>Zug {{ step }} von {{ total }}: <strong>{{ currentText }}</strong>
        <span class="text-grey-8">({{ candidate.line[step - 1]?.numbered }})</span></template>
    </div>
    <div class="variation-moves q-mt-xs">
      <span v-for="(m, i) in candidate.line" :key="i" class="vm" :class="{ current: i + 1 === step }">{{ m.numbered }}</span>
    </div>
    <template #action>
      <q-btn flat round icon="skip_previous" aria-label="Zum Variantenanfang" :disable="step === 0" @click="emit('step', -999)" />
      <q-btn flat round icon="chevron_left" aria-label="Variante zurück" :disable="step === 0" data-testid="variation-back"
             @click="emit('step', -1)" />
      <q-btn flat round icon="chevron_right" aria-label="Variante vor" :disable="step >= total" data-testid="variation-forward"
             @click="emit('step', 1)" />
      <q-btn flat color="primary" label="Variante verlassen" no-caps data-testid="variation-exit" @click="emit('exit')" />
    </template>
  </q-banner>
</template>

<style scoped>
.variation-moves { font-family: ui-monospace, Consolas, monospace; font-size: 13px; display: flex; flex-wrap: wrap; gap: 2px 8px; }
.variation-moves .current { background: #1d4ed8; color: #fff; border-radius: 3px; padding: 0 2px; }
</style>
