<script setup lang="ts">
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Select from 'primevue/select';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import { prototypeStore as store } from '../prototypeStore';

const router = useRouter();
const portfolio = store.currentPortfolio;
const saved = ref(false);
const newOwner = ref('');
const owners = computed(() => store.members.map((member) => member.name));
const brokers = ['Т-Инвестиции', 'СберИнвестиции', 'Другой брокер'];

function addOwner(): void {
  const value = newOwner.value.trim();
  if (!value || owners.value.includes(value)) return;
  store.addMember(value);
  portfolio.value.owner = value;
  newOwner.value = '';
}

function save(): void {
  saved.value = true;
  window.setTimeout(() => {
    saved.value = false;
  }, 2200);
}
</script>

<template>
  <div class="page-stack setup-page">
    <section class="page-hero">
      <div>
        <button
          class="back-link"
          type="button"
          @click="router.push('/settings/portfolios')"
        >
          ← К списку портфелей
        </button>
        <p class="eyebrow">Настройки · Портфели</p>
        <h1>Настройки портфеля</h1>
        <p class="hero-copy">Название, владелец, брокер и комиссии.</p>
      </div>
      <Button label="Сохранить изменения" @click="save" />
    </section>

    <div v-if="saved" class="toast-note" role="status">
      <strong>Изменения сохранены</strong
      ><span>Прототип запомнит их до обновления страницы</span>
    </div>

    <div class="settings-form-layout">
      <section class="panel form-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Основные параметры</p>
            <h2>Портфель и владелец</h2>
          </div>
          <span class="currency-chip">RUB</span>
        </div>
        <div class="form-grid">
          <label class="field field--wide"
            ><span>Название портфеля</span><InputText v-model="portfolio.name"
          /></label>
          <label class="field"
            ><span>Владелец</span
            ><Select v-model="portfolio.owner" :options="owners"
          /></label>
          <label class="field"
            ><span>Брокер</span
            ><Select v-model="portfolio.broker" :options="brokers"
          /></label>
          <label class="field"
            ><span>Комиссия на покупку, %</span
            ><InputText v-model="portfolio.buyCommission" inputmode="decimal"
          /></label>
          <label class="field"
            ><span>Комиссия на продажу, %</span
            ><InputText v-model="portfolio.sellCommission" inputmode="decimal"
          /></label>
        </div>
        <div class="owner-creator">
          <div>
            <strong>Новый член семьи</strong
            ><span
              >Владелец нужен только для структуры активов, не для входа.</span
            >
          </div>
          <InputText
            v-model="newOwner"
            placeholder="Имя владельца"
            @keyup.enter="addOwner"
          />
          <Button
            label="Добавить"
            severity="secondary"
            outlined
            @click="addOwner"
          />
        </div>
      </section>
    </div>
  </div>
</template>
