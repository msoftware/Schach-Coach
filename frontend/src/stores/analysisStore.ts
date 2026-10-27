import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type { Candidate, MoveAnalysis } from '@/models/analysis';
import { isFinalStatus } from '@/models/analysis';
import type { LearningStats } from '@/models/coaching';
import type { AnalysisProfile } from '@/models/game';
import { analysisApi } from '@/services/analysisApi';
import { ApiError } from '@/services/apiClient';
import { analysisCacheKey, localGameStorage } from '@/services/localGameStorage';

const POLL_MS = 400;

/**
 * Zustand der Coaching-Analysen. Jede Analyse ist an Partie, Halbzug und Zug-ID gebunden; Ergebnisse älterer
 * Partieversionen (z. B. nach Undo) werden verworfen. Varianten werden nur angezeigt, nie in die Partie übernommen.
 */
export const useAnalysisStore = defineStore('analysis', () => {
  const byPly = ref<Record<number, MoveAnalysis>>({});
  const errors = ref<Record<number, string>>({});
  const selectedPly = ref<number | null>(null);
  const variation = ref<{ ply: number; uci: string; step: number } | null>(null);
  const stats = ref<LearningStats | null>(null);
  const engineVersion = ref('6.1.1');
  const rulesVersion = ref('1.0.0');

  let gameId: string | null = null;
  const moveIds: Record<number, string> = {};
  const pollers = new Map<number, ReturnType<typeof setTimeout>>();
  const waiters = new Map<number, Array<() => void>>();

  const selected = computed(() => (selectedPly.value == null ? null : byPly.value[selectedPly.value] ?? null));

  const selectedCandidate = computed<Candidate | null>(() => {
    if (!variation.value) return null;
    const a = byPly.value[variation.value.ply];
    return a?.candidates?.find((c) => c.uci === variation.value?.uci) ?? null;
  });

  /** Stellung, die während der Variantenanzeige auf dem Brett gezeigt wird (oder null). */
  const variationFen = computed<string | null>(() => {
    const v = variation.value;
    const c = selectedCandidate.value;
    if (!v || !c) return null;
    if (v.step === 0) return byPly.value[v.ply]?.fenBefore ?? null;
    return c.line[v.step - 1]?.fenAfter ?? null;
  });

  const variationLastMove = computed(() => {
    const v = variation.value;
    const c = selectedCandidate.value;
    if (!v || !c || v.step === 0) return null;
    const m = c.line[v.step - 1];
    return m ? { from: m.uci.slice(0, 2), to: m.uci.slice(2, 4) } : null;
  });

  function reset(newGameId: string | null) {
    pollers.forEach((t) => clearTimeout(t));
    pollers.clear();
    waiters.forEach((list) => list.forEach((resolve) => resolve()));
    waiters.clear();
    byPly.value = {};
    errors.value = {};
    selectedPly.value = null;
    variation.value = null;
    gameId = newGameId;
    Object.keys(moveIds).forEach((k) => delete moveIds[Number(k)]);
  }

  function setVersions(engine: string, rules: string) {
    engineVersion.value = engine;
    rulesVersion.value = rules;
  }

  function resolveWaiters(ply: number) {
    waiters.get(ply)?.forEach((resolve) => resolve());
    waiters.delete(ply);
  }

  /** Verfolgt die Analyse eines Benutzerzuges bis zum Abschluss. */
  async function track(currentGameId: string, ply: number, moveId: string, fenBefore: string, uci: string,
                       profile: AnalysisProfile, select = true) {
    if (currentGameId !== gameId) reset(currentGameId);
    moveIds[ply] = moveId;
    if (select) {
      selectedPly.value = ply;
      variation.value = null;
    }
    const key = analysisCacheKey(fenBefore, uci, engineVersion.value, profile, 1, rulesVersion.value);
    const cached = await localGameStorage.getCachedAnalysis(key);
    if (cached && cached.status === 'COMPLETED' && moveIds[ply] === moveId) {
      // Gecachte Analyse gilt nur für identische Stellung, Engine-Version, Parameter und Regelversion.
      byPly.value = { ...byPly.value, [ply]: { ...cached, ply, moveId, gameId: currentGameId, outdated: false } };
      resolveWaiters(ply);
      return;
    }
    poll(ply, moveId);
  }

  function poll(ply: number, moveId: string) {
    const existing = pollers.get(ply);
    if (existing) clearTimeout(existing);
    const run = async () => {
      if (!gameId || moveIds[ply] !== moveId) return;
      try {
        const a = await analysisApi.get(gameId, ply);
        if (moveIds[ply] !== moveId || a.moveId !== moveId) return; // veraltet (Undo/Neustart)
        byPly.value = { ...byPly.value, [ply]: a };
        delete errors.value[ply];
        if (isFinalStatus(a.status)) {
          pollers.delete(ply);
          resolveWaiters(ply);
          if (a.status === 'COMPLETED') await onCompleted(a);
          return;
        }
      } catch (e) {
        if (e instanceof ApiError && e.status === 404) {
          pollers.delete(ply);
          resolveWaiters(ply);
          return;
        }
        errors.value = { ...errors.value, [ply]: e instanceof Error ? e.message : 'Analyse nicht verfügbar' };
      }
      pollers.set(ply, setTimeout(run, POLL_MS));
    };
    pollers.set(ply, setTimeout(run, 50));
  }

  async function onCompleted(a: MoveAnalysis) {
    const key = analysisCacheKey(a.fenBefore, a.playedMoveUci, a.engine?.version ?? engineVersion.value,
      currentProfile ?? 'NORMAL', a.depthLevel, a.rulesVersion ?? rulesVersion.value);
    if (!a.outdated) void localGameStorage.cacheAnalysis(key, a);
    await recordStats(a);
  }

  let currentProfile: AnalysisProfile | null = null;
  function setProfile(profile: AnalysisProfile) {
    currentProfile = profile;
  }

  /** Wartet auf das Ende einer Analyse (höchstens {@code timeoutMs}). */
  function waitFor(ply: number, timeoutMs: number): Promise<void> {
    const a = byPly.value[ply];
    if (a && isFinalStatus(a.status)) return Promise.resolve();
    return new Promise((resolve) => {
      const timer = setTimeout(resolve, timeoutMs);
      const list = waiters.get(ply) ?? [];
      list.push(() => {
        clearTimeout(timer);
        resolve();
      });
      waiters.set(ply, list);
    });
  }

  /** Verwirft Analysen ab einem Halbzug (z. B. nach Undo). */
  function discardFrom(ply: number) {
    const next = { ...byPly.value };
    for (const key of Object.keys(next).map(Number)) {
      if (key >= ply) {
        delete next[key];
        delete moveIds[key];
        const t = pollers.get(key);
        if (t) clearTimeout(t);
        pollers.delete(key);
        resolveWaiters(key);
      }
    }
    byPly.value = next;
    if (selectedPly.value != null && selectedPly.value >= ply) selectedPly.value = null;
    if (variation.value && variation.value.ply >= ply) variation.value = null;
  }

  async function requestAgain(ply: number, force: boolean) {
    if (!gameId) return;
    const a = await analysisApi.request(gameId, ply, force);
    moveIds[ply] = a.moveId;
    byPly.value = { ...byPly.value, [ply]: a };
    poll(ply, a.moveId);
  }

  async function deepen(ply: number) {
    if (!gameId) return;
    const a = await analysisApi.deepen(gameId, ply);
    moveIds[ply] = a.moveId;
    byPly.value = { ...byPly.value, [ply]: a };
    poll(ply, a.moveId);
  }

  function select(ply: number | null) {
    selectedPly.value = ply;
    variation.value = null;
  }

  function showVariation(ply: number, uci: string) {
    variation.value = { ply, uci, step: 1 };
  }

  function stepVariation(delta: number) {
    const v = variation.value;
    const c = selectedCandidate.value;
    if (!v || !c) return;
    variation.value = { ...v, step: Math.max(0, Math.min(c.line.length, v.step + delta)) };
  }

  function exitVariation() {
    variation.value = null;
  }

  async function loadStats() {
    stats.value = (await localGameStorage.loadLearningStats()) ?? null;
  }

  /** Wiederkehrende Fehler als Lernmuster (nur lokal, ohne Benutzerkonto). */
  async function recordStats(a: MoveAnalysis) {
    const current: LearningStats = stats.value ?? (await localGameStorage.loadLearningStats()) ?? {
      id: 'stats', analysedMoves: 0, classifications: {}, ruleCounts: {}, updatedAt: new Date().toISOString(),
    };
    const next: LearningStats = JSON.parse(JSON.stringify(current)) as LearningStats;
    next.analysedMoves++;
    if (a.classification) next.classifications[a.classification] = (next.classifications[a.classification] ?? 0) + 1;
    for (const tag of a.learningTags ?? []) {
      const title = a.findings?.find((f) => f.ruleId === tag)?.title ?? tag;
      const entry = next.ruleCounts[tag] ?? { title, count: 0, lastSeen: '' };
      next.ruleCounts[tag] = { title, count: entry.count + 1, lastSeen: new Date().toISOString() };
    }
    next.updatedAt = new Date().toISOString();
    stats.value = next;
    await localGameStorage.saveLearningStats(next);
  }

  return {
    byPly, errors, selectedPly, selected, variation, selectedCandidate, variationFen, variationLastMove, stats,
    reset, setVersions, setProfile, track, waitFor, discardFrom, requestAgain, deepen, select, showVariation,
    stepVariation, exitVariation, loadStats,
  };
});
