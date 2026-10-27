import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type { Game, Hint, MoveRecord } from '@/models/game';
import { isFinished } from '@/models/game';
import { gameApi } from '@/services/gameApi';
import { ApiError } from '@/services/apiClient';
import { validateGame } from '@/services/validate';
import { localGameStorage, type StoredGame } from '@/services/localGameStorage';
import { useAnalysisStore } from './analysisStore';
import { useSettingsStore } from './settingsStore';

export type Busy = 'idle' | 'starting' | 'move' | 'engine' | 'undo' | 'redo' | 'resign' | 'hint';

/**
 * Partiezustand im Frontend. Die Partie selbst (Regeln, legale Züge, Partieende) verwaltet das Backend;
 * dieser Store hält die zuletzt bestätigte Momentaufnahme, steuert den Ablauf (Benutzerzug → Coaching →
 * Computerzug) und speichert die Partie lokal im Browser.
 */
export const useGameStore = defineStore('game', () => {
  const game = ref<Game | null>(null);
  const busy = ref<Busy>('idle');
  const offline = ref(false);
  const error = ref<string | null>(null);
  const storageWarning = ref<string | null>(null);
  const lastSaved = ref<string | null>(null);
  /** Angezeigter Halbzug beim Navigieren in der Zugliste (null = aktuelle Stellung). */
  const viewPly = ref<number | null>(null);
  const hint = ref<Hint | null>(null);
  const hintLevel = ref(0);

  const analysis = useAnalysisStore();
  const settings = useSettingsStore();

  const isLive = computed(() => viewPly.value === null);
  const finished = computed(() => (game.value ? isFinished(game.value.status) : false));
  const userToMove = computed(() => !!game.value && game.value.sideToMove === game.value.playerColor && !finished.value);
  const canMove = computed(() => userToMove.value && busy.value === 'idle' && isLive.value && !offline.value);
  const computerThinking = computed(() => busy.value === 'engine');

  const displayedFen = computed(() => {
    if (!game.value) return null;
    if (viewPly.value === null) return game.value.fen;
    if (viewPly.value === 0) return game.value.initialFen;
    return game.value.moves[viewPly.value - 1]?.fenAfter ?? game.value.fen;
  });

  const displayedLastMove = computed(() => {
    if (!game.value) return null;
    const ply = viewPly.value ?? game.value.moves.length;
    const m = game.value.moves[ply - 1];
    return m ? { from: m.from, to: m.to } : null;
  });

  function setGame(g: Game) {
    game.value = g;
    offline.value = false;
    error.value = null;
  }

  function handleError(e: unknown) {
    if (e instanceof ApiError) {
      if (e.offline) {
        offline.value = true;
        error.value = e.message;
        return;
      }
      if (e.code === 'POSITION_VERSION_CONFLICT' && e.problem.currentState) {
        // Der Server ist maßgeblich: aktuellen Zustand übernehmen.
        setGame(validateGame(e.problem.currentState));
        error.value = 'Die Partie wurde zwischenzeitlich verändert. Die aktuelle Stellung wurde übernommen.';
        return;
      }
      if (e.code === 'GAME_NOT_FOUND') {
        error.value = 'Die Partie ist auf dem Server nicht mehr vorhanden. Sie wird aus dem Browser-Speicher wiederhergestellt.';
        void restoreFromStorage();
        return;
      }
      error.value = e.message;
      if (e.problem.currentState) setGame(validateGame(e.problem.currentState));
      return;
    }
    error.value = e instanceof Error ? e.message : 'Unbekannter Fehler';
  }

  async function persist() {
    const g = game.value;
    if (!g) return;
    const record: Omit<StoredGame, 'id'> = {
      gameId: g.gameId,
      initialFen: g.initialFen,
      moves: g.moves.map((m) => m.uci),
      sanMoves: g.moves.map((m) => m.san),
      fen: g.fen,
      playerColor: g.playerColor,
      difficulty: g.difficulty,
      analysisProfile: g.analysisProfile,
      status: g.status,
      result: g.result,
      savedAt: new Date().toISOString(),
      finished: isFinished(g.status),
    };
    const result = await localGameStorage.saveCurrentGame(record);
    if (result.ok) {
      storageWarning.value = null;
      lastSaved.value = record.savedAt;
      if (record.finished && g.moves.length > 0) await localGameStorage.archiveGame(record);
    } else {
      storageWarning.value = 'Die Partie konnte nicht im Browser gespeichert werden. Sie läuft im Arbeitsspeicher weiter, '
        + 'geht aber beim Neuladen verloren.';
      lastSaved.value = null;
    }
  }

  async function newGame() {
    busy.value = 'starting';
    try {
      const g = await gameApi.create({
        playerColor: settings.playerColor,
        difficulty: settings.difficulty,
        analysisProfile: settings.analysisProfile,
      });
      analysis.reset(g.gameId);
      analysis.setProfile(g.analysisProfile);
      viewPly.value = null;
      hint.value = null;
      setGame(g);
      await persist();
    } catch (e) {
      handleError(e);
    } finally {
      busy.value = 'idle';
    }
    await continueIfComputerToMove();
  }

  /**
   * Stellt die lokal gespeicherte Partie wieder her: Der Server rekonstruiert die Stellung aus der Zughistorie,
   * anschließend wird sie mit der gespeicherten FEN verglichen.
   */
  async function restoreFromStorage(): Promise<'restored' | 'none' | 'inconsistent' | 'failed'> {
    const stored = await localGameStorage.loadCurrentGame();
    if (!stored || stored.finished) return 'none';
    busy.value = 'starting';
    try {
      const g = await gameApi.create({
        playerColor: stored.playerColor,
        difficulty: stored.difficulty,
        analysisProfile: stored.analysisProfile,
        initialFen: stored.initialFen,
        moves: stored.moves,
      });
      if (g.fen !== stored.fen) {
        error.value = 'Die gespeicherte Partie ist inkonsistent (rekonstruierte Stellung weicht ab). Es wurde der aus der '
          + 'Zughistorie rekonstruierte Stand übernommen.';
      }
      analysis.reset(g.gameId);
      analysis.setProfile(g.analysisProfile);
      viewPly.value = null;
      game.value = g;
      offline.value = false;
      await persist();
      busy.value = 'idle';
      await continueIfComputerToMove();
      return g.fen === stored.fen ? 'restored' : 'inconsistent';
    } catch (e) {
      handleError(e);
      if (e instanceof ApiError && e.code === 'INVALID_RESTORE') {
        error.value = 'Die gespeicherte Partie konnte nicht wiederhergestellt werden (ungültige Zugfolge). Bitte starte eine neue Partie.';
        return 'inconsistent';
      }
      return 'failed';
    } finally {
      busy.value = 'idle';
    }
  }

  async function init() {
    const restored = await restoreFromStorage();
    if (restored === 'none' || restored === 'inconsistent' && !game.value) await newGame();
  }

  async function playMove(uci: string) {
    const g = game.value;
    if (!g || !canMove.value) return;
    busy.value = 'move';
    hint.value = null;
    hintLevel.value = 0;
    let ply = -1;
    let record: MoveRecord | undefined;
    try {
      const result = await gameApi.move(g.gameId, uci, g.positionVersion);
      setGame(result.game);
      ply = result.ply;
      record = result.game.moves[ply - 1];
      await persist();
    } catch (e) {
      handleError(e);
    } finally {
      busy.value = 'idle';
    }
    if (record && game.value) {
      void analysis.track(game.value.gameId, record.ply, record.moveId, record.fenBefore, record.uci, game.value.analysisProfile);
      if (!finished.value) {
        if (settings.waitForCoaching) await analysis.waitFor(ply, 20_000);
        await computerMove();
      }
    }
  }

  async function computerMove() {
    const g = game.value;
    if (!g || finished.value || g.sideToMove === g.playerColor) return;
    busy.value = 'engine';
    try {
      const result = await gameApi.engineMove(g.gameId, g.positionVersion);
      setGame(result.game);
      await persist();
    } catch (e) {
      handleError(e);
    } finally {
      busy.value = 'idle';
    }
  }

  async function continueIfComputerToMove() {
    if (game.value && !finished.value && game.value.sideToMove !== game.value.playerColor) await computerMove();
  }

  async function undo() {
    const g = game.value;
    if (!g || !g.canUndo || busy.value !== 'idle') return;
    busy.value = 'undo';
    try {
      const updated = await gameApi.undo(g.gameId, g.positionVersion);
      analysis.discardFrom(updated.moves.length + 1);
      viewPly.value = null;
      setGame(updated);
      await persist();
    } catch (e) {
      handleError(e);
    } finally {
      busy.value = 'idle';
    }
  }

  async function redo() {
    const g = game.value;
    if (!g || !g.canRedo || busy.value !== 'idle') return;
    busy.value = 'redo';
    try {
      const updated = await gameApi.redo(g.gameId, g.positionVersion);
      setGame(updated);
      viewPly.value = null;
      for (const m of updated.moves.slice(g.moves.length)) {
        if (m.byUser) void analysis.track(updated.gameId, m.ply, m.moveId, m.fenBefore, m.uci, updated.analysisProfile, false);
      }
      await persist();
    } catch (e) {
      handleError(e);
    } finally {
      busy.value = 'idle';
    }
    await continueIfComputerToMove();
  }

  async function resign() {
    const g = game.value;
    if (!g || finished.value) return;
    busy.value = 'resign';
    try {
      setGame(await gameApi.resign(g.gameId, g.positionVersion));
      await persist();
    } catch (e) {
      handleError(e);
    } finally {
      busy.value = 'idle';
    }
  }

  async function requestHint() {
    const g = game.value;
    if (!g || !canMove.value) return;
    if (hint.value) {
      hintLevel.value = Math.min(2, hintLevel.value + 1);
      return;
    }
    busy.value = 'hint';
    try {
      hint.value = await gameApi.hint(g.gameId);
      hintLevel.value = 1;
    } catch (e) {
      handleError(e);
    } finally {
      busy.value = 'idle';
    }
  }

  async function applySettings() {
    const g = game.value;
    if (!g) return;
    try {
      await gameApi.updateSettings(g.gameId, settings.difficulty, settings.analysisProfile);
      analysis.setProfile(settings.analysisProfile);
      setGame(await gameApi.get(g.gameId));
    } catch (e) {
      handleError(e);
    }
  }

  async function reconnect() {
    const g = game.value;
    try {
      if (g) {
        setGame(await gameApi.get(g.gameId));
      } else {
        await init();
      }
    } catch (e) {
      handleError(e);
    }
  }

  function navigate(ply: number | null) {
    const g = game.value;
    if (!g) return;
    if (ply === null || ply >= g.moves.length) {
      viewPly.value = null;
    } else {
      viewPly.value = Math.max(0, ply);
    }
    analysis.exitVariation();
    const shown = viewPly.value ?? g.moves.length;
    const move = g.moves[shown - 1];
    const userMove = move?.byUser ? move : g.moves[shown - 2]?.byUser ? g.moves[shown - 2] : undefined;
    if (userMove) {
      analysis.select(userMove.ply);
      if (!analysis.byPly[userMove.ply] && userMove.analysisStatus) {
        void analysis.track(g.gameId, userMove.ply, userMove.moveId, userMove.fenBefore, userMove.uci, g.analysisProfile, false);
      }
    }
  }

  function clearError() {
    error.value = null;
  }

  return {
    game, busy, offline, error, storageWarning, lastSaved, viewPly, hint, hintLevel,
    isLive, finished, userToMove, canMove, computerThinking, displayedFen, displayedLastMove,
    init, newGame, restoreFromStorage, playMove, computerMove, undo, redo, resign, requestHint, applySettings,
    reconnect, navigate, clearError, persist,
  };
});
