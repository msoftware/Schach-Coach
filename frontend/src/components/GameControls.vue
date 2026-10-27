<script setup lang="ts">
defineProps<{
  canUndo: boolean;
  canRedo: boolean;
  canHint: boolean;
  canResign: boolean;
  busy: boolean;
}>();
const emit = defineEmits<{ new: []; undo: []; redo: []; hint: []; resign: []; settings: []; flip: [] }>();
</script>

<template>
  <div class="game-controls row q-gutter-sm justify-center" role="toolbar" aria-label="Spielsteuerung">
    <q-btn color="primary" icon="add" label="Neu" no-caps data-testid="btn-new" @click="emit('new')" />
    <q-btn outline color="primary" icon="undo" label="Zurück" no-caps :disable="!canUndo || busy"
           data-testid="btn-undo" @click="emit('undo')">
      <q-tooltip>Letzten eigenen Zug zurücknehmen</q-tooltip>
    </q-btn>
    <q-btn outline color="primary" icon="redo" label="Vor" no-caps :disable="!canRedo || busy"
           data-testid="btn-redo" @click="emit('redo')">
      <q-tooltip>Zurückgenommenen Zug wiederholen</q-tooltip>
    </q-btn>
    <q-btn outline color="secondary" icon="lightbulb" label="Tipp" no-caps :disable="!canHint || busy"
           data-testid="btn-hint" @click="emit('hint')" />
    <q-btn outline color="negative" icon="flag" label="Aufgeben" no-caps :disable="!canResign || busy"
           data-testid="btn-resign" @click="emit('resign')" />
    <q-btn flat round icon="swap_vert" aria-label="Brett drehen" @click="emit('flip')" />
    <q-btn flat round icon="settings" aria-label="Einstellungen" data-testid="btn-settings" @click="emit('settings')" />
  </div>
</template>
