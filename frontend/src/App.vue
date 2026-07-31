<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
</script>

<template>
  <div class="application-shell">
    <header class="application-header">
      <RouterLink class="brand" to="/">Family Money</RouterLink>
      <span class="stage-label">Локальный MVP</span>
    </header>

    <main class="application-content">
      <RouterView />
    </main>
  </div>
</template>
