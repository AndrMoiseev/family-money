import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it } from 'vitest';

import InlineDecimalCell from './InlineDecimalCell.vue';

function mountCell() {
  return mount(InlineDecimalCell, {
    attachTo: document.body,
    props: {
      modelValue: '48.50',
      formattedValue: '48,5',
      suffix: ' ₽',
      label: 'Свободные деньги',
    },
  });
}

function mountPercentageCell() {
  return mount(InlineDecimalCell, {
    attachTo: document.body,
    props: {
      modelValue: '58.33',
      formattedValue: '58.33',
      suffix: '%',
      label: 'Цель TMOS',
      maximum: '100',
    },
  });
}

afterEach(() => {
  document.body.replaceChildren();
});

describe('InlineDecimalCell', () => {
  it('opens on double click and commits a canonical decimal on Enter', async () => {
    const wrapper = mountCell();

    await wrapper.get('button').trigger('dblclick');
    const input = wrapper.get('input');
    expect(input.element).toBe(document.activeElement);

    await input.setValue(' 0012,500 ');
    await input.trigger('keydown', { key: 'Enter' });

    expect(wrapper.emitted('update:modelValue')).toEqual([['12.5']]);
    expect(wrapper.find('input').exists()).toBe(false);
  });

  it('supports keyboard editing and cancels with Escape', async () => {
    const wrapper = mountCell();

    await wrapper.get('button').trigger('keydown', { key: 'Enter' });
    const input = wrapper.get('input');
    await input.setValue('99');
    await input.trigger('keydown', { key: 'Escape' });

    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
    expect(wrapper.find('input').exists()).toBe(false);
  });

  it('keeps an invalid value in edit mode and explains the error', async () => {
    const wrapper = mountCell();

    await wrapper.get('button').trigger('dblclick');
    const input = wrapper.get('input');
    await input.setValue('-1');
    await input.trigger('keydown', { key: 'Enter' });

    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
    expect(wrapper.get('input').attributes('aria-invalid')).toBe('true');
    expect(wrapper.get('[role="alert"]').text()).toContain('не меньше нуля');
  });

  it('rejects a value above the configured maximum', async () => {
    const wrapper = mountPercentageCell();

    await wrapper.get('button').trigger('dblclick');
    const input = wrapper.get('input');
    await input.setValue('100.01');
    await input.trigger('keydown', { key: 'Enter' });

    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
    expect(wrapper.get('input').attributes('aria-invalid')).toBe('true');
    expect(wrapper.get('[role="alert"]').text()).toContain('от 0 до 100');
  });
});
