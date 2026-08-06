import { mount } from '@vue/test-utils';
import Decimal from 'decimal.js';
import PrimeVue from 'primevue/config';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { prototypeStore as store } from '../prototypeStore';
import PlannerView from './PlannerView.vue';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

const portfolio = store.portfolios.find(({ id }) => id === 'andrey-iis')!;
const originalUpdatedAt = portfolio.updatedAt;
const originalFreshness = portfolio.positions.map(({ fresh }) => fresh);

function mountPlanner() {
  return mount(PlannerView, {
    attachTo: document.body,
    global: {
      plugins: [[PrimeVue, { unstyled: true }]],
    },
  });
}

function buttonWithText(
  wrapper: ReturnType<typeof mountPlanner>,
  text: string,
) {
  const button = wrapper
    .findAll('button')
    .find((candidate) => candidate.text().includes(text));
  if (!button) throw new Error(`Button not found: ${text}`);
  return button;
}

afterEach(() => {
  store.setCurrentPortfolio(portfolio.id);
  store.planning.mode = 'buy-only';
  store.generatePlan();
  portfolio.updatedAt = originalUpdatedAt;
  portfolio.positions.forEach((position, index) => {
    position.fresh = originalFreshness[index] ?? position.fresh;
  });
  document.body.replaceChildren();
});

describe('PlannerView calculation modes', () => {
  it('offers only purchases and rebalancing without a contribution input', () => {
    store.setCurrentPortfolio(portfolio.id);
    store.planning.mode = 'buy-only';

    const wrapper = mountPlanner();
    const modes = wrapper.findAll('.mode-grid button');

    expect(modes).toHaveLength(2);
    expect(modes.map((mode) => mode.text())).toEqual([
      expect.stringContaining('Только покупки'),
      expect.stringContaining('Ребалансировка'),
    ]);
    expect(wrapper.text()).not.toContain('Пополнение');
    expect(wrapper.find('input[aria-label="Сумма пополнения"]').exists()).toBe(
      false,
    );
  });

  it('funds a purchases-only plan from each portfolio existing free cash', async () => {
    for (const candidatePortfolio of store.portfolios) {
      store.setCurrentPortfolio(candidatePortfolio.id);
      store.planning.mode = 'buy-only';
      store.generatePlan();

      expect(store.plan.every((trade) => trade.side === 'Купить')).toBe(true);
      const requiredCash = store.plan.reduce(
        (total, trade) =>
          total
            .plus(store.tradeValue(trade))
            .plus(store.tradeCommission(trade)),
        new Decimal(0),
      );
      expect(requiredCash.lessThanOrEqualTo(candidatePortfolio.cash)).toBe(
        true,
      );
    }

    store.setCurrentPortfolio(portfolio.id);
    const wrapper = mountPlanner();
    await buttonWithText(wrapper, 'Обновить').trigger('click');
    await buttonWithText(wrapper, 'Рассчитать план').trigger('click');
    expect(wrapper.get('.plan-summary-strip').text()).toContain(
      `Свободные деньги до сделок${store.money(portfolio.cash)} ₽`,
    );
  });

  it('allows sales only after rebalancing is selected', async () => {
    store.setCurrentPortfolio(portfolio.id);
    store.planning.mode = 'buy-only';
    const wrapper = mountPlanner();

    await buttonWithText(wrapper, 'Ребалансировка').trigger('click');
    store.generatePlan();

    expect(store.planning.mode).toBe('rebalance');
    expect(store.plan.some((trade) => trade.side === 'Продать')).toBe(true);
  });
});
