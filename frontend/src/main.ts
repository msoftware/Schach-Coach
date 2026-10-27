import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { Quasar, Notify, Dialog } from 'quasar';
import quasarLang from 'quasar/lang/de-DE';
import '@quasar/extras/material-icons/material-icons.css';
import 'quasar/dist/quasar.css';
import './app/app.css';
import App from './app/App.vue';
import { router } from './app/router';

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.use(Quasar, { plugins: { Notify, Dialog }, lang: quasarLang });
app.mount('#app');
