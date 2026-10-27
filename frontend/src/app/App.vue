<script setup lang="ts">
</script>

<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated class="bg-brown-9 text-white">
      <q-toolbar>
        <q-icon name="school" size="28px" class="q-mr-sm" />
        <q-toolbar-title class="text-weight-bold">Schach-Coach</q-toolbar-title>
        <span class="text-caption gt-xs">Spielen · Verstehen · Lernen</span>
      </q-toolbar>
    </q-header>
    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>
