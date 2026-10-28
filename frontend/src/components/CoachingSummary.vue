<script setup lang="ts">
import { computed } from 'vue';
import type { MoveAnalysis } from '@/models/analysis';
import { CLASSIFICATION_COLORS, CLASSIFICATION_ICONS } from '@/models/analysis';
import { describeMove } from '@/utils/moveText';
import { evaluationColor, evaluationWords, pawnsWords } from '@/utils/scoreText';

const props = defineProps<{ analysis: MoveAnalysis | null; error?: string | null }>();
const emit = defineEmits<{ details: []; retry: [] }>();

const a = computed(() => props.analysis);
const completed = computed(() => a.value?.status === 'COMPLETED' && a.value.classification);
const color = computed(() => (a.value?.classification ? CLASSIFICATION_COLORS[a.value.classification] : 'grey-7'));
const icon = computed(() => (a.value?.classification ? CLASSIFICATION_ICONS[a.value.classification] : 'help'));
const plyLabel = computed(() => {
  if (!a.value) return '';
  const number = Number(a.value.fenBefore.split(' ')[5] ?? '1');
  const black = a.value.fenBefore.split(' ')[1] === 'b';
  return `${number}${black ? '...' : '.'}${a.value.playedMove}`;
});
const bestLabel = computed(() => {
  if (!a.value?.bestMove) return null;
  const number = Number(a.value.fenBefore.split(' ')[5] ?? '1');
  const black = a.value.fenBefore.split(' ')[1] === 'b';
  return `${number}${black ? '...' : '.'}${a.value.bestMove}`;
});
const playedText = computed(() => (a.value ? describeMove(a.value.fenBefore, a.value.playedMoveUci, a.value.playedMove) : ''));
const bestText = computed(() => (a.value?.bestMoveUci ? describeMove(a.value.fenBefore, a.value.bestMoveUci, a.value.bestMove) : ''));
const loss = computed(() => a.value?.evaluationLoss?.centipawns ?? null);
/** Ein Bestzug (innerhalb der Toleranz) hat höchstens gleich gute Alternativen – „besser“ wäre irreführend. */
const equalAlternative = computed(() => a.value?.classification === 'BEST');
const alternativeHeading = computed(() => {
  if (a.value?.classification === 'BEST') return 'Gleich gute Alternative';
  if (a.value?.classification === 'EXCELLENT') return 'Etwas genauer wäre gewesen';
  return 'Bessere Alternative';
});
</script>

<template>
  <q-card flat bordered class="coaching-summary" data-testid="coaching-summary" aria-live="polite">
    <q-card-section v-if="!a" class="text-grey-7">
      Spiele einen Zug – danach erscheint hier die Analyse deines Zuges.
    </q-card-section>

    <q-card-section v-else-if="a.status === 'PENDING' || a.status === 'RUNNING'">
      <div class="row items-center q-gutter-sm">
        <q-spinner-hourglass color="secondary" size="22px" />
        <div>
          <div class="text-subtitle1">{{ plyLabel }} wird analysiert …</div>
          <div class="text-caption text-grey-8">{{ a.stage }}</div>
        </div>
      </div>
      <q-linear-progress :value="a.progressPercent / 100" color="secondary" class="q-mt-sm" rounded
                         :aria-label="`Analysefortschritt ${a.progressPercent} Prozent`" />
    </q-card-section>

    <q-card-section v-else-if="a.status === 'FAILED' || a.status === 'CANCELLED'">
      <div class="row items-center q-gutter-sm">
        <q-icon name="warning" color="warning" size="24px" />
        <div class="col">
          <div class="text-subtitle1">Keine Analyse für {{ plyLabel }}</div>
          <div class="text-body2">{{ a.errorMessage ?? 'Die Analyse wurde verworfen.' }}</div>
        </div>
        <q-btn flat color="primary" label="Erneut analysieren" no-caps @click="emit('retry')" />
      </div>
    </q-card-section>

    <q-card-section v-else-if="completed">
      <div class="row items-center no-wrap q-gutter-sm">
        <q-chip :color="color" text-color="white" :icon="icon" class="text-weight-bold" data-testid="classification">
          {{ a.classificationLabel }}
        </q-chip>
        <div class="text-h6" data-testid="played-move">{{ plyLabel }}</div>
        <q-space />
        <q-badge v-if="a.classificationProvisional" color="grey-7" outline>vorläufig</q-badge>
        <q-badge v-if="a.outdated" color="orange-9" outline>veraltet</q-badge>
      </div>
      <div class="text-body1 q-mt-xs" data-testid="played-move-text">{{ playedText }}</div>
      <div class="text-caption text-grey-8">{{ a.explanationTypeLabel }}</div>

      <div class="q-mt-sm">
        <div class="text-subtitle2">Warum?</div>
        <p class="q-mb-sm" data-testid="main-reason">{{ a.mainReason }}</p>
      </div>

      <div v-if="bestLabel && a.bestMoveUci !== a.playedMoveUci" class="q-mb-sm">
        <div class="text-subtitle2" data-testid="alternative-heading">{{ alternativeHeading }}</div>
        <div class="text-body1"><strong>{{ bestText }}</strong> <span class="text-grey-8">({{ bestLabel }})</span></div>
        <div class="text-body2 text-grey-8">Mit diesem Zug: {{ evaluationWords(a.bestScore) }}</div>
        <div v-if="equalAlternative" class="text-body2 text-positive">Dein Zug ist genauso stark – beide sind eine gute Wahl.</div>
      </div>
      <div v-else class="q-mb-sm text-positive">Du hast den besten Zug gespielt, den der Computer gefunden hat.</div>

      <div class="evaluation text-body2" data-testid="evaluation">
        <div>Vor deinem Zug: <strong :class="evaluationColor(a.scoreBefore)">{{ evaluationWords(a.scoreBefore) }}</strong></div>
        <div>Nach deinem Zug: <strong :class="evaluationColor(a.scoreAfter)">{{ evaluationWords(a.scoreAfter) }}</strong></div>
        <div v-if="loss != null && loss >= 30">Verschenkt gegenüber dem besten Zug: <strong>{{ pawnsWords(loss) }}</strong></div>
        <div v-if="a.analysisConfidence !== 'HIGH'" class="text-orange-10">
          Der Computer ist sich hier nicht ganz sicher – nimm die Bewertung als Einschätzung.
        </div>
      </div>
      <div class="text-caption text-grey-7 q-mt-xs">Die Bewertung stammt vom Schachcomputer, die Erklärung von den Coaching-Regeln.</div>
    </q-card-section>

    <q-card-actions v-if="completed" align="right">
      <q-btn flat color="primary" icon="article" label="Details ansehen" no-caps data-testid="btn-details" @click="emit('details')" />
    </q-card-actions>
    <q-card-section v-if="error" class="text-negative text-body2">{{ error }}</q-card-section>
  </q-card>
</template>

<style scoped>
.evaluation { display: grid; gap: 2px; }
</style>
