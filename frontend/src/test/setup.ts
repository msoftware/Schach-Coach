import 'fake-indexeddb/auto';
import { config } from '@vue/test-utils';
import { Quasar, Notify, Dialog } from 'quasar';

config.global.plugins = [[Quasar, { plugins: { Notify, Dialog } }]];
