<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useQuasar } from 'quasar';
import ChessBoard from '@/components/ChessBoard.vue';
import MoveList from '@/components/MoveList.vue';
import GameControls from '@/components/GameControls.vue';
import EngineStatus from '@/components/EngineStatus.vue';
import CoachingSummary from '@/components/CoachingSummary.vue';
import CoachingDetails from '@/components/CoachingDetails.vue';
import VariationViewer from '@/components/VariationViewer.vue';
import SettingsDialog from '@/components/SettingsDialog.vue';
import { useGameStore } from '@/stores/gameStore';
import { useAnalysisStore } from '@/stores/analysisStore';
import { useSettingsStore } from '@/stores/settingsStore';
import { gameApi } from '@/services/gameApi';
import { DIFFICULTY_LABELS } from '@/models/game';

const $q = useQuasar();
const game = useGameStore();
const analysis = useAnalysisStore();
const settings = useSettingsStore();

const showSettings = ref(false);
const showDetails = ref(settings.autoOpenDetails);
const flipped = ref(false);
const highlighted = ref<string[]>([]);
const engineName = ref<string | undefined>();
const engineAvailable = ref(true);

const g = computed(() => game.game);
const orientation = computed(() => {
  const base = g.value?.playerColor ?? 'WHITE';
  return flipped.value ? (base === 'WHITE' ? 'BLACK' : 'WHITE') : base;
});
const inVariation = computed(() => !!analysis.variationFen);
const boardFen = computed(() => analysis.variationFen ?? game.displayedFen ?? 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1');
const boardLastMove = computed(() => (inVariation.value ? analysis.variationLastMove : game.displayedLastMove));
const boardCheck = computed(() => (!inVariation.value && game.isLive ? g.value?.checkSquare ?? null : null));
const interactive = computed(() => game.canMove && !inVariation.value);
const pendingAnalysis = computed(() => Object.values(analysis.byPly).find((a) => a.status === 'PENDING' || a.status === 'RUNNING'));
const hintSquares = computed(() => {
  if (!game.hint || !game.isLive) return [];
  return game.hintLevel >= 2 ? [game.hint.from, game.hint.uci.slice(2, 4)] : [game.hint.from];
});
const marked = computed(() => (highlighted.value.length ? highlighted.value : hintSquares.value));
const statusMessage = computed(() => {
  if (!g.value) return 'Partie wird geladen …';
  if (!game.isLive) return `Ansicht nach Halbzug ${game.viewPly} – zum Weiterspielen zur aktuellen Stellung zurückkehren.`;
  return g.value.statusText;
});

onMounted(async () => {
  try {
    const status = await gameApi.engineStatus();
    engineAvailable.value = status.available;
    const info = status.engines.find((e) => e.role === 'analysis') ?? status.engines[0];
    engineName.value = info ? `${info.name} ${info.version}` : undefined;
    if (info) analysis.setVersions(info.version, (await gameApi.settingsOptions()).rulesVersion);
  } catch {
    engineAvailable.value = false;
  }
  await game.init();
});

watch(() => game.error, (message) => {
  if (message) {
    $q.notify({ type: 'warning', message, timeout: 6000, actions: [{ label: 'OK', color: 'white' }] });
    game.clearError();
  }
});

watch(() => g.value?.status, (status, previous) => {
  if (status && previous === 'IN_PROGRESS' && status !== 'IN_PROGRESS') {
    $q.notify({ type: 'info', message: g.value?.statusText ?? 'Partie beendet', timeout: 8000, icon: 'emoji_events' });
  }
});

function confirmNewGame() {
  if (g.value && g.value.moves.length > 0 && !game.finished) {
    $q.dialog({
      title: 'Neue Partie starten?',
      message: 'Die laufende Partie wird beendet. Sie ist im Browser gespeichert, wird aber durch die neue Partie ersetzt.',
      cancel: { label: 'Abbrechen', flat: true },
      ok: { label: 'Neue Partie', color: 'primary' },
    }).onOk(() => void startNew());
  } else {
    void startNew();
  }
}

async function startNew() {
  if (g.value && g.value.moves.length > 0) {
    await game.persist();
  }
  await game.newGame();
}

function confirmResign() {
  $q.dialog({
    title: 'Aufgeben?',
    message: 'Die Partie wird als verloren gewertet. Die Analyse bleibt verfügbar.',
    cancel: { label: 'Abbrechen', flat: true },
    ok: { label: 'Aufgeben', color: 'negative' },
  }).onOk(() => void game.resign());
}

function onCandidate(uci: string) {
  if (analysis.selectedPly != null) analysis.showVariation(analysis.selectedPly, uci);
}
</script>

<template>
  <q-page class="game-page q-pa-sm q-pa-md-md">
    <div class="game-layout">
      <section class="board-column" aria-label="Brett und Spielsteuerung">
        <div class="row items-center q-mb-sm">
          <div class="col">
            <div class="text-subtitle1 text-weight-medium" role="status" aria-live="polite" data-testid="game-status">{{ statusMessage }}</div>
            <div v-if="g" class="text-caption text-grey-8">
              Computergegner · {{ DIFFICULTY_LABELS[g.difficulty] }} · Du spielst {{ g.playerColor === 'WHITE' ? 'Weiß' : 'Schwarz' }}
            </div>
          </div>
          <q-chip v-if="game.finished" color="grey-9" text-color="white" icon="emoji_events" data-testid="game-result">{{ g?.result }}</q-chip>
        </div>

        <VariationViewer v-if="inVariation && analysis.selectedCandidate" class="q-mb-sm" :candidate="analysis.selectedCandidate"
                         :step="analysis.variation?.step ?? 0"
                         :start-fen="analysis.byPly[analysis.variation!.ply]?.fenBefore ?? boardFen" @step="analysis.stepVariation" @exit="analysis.exitVariation" />

        <ChessBoard
          :fen="boardFen"
          :orientation="orientation"
          :legal-moves="interactive ? g?.legalMoves ?? [] : []"
          :interactive="interactive"
          :last-move="boardLastMove"
          :check-square="boardCheck"
          :marked-squares="marked"
          :show-coordinates="settings.showCoordinates"
          @move="game.playMove"
        />

        <div v-if="game.hint && game.isLive" class="q-mt-sm text-body2" role="status" data-testid="hint">
          <q-icon name="lightbulb" color="amber-8" />
          <template v-if="game.hintLevel < 2"> Tipp: Ziehe mit deinem {{ game.hint.pieceName }} auf {{ game.hint.from }}. (Nochmals „Tipp“ zeigt den Zug.)</template>
          <template v-else> Tipp: {{ game.hint.san }} (Bewertung laut Engine {{ game.hint.score }})</template>
        </div>

        <EngineStatus class="q-mt-sm" :thinking="game.computerThinking" :analysing="!!pendingAnalysis" :offline="game.offline"
                      :engine-available="engineAvailable && g?.engineStatus !== 'UNAVAILABLE'" :engine-name="engineName"
                      :stage="pendingAnalysis?.stage" :progress="pendingAnalysis?.progressPercent" @retry="game.reconnect" />

        <GameControls class="q-mt-sm" :can-undo="!!g?.canUndo" :can-redo="!!g?.canRedo" :can-hint="game.canMove"
                      :can-resign="!!g && !game.finished && g.moves.length > 0" :busy="game.busy !== 'idle'"
                      @new="confirmNewGame" @undo="game.undo" @redo="game.redo" @hint="game.requestHint"
                      @resign="confirmResign" @settings="showSettings = true" @flip="flipped = !flipped" />

        <q-banner v-if="game.storageWarning" dense rounded class="bg-orange-2 text-black q-mt-sm" role="alert">
          <template #avatar><q-icon name="save" color="orange-9" /></template>
          {{ game.storageWarning }}
        </q-banner>
        <div v-else-if="game.lastSaved" class="text-caption text-grey-7 q-mt-xs text-center">Im Browser gespeichert</div>
      </section>

      <section class="coaching-column" aria-label="Zug-Coaching">
        <h2 class="text-h6 q-my-sm">Zug-Coaching</h2>
        <CoachingSummary :analysis="analysis.selected" :error="analysis.selectedPly != null ? analysis.errors[analysis.selectedPly] : null"
                         @details="showDetails = !showDetails"
                         @retry="analysis.selectedPly != null && analysis.requestAgain(analysis.selectedPly, true)" />
        <CoachingDetails v-if="showDetails && analysis.selected?.status === 'COMPLETED'" class="q-mt-sm" :analysis="analysis.selected"
                         :selected-candidate="analysis.variation?.uci ?? null" @candidate="onCandidate"
                         @highlight="highlighted = $event"
                         @deepen="analysis.selectedPly != null && analysis.deepen(analysis.selectedPly)"
                         @reanalyse="analysis.selectedPly != null && analysis.requestAgain(analysis.selectedPly, true)" />
        <MoveList class="q-mt-md" :moves="g?.moves ?? []" :current-ply="game.viewPly" :analyses="analysis.byPly" @select="game.navigate" />
      </section>
    </div>

    <SettingsDialog v-model="showSettings" @apply="game.applySettings" @new-game="confirmNewGame" />
  </q-page>
</template>

<style scoped>
.game-layout {
  display: grid;
  grid-template-columns: minmax(280px, 640px) minmax(300px, 1fr);
  gap: 20px;
  align-items: start;
  max-width: 1300px;
  margin: 0 auto;
}
.game-layout > section { min-width: 0; }
@media (max-width: 1023px) {
  .game-layout { grid-template-columns: 1fr; }
}
</style>
