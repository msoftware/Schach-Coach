import { createRouter, createWebHistory } from 'vue-router';
import GamePage from '@/pages/GamePage.vue';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'game', component: GamePage },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});
