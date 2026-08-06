<script setup lang="ts">
import Decimal from 'decimal.js';
import Button from 'primevue/button';
import Select from 'primevue/select';
import vTooltip from 'primevue/tooltip';
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import InlineDecimalCell from '../components/InlineDecimalCell.vue';
import TableFieldHeader from '../components/TableFieldHeader.vue';
import {
  prototypeStore as store,
  type AssetClass,
  type Position,
} from '../prototypeStore';

const router = useRouter();
const portfolio = store.currentPortfolio;
const editingPositions = ref(false);
const updateNotice = ref(false);
const total = computed(() => store.portfolioValue(portfolio.value));
const classMeta: { name: AssetClass; color: string }[] = [
  { name: 'Акции', color: 'pine' },
  { name: 'Облигации', color: 'sage' },
  { name: 'Фонды ликвидности', color: 'gold' },
];
const assetClassOptions: AssetClass[] = [...store.assetClasses];

function parseDecimal(value: string): Decimal | null {
  try {
    const decimal = new Decimal(value.trim());
    return decimal.isFinite() && !decimal.isNegative() ? decimal : null;
  } catch {
    return null;
  }
}

const allocationRows = computed(() =>
  classMeta.map((item) => {
    const value = store.classValue(portfolio.value, item.name);
    const actual = store.share(value, total.value);
    const target = portfolio.value.classTargets[item.name];
    return {
      ...item,
      value,
      actual,
      target,
      delta: actual.minus(target),
    };
  }),
);

const cashAllocation = computed(() => {
  const value = new Decimal(portfolio.value.cash);
  const actual = store.share(value, total.value);
  return { value, actual, delta: actual };
});

const maxClassDeviation = computed(() =>
  allocationRows.value.reduce(
    (maximum, row) => Decimal.max(maximum, row.delta.abs()),
    cashAllocation.value.delta.abs(),
  ),
);

const targetTotal = computed(() => {
  let sum = new Decimal(0);
  for (const assetClass of store.assetClasses) {
    const value = parseDecimal(portfolio.value.classTargets[assetClass]);
    if (!value) return null;
    sum = sum.plus(value);
  }
  return sum;
});

const targetTotalLabel = computed(() => targetTotal.value?.toString() ?? '—');

const targetTotalIsValid = computed(
  () => targetTotal.value?.equals(100) === true,
);

const targetTotalHint = computed(() => {
  const sum = targetTotal.value;
  if (!sum) return 'Введите корректные значения от 0 до 100%';
  if (sum.equals(100)) return 'Распределение заполнено полностью';
  if (sum.lessThan(100)) {
    return `Не хватает ${new Decimal(100).minus(sum).toString()}%`;
  }
  return `Превышение на ${sum.minus(100).toString()}%`;
});

function positionsForClass(assetClass: AssetClass) {
  return portfolio.value.positions
    .map((position, index) => ({ position, index }))
    .filter(({ position }) => position.assetClass === assetClass);
}

function instrumentTargetTotal(assetClass: AssetClass): Decimal | null {
  let sum = new Decimal(0);
  for (const { position } of positionsForClass(assetClass)) {
    const value = parseDecimal(position.target);
    if (!value) return null;
    sum = sum.plus(value);
  }
  return sum;
}

function instrumentTargetTotalLabel(assetClass: AssetClass): string {
  return instrumentTargetTotal(assetClass)?.toString() ?? '—';
}

function formatTarget(value: string): string {
  return parseDecimal(value)?.toFixed(2) ?? '—';
}

function positionShareWithinClass(
  position: Position,
  assetClass: AssetClass,
): Decimal {
  return store.share(
    store.positionValue(position),
    store.classValue(portfolio.value, assetClass),
  );
}

function positionTargetWeight(
  position: Position,
  assetClass: AssetClass,
): Decimal {
  return new Decimal(portfolio.value.classTargets[assetClass])
    .mul(position.target)
    .div(100);
}

function updatePositionTarget(index: number, value: string): void {
  const position = portfolio.value.positions[index];
  if (!position) return;

  position.target = value;
}

function updateClassTarget(assetClass: AssetClass, value: string): void {
  portfolio.value.classTargets[assetClass] = value;
}

const invalidInstrumentClasses = computed(() =>
  store.assetClasses.filter((assetClass) => {
    const positions = positionsForClass(assetClass);
    if (positions.length === 0) {
      return (
        parseDecimal(portfolio.value.classTargets[assetClass])?.isPositive() ??
        false
      );
    }
    return instrumentTargetTotal(assetClass)?.equals(100) !== true;
  }),
);

function finishPositionEditing(): void {
  editingPositions.value = false;
}

function addPosition(): void {
  editingPositions.value = true;
  portfolio.value.positions.push({
    ticker: 'NEW',
    name: 'Новый инструмент',
    assetClass: 'Акции',
    quantity: '0',
    price: '0',
    lotSize: '1',
    target: '0',
    fresh: false,
  });
}

function removePosition(index: number): void {
  portfolio.value.positions.splice(index, 1);
}

function updatePrices(): void {
  store.updatePrices();
  updateNotice.value = true;
  window.setTimeout(() => {
    updateNotice.value = false;
  }, 2400);
}

function goToPortfolioPage(page = ''): void {
  void router.push(`/family/portfolios/${portfolio.value.id}${page}`);
}

watch(
  () => portfolio.value.id,
  () => {
    editingPositions.value = false;
  },
);
</script>

<template>
  <div class="page-stack">
    <section class="page-hero portfolio-hero">
      <div>
        <button class="back-link" type="button" @click="router.push('/')">
          ← Семейная сводка
        </button>
        <p class="eyebrow">{{ portfolio.owner }} · {{ portfolio.broker }}</p>
        <h1>{{ portfolio.name }}</h1>
        <p class="hero-copy">Данные обновлены {{ portfolio.updatedAt }}</p>
      </div>
      <div class="hero-actions">
        <Button
          label="Синхронизировать"
          severity="secondary"
          outlined
          @click="goToPortfolioPage('/sync')"
        />
        <Button
          label="Рассчитать сделки"
          @click="goToPortfolioPage('/planner')"
        />
      </div>
    </section>

    <div v-if="updateNotice" class="toast-note" role="status">
      <strong>Цены обновлены</strong
      ><span>Источник: Московская биржа · только что</span>
    </div>
    <section class="portfolio-overview">
      <div class="value-block">
        <span>Стоимость портфеля</span
        ><strong>{{ store.money(total) }} ₽</strong
        ><small
          >Включая {{ store.money(portfolio.cash) }} ₽ свободных денег</small
        >
      </div>
      <div class="overview-divider" />
      <div class="overview-stat">
        <span>Отклонение от цели</span
        ><strong class="negative-text"
          >{{ maxClassDeviation.toDecimalPlaces(1).toString() }} п.п.</strong
        ><small>максимальное по классам</small>
      </div>
      <div class="overview-stat">
        <span>Цены</span
        ><strong
          >{{
            portfolio.positions.filter((position) => position.fresh).length
          }}
          из {{ portfolio.positions.length }}</strong
        ><small>актуальны менее 60 минут</small>
      </div>
      <Button
        label="Обновить цены"
        severity="secondary"
        text
        @click="updatePrices"
      />
    </section>

    <div class="content-grid content-grid--wide">
      <section class="panel allocation-detail">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Распределение</p>
            <h2>Факт и цель</h2>
          </div>
          <div
            class="allocation-target-status"
            :class="{ invalid: !targetTotalIsValid }"
            :role="targetTotalIsValid ? 'status' : 'alert'"
            aria-live="polite"
          >
            <span aria-hidden="true">{{ targetTotalIsValid ? '✓' : '!' }}</span>
            <span>
              <small>Сумма целей классов</small>
              <strong>{{ targetTotalLabel }}%</strong>
              <small>{{ targetTotalHint }}</small>
            </span>
          </div>
        </div>
        <div
          class="allocation-table"
          role="table"
          aria-label="Фактическое и целевое распределение"
        >
          <div class="allocation-table__head" role="row">
            <span>Класс</span><span>Стоимость</span><span>Факт</span
            ><span>Цель</span><span>Отклонение</span>
          </div>
          <div
            v-for="row in allocationRows"
            :key="row.name"
            class="allocation-table__row"
            role="row"
          >
            <span
              ><i class="legend-dot" :class="`legend-dot--${row.color}`" />{{
                row.name
              }}</span
            >
            <strong>{{ store.money(row.value) }} ₽</strong>
            <span>{{ row.actual.toDecimalPlaces(1).toString() }}%</span>
            <span>
              <InlineDecimalCell
                :model-value="row.target"
                :formatted-value="formatTarget(row.target)"
                suffix="%"
                :label="`Цель класса ${row.name}`"
                maximum="100"
                @update:model-value="updateClassTarget(row.name, $event)"
              />
            </span>
            <strong
              :class="
                row.delta.isNegative() ? 'negative-text' : 'positive-text'
              "
              >{{ store.percent(row.delta) }}</strong
            >
          </div>
          <div class="allocation-table__row" role="row">
            <span
              ><i class="legend-dot legend-dot--cash" />Свободные деньги</span
            >
            <strong>
              <InlineDecimalCell
                v-model="portfolio.cash"
                :formatted-value="store.money(cashAllocation.value)"
                suffix=" ₽"
                label="Сумма свободных денег"
              />
            </strong>
            <span
              >{{ cashAllocation.actual.toDecimalPlaces(1).toString() }}%</span
            >
            <span>0%</span>
            <strong class="positive-text">{{
              store.percent(cashAllocation.delta)
            }}</strong>
          </div>
        </div>
      </section>

      <aside class="insight-card">
        <span class="insight-kicker">Наблюдение</span>
        <h3>Доля свободных денег выше цели</h3>
        <p>
          Свободные деньги можно распределить по целевым долям без продажи
          активов.
        </p>
        <button type="button" @click="goToPortfolioPage('/planner')">
          Рассчитать покупки <span>→</span>
        </button>
      </aside>
    </div>

    <section class="panel positions-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">{{ portfolio.positions.length }} инструмента</p>
          <h2>Состав портфеля</h2>
        </div>
        <div class="inline-actions">
          <Button
            v-if="editingPositions"
            label="Готово"
            severity="secondary"
            outlined
            @click="finishPositionEditing"
          />
          <Button
            label="+ Добавить"
            severity="secondary"
            text
            @click="addPosition"
          />
        </div>
      </div>
      <div class="positions-table">
        <section
          v-for="assetClass in classMeta"
          :key="assetClass.name"
          class="position-class-group"
        >
          <div class="position-class-heading">
            <div>
              <i
                class="legend-dot"
                :class="`legend-dot--${assetClass.color}`"
              />
              <span>
                <strong>{{ assetClass.name }}</strong>
                <small
                  >{{
                    positionsForClass(assetClass.name).length
                  }}
                  инструмента</small
                >
              </span>
            </div>
            <span
              v-if="positionsForClass(assetClass.name).length > 0"
              class="instrument-target-status"
              :class="{
                invalid: invalidInstrumentClasses.includes(assetClass.name),
              }"
              :role="
                invalidInstrumentClasses.includes(assetClass.name)
                  ? 'alert'
                  : 'status'
              "
              aria-live="polite"
            >
              <span aria-hidden="true">{{
                invalidInstrumentClasses.includes(assetClass.name) ? '!' : '✓'
              }}</span>
              Цели инструментов:
              <strong
                >{{ instrumentTargetTotalLabel(assetClass.name) }}%</strong
              >
              <small v-if="invalidInstrumentClasses.includes(assetClass.name)"
                >Нужно 100%</small
              >
            </span>
          </div>

          <template v-if="positionsForClass(assetClass.name).length > 0">
            <div class="positions-head">
              <TableFieldHeader
                label="Инструмент"
                description="Тикер и название позиции. Инструменты сгруппированы по классам активов."
              />
              <TableFieldHeader
                label="Количество"
                description="Текущее количество единиц инструмента в портфеле. Значение можно изменить двойным щелчком."
              />
              <TableFieldHeader
                label="Цена"
                description="Цена одной единицы инструмента в рублях. Значение можно изменить двойным щелчком."
              />
              <TableFieldHeader
                label="Стоимость"
                description="Текущая стоимость позиции: количество, умноженное на цену."
              />
              <TableFieldHeader
                label="Вес (факт)"
                description="Фактический вес позиции во всём портфеле, включая свободные деньги."
              />
              <TableFieldHeader
                label="Вес (цель)"
                description="Целевой вес позиции во всём портфеле: цель класса, умноженная на цель инструмента внутри класса."
              />
              <TableFieldHeader
                label="Факт"
                description="Фактическая доля инструмента внутри его класса активов."
              />
              <TableFieldHeader
                label="Цель"
                description="Целевая доля инструмента внутри класса активов. Цели инструментов одного класса должны давать 100%."
              />
              <span class="sr-only">Действия</span>
            </div>
            <div
              v-for="item in positionsForClass(assetClass.name)"
              :key="item.index"
              class="positions-row"
            >
              <span class="instrument">
                <template v-if="editingPositions">
                  <input
                    v-model="item.position.ticker"
                    class="table-input table-input--wide"
                    :aria-label="`Тикер инструмента ${item.index + 1}`"
                  />
                  <input
                    v-model="item.position.name"
                    class="table-input table-input--wide"
                    :aria-label="`Название инструмента ${item.index + 1}`"
                  />
                  <Select
                    v-model="item.position.assetClass"
                    class="table-select instrument-class-select"
                    :options="assetClassOptions"
                    :aria-label="`Класс активов ${item.position.ticker}`"
                  />
                </template>
                <template v-else>
                  <b>{{ item.position.ticker }}</b
                  ><small>{{ item.position.name }}</small>
                </template>
              </span>
              <span>
                <InlineDecimalCell
                  v-model="item.position.quantity"
                  :formatted-value="item.position.quantity"
                  suffix=" шт."
                  :label="`Количество ${item.position.ticker}`"
                />
              </span>
              <span class="price-cell"
                ><InlineDecimalCell
                  v-model="item.position.price"
                  :formatted-value="store.money(item.position.price)"
                  suffix=" ₽"
                  :label="`Цена ${item.position.ticker}`"
                />
                <span
                  v-if="!item.position.fresh"
                  v-tooltip.top="
                    'Цена устарела. Обновите цены перед расчётом сделок'
                  "
                  class="stale-price-marker"
                  role="img"
                  tabindex="0"
                  aria-label="Цена устарела. Обновите цены перед расчётом сделок"
                >
                  <svg aria-hidden="true" viewBox="0 0 20 20">
                    <circle cx="10" cy="10" r="7" />
                    <path d="M10 6v4l2.5 1.5" />
                  </svg>
                </span>
              </span>
              <strong
                >{{ store.money(store.positionValue(item.position)) }} ₽</strong
              >
              <span
                >{{
                  store
                    .share(store.positionValue(item.position), total)
                    .toDecimalPlaces(1)
                    .toString()
                }}%</span
              >
              <span
                >{{
                  positionTargetWeight(item.position, assetClass.name)
                    .toDecimalPlaces(1)
                    .toString()
                }}%</span
              >
              <span
                >{{
                  positionShareWithinClass(item.position, assetClass.name)
                    .toDecimalPlaces(1)
                    .toString()
                }}%</span
              >
              <span>
                <InlineDecimalCell
                  :model-value="item.position.target"
                  :formatted-value="formatTarget(item.position.target)"
                  suffix="%"
                  :label="`Цель ${item.position.ticker} внутри класса ${assetClass.name}`"
                  maximum="100"
                  @update:model-value="updatePositionTarget(item.index, $event)"
                />
              </span>
              <button
                v-if="editingPositions"
                class="icon-button"
                type="button"
                :aria-label="`Удалить ${item.position.ticker}`"
                @click="removePosition(item.index)"
              >
                ×
              </button>
              <span v-else />
            </div>
          </template>
          <p v-else class="empty-position-class">
            В этом классе пока нет инструментов
          </p>
        </section>
      </div>
    </section>
  </div>
</template>
