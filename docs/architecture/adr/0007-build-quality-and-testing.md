# ADR-0007: Сборка, CI и тестирование

Статус: Принято  
Дата: 29 июля 2026 года

## Контекст

Репозиторий объединяет JVM backend, TypeSpec и Vue frontend. Люди и агенты
должны пользоваться одной воспроизводимой точкой входа, а архитектурные и
финансовые инварианты должны проверяться автоматически.

## Решение

Бэкенд является одним корневым Gradle-модулем со стандартными `src/main` и
`src/test`. Node.js 24 LTS и npm используются для TypeSpec и frontend. Корневой
`package.json` объявляет `frontend` как npm workspace; один корневой
`package-lock.json` хранится в Git. Прямые зависимости фиксируются точно через
`save-exact=true`. Разработчик использует `npm install`, CI — `npm ci`.

```text
/
├── api/
│   ├── main.tsp
│   ├── tspconfig.yaml
│   └── generated/openapi.yaml
├── frontend/
│   ├── src/
│   └── package.json
├── src/
│   ├── main/kotlin/io/familymoney/
│   ├── main/resources/
│   └── test/kotlin/io/familymoney/
├── gradle/
├── build.gradle.kts
├── package.json
├── package-lock.json
├── settings.gradle.kts
└── gradlew
```

Gradle является верхнеуровневой точкой входа:

- `generateOpenApi` компилирует TypeSpec;
- `generateBackendApi` и `generateFrontendClient` зависят от `generateOpenApi`;
- `check` запускает backend tests, ArchUnit, contract verification, frontend
  typecheck и frontend tests;
- `build` зависит от `check` и собирает backend artifact и production frontend.

CI выполняется GitHub Actions на Linux runner с одной комбинацией Java 25 и
Node.js 24. `./gradlew build` — обязательная build job. Playwright запускается
отдельным job после сборки и старта приложения. Обе jobs обязательны для pull
request; собственный runner на production VPS не используется.

Качество кода:

- Spotless предоставляет Gradle-команды форматирования Kotlin и Gradle Kotlin
  DSL, используя ktlint;
- Detekt выполняет смысловой анализ; его formatting rules отключены;
- ESLint с TypeScript/Vue plugins анализирует frontend;
- Prettier форматирует Vue, TypeScript, TypeSpec, JSON, YAML и Markdown;
- ArchUnit проверяет правила ADR-0001.

Backend tests используют JUnit 5, `ru.tinkoff.kora:test-junit5`, Kotest
Assertions и Property, MockK только на внешних портах и в сценариях отказа,
Embedded PostgreSQL с Flyway, ArchUnit и JaCoCo. Frontend tests используют
Vitest, Vue Testing Library, Playwright, V8 coverage и обязательный
`vue-tsc --noEmit`. Для MVP достаточно Chromium.

Уровни тестов:

1. unit tests чистого домена;
2. component tests use cases с fake или MockK ports;
3. repository/transaction integration tests с PostgreSQL;
4. небольшое число black-box HTTP tests;
5. ключевые Playwright user journeys.

Контрольные примеры расчёта хранятся как versioned JSON fixtures с decimal
strings. Fixture содержит исходный портфель со свободными деньгами, режим, цены, лоты,
комиссии, ожидаемые операции, остаток, распределение и объяснение. Контрольные
случаи из исходной Google-таблицы переносятся в этот формат. Property-based tests
проверяют целые лоты, неотрицательный остаток, ограничение продаж, режим без
продаж, баланс с комиссиями, детерминированность, score/tie-break rules и
независимость от порядка входных позиций.

## Последствия

- `./gradlew build` должен работать одинаково локально и в CI.
- Изменения generated OpenAPI, formatting и package boundaries проверяются до
  merge.
- JaCoCo/V8 coverage служат диагностикой; процент покрытия сам по себе не
  является quality gate.
- Большинство доменных тестов остаются быстрыми и не требуют инфраструктуры.

## Рассмотренные альтернативы

- Раздельные несвязанные entry points Gradle/npm отклонены для агентской работы.
- Прямой ktlint без Spotless не выбран, чтобы сохранить единый Gradle-интерфейс.
- Biome вместо ESLint/Prettier не выбран: консервативный Vue toolchain
  предсказуемее для MVP.
- Матрица JDK/Node версий и self-hosted runner отклонены как лишняя сложность.
