import { mount } from '@vue/test-utils';
import PrimeVue from 'primevue/config';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { prototypeStore as store } from '../prototypeStore';
import PortfolioView from './PortfolioView.vue';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

const originalClassTargets = { ...store.currentPortfolio.value.classTargets };
const originalPositionTargets = store.currentPortfolio.value.positions.map(
  (position) => position.target,
);

afterEach(() => {
  Object.assign(
    store.currentPortfolio.value.classTargets,
    originalClassTargets,
  );
  store.currentPortfolio.value.positions.forEach((position, index) => {
    position.target = originalPositionTargets[index] ?? position.target;
  });
  document.body.replaceChildren();
});

describe('PortfolioView composition', () => {
  it('shows both portfolio weights and edits an instrument target inline', async () => {
    const wrapper = mount(PortfolioView, {
      attachTo: document.body,
      global: {
        plugins: [[PrimeVue, { unstyled: true }]],
      },
    });

    const headings = wrapper
      .findAll('.table-field-heading')
      .map((heading) => heading.text());
    expect(headings).toContain('Вес (факт)');
    expect(headings).toContain('Вес (цель)');

    const goal = wrapper.get(
      'button[aria-label^="Цель TMOS внутри класса Акции"]',
    );
    await goal.trigger('dblclick');
    const input = wrapper.get(
      'input[aria-label="Цель TMOS внутри класса Акции"]',
    );
    await input.setValue('50');
    await input.trigger('keydown', { key: 'Enter' });

    expect(store.currentPortfolio.value.positions[0]!.target).toBe('50');
    const tmosRow = wrapper
      .findAll('.positions-row')
      .find((row) => row.text().includes('TMOS'));
    expect(tmosRow?.text()).toContain('30%');
    expect(tmosRow?.text()).toContain('50.00%');

    const stocksHeading = wrapper
      .findAll('.position-class-heading')
      .find((heading) => heading.text().includes('Акции'));
    const instrumentStatus = stocksHeading?.get('[role="alert"]').text();
    expect(instrumentStatus).toContain('Цели инструментов:');
    expect(instrumentStatus).toContain('91.67%');
    expect(instrumentStatus).toContain('Нужно 100%');
    expect(store.targetAllocationIsValid(store.currentPortfolio.value)).toBe(
      false,
    );
  });

  it('edits class targets inline and updates the total immediately', async () => {
    const wrapper = mount(PortfolioView, {
      attachTo: document.body,
      global: {
        plugins: [[PrimeVue, { unstyled: true }]],
      },
    });

    expect(wrapper.get('.allocation-target-status').text()).toContain(
      'Сумма целей классов100%',
    );
    expect(store.targetAllocationIsValid(store.currentPortfolio.value)).toBe(
      true,
    );
    expect(wrapper.find('.allocation-detail .text-button').exists()).toBe(
      false,
    );

    await wrapper
      .get('button[aria-label^="Цель класса Акции"]')
      .trigger('dblclick');
    const stocksTarget = wrapper.get('input[aria-label="Цель класса Акции"]');
    await stocksTarget.setValue('50');
    await stocksTarget.trigger('keydown', { key: 'Enter' });

    const targetStatus = wrapper.get('.allocation-target-status');
    expect(targetStatus.attributes('role')).toBe('alert');
    expect(targetStatus.text()).toContain('Сумма целей классов90%');
    expect(targetStatus.text()).toContain('Не хватает 10%');
    expect(store.currentPortfolio.value.classTargets.Акции).toBe('50');

    await wrapper
      .get('button[aria-label^="Цель класса Фонды ликвидности"]')
      .trigger('dblclick');
    const liquidityTarget = wrapper.get(
      'input[aria-label="Цель класса Фонды ликвидности"]',
    );
    await liquidityTarget.setValue('25');
    await liquidityTarget.trigger('keydown', { key: 'Enter' });

    expect(wrapper.get('.allocation-target-status').attributes('role')).toBe(
      'status',
    );
    expect(store.currentPortfolio.value.classTargets['Фонды ликвидности']).toBe(
      '25',
    );
  });

  it('marks a shortfall red and an excess green', () => {
    const wrapper = mount(PortfolioView, {
      attachTo: document.body,
      global: {
        plugins: [[PrimeVue, { unstyled: true }]],
      },
    });

    const rows = wrapper.findAll('.allocation-table__row');
    const stocks = rows.find((row) => row.text().includes('Акции'));
    const bonds = rows.find((row) => row.text().includes('Облигации'));
    const cash = rows.find((row) => row.text().includes('Свободные деньги'));

    expect(stocks?.get('.negative-text').text()).toContain('-');
    expect(bonds?.get('.positive-text').text()).toContain('+');
    expect(cash?.get('.positive-text').text()).toContain('+');
  });
});
