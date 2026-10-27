import { defineStore } from 'pinia';
import { ref, watch } from 'vue';
import type { AnalysisProfile, Color, Difficulty } from '@/models/game';
import { localGameStorage } from '@/services/localGameStorage';

const KEY = 'chess-coach-settings';

interface StoredSettings {
  difficulty: Difficulty;
  analysisProfile: AnalysisProfile;
  playerColor: Color;
  waitForCoaching: boolean;
  showCoordinates: boolean;
  autoOpenDetails: boolean;
}

const DEFAULTS: StoredSettings = {
  difficulty: 'MEDIUM',
  analysisProfile: 'NORMAL',
  playerColor: 'WHITE',
  waitForCoaching: true,
  showCoordinates: true,
  autoOpenDetails: false,
};

function load(): StoredSettings {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? { ...DEFAULTS, ...(JSON.parse(raw) as Partial<StoredSettings>) } : { ...DEFAULTS };
  } catch {
    return { ...DEFAULTS };
  }
}

/** Einstellungen werden lokal im Browser (localStorage, gespiegelt in IndexedDB) gespeichert. */
export const useSettingsStore = defineStore('settings', () => {
  const initial = load();
  const difficulty = ref<Difficulty>(initial.difficulty);
  const analysisProfile = ref<AnalysisProfile>(initial.analysisProfile);
  const playerColor = ref<Color>(initial.playerColor);
  const waitForCoaching = ref(initial.waitForCoaching);
  const showCoordinates = ref(initial.showCoordinates);
  const autoOpenDetails = ref(initial.autoOpenDetails);
  const persisted = ref(true);

  function snapshot(): StoredSettings {
    return {
      difficulty: difficulty.value,
      analysisProfile: analysisProfile.value,
      playerColor: playerColor.value,
      waitForCoaching: waitForCoaching.value,
      showCoordinates: showCoordinates.value,
      autoOpenDetails: autoOpenDetails.value,
    };
  }

  watch([difficulty, analysisProfile, playerColor, waitForCoaching, showCoordinates, autoOpenDetails], () => {
    const value = snapshot();
    try {
      localStorage.setItem(KEY, JSON.stringify(value));
      persisted.value = true;
    } catch {
      persisted.value = false;
    }
    void localGameStorage.saveSetting('settings', value);
  });

  return { difficulty, analysisProfile, playerColor, waitForCoaching, showCoordinates, autoOpenDetails, persisted };
});
