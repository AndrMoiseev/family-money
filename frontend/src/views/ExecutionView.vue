<script setup lang="ts">
import Decimal from 'decimal.js';
import Button from 'primevue/button';
import Checkbox from 'primevue/checkbox';
import InputText from 'primevue/inputtext';
import Tag from 'primevue/tag';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { prototypeStore as store } from '../prototypeStore';

const router = useRouter();
const portfolio = store.currentPortfolio;
const completed = ref<number[]>([]);
const actualLots = ref(store.plan.map((trade) => trade.lots));
const actualCash = ref('2141.08');
const saved = ref(false);

function saveActual(): void {
  store.plan.forEach((trade, index) => {
    const position = store.currentPortfolio.value.positions.find(
      (item) => item.ticker === trade.ticker,
    );
    const lots = actualLots.value[index] ?? '0';
    if (!position || !completed.value.includes(index)) return;
    const change = new Decimal(lots).times(trade.lotSize);
    const quantity = new Decimal(position.quantity);
    position.quantity = (
      trade.side === 'Купить' ? quantity.plus(change) : quantity.minus(change)
    ).toString();
  });
  store.currentPortfolio.value.cash = actualCash.value;
  store.currentPortfolio.value.updatedAt = 'только что';
  saved.value = true;
}
</script>

<template>
  <div class="page-stack execution-page">
    <section class="page-hero">
      <div>
        <button
          class="back-link"
          type="button"
          @click="router.push(`/family/portfolios/${portfolio.id}/planner`)"
        >
          ← К плану сделок
        </button>
        <p class="eyebrow">Исполнение вне приложения</p>
        <h1>{{ saved ? 'Портфель обновлён' : 'Сверьте с брокером' }}</h1>
        <p class="hero-copy">
          Отметьте только реально выполненные операции и внесите фактический
          остаток.
        </p>
      </div>
    </section>

    <section v-if="saved" class="completion-card">
      <span class="completion-icon">✓</span>
      <div>
        <p class="eyebrow">Цикл завершён</p>
        <h2>Фактические позиции сохранены</h2>
        <p>
          Стоимость и доли пересчитаны по введённым значениям. План остался
          только подсказкой.
        </p>
      </div>
      <Button
        label="Посмотреть портфель"
        @click="router.push(`/family/portfolios/${portfolio.id}`)"
      />
    </section>

    <template v-else>
      <section class="broker-reminder">
        <span>1</span>
        <div>
          <strong>Откройте приложение брокера</strong
          ><small
            >Разместите заявки самостоятельно. Цены исполнения могут отличаться
            от расчётных.</small
          >
        </div>
        <span>2</span>
        <div>
          <strong>Вернитесь и зафиксируйте факт</strong
          ><small
            >Можно исполнить не все операции или указать другое
            количество.</small
          >
        </div>
      </section>

      <section class="panel execution-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Фактическое исполнение</p>
            <h2>Операции у брокера</h2>
          </div>
          <span class="progress-label"
            >Отмечено {{ completed.length }} из {{ store.plan.length }}</span
          >
        </div>
        <div class="execution-list">
          <article
            v-for="(trade, index) in store.plan"
            :key="`${trade.ticker}-${index}`"
            :class="{ complete: completed.includes(index) }"
          >
            <Checkbox
              v-model="completed"
              :value="index"
              :input-id="`trade-${index}`"
            />
            <label :for="`trade-${index}`" class="trade-instrument"
              ><Tag
                :severity="trade.side === 'Купить' ? 'success' : 'danger'"
                :value="trade.side"
              /><strong>{{ trade.ticker }}</strong
              ><small>План: {{ trade.lots }} лотов</small></label
            >
            <label class="field actual-field"
              ><span>Исполнено лотов</span
              ><InputText
                v-model="actualLots[index]"
                inputmode="numeric"
                :disabled="!completed.includes(index)"
            /></label>
            <div class="trade-amount">
              <strong
                >{{
                  store.money(
                    store.tradeValue({
                      ...trade,
                      lots: actualLots[index] ?? '0',
                    }),
                  )
                }}
                ₽</strong
              ><small>по расчётной цене</small>
            </div>
          </article>
        </div>
      </section>

      <section class="panel cash-confirmation">
        <div>
          <p class="eyebrow">После всех операций</p>
          <h2>Свободные деньги</h2>
          <p>Укажите точный остаток, который показывает брокер.</p>
        </div>
        <label class="money-input"
          ><InputText
            v-model="actualCash"
            inputmode="decimal"
            aria-label="Фактический остаток"
          /><span>₽</span></label
        >
      </section>

      <section class="save-bar">
        <div>
          <strong>Сохраняется только фактическое состояние</strong
          ><span>Неотмеченные операции не изменят позиции.</span>
        </div>
        <Button label="Обновить портфель" size="large" @click="saveActual" />
      </section>
    </template>
  </div>
</template>
