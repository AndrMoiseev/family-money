<script setup lang="ts">
import Decimal from 'decimal.js';
import { nextTick, ref } from 'vue';

const props = withDefaults(
  defineProps<{
    modelValue: string;
    formattedValue: string;
    suffix?: string;
    label: string;
    maximum?: string;
  }>(),
  { suffix: '', maximum: undefined },
);

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const editing = ref(false);
const draft = ref('');
const invalid = ref(false);
const input = ref<HTMLInputElement>();

function canonicalDecimal(value: string): string | null {
  const normalized = value
    .trim()
    .replace(/[\s\u00a0]/g, '')
    .replace(',', '.');
  if (!normalized) return null;

  try {
    const decimal = new Decimal(normalized);
    if (!decimal.isFinite() || decimal.isNegative()) return null;
    if (props.maximum !== undefined && decimal.greaterThan(props.maximum)) {
      return null;
    }
    if (decimal.isZero()) return '0';
    return decimal.toFixed(decimal.decimalPlaces());
  } catch {
    return null;
  }
}

async function startEditing(): Promise<void> {
  if (editing.value) return;
  draft.value = props.modelValue;
  invalid.value = false;
  editing.value = true;
  await nextTick();
  input.value?.focus();
  input.value?.select();
}

function commit(): void {
  const value = canonicalDecimal(draft.value);
  if (value === null) {
    invalid.value = true;
    void nextTick(() => input.value?.focus());
    return;
  }

  if (value !== props.modelValue) emit('update:modelValue', value);
  invalid.value = false;
  editing.value = false;
}

function cancel(): void {
  draft.value = props.modelValue;
  invalid.value = false;
  editing.value = false;
}
</script>

<template>
  <span
    class="editable-decimal"
    :class="{
      'editable-decimal--editing': editing,
      'editable-decimal--invalid': invalid,
    }"
  >
    <template v-if="editing">
      <input
        ref="input"
        v-model="draft"
        class="editable-decimal__input"
        inputmode="decimal"
        autocomplete="off"
        :aria-label="label"
        :aria-invalid="invalid"
        @blur="commit"
        @input="invalid = false"
        @keydown.enter.prevent="commit"
        @keydown.esc.prevent="cancel"
      />
      <span v-if="suffix" class="editable-decimal__suffix">{{ suffix }}</span>
      <span v-if="invalid" class="editable-decimal__error" role="alert">
        <template v-if="maximum !== undefined">
          Введите число от 0 до {{ maximum }}
        </template>
        <template v-else>Введите число не меньше нуля</template>
      </span>
    </template>
    <button
      v-else
      class="editable-decimal__display"
      type="button"
      :aria-label="`${label}. Двойной клик или Enter для редактирования`"
      title="Двойной клик или Enter для редактирования"
      @dblclick="startEditing"
      @keydown.enter.prevent="startEditing"
      @keydown.space.prevent="startEditing"
    >
      <span>{{ formattedValue }}{{ suffix }}</span>
      <svg aria-hidden="true" viewBox="0 0 16 16">
        <path
          d="m3 11.8.3-2.3 6.6-6.6a1.1 1.1 0 0 1 1.6 0l.6.6a1.1 1.1 0 0 1 0 1.6l-6.6 6.6-2.5.1Z"
        />
        <path d="m8.9 3.9 3.2 3.2" />
      </svg>
    </button>
  </span>
</template>
