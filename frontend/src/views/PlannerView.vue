<script setup lang="ts">
import Decimal from 'decimal.js';
import Button from 'primevue/button';
import Checkbox from 'primevue/checkbox';
import InputText from 'primevue/inputtext';
import Tag from 'primevue/tag';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import { prototypeStore as store } from '../prototypeStore';

const router = useRouter();
const calculated = ref(false);
const staleAccepted = ref(false);
const portfolio = store.currentPortfolio;
const targetAllocationIsValid = computed(() =>
  store.targetAllocationIsValid(portfolio.value),
);

const modeOptions = [
  {
    id: 'buy-only' as const,
    title: 'Только покупки',
    description: 'Использовать свободные деньги без продаж',
    icon: '+',
  },
  {
    id: 'rebalance' as const,
    title: 'Ребалансировка',
    description: 'Разрешить покупки и продажи',
    icon: '↻',
  },
];

const totalBuys = computed(() =>
  store.plan
    .filter((trade) => trade.side === 'Купить')
    .reduce((sum, trade) => sum.plus(store.tradeValue(trade)), new Decimal(0)),
);
const totalSales = computed(() =>
  store.plan
    .filter((trade) => trade.side === 'Продать')
    .reduce((sum, trade) => sum.plus(store.tradeValue(trade)), new Decimal(0)),
);
const totalCommission = computed(() =>
  store.plan.reduce(
    (sum, trade) => sum.plus(store.tradeCommission(trade)),
    new Decimal(0),
  ),
);
const expectedCash = computed(() =>
  new Decimal(portfolio.value.cash)
    .plus(totalSales.value)
    .minus(totalBuys.value)
    .minus(totalCommission.value),
);

function calculate(): void {
  if (!targetAllocationIsValid.value) return;
  store.generatePlan();
  calculated.value = true;
}

function selectMode(mode: typeof store.planning.mode): void {
  store.planning.mode = mode;
  calculated.value = false;
}
</script>

<template>
  <div class="page-stack planner-page">
    <section class="page-hero">
      <div>
        <button
          class="back-link"
          type="button"
          @click="router.push(`/family/portfolios/${portfolio.id}`)"
        >
          ← {{ portfolio.name }}
        </button>
        <p class="eyebrow">План сделок</p>
        <h1>
          {{ calculated ? 'Предложение готово' : 'Как рассчитаем сделки?' }}
        </h1>
        <p class="hero-copy">
          Расчёт ничего не меняет в портфеле — окончательное решение остаётся за
          вами.
        </p>
      </div>
      <div v-if="calculated" class="hero-actions">
        <Button
          label="Новый расчёт"
          severity="secondary"
          outlined
          @click="calculated = false"
        /><Button
          label="К исполнению"
          @click="router.push(`/family/portfolios/${portfolio.id}/execution`)"
        />
      </div>
    </section>

    <template v-if="!calculated">
      <section class="mode-grid" aria-label="Режим расчёта">
        <button
          v-for="option in modeOptions"
          :key="option.id"
          type="button"
          :class="{ selected: store.planning.mode === option.id }"
          @click="selectMode(option.id)"
        >
          <span class="mode-icon">{{ option.icon }}</span
          ><span
            ><strong>{{ option.title }}</strong
            ><small>{{ option.description }}</small></span
          ><i>{{ store.planning.mode === option.id ? '●' : '○' }}</i>
        </button>
      </section>

      <section class="panel calculation-card">
        <div>
          <p class="eyebrow">Параметры</p>
          <h2>
            {{
              store.planning.mode === 'rebalance'
                ? 'Ребалансировка'
                : 'Только покупки'
            }}
          </h2>
          <p>
            {{
              store.planning.mode === 'buy-only'
                ? 'План будет содержать только покупки целыми лотами в пределах свободных денег на счёте.'
                : 'Покупки будут профинансированы свободными деньгами и продажами.'
            }}
          </p>
        </div>
        <div class="available-cash">
          <span>Доступно для сделок</span
          ><strong>{{ store.money(portfolio.cash) }} ₽</strong>
        </div>
      </section>

      <section class="data-warning">
        <div class="warning-icon">!</div>
        <div>
          <strong>Цена LQDT старше 60 минут</strong
          ><span
            >Последняя известная цена 1,82 ₽ получена вчера в 18:20. Можно
            обновить вручную или продолжить с предупреждением.</span
          >
        </div>
        <Button
          label="Обновить"
          severity="secondary"
          outlined
          @click="
            store.updatePrices();
            staleAccepted = true;
          "
        />
      </section>
      <label class="accept-row"
        ><Checkbox v-model="staleAccepted" binary /><span
          >Я понимаю, что расчёт использует последнюю известную цену</span
        ></label
      >

      <section
        v-if="!targetAllocationIsValid"
        class="target-allocation-warning"
        role="alert"
      >
        <span>!</span>
        <div>
          <strong>Сумма целей должна быть равна 100%</strong>
          <small
            >Проверьте цели классов и инструментов перед расчётом сделок.</small
          >
        </div>
        <Button
          label="Исправить цели"
          severity="secondary"
          outlined
          @click="router.push(`/family/portfolios/${portfolio.id}`)"
        />
      </section>

      <div class="planner-submit">
        <Button
          label="Рассчитать план"
          size="large"
          :disabled="!staleAccepted || !targetAllocationIsValid"
          @click="calculate"
        /><small>Обычно расчёт занимает несколько секунд</small>
      </div>
    </template>

    <template v-else>
      <section class="plan-summary-strip">
        <div>
          <span>Свободные деньги до сделок</span
          ><strong>{{ store.money(portfolio.cash) }} ₽</strong>
        </div>
        <div>
          <span>Операций</span><strong>{{ store.plan.length }}</strong>
        </div>
        <div>
          <span>Комиссии</span
          ><strong>{{ store.money(totalCommission) }} ₽</strong>
        </div>
        <div>
          <span>Остаток после сделок</span
          ><strong :class="{ 'negative-text': expectedCash.isNegative() }"
            >{{ store.money(expectedCash) }} ₽</strong
          >
        </div>
      </section>

      <div class="content-grid content-grid--plan">
        <section class="panel trades-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Можно изменить до исполнения</p>
              <h2>Операции</h2>
            </div>
            <Button
              label="+ Добавить"
              severity="secondary"
              text
              @click="
                store.plan.push({
                  ticker: 'LQDT',
                  side: 'Купить',
                  lots: '1',
                  lotSize: '1',
                  price: '1.82',
                })
              "
            />
          </div>
          <div class="trade-list">
            <article
              v-for="(trade, index) in store.plan"
              :key="`${trade.ticker}-${index}`"
            >
              <Tag
                :severity="trade.side === 'Купить' ? 'success' : 'danger'"
                :value="trade.side"
              />
              <div class="trade-instrument">
                <strong>{{ trade.ticker }}</strong
                ><small>по {{ store.money(trade.price) }} ₽</small>
              </div>
              <label
                ><span>Лотов</span
                ><InputText v-model="trade.lots" inputmode="numeric"
              /></label>
              <div class="trade-amount">
                <strong>{{ store.money(store.tradeValue(trade)) }} ₽</strong
                ><small
                  >комиссия
                  {{ store.money(store.tradeCommission(trade)) }} ₽</small
                >
              </div>
              <button
                class="icon-button"
                type="button"
                aria-label="Удалить операцию"
                @click="store.removeTrade(index)"
              >
                ×
              </button>
            </article>
          </div>
          <div class="trade-total">
            <span>Денежный эффект покупок</span
            ><strong
              >−{{ store.money(totalBuys.plus(totalCommission)) }} ₽</strong
            >
          </div>
        </section>

        <aside class="panel expected-card">
          <p class="eyebrow">После операций</p>
          <h2>Ближе к цели</h2>
          <div class="before-after">
            <span>Макс. отклонение<strong>8,4 → 2,1 п.п.</strong></span
            ><span
              >Свободные деньги<strong
                >{{ store.money(expectedCash) }} ₽</strong
              ></span
            >
          </div>
          <div class="expected-bars">
            <div>
              <span>Акции <b>59,1% / 60%</b></span
              ><i><em style="width: 98.5%" /></i>
            </div>
            <div>
              <span>Облигации <b>24,2% / 25%</b></span
              ><i><em style="width: 96.8%" /></i>
            </div>
            <div>
              <span>Ликвидность <b>14,6% / 15%</b></span
              ><i><em style="width: 97.3%" /></i>
            </div>
          </div>
          <p class="explanation">
            Точно попасть в цель мешают целые лоты и комиссии. План предпочитает
            меньше операций при одинаковом качестве.
          </p>
        </aside>
      </div>

      <section class="decision-banner">
        <div>
          <strong>Проверьте операции у брокера</strong
          ><span
            >Family Money не отправляет заявки и не изменяет фактические
            позиции.</span
          >
        </div>
        <Button
          label="Я готов исполнить план"
          @click="router.push(`/family/portfolios/${portfolio.id}/execution`)"
        />
      </section>
    </template>
  </div>
</template>
