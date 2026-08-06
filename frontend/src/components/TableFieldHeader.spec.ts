import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import TableFieldHeader from './TableFieldHeader.vue';

describe('TableFieldHeader', () => {
  it('makes the explanation available to keyboard and assistive technology', () => {
    const wrapper = mount(TableFieldHeader, {
      props: {
        label: 'Вес (цель)',
        description: 'Целевой вес инструмента во всём портфеле.',
      },
      global: {
        directives: {
          tooltip: {},
        },
      },
    });

    const heading = wrapper.get('.table-field-heading');
    expect(heading.attributes('tabindex')).toBe('0');
    expect(heading.attributes('aria-label')).toBe(
      'Вес (цель): Целевой вес инструмента во всём портфеле.',
    );
  });
});
