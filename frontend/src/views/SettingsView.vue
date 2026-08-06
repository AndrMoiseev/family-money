<script setup lang="ts">
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { prototypeStore as store, type FamilyMember } from '../prototypeStore';

const route = useRoute();
const router = useRouter();
const section = computed(() =>
  route.name === 'settings-portfolios' ? 'portfolios' : 'members',
);
const newMemberName = ref('');
const editingMemberId = ref<string | null>(null);
const memberDraft = ref('');

function addMember(): void {
  const name = newMemberName.value.trim();
  if (!name) return;
  store.addMember(name);
  newMemberName.value = '';
}

function startRename(member: FamilyMember): void {
  editingMemberId.value = member.id;
  memberDraft.value = member.name;
}

function saveRename(member: FamilyMember): void {
  store.renameMember(member.id, memberDraft.value);
  editingMemberId.value = null;
}

function openPortfolio(id: string, edit = false): void {
  store.setCurrentPortfolio(id);
  void router.push(
    edit ? `/settings/portfolios/${id}` : `/family/portfolios/${id}`,
  );
}

function createPortfolio(): void {
  const portfolio = store.createPortfolio();
  void router.push(`/settings/portfolios/${portfolio.id}`);
}
</script>

<template>
  <div class="page-stack settings-page">
    <section class="page-hero settings-hero">
      <div>
        <p class="eyebrow">Управление данными</p>
        <h1>Настройки</h1>
        <p class="hero-copy">
          Члены семьи и принадлежащие им инвестиционные портфели.
        </p>
      </div>
    </section>

    <nav class="settings-tabs" aria-label="Разделы настроек">
      <RouterLink
        to="/settings/members"
        :class="{ active: section === 'members' }"
      >
        Члены семьи
        <span>{{ store.members.length }}</span>
      </RouterLink>
      <RouterLink
        to="/settings/portfolios"
        :class="{ active: section === 'portfolios' }"
      >
        Портфели
        <span>{{ store.portfolios.length }}</span>
      </RouterLink>
    </nav>

    <section v-if="section === 'members'" class="panel settings-panel">
      <div class="panel-heading settings-panel__heading">
        <div>
          <p class="eyebrow">Справочник владельцев</p>
          <h2>Члены семьи</h2>
          <p>Владельцы активов — без отдельных аккаунтов и прав доступа.</p>
        </div>
        <form class="inline-create" @submit.prevent="addMember">
          <label class="sr-only" for="new-member">Имя нового члена семьи</label>
          <InputText
            id="new-member"
            v-model="newMemberName"
            placeholder="Имя члена семьи"
          />
          <Button
            label="Добавить"
            type="submit"
            :disabled="!newMemberName.trim()"
          />
        </form>
      </div>

      <div class="settings-list">
        <article
          v-for="member in store.members"
          :key="member.id"
          class="settings-row"
        >
          <span class="member-avatar">{{ member.name.slice(0, 1) }}</span>
          <div v-if="editingMemberId !== member.id" class="settings-row__main">
            <strong>{{ member.name }}</strong>
            <small>
              {{
                store.portfolios.filter((item) => item.owner === member.name)
                  .length
              }}
              портфеля
            </small>
          </div>
          <form v-else class="rename-form" @submit.prevent="saveRename(member)">
            <InputText v-model="memberDraft" aria-label="Новое имя" autofocus />
            <Button label="Сохранить" type="submit" size="small" />
            <Button
              label="Отмена"
              type="button"
              size="small"
              severity="secondary"
              text
              @click="editingMemberId = null"
            />
          </form>
          <Button
            v-if="editingMemberId !== member.id"
            label="Переименовать"
            severity="secondary"
            text
            @click="startRename(member)"
          />
        </article>
      </div>
    </section>

    <section v-else class="panel settings-panel">
      <div class="panel-heading settings-panel__heading">
        <div>
          <p class="eyebrow">Структура семьи</p>
          <h2>Портфели</h2>
          <p>Каждый портфель связан с одним владельцем и одним брокером.</p>
        </div>
        <Button label="Добавить портфель" @click="createPortfolio" />
      </div>

      <div class="settings-list">
        <article
          v-for="portfolio in store.portfolios"
          :key="portfolio.id"
          class="settings-row settings-row--portfolio"
        >
          <span class="portfolio-symbol">{{
            portfolio.name.slice(0, 2).toUpperCase()
          }}</span>
          <button
            class="settings-row__main settings-row__link"
            type="button"
            @click="openPortfolio(portfolio.id)"
          >
            <strong>{{ portfolio.name }}</strong>
            <small>{{ portfolio.owner }} · {{ portfolio.broker }}</small>
          </button>
          <div class="settings-row__value">
            <strong
              >{{ store.money(store.portfolioValue(portfolio)) }} ₽</strong
            >
            <small>Обновлён {{ portfolio.updatedAt }}</small>
          </div>
          <Button
            label="Настроить"
            severity="secondary"
            outlined
            @click="openPortfolio(portfolio.id, true)"
          />
        </article>
      </div>
    </section>
  </div>
</template>
