<script setup lang="ts">
defineProps<{
  thinking: boolean;
  analysing: boolean;
  offline: boolean;
  engineAvailable: boolean;
  engineName?: string;
  stage?: string | null;
  progress?: number;
}>();
const emit = defineEmits<{ retry: [] }>();
</script>

<template>
  <div class="engine-status" role="status" aria-live="polite" data-testid="engine-status">
    <q-banner v-if="offline" dense class="bg-negative text-white" rounded>
      <template #avatar><q-icon name="cloud_off" /></template>
      Backend nicht erreichbar.
      <template #action>
        <q-btn flat color="white" label="Erneut verbinden" no-caps @click="emit('retry')" />
      </template>
    </q-banner>
    <q-banner v-else-if="!engineAvailable" dense class="bg-warning text-black" rounded>
      <template #avatar><q-icon name="warning" /></template>
      Die Schach-Engine ist nicht verfügbar. Computerzüge und Analysen sind derzeit nicht möglich.
    </q-banner>
    <div v-else class="row items-center q-gutter-sm text-body2">
      <template v-if="thinking">
        <q-spinner-dots color="primary" size="20px" />
        <span data-testid="computer-thinking">Der Computer rechnet …</span>
      </template>
      <template v-else-if="analysing">
        <q-spinner-hourglass color="secondary" size="18px" />
        <span>Coaching-Analyse: {{ stage ?? 'läuft' }}</span>
        <q-linear-progress v-if="progress != null" :value="(progress ?? 0) / 100" color="secondary" class="col" rounded
                           style="max-width: 140px" :aria-label="`Fortschritt ${progress} Prozent`" />
      </template>
      <template v-else>
        <q-icon name="memory" color="positive" />
        <span>{{ engineName ?? 'Engine' }} bereit</span>
      </template>
    </div>
  </div>
</template>
