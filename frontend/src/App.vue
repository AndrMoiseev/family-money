<script setup lang="ts">
import Button from 'primevue/button';
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const mobileMenuOpen = ref(false);

const navigation = [
  { to: '/', label: 'Семья', marker: 'С' },
  { to: '/settings/members', label: 'Настройки', marker: 'Н' },
];

const activeSection = computed(() =>
  route.path.startsWith('/settings') ? '/settings/members' : '/',
);

function navigate(to: string): void {
  mobileMenuOpen.value = false;
  void router.push(to);
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ 'sidebar--open': mobileMenuOpen }">
      <button class="brand" type="button" @click="navigate('/')">
        <span class="brand-mark">FM</span>
        <span
          ><strong>Family Money</strong><small>семейный капитал</small></span
        >
      </button>

      <nav class="main-nav" aria-label="Основная навигация">
        <button
          v-for="item in navigation"
          :key="item.to"
          class="nav-item"
          :class="{ 'nav-item--active': activeSection === item.to }"
          type="button"
          @click="navigate(item.to)"
        >
          <span class="nav-marker">{{ item.marker }}</span>
          {{ item.label }}
        </button>
      </nav>

      <div class="sidebar-note">
        <span class="pulse-dot" />
        <div>
          <strong>Все данные локальны</strong
          ><small>Прототип без подключения к брокеру</small>
        </div>
      </div>
    </aside>

    <div class="app-main">
      <header class="topbar">
        <Button
          class="menu-button"
          text
          rounded
          aria-label="Открыть меню"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <span aria-hidden="true">☰</span>
        </Button>
        <p>
          Последнее обновление портфелей:
          <strong>сегодня, 09:42</strong>
        </p>
        <div class="profile">
          <span>А</span>
          <div><strong>Андрей</strong><small>Управляющий</small></div>
        </div>
      </header>

      <main class="page-container">
        <RouterView />
      </main>
    </div>

    <button
      v-if="mobileMenuOpen"
      class="menu-backdrop"
      aria-label="Закрыть меню"
      @click="mobileMenuOpen = false"
    />
  </div>
</template>
