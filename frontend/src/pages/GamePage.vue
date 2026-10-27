<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useQuasar } from 'quasar';
import ChessBoard from '@/components/ChessBoard.vue';
import MoveList from '@/components/MoveList.vue';
import GameControls from '@/components/GameControls.vue';
import EngineStatus from '@/components/EngineStatus.vue';
import CoachingSummary from '@/components/CoachingSummary.vue';
import CoachingDetails from '@/components/CoachingDetails.vue';
import EvaluationBar from '@/components/EvaluationBar.vue';
import VariationViewer from '@/components/VariationViewer.vue';
import SettingsDialog from '@/components/SettingsDialog.vue';
import { useGameStore } from '@/stores/gameStore';
import { useAnalysisStore } from '@/stores/analysisStore';
import { useSettingsStore } from '@/stores/settingsStore';
import { useEvaluationStore } from '@/stores/evaluationStore';
import { gameApi } from '@/services/gameApi';
import { DIFFICULTY_LABELS } from '@/models/game';
import { CLASSIFICATION_COLORS, CLASSIFICATION_ICONS } from '@/models/analysis';

const $q = useQuasar();
const game = useGameStore();
const analysis = useAnalysisStore();
const settings = useSettingsStore();
const evaluation = useEvaluationStore();

const showSettings = ref(false);
const showDetails = ref(settings.autoOpenDetails);
const flipped = ref(false);
const highlighted = ref<string[]>([]);
const engineName = ref<string | undefined>();
const engineAvailable = ref(true);

const g = computed(() => game.game);
/** Handy und kleines Tablet: Aktionsleiste unten, kompakte Rückmeldung direkt unter dem Brett. */
const compact = computed(() => $q.screen.lt.md);
/** Handy quer: Brett und Coaching nebeneinander. */
const landscape = computed(() => compact.value && $q.screen.width > $q.screen.height);
const portraitCompact = computed(() => compact.value && !landscape.value);
/** Kleine Handys (z. B. iPhone SE): Zusatzzeile unter dem Status weglassen, damit Coach-Vorschau und Brett passen. */
const shortScreen = computed(() => compact.value && $q.screen.height < 700);
const engineProblem = computed(() => game.offline || !engineAvailable.value || g.value?.engineStatus === 'UNAVAILABLE');
const peek = computed(() => {
  const a = analysis.selected;
  if (!a) return null;
  const running = a.status === 'PENDING' || a.status === 'RUNNING';
  const done = a.status === 'COMPLETED' && !!a.classification;
  return {
    running,
    label: done ? a.classificationLabel ?? '' : running ? 'Analyse läuft' : 'Keine Analyse',
    icon: done ? CLASSIFICATION_ICONS[a.classification!] : running ? 'hourglass_empty' : 'warning',
    color: done ? CLASSIFICATION_COLORS[a.classification!] : running ? 'grey-7' : 'warning',
    text: done ? a.mainReason ?? a.headline ?? '' : running ? `Dein Zug ${a.playedMove} wird bewertet …` : a.errorMessage ?? '',
  };
});

function scrollToCoaching() {
  document.getElementById('coaching')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// Bewertungsleiste nach jedem Zug (jede neue Positionsversion: Benutzer- und Computerzug, Undo, Redo, neue Partie).
watch(() => [g.value?.gameId, g.value?.positionVersion] as const, ([gameId, version]) => {
  if (gameId && version !== undefined) void evaluation.refresh(gameId, version);
}, { immediate: true });
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
    $q.notify({ type: 'warning', message, timeout: 6000, position: compact.value ? 'top' : 'bottom',
      actions: [{ label: 'OK', color: 'white' }] });
    game.clearError();
  }
});

watch(() => g.value?.status, (status, previous) => {
  if (status && previous === 'IN_PROGRESS' && status !== 'IN_PROGRESS') {
    $q.notify({ type: 'info', message: g.value?.statusText ?? 'Partie beendet', timeout: 8000, icon: 'emoji_events',
      position: compact.value ? 'top' : 'bottom' });
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
  <q-page class="game-page q-pa-sm q-pa-md-md" :class="{ compact, landscape }">
    <div class="game-layout">
      <section class="board-column" aria-label="Brett und Spielsteuerung">
        <div class="row items-center q-mb-sm">
          <div class="col">
            <div class="row items-center no-wrap q-gutter-x-sm">
              <div class="text-subtitle1 text-weight-medium" role="status" aria-live="polite" data-testid="game-status">{{ statusMessage }}</div>
              <q-spinner-dots v-if="compact && game.computerThinking" color="primary" size="22px" aria-label="Der Computer rechnet" />
            </div>
            <div v-if="g && !shortScreen" class="text-caption text-grey-8 ellipsis">
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

        <div v-if="portraitCompact" class="mobile-feedback q-mt-sm" data-testid="mobile-feedback">
          <EvaluationBar compact :evaluation="evaluation.evaluation" :loading="evaluation.loading" :failed="evaluation.failed"
                         :player-color="g?.playerColor ?? 'WHITE'" />
          <button v-if="peek" type="button" class="coach-peek q-mt-sm" data-testid="coach-peek"
                  aria-label="Zum ausführlichen Zug-Coaching" @click="scrollToCoaching">
            <q-spinner-hourglass v-if="peek.running" color="secondary" size="20px" />
            <q-icon v-else :name="peek.icon" :color="peek.color" size="22px" />
            <span class="peek-label" :class="`text-${peek.color}`">{{ peek.label }}</span>
            <span class="peek-text ellipsis">{{ peek.text }}</span>
            <q-icon name="expand_more" size="22px" color="grey-7" />
          </button>
          <div v-else class="coach-peek placeholder text-grey-7">Spiele einen Zug – der Coach bewertet ihn sofort.</div>
        </div>

        <div v-if="game.hint && game.isLive" class="q-mt-sm text-body2" role="status" data-testid="hint">
          <q-icon name="lightbulb" color="amber-8" />
          <template v-if="game.hintLevel < 2"> Tipp: Ziehe mit deinem {{ game.hint.pieceName }} auf {{ game.hint.from }}. (Nochmals „Tipp“ zeigt den Zug.)</template>
          <template v-else> Tipp: {{ game.hint.san }} (Bewertung laut Engine {{ game.hint.score }})</template>
        </div>

        <EngineStatus v-if="!compact || engineProblem" class="q-mt-sm" :thinking="game.computerThinking" :analysing="!!pendingAnalysis" :offline="game.offline"
                      :engine-available="engineAvailable && g?.engineStatus !== 'UNAVAILABLE'" :engine-name="engineName"
                      :stage="pendingAnalysis?.stage" :progress="pendingAnalysis?.progressPercent" @retry="game.reconnect" />

        <GameControls :class="{ 'q-mt-sm': !compact }" :compact="compact" :can-undo="!!g?.canUndo" :can-redo="!!g?.canRedo" :can-hint="game.canMove"
                      :can-resign="!!g && !game.finished && g.moves.length > 0" :busy="game.busy !== 'idle'"
                      @new="confirmNewGame" @undo="game.undo" @redo="game.redo" @hint="game.requestHint"
                      @resign="confirmResign" @settings="showSettings = true" @flip="flipped = !flipped" />

        <q-banner v-if="game.storageWarning" dense rounded class="bg-orange-2 text-black q-mt-sm" role="alert">
          <template #avatar><q-icon name="save" color="orange-9" /></template>
          {{ game.storageWarning }}
        </q-banner>
        <div v-else-if="game.lastSaved && !landscape" class="text-caption text-grey-7 q-mt-xs text-center">Im Browser gespeichert</div>
      </section>

      <section class="coaching-column" aria-label="Zug-Coaching">
        <h2 id="coaching" class="text-h6 q-my-sm">Zug-Coaching</h2>
        <EvaluationBar v-if="landscape" class="q-mb-sm" compact :evaluation="evaluation.evaluation" :loading="evaluation.loading" :failed="evaluation.failed"
                       :player-color="g?.playerColor ?? 'WHITE'" />
        <CoachingSummary :analysis="analysis.selected" :error="analysis.selectedPly != null ? analysis.errors[analysis.selectedPly] : null"
                         @details="showDetails = !showDetails"
                         @retry="analysis.selectedPly != null && analysis.requestAgain(analysis.selectedPly, true)" />
        <EvaluationBar v-if="!compact" class="q-mt-sm" :evaluation="evaluation.evaluation" :loading="evaluation.loading" :failed="evaluation.failed"
                       :player-color="g?.playerColor ?? 'WHITE'" />
        <CoachingDetails v-if="showDetails && analysis.selected?.status === 'COMPLETED'" class="q-mt-sm" :analysis="analysis.selected"
                         :selected-candidate="analysis.variation?.uci ?? null" @candidate="onCandidate"
                         @highlight="highlighted = $event"
                         @deepen="analysis.selectedPly != null && analysis.deepen(analysis.selectedPly)"
                         @reanalyse="analysis.selectedPly != null && analysis.requestAgain(analysis.selectedPly, true)" />
        <MoveList class="q-mt-md" :moves="g?.moves ?? []" :current-ply="game.viewPly" :analyses="analysis.byPly" @select="game.navigate" />
      </section>
    </div>

    <q-page-scroller v-if="compact" position="bottom-right" :scroll-offset="350" :offset="[14, 84]">
      <q-btn round color="primary" icon="keyboard_arrow_up" aria-label="Zurück zum Brett" data-testid="btn-to-board" />
    </q-page-scroller>

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
  .game-layout { grid-template-columns: 1fr; gap: 8px; }
}

/* Handy: Brett so groß wie möglich, Platz für Bewertung und Coach-Vorschau darunter und die Aktionsleiste unten. */
.game-page.compact {
  padding: 6px 6px calc(76px + env(safe-area-inset-bottom)) !important;
  /* Höhe für Kopfzeile, Status, Bewertung, Coach-Vorschau und Aktionsleiste freihalten. */
  --board-max: min(100%, calc(100vh - 262px));
  --board-max: min(100%, calc(100svh - 262px));
}
.game-page.compact .text-subtitle1 { font-size: 15px; line-height: 1.3; }
.game-page.compact h2 { scroll-margin-top: 56px; }
.mobile-feedback { max-width: 640px; margin-left: auto; margin-right: auto; }
.coach-peek {
  width: 100%;
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid #d6cfc4;
  border-radius: 10px;
  background: #fff;
  font: inherit;
  text-align: left;
  color: #111827;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.coach-peek:active { background: #f3efe8; }
.coach-peek.placeholder { cursor: default; font-size: 13px; }
.peek-label { font-weight: 700; white-space: nowrap; }
.peek-text { flex: 1 1 auto; min-width: 0; font-size: 13px; color: #374151; }

/* Handy quer: Brett links (an der Höhe ausgerichtet), Coaching rechts. */
.game-page.landscape .game-layout { grid-template-columns: auto minmax(0, 1fr); gap: 12px; }
.game-page.landscape .board-column { width: min(58vw, calc(100vh - 130px)); width: min(58vw, calc(100svh - 130px)); }
.game-page.landscape { --board-max: 100%; }
</style>
