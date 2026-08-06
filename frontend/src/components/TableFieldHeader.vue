<script setup lang="ts">
import vTooltip from 'primevue/tooltip';

defineProps<{
  label: string;
  description: string;
}>();
</script>

<template>
  <span
    v-tooltip.top="description"
    class="table-field-heading"
    tabindex="0"
    :aria-label="`${label}: ${description}`"
  >
    <span>{{ label }}</span>
    <svg aria-hidden="true" viewBox="0 0 16 16">
      <circle cx="8" cy="8" r="6.25" />
      <path d="M6.7 6.1a1.45 1.45 0 1 1 2.25 1.2C8.3 7.7 8 8.05 8 8.8" />
      <path d="M8 11.25h.01" />
    </svg>
  </span>
</template>
