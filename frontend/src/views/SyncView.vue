<script setup lang="ts">
import Button from 'primevue/button';
import Tag from 'primevue/tag';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { prototypeStore as store } from '../prototypeStore';

const router = useRouter();
const portfolio = store.currentPortfolio;
const loading = ref(false);
const connected = ref(false);
const applied = ref(false);

function connect(): void {
  loading.value = true;
  window.setTimeout(() => {
    loading.value = false;
    connected.value = true;
  }, 700);
}

function apply(): void {
  store.applyBrokerSync();
  applied.value = true;
}
</script>

<template>
  <div class="page-stack sync-page">
    <section class="page-hero">
      <div>
        <button
          class="back-link"
          type="button"
          @click="router.push(`/family/portfolios/${portfolio.id}`)"
        >
          ← К портфелю
        </button>
        <p class="eyebrow">Синхронизация позиций</p>
        <h1>
          {{
            applied
              ? 'Данные синхронизированы'
              : connected
                ? 'Проверьте расхождения'
                : 'Свериться с брокером'
          }}
        </h1>
        <p class="hero-copy">
          Ни одно локальное значение не изменится без вашего подтверждения.
        </p>
      </div>
    </section>

    <section v-if="!connected && !applied" class="connect-card">
      <div class="broker-logo">Т</div>
      <div>
        <p class="eyebrow">Демо-подключение</p>
        <h2>Т-Инвестиции</h2>
        <p>
          Получим только позиции и свободные деньги. Операции с ценными бумагами
          недоступны.
        </p>
      </div>
      <ul>
        <li><span>✓</span> Токен только для чтения</li>
        <li><span>✓</span> Локальные данные сохранятся при ошибке</li>
        <li><span>✓</span> Изменения сначала появятся в сравнении</li>
      </ul>
      <Button
        label="Получить актуальные позиции"
        :loading="loading"
        size="large"
        @click="connect"
      />
      <Button
        label="Оставить ручной режим"
        severity="secondary"
        text
        @click="router.push(`/family/portfolios/${portfolio.id}`)"
      />
    </section>

    <section v-else-if="!applied" class="panel diff-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">Получено только что</p>
          <h2>3 расхождения</h2>
        </div>
        <Tag severity="success" value="Соединение защищено" />
      </div>
      <div class="diff-table">
        <div class="diff-head">
          <span>Позиция</span><span>В Family Money</span><span>У брокера</span
          ><span>Изменение</span>
        </div>
        <div class="diff-row">
          <span><strong>TMOS</strong><small>Тинькофф iMOEX</small></span
          ><span>1 834 шт.</span><span><strong>8 320 шт.</strong></span
          ><Tag severity="success" value="+6 486" />
        </div>
        <div class="diff-row">
          <span><strong>SBMX</strong><small>Фонд российских акций</small></span
          ><span>940 шт.</span><span><strong>2 070 шт.</strong></span
          ><Tag severity="success" value="+1 130" />
        </div>
        <div class="diff-row">
          <span><strong>Свободные деньги</strong><small>Рубли</small></span
          ><span>48 650,32 ₽</span><span><strong>2 141,08 ₽</strong></span
          ><Tag severity="warn" value="−46 509,24" />
        </div>
      </div>
      <div class="sync-explanation">
        <span>i</span>
        <p>
          <strong>Почему данные отличаются?</strong>Вероятно, часть
          предложенного плана уже исполнена у брокера. Проверьте значения перед
          применением.
        </p>
      </div>
      <div class="diff-actions">
        <Button
          label="Отменить"
          severity="secondary"
          text
          @click="router.push(`/family/portfolios/${portfolio.id}`)"
        /><Button label="Применить 3 изменения" @click="apply" />
      </div>
    </section>

    <section v-else class="completion-card">
      <span class="completion-icon">✓</span>
      <div>
        <p class="eyebrow">Готово</p>
        <h2>Позиции обновлены</h2>
        <p>
          Локальное состояние теперь соответствует данным брокера. Цели портфеля
          не изменились.
        </p>
      </div>
      <Button
        label="Вернуться в портфель"
        @click="router.push(`/family/portfolios/${portfolio.id}`)"
      />
    </section>
  </div>
</template>
