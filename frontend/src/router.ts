import { createRouter, createWebHistory } from 'vue-router';

import { prototypeStore as store } from './prototypeStore';
import ExecutionView from './views/ExecutionView.vue';
import FamilySummaryView from './views/FamilySummaryView.vue';
import PlannerView from './views/PlannerView.vue';
import PortfolioView from './views/PortfolioView.vue';
import SettingsView from './views/SettingsView.vue';
import SetupView from './views/SetupView.vue';
import SyncView from './views/SyncView.vue';

const currentPortfolioPath = (page = ''): string =>
  `/family/portfolios/${store.currentPortfolio.value.id}${page}`;

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', name: 'family', component: FamilySummaryView },
    { path: '/family', redirect: '/' },
    {
      path: '/family/portfolios/:portfolioId',
      name: 'portfolio',
      component: PortfolioView,
    },
    {
      path: '/family/portfolios/:portfolioId/planner',
      name: 'planner',
      component: PlannerView,
    },
    {
      path: '/family/portfolios/:portfolioId/execution',
      name: 'execution',
      component: ExecutionView,
    },
    {
      path: '/family/portfolios/:portfolioId/sync',
      name: 'sync',
      component: SyncView,
    },
    { path: '/settings', redirect: '/settings/members' },
    {
      path: '/settings/members',
      name: 'settings-members',
      component: SettingsView,
    },
    {
      path: '/settings/portfolios',
      name: 'settings-portfolios',
      component: SettingsView,
    },
    {
      path: '/settings/portfolios/:portfolioId',
      name: 'portfolio-settings',
      component: SetupView,
    },
    { path: '/portfolio', redirect: () => currentPortfolioPath() },
    {
      path: '/setup',
      redirect: () => `/settings/portfolios/${store.currentPortfolio.value.id}`,
    },
    { path: '/planner', redirect: () => currentPortfolioPath('/planner') },
    {
      path: '/execution',
      redirect: () => currentPortfolioPath('/execution'),
    },
    { path: '/sync', redirect: () => currentPortfolioPath('/sync') },
  ],
});

router.beforeEach((to) => {
  const portfolioId = to.params.portfolioId;
  if (typeof portfolioId === 'string') store.setCurrentPortfolio(portfolioId);
});

export default router;
