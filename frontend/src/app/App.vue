<script setup lang="ts">
import { computed } from 'vue';
import { useQuasar } from 'quasar';

const $q = useQuasar();
/** Handy quer: Die Kopfzeile wird ausgeblendet, damit das Brett die volle Höhe nutzen kann. */
const showHeader = computed(() => !($q.screen.lt.md && $q.screen.width > $q.screen.height && $q.screen.height < 500));
</script>

<template>
  <q-layout view="hHh lpR fFf">
    <q-header v-if="showHeader" elevated class="bg-brown-9 text-white app-header">
      <q-toolbar :class="{ 'mobile-toolbar': $q.screen.lt.md }">
        <q-icon name="school" :size="$q.screen.lt.md ? '24px' : '28px'" class="q-mr-sm" />
        <q-toolbar-title class="text-weight-bold">Schach-Coach</q-toolbar-title>
        <span class="text-caption gt-xs">Spielen · Verstehen · Lernen</span>
      </q-toolbar>
    </q-header>
    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<style scoped>
.app-header { padding-top: env(safe-area-inset-top); }
.mobile-toolbar { min-height: 46px; }
.mobile-toolbar :deep(.q-toolbar__title) { font-size: 18px; }
</style>
