import { VueQueryPlugin } from '@tanstack/vue-query';
import { client } from '@api/client.gen';
import Aura from '@primeuix/themes/aura';
import PrimeVue from 'primevue/config';
import { createApp } from 'vue';

import App from './App.vue';
import router from './router';
import './styles.css';

client.setConfig({
  baseUrl: window.location.origin,
});

createApp(App)
  .use(PrimeVue, {
    theme: {
      preset: Aura,
    },
  })
  .use(VueQueryPlugin)
  .use(router)
  .mount('#app');
