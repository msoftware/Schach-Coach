<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useQuasar } from 'quasar';
import { useSettingsStore } from '@/stores/settingsStore';
import { useAnalysisStore } from '@/stores/analysisStore';
import { gameApi } from '@/services/gameApi';
import { localGameStorage, type StoredGame } from '@/services/localGameStorage';
import { DIFFICULTY_LABELS, PROFILE_LABELS, type AnalysisProfile, type Color, type Difficulty, type DifficultyOption } from '@/models/game';

const model = defineModel<boolean>({ required: true });
const emit = defineEmits<{ apply: []; newGame: [] }>();

const $q = useQuasar();
const settings = useSettingsStore();
const analysis = useAnalysisStore();
const options = ref<DifficultyOption[]>([]);
const archived = ref<StoredGame[]>([]);
const storageAvailable = ref(true);

const difficultyOptions = computed(() => (Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => ({
  label: DIFFICULTY_LABELS[d],
  value: d,
  description: options.value.find((o) => o.id === d)?.description ?? '',
})));
const profileOptions = (Object.keys(PROFILE_LABELS) as AnalysisProfile[]).map((p) => ({ label: PROFILE_LABELS[p], value: p }));
const colorOptions: { label: string; value: Color }[] = [{ label: 'Weiß', value: 'WHITE' }, { label: 'Schwarz', value: 'BLACK' }];

const recurring = computed(() => Object.entries(analysis.stats?.ruleCounts ?? {})
  .sort((a, b) => b[1].count - a[1].count).slice(0, 6));

onMounted(async () => {
  try {
    options.value = (await gameApi.settingsOptions()).difficulties;
  } catch {
    options.value = [];
  }
  storageAvailable.value = await localGameStorage.isAvailable();
  archived.value = await localGameStorage.listArchivedGames();
  await analysis.loadStats();
});

function confirmClear() {
  $q.dialog({
    title: 'Lokale Daten löschen',
    message: 'Alle im Browser gespeicherten Partien, Einstellungen, Analysen und Lernstatistiken werden gelöscht.',
    cancel: { label: 'Abbrechen', flat: true },
    ok: { label: 'Löschen', color: 'negative' },
  }).onOk(async () => {
    const result = await localGameStorage.clearAll();
    $q.notify(result.ok ? { type: 'positive', message: 'Lokale Daten wurden gelöscht.' }
      : { type: 'negative', message: `Löschen fehlgeschlagen: ${result.error}` });
    archived.value = [];
    analysis.stats = null;
  });
}
</script>

<template>
  <q-dialog v-model="model" :maximized="$q.screen.lt.sm">
    <q-card style="min-width: min(560px, 96vw)" data-testid="settings-dialog">
      <q-card-section class="row items-center">
        <div class="text-h6">Einstellungen</div>
        <q-space />
        <q-btn flat round icon="close" aria-label="Schließen" v-close-popup />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-gutter-md">
        <q-select v-model="settings.difficulty" :options="difficultyOptions" emit-value map-options label="Spielstärke des Computers"
                  outlined data-testid="select-difficulty">
          <template #option="scope">
            <q-item v-bind="scope.itemProps">
              <q-item-section>
                <q-item-label>{{ scope.opt.label }}</q-item-label>
                <q-item-label caption>{{ scope.opt.description }}</q-item-label>
              </q-item-section>
            </q-item>
          </template>
        </q-select>
        <div class="text-caption text-grey-8">
          Die Stufen sind über Engine-Parameter definiert (siehe Beschreibung); eine Elo-Zahl wird bewusst nicht angegeben.
        </div>
        <q-select v-model="settings.analysisProfile" :options="profileOptions" emit-value map-options label="Gründlichkeit der Analyse" outlined />
        <q-select v-model="settings.playerColor" :options="colorOptions" emit-value map-options label="Meine Farbe (ab der nächsten Partie)" outlined />
        <q-toggle v-model="settings.waitForCoaching" label="Computer wartet mit seinem Zug, bis das Coaching fertig ist" />
        <q-toggle v-model="settings.showCoordinates" label="Koordinaten am Brett anzeigen" />
      </q-card-section>
      <q-card-actions align="right">
        <q-btn flat label="Neue Partie mit diesen Einstellungen" color="primary" no-caps @click="emit('newGame')" v-close-popup />
        <q-btn color="primary" label="Übernehmen" no-caps @click="emit('apply')" v-close-popup />
      </q-card-actions>
      <q-separator />
      <q-card-section>
        <div class="text-subtitle2">Lernstatistik (nur in diesem Browser)</div>
        <div v-if="!analysis.stats" class="text-body2 text-grey-7">Noch keine analysierten Züge.</div>
        <div v-else class="text-body2">
          Analysierte Züge: {{ analysis.stats.analysedMoves }}
          <div v-if="recurring.length" class="q-mt-xs">Wiederkehrende Muster:
            <ul class="q-my-xs">
              <li v-for="[rule, entry] in recurring" :key="rule">{{ entry.title }} ({{ rule }}): {{ entry.count }}×</li>
            </ul>
          </div>
        </div>
        <div class="text-subtitle2 q-mt-md">Gespeicherte Partien</div>
        <div v-if="archived.length === 0" class="text-body2 text-grey-7">Keine abgeschlossenen Partien gespeichert.</div>
        <ul v-else class="q-my-xs text-body2">
          <li v-for="g in archived.slice(0, 8)" :key="g.id">
            {{ new Date(g.savedAt).toLocaleString('de-DE') }} – {{ g.result }} ({{ g.sanMoves.length }} Halbzüge)
          </li>
        </ul>
      </q-card-section>
      <q-separator />
      <q-card-section class="text-body2">
        <div class="text-subtitle2">Datenschutz</div>
        <p class="q-mb-xs">
          Es gibt keine Benutzerkonten. Partien, Einstellungen und Lernstatistik werden ausschließlich in diesem Browser gespeichert.
          Die Schach-Engine läuft lokal im Backend; es werden keine Daten an externe Dienste gesendet und keine Telemetrie erhoben.
          Das Backend hält eine laufende Partie nur im Arbeitsspeicher.
        </p>
        <p v-if="!storageAvailable" class="text-negative">Die Speicherung im Browser ist nicht verfügbar – Partien gehen beim Neuladen verloren.</p>
        <q-btn outline color="negative" icon="delete" label="Lokale Daten löschen" no-caps @click="confirmClear" />
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
