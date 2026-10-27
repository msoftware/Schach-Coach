import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { Evaluation } from '@/models/game';
import { gameApi } from '@/services/gameApi';

/**
 * Bewertung der aktuellen Stellung für die Bewertungsleiste. Wird nach jeder Änderung der Partie
 * (neue Positionsversion) neu angefordert; Antworten zu veralteten Stellungen werden verworfen.
 */
export const useEvaluationStore = defineStore('evaluation', () => {
  const evaluation = ref<Evaluation | null>(null);
  const loading = ref(false);
  const failed = ref(false);
  let latestRequest = 0;

  async function refresh(gameId: string, positionVersion: number) {
    const requestId = ++latestRequest;
    if (evaluation.value?.gameId !== gameId) evaluation.value = null;
    loading.value = true;
    try {
      const result = await gameApi.evaluate(gameId);
      if (requestId !== latestRequest || result.positionVersion < positionVersion) return;
      evaluation.value = result;
      failed.value = false;
    } catch {
      // Die Leiste ist eine Zusatzanzeige: Fehler blockieren das Spiel nicht, der letzte Stand bleibt sichtbar.
      if (requestId === latestRequest) failed.value = true;
    } finally {
      if (requestId === latestRequest) loading.value = false;
    }
  }

  return { evaluation, loading, failed, refresh };
});
