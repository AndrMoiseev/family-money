<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query';
import Button from 'primevue/button';
import Card from 'primevue/card';
import Message from 'primevue/message';
import Tag from 'primevue/tag';
import { computed } from 'vue';

import { getSystemStatus } from '../api/systemStatus';

const statusQuery = useQuery({
  queryKey: ['system-status'],
  queryFn: getSystemStatus,
  retry: false,
});

const statusSeverity = computed(() =>
  statusQuery.isSuccess.value ? 'success' : 'secondary',
);
</script>

<template>
  <section class="dashboard" aria-labelledby="dashboard-title">
    <div>
      <p class="eyebrow">Каркас приложения</p>
      <h1 id="dashboard-title">Семейные инвестиции в одном месте</h1>
      <p class="introduction">
        Backend, API-контракт и frontend запускаются локально. Бизнес-функции
        будут добавляться следующими вертикальными срезами.
      </p>
    </div>

    <Card class="status-card">
      <template #title>Состояние системы</template>
      <template #content>
        <div class="status-row">
          <span>Backend API</span>
          <Tag
            :severity="statusSeverity"
            :value="statusQuery.isSuccess.value ? 'Доступен' : 'Проверка'"
          />
        </div>

        <Message
          v-if="statusQuery.isError.value"
          severity="warn"
          :closable="false"
        >
          Backend пока недоступен. Запустите приложение через корневую
          Gradle-команду.
        </Message>

        <Button
          label="Проверить снова"
          severity="secondary"
          :loading="statusQuery.isFetching.value"
          @click="statusQuery.refetch()"
        />
      </template>
    </Card>
  </section>
</template>
