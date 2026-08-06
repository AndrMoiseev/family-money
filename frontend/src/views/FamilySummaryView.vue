<script setup lang="ts">
import Decimal from 'decimal.js';
import Button from 'primevue/button';
import Checkbox from 'primevue/checkbox';
import { computed } from 'vue';
import { useRouter } from 'vue-router';

import {
  prototypeStore as store,
  type AssetClass,
  type Portfolio,
} from '../prototypeStore';

const router = useRouter();
const selected = computed(() =>
  store.portfolios.filter((portfolio) =>
    store.selectedPortfolioIds.has(portfolio.id),
  ),
);
const total = computed(() =>
  selected.value.reduce(
    (sum, portfolio) => sum.plus(store.portfolioValue(portfolio)),
    new Decimal(0),
  ),
);
const classes: AssetClass[] = ['Акции', 'Облигации', 'Фонды ликвидности'];

const classRows = computed(() => {
  const rows = classes.map((assetClass) => ({
    name: assetClass,
    value: selected.value.reduce(
      (sum, portfolio) => sum.plus(store.classValue(portfolio, assetClass)),
      new Decimal(0),
    ),
  }));
  rows.push({
    name: 'Свободные деньги' as AssetClass,
    value: selected.value.reduce(
      (sum, portfolio) => sum.plus(portfolio.cash),
      new Decimal(0),
    ),
  });
  return rows;
});

const ownerRows = computed(() =>
  store.members.map((member) => ({
    owner: member.name,
    value: selected.value
      .filter((portfolio) => portfolio.owner === member.name)
      .reduce(
        (sum, portfolio) => sum.plus(store.portfolioValue(portfolio)),
        new Decimal(0),
      ),
  })),
);

function barWidth(value: Decimal): string {
  return `${store.share(value, total.value).toDecimalPlaces(2).toString()}%`;
}

function openPortfolio(portfolio: Portfolio): void {
  store.setCurrentPortfolio(portfolio.id);
  void router.push(`/family/portfolios/${portfolio.id}`);
}
</script>

<template>
  <div class="page-stack">
    <section class="page-hero page-hero--summary">
      <div>
        <p class="eyebrow">Семейная сводка</p>
        <h1>Капитал семьи</h1>
        <p class="hero-copy">
          Общая картина без смешивания целей отдельных портфелей.
        </p>
      </div>
      <Button label="Выгрузить в XLSX" severity="secondary" outlined>
        <template #icon><span aria-hidden="true">↓</span></template>
      </Button>
    </section>

    <section class="wealth-card">
      <div>
        <span class="metric-label"
          >Выбрано портфелей: {{ selected.length }}</span
        >
        <strong class="wealth-total">{{ store.money(total) }} ₽</strong>
        <span class="positive-note">↑ 2,8% за текущий месяц</span>
      </div>
      <div class="owner-breakdown" aria-label="Капитал по владельцам">
        <div v-for="row in ownerRows" :key="row.owner">
          <span>{{ row.owner }}</span
          ><strong>{{ store.money(row.value) }} ₽</strong>
          <div class="mini-track">
            <i :style="{ width: barWidth(row.value) }" />
          </div>
        </div>
      </div>
    </section>

    <div class="summary-grid">
      <section class="panel portfolio-selector">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Включено в расчёт</p>
            <h2>Портфели</h2>
          </div>
          <button
            class="text-button"
            type="button"
            @click="
              store.portfolios.forEach((item) =>
                store.selectedPortfolioIds.add(item.id),
              )
            "
          >
            Выбрать все
          </button>
        </div>
        <div class="portfolio-list">
          <article
            v-for="portfolio in store.portfolios"
            :key="portfolio.id"
            class="portfolio-row"
          >
            <Checkbox
              :model-value="store.selectedPortfolioIds.has(portfolio.id)"
              binary
              :input-id="`portfolio-${portfolio.id}`"
              @update:model-value="store.toggleSelectedPortfolio(portfolio.id)"
            />
            <button
              type="button"
              class="portfolio-row__body"
              @click="openPortfolio(portfolio)"
            >
              <span
                ><strong>{{ portfolio.name }}</strong
                ><small
                  >{{ portfolio.owner }} · {{ portfolio.broker }}</small
                ></span
              >
              <span class="portfolio-value"
                ><strong
                  >{{ store.money(store.portfolioValue(portfolio)) }} ₽</strong
                ><small>Обновлён {{ portfolio.updatedAt }}</small></span
              >
              <span class="row-arrow" aria-hidden="true">›</span>
            </button>
          </article>
        </div>
      </section>

      <section class="panel allocation-card">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Фактическая структура</p>
            <h2>По классам активов</h2>
          </div>
        </div>
        <div class="allocation-list">
          <div
            v-for="(row, index) in classRows"
            :key="row.name"
            class="allocation-row"
          >
            <span class="legend-dot" :class="`legend-dot--${index}`" />
            <span class="allocation-name">{{ row.name }}</span>
            <div class="allocation-track">
              <i
                :class="`bar-${index}`"
                :style="{ width: barWidth(row.value) }"
              />
            </div>
            <strong
              >{{
                store.share(row.value, total).toDecimalPlaces(1).toString()
              }}%</strong
            >
          </div>
        </div>
        <p class="panel-footnote">
          Сводка показывает только фактическое распределение. Общая семейная
          цель не рассчитывается.
        </p>
      </section>
    </div>

    <section class="next-action">
      <div class="next-action__icon">→</div>
      <div>
        <strong>Регулярный цикл</strong
        ><span>Проверьте цены и рассчитайте очередные сделки для ИИС.</span>
      </div>
      <Button
        label="Перейти к портфелю"
        @click="openPortfolio(store.portfolios[0]!)"
      />
    </section>
  </div>
</template>
