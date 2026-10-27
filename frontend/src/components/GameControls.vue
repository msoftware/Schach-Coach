<script setup lang="ts">
withDefaults(defineProps<{
  canUndo: boolean;
  canRedo: boolean;
  canHint: boolean;
  canResign: boolean;
  busy: boolean;
  /** Mobile Darstellung: feste Aktionsleiste am unteren Bildschirmrand (Daumenbereich). */
  compact?: boolean;
}>(), { compact: false });
const emit = defineEmits<{ new: []; undo: []; redo: []; hint: []; resign: []; settings: []; flip: [] }>();
</script>

<template>
  <nav v-if="compact" class="bottom-bar" role="toolbar" aria-label="Spielsteuerung" data-testid="bottom-bar">
    <q-btn flat stack no-caps icon="add" label="Neu" class="bar-btn" data-testid="btn-new" @click="emit('new')" />
    <q-btn flat stack no-caps icon="undo" label="Zurück" class="bar-btn" :disable="!canUndo || busy"
           aria-label="Letzten eigenen Zug zurücknehmen" data-testid="btn-undo" @click="emit('undo')" />
    <q-btn flat stack no-caps icon="redo" label="Vor" class="bar-btn" :disable="!canRedo || busy"
           aria-label="Zurückgenommenen Zug wiederholen" data-testid="btn-redo" @click="emit('redo')" />
    <q-btn flat stack no-caps icon="lightbulb" label="Tipp" class="bar-btn" :disable="!canHint || busy"
           data-testid="btn-hint" @click="emit('hint')" />
    <q-btn flat stack no-caps icon="more_horiz" label="Mehr" class="bar-btn" aria-label="Weitere Aktionen" data-testid="btn-more">
      <q-menu anchor="top right" self="bottom right" :offset="[0, 6]">
        <q-list style="min-width: 210px">
          <q-item v-close-popup clickable class="menu-item" @click="emit('flip')">
            <q-item-section avatar><q-icon name="swap_vert" /></q-item-section>
            <q-item-section>Brett drehen</q-item-section>
          </q-item>
          <q-item v-close-popup clickable class="menu-item" data-testid="btn-settings" @click="emit('settings')">
            <q-item-section avatar><q-icon name="settings" /></q-item-section>
            <q-item-section>Einstellungen</q-item-section>
          </q-item>
          <q-separator />
          <q-item v-close-popup clickable class="menu-item text-negative" :disable="!canResign || busy"
                  data-testid="btn-resign" @click="emit('resign')">
            <q-item-section avatar><q-icon name="flag" color="negative" /></q-item-section>
            <q-item-section>Aufgeben</q-item-section>
          </q-item>
        </q-list>
      </q-menu>
    </q-btn>
  </nav>

  <div v-else class="game-controls row q-gutter-sm justify-center" role="toolbar" aria-label="Spielsteuerung">
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

<style scoped>
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
  display: flex;
  justify-content: space-around;
  align-items: stretch;
  background: #fff;
  border-top: 1px solid #d6cfc4;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  padding: 2px 4px calc(2px + env(safe-area-inset-bottom));
}
.bar-btn {
  flex: 1 1 0;
  min-width: 0;
  min-height: 56px;
  color: #3e2723;
  font-size: 12px;
  border-radius: 10px;
}
.bar-btn :deep(.q-icon) { font-size: 26px; }
.menu-item { min-height: 52px; }
</style>
