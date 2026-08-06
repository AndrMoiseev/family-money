import Decimal from 'decimal.js';
import { computed, reactive } from 'vue';

export const assetClasses = [
  'Акции',
  'Облигации',
  'Фонды ликвидности',
] as const;

export type AssetClass = (typeof assetClasses)[number];

export interface Position {
  ticker: string;
  name: string;
  assetClass: AssetClass;
  quantity: string;
  price: string;
  lotSize: string;
  target: string;
  fresh: boolean;
}

export interface Portfolio {
  id: string;
  name: string;
  owner: string;
  broker: string;
  cash: string;
  buyCommission: string;
  sellCommission: string;
  updatedAt: string;
  classTargets: Record<AssetClass, string>;
  positions: Position[];
}

export interface FamilyMember {
  id: string;
  name: string;
}

export interface Trade {
  ticker: string;
  side: 'Купить' | 'Продать';
  lots: string;
  lotSize: string;
  price: string;
}

const members = reactive<FamilyMember[]>([
  { id: 'andrey', name: 'Андрей' },
  { id: 'maria', name: 'Мария' },
]);

const portfolios = reactive<Portfolio[]>([
  {
    id: 'andrey-iis',
    name: 'ИИС — долгосрочный',
    owner: 'Андрей',
    broker: 'Т-Инвестиции',
    cash: '48650.32',
    buyCommission: '0.05',
    sellCommission: '0.05',
    updatedAt: 'сегодня, 09:42',
    classTargets: {
      Акции: '60',
      Облигации: '25',
      'Фонды ликвидности': '15',
    },
    positions: [
      {
        ticker: 'TMOS',
        name: 'Тинькофф iMOEX',
        assetClass: 'Акции',
        quantity: '1834',
        price: '8.14',
        lotSize: '1',
        target: '58.33',
        fresh: true,
      },
      {
        ticker: 'SBMX',
        name: 'Первая — Фонд Топ Российских акций',
        assetClass: 'Акции',
        quantity: '940',
        price: '19.27',
        lotSize: '1',
        target: '41.67',
        fresh: true,
      },
      {
        ticker: 'SU26238RMFS4',
        name: 'ОФЗ 26238',
        assetClass: 'Облигации',
        quantity: '61',
        price: '592.20',
        lotSize: '1',
        target: '100',
        fresh: true,
      },
      {
        ticker: 'LQDT',
        name: 'ВИМ Ликвидность',
        assetClass: 'Фонды ликвидности',
        quantity: '920',
        price: '1.82',
        lotSize: '1',
        target: '100',
        fresh: false,
      },
    ],
  },
  {
    id: 'maria-brokerage',
    name: 'Портфель Марии',
    owner: 'Мария',
    broker: 'СберИнвестиции',
    cash: '12980.00',
    buyCommission: '0.06',
    sellCommission: '0.06',
    updatedAt: 'вчера, 18:20',
    classTargets: {
      Акции: '40',
      Облигации: '45',
      'Фонды ликвидности': '15',
    },
    positions: [
      {
        ticker: 'SBER',
        name: 'Сбербанк',
        assetClass: 'Акции',
        quantity: '160',
        price: '319.48',
        lotSize: '10',
        target: '100',
        fresh: true,
      },
      {
        ticker: 'AKME',
        name: 'Альфа-Капитал Управляемые облигации',
        assetClass: 'Облигации',
        quantity: '402',
        price: '112.36',
        lotSize: '1',
        target: '100',
        fresh: true,
      },
      {
        ticker: 'LQDT',
        name: 'ВИМ Ликвидность',
        assetClass: 'Фонды ликвидности',
        quantity: '2200',
        price: '1.82',
        lotSize: '1',
        target: '100',
        fresh: false,
      },
    ],
  },
  {
    id: 'andrey-brokerage',
    name: 'Резервный портфель',
    owner: 'Андрей',
    broker: 'Т-Инвестиции',
    cash: '84120.75',
    buyCommission: '0.05',
    sellCommission: '0.05',
    updatedAt: '30 июля, 14:11',
    classTargets: {
      Акции: '0',
      Облигации: '70',
      'Фонды ликвидности': '30',
    },
    positions: [
      {
        ticker: 'SBGB',
        name: 'Первая — Государственные облигации',
        assetClass: 'Облигации',
        quantity: '690',
        price: '13.42',
        lotSize: '1',
        target: '100',
        fresh: true,
      },
      {
        ticker: 'LQDT',
        name: 'ВИМ Ликвидность',
        assetClass: 'Фонды ликвидности',
        quantity: '4200',
        price: '1.82',
        lotSize: '1',
        target: '100',
        fresh: true,
      },
    ],
  },
]);

const selectedPortfolioIds = reactive(
  new Set(portfolios.map((portfolio) => portfolio.id)),
);
const currentPortfolioId = reactive({ value: 'andrey-iis' });
const planning = reactive({
  mode: 'buy-only' as 'buy-only' | 'rebalance',
});
const plan = reactive<Trade[]>([
  { ticker: 'TMOS', side: 'Купить', lots: '2000', lotSize: '1', price: '8.14' },
  {
    ticker: 'SBMX',
    side: 'Купить',
    lots: '700',
    lotSize: '1',
    price: '19.27',
  },
  {
    ticker: 'SU26238RMFS4',
    side: 'Купить',
    lots: '30',
    lotSize: '1',
    price: '592.20',
  },
]);

const currentPortfolio = computed(
  () =>
    portfolios.find((portfolio) => portfolio.id === currentPortfolioId.value) ??
    portfolios[0]!,
);

function positionValue(position: Position): Decimal {
  return new Decimal(position.quantity).times(position.price);
}

function portfolioValue(portfolio: Portfolio): Decimal {
  return portfolio.positions.reduce(
    (sum, position) => sum.plus(positionValue(position)),
    new Decimal(portfolio.cash),
  );
}

function money(value: Decimal.Value): string {
  const [integer = '0', fraction = ''] = new Decimal(value)
    .toDecimalPlaces(2)
    .toFixed(2)
    .split('.');
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '\u00a0');
  return fraction === '00'
    ? grouped
    : `${grouped},${fraction.replace(/0$/, '')}`;
}

function percent(value: Decimal.Value): string {
  const decimal = new Decimal(value);
  return `${decimal.isPositive() ? '+' : ''}${decimal.toDecimalPlaces(1).toString()} п.п.`;
}

function share(value: Decimal.Value, total: Decimal.Value): Decimal {
  const denominator = new Decimal(total);
  return denominator.isZero()
    ? new Decimal(0)
    : new Decimal(value).div(denominator).times(100);
}

function classValue(portfolio: Portfolio, assetClass: AssetClass): Decimal {
  return portfolio.positions
    .filter((position) => position.assetClass === assetClass)
    .reduce(
      (sum, position) => sum.plus(positionValue(position)),
      new Decimal(0),
    );
}

function validTarget(value: string): Decimal | null {
  try {
    const target = new Decimal(value);
    return target.isFinite() &&
      !target.isNegative() &&
      target.lessThanOrEqualTo(100)
      ? target
      : null;
  } catch {
    return null;
  }
}

function targetAllocationIsValid(portfolio: Portfolio): boolean {
  let classTotal = new Decimal(0);
  for (const assetClass of assetClasses) {
    const classTarget = validTarget(portfolio.classTargets[assetClass]);
    if (!classTarget) return false;
    classTotal = classTotal.plus(classTarget);

    const positions = portfolio.positions.filter(
      (position) => position.assetClass === assetClass,
    );
    if (positions.length === 0) {
      if (classTarget.isPositive()) return false;
      continue;
    }

    let instrumentTotal = new Decimal(0);
    for (const position of positions) {
      const instrumentTarget = validTarget(position.target);
      if (!instrumentTarget) return false;
      instrumentTotal = instrumentTotal.plus(instrumentTarget);
    }
    if (!instrumentTotal.equals(100)) return false;
  }
  return classTotal.equals(100);
}

function setCurrentPortfolio(id: string): void {
  currentPortfolioId.value = id;
}

function toggleSelectedPortfolio(id: string): void {
  if (selectedPortfolioIds.has(id)) selectedPortfolioIds.delete(id);
  else selectedPortfolioIds.add(id);
}

function addPurchases(availableCash: Decimal, excludedTicker?: string): void {
  const candidates = [
    { ticker: 'TMOS', lotSize: '1', price: '8.14' },
    { ticker: 'SBMX', lotSize: '1', price: '19.27' },
    { ticker: 'SU26238RMFS4', lotSize: '1', price: '592.20' },
  ].filter(({ ticker }) => ticker !== excludedTicker);
  if (!availableCash.isPositive() || candidates.length === 0) return;

  const allocation = availableCash.div(candidates.length);
  const commissionMultiplier = new Decimal(1).plus(
    new Decimal(currentPortfolio.value.buyCommission).div(100),
  );
  for (const candidate of candidates) {
    const fullLotCost = new Decimal(candidate.price)
      .times(candidate.lotSize)
      .times(commissionMultiplier);
    const lots = allocation.div(fullLotCost).floor();
    if (!lots.isPositive()) continue;
    plan.push({
      ...candidate,
      side: 'Купить',
      lots: lots.toFixed(0),
    });
  }
}

function generatePlan(): void {
  plan.splice(0, plan.length);
  const portfolio = currentPortfolio.value;
  let availableCash = new Decimal(portfolio.cash);

  if (planning.mode === 'rebalance') {
    const positionToSell =
      portfolio.positions.find(({ ticker }) => ticker === 'SBMX') ??
      portfolio.positions.find(({ quantity, lotSize }) =>
        new Decimal(quantity).greaterThanOrEqualTo(lotSize),
      );
    if (positionToSell) {
      const maximumLots = new Decimal(positionToSell.quantity)
        .div(positionToSell.lotSize)
        .floor();
      let saleLots = maximumLots.times('0.1').floor();
      if (saleLots.lessThan(1)) saleLots = new Decimal(1);
      const sale: Trade = {
        ticker: positionToSell.ticker,
        side: 'Продать',
        lots: saleLots.toFixed(0),
        lotSize: positionToSell.lotSize,
        price: positionToSell.price,
      };
      plan.push(sale);
      availableCash = availableCash
        .plus(tradeValue(sale))
        .minus(tradeCommission(sale));
    }
    addPurchases(availableCash, positionToSell?.ticker);
    return;
  }

  addPurchases(availableCash);
}

function removeTrade(index: number): void {
  plan.splice(index, 1);
}

function tradeValue(trade: Trade): Decimal {
  return new Decimal(trade.lots).times(trade.lotSize).times(trade.price);
}

function tradeCommission(trade: Trade): Decimal {
  const rate =
    trade.side === 'Купить'
      ? currentPortfolio.value.buyCommission
      : currentPortfolio.value.sellCommission;
  return tradeValue(trade).times(rate).div(100);
}

function updatePrices(): void {
  currentPortfolio.value.positions.forEach((position) => {
    position.fresh = true;
  });
  currentPortfolio.value.updatedAt = 'только что';
}

function addMember(name: string): FamilyMember {
  const baseId =
    name
      .trim()
      .toLocaleLowerCase('ru')
      .replace(/[^a-zа-яё0-9]+/giu, '-')
      .replace(/(^-|-$)/g, '') || 'member';
  let id = baseId;
  let suffix = 2;
  while (members.some((member) => member.id === id)) {
    id = `${baseId}-${suffix}`;
    suffix += 1;
  }
  const member = reactive({ id, name: name.trim() });
  members.push(member);
  return member;
}

function renameMember(id: string, name: string): void {
  const member = members.find((item) => item.id === id);
  const nextName = name.trim();
  if (!member || !nextName) return;
  const previousName = member.name;
  member.name = nextName;
  portfolios.forEach((portfolio) => {
    if (portfolio.owner === previousName) portfolio.owner = nextName;
  });
}

function createPortfolio(): Portfolio {
  let index = portfolios.length + 1;
  let id = `portfolio-${index}`;
  while (portfolios.some((portfolio) => portfolio.id === id)) {
    index += 1;
    id = `portfolio-${index}`;
  }
  const portfolio: Portfolio = reactive({
    id,
    name: 'Новый портфель',
    owner: members[0]?.name ?? 'Владелец',
    broker: 'Т-Инвестиции',
    cash: '0',
    buyCommission: '0.05',
    sellCommission: '0.05',
    updatedAt: 'ещё не обновлялся',
    classTargets: {
      Акции: '100',
      Облигации: '0',
      'Фонды ликвидности': '0',
    },
    positions: [],
  });
  portfolios.push(portfolio);
  selectedPortfolioIds.add(portfolio.id);
  setCurrentPortfolio(portfolio.id);
  return portfolio;
}

function applyBrokerSync(): void {
  const portfolio = currentPortfolio.value;
  const tmos = portfolio.positions.find(
    (position) => position.ticker === 'TMOS',
  );
  const sbmx = portfolio.positions.find(
    (position) => position.ticker === 'SBMX',
  );
  if (tmos) tmos.quantity = '8320';
  if (sbmx) sbmx.quantity = '2070';
  portfolio.cash = '2141.08';
  portfolio.updatedAt = 'только что';
}

export const prototypeStore = {
  assetClasses,
  members,
  portfolios,
  selectedPortfolioIds,
  currentPortfolioId,
  currentPortfolio,
  planning,
  plan,
  positionValue,
  portfolioValue,
  money,
  percent,
  share,
  classValue,
  targetAllocationIsValid,
  setCurrentPortfolio,
  toggleSelectedPortfolio,
  generatePlan,
  removeTrade,
  tradeValue,
  tradeCommission,
  updatePrices,
  applyBrokerSync,
  addMember,
  renameMember,
  createPortfolio,
};
