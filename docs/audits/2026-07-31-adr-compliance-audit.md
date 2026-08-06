# Аудит соответствия ADR от 31 июля 2026 года

## Контекст

Проверено состояние ветки `skeleton` после перезагрузки ОС. На момент аудита
рабочая копия была чистой, `HEAD` — `0d66cae` (`feature: skeleton`).

После явного подключения сохранившихся локальных Java 25 и Node.js 24:

- `check` завершился успешно;
- frontend содержал один test file и два теста;
- backend-тесты отсутствовали (`NO-SOURCE`);
- `build` завершился успешно;
- runtime-проверка вернула `200` для `/api/v1/system/status` и `404` для `/`.

## Обнаруженные проблемы

### 1. SPA не обслуживается backend-приложением

**Приоритет: критичный. ADR-0004.**

Frontend попадает в JAR как `static/`, но HTTP-маршрут для него не настроен.
Runtime-проверка собранной distribution показала:

- `/api/v1/system/status` → `200 {"status":"UP"}`;
- `/` → `404`.

Это противоречит требованию ADR-0004 обслуживать frontend bundle тем же
приложением, что и API.

**Статус: исправлено после аудита.** Backend обслуживает `/`, hashed assets и
SPA client routes; неизвестные API и static resources по-прежнему возвращают
`404`.

### 2. Backend обходит contract-first server API

**Приоритет: критичный. ADR-0003.**

OpenAPI Generator создаёт только обычные Kotlin-модели и явно отключает
генерацию API (`"apis" to "false"`). Маршрут, HTTP-статус и JSON вручную
дублируются в `SystemStatusController`; сгенерированный `SystemStatus` не
используется.

Это оставляет возможность расхождения TypeSpec и фактической реализации сервера.

**Статус: исправлено после аудита.** Kora server API, маршрут, response mapper и
модель генерируются из OpenAPI в `build/`; backend реализует только типизированный
`SystemApiDelegate`. Ручной `SystemStatusController` удалён.

### 3. Проверка OpenAPI drift может скрыть drift

**Приоритет: высокий. ADR-0003 и ADR-0007.**

`generateBackendApi` и `generateFrontendClient` зависят от `generateOpenApi`,
который перезаписывает сохранённый `api/generated/openapi.yaml`. Во время
фактического `check` эта генерация произошла до `verifyOpenApi`.

Устаревший сохранённый контракт может быть исправлен до сравнения, из-за чего
проверка не гарантирует обнаружение drift.

**Статус: исправлено после аудита.** Обычные `check`, `build` и задачи codegen
сначала сравнивают сохранённый OpenAPI с отдельной контрольной генерацией в
`build/` и не запускают `generateOpenApi`. Обновление сохранённого контракта
остаётся отдельным явным действием.

### 4. `check` не реализует обязательные quality gates

**Приоритет: высокий. ADR-0001 и ADR-0007.**

Сейчас `check` подключает OpenAPI-проверку и frontend lint/test/typecheck, но не
обеспечивает:

- ArchUnit-тесты границ модулей;
- Detekt;
- Spotless/ktlint;
- Prettier-проверку TypeSpec, Vue, JSON, YAML и Markdown;
- backend-тесты;
- JaCoCo и V8 coverage.

ArchUnit, Detekt и Spotless объявлены в version catalog, но фактически не
подключены. Задача `spotlessApply` отсутствует.

**Статус: исправлено после аудита.** `check` запускает backend- и
ArchUnit-тесты, Detekt, Spotless/ktlint, штатную проверку форматирования TypeSpec,
Prettier, проверку OpenAPI drift, frontend lint/typecheck/tests, а также
формирует диагностические отчёты JaCoCo и V8 coverage без обязательных порогов
покрытия.

### 5. Используется KAPT вместо KSP

**Приоритет: высокий. Repository-wide engineering guide.**

В сборке подключены `kotlin-kapt` и `kapt(libs.kora.annotation.processors)`.
Инженерные правила разрешают KAPT только как временную диагностику и требуют
восстановить KSP до завершения bootstrap.

**Статус: исправлено после аудита.** KAPT удалён; сборка использует
`symbol-processors` Kora и KSP `2.2.20-2.0.4` во временном режиме KSP1. В
ADR-0002 зафиксированы актуальные Kotlin 2.2.20, Java 25 toolchain, JVM bytecode
target 24, причина временного режима и проверяемые условия перехода на KSP2.

### 6. Отсутствуют CI и Playwright job

**Приоритет: высокий. ADR-0007.**

В Git отсутствуют `.github/workflows`, Playwright-конфигурация и e2e-тесты.
Обязательные Linux build job и Chromium job не реализованы.

### 7. Версии не полностью централизованы

**Приоритет: средний. ADR-0002.**

Версия Foojay resolver жёстко задана в `settings.gradle.kts`, хотя она также
объявлена, но не используется в version catalog.

### 8. Custom CSS не является scoped

**Приоритет: низкий. ADR-0004.**

Практически всё оформление компонентов находится в общем `frontend/src/styles.css`,
тогда как ADR-0004 предписывает scoped CSS и ограничение собственного
оформления.

## Ещё не реализованные области

Это не конфликтующие альтернативные реализации, а незаполненные части каркаса:

- ADR-0005: нет PostgreSQL Compose-сервиса, Flyway migrations, JDBC repositories
  и Embedded PostgreSQL tests;
- ADR-0006: нет Timefold и `TradePlanSolver`;
- ADR-0008: нет Docker/Compose deployment, resource limits и backup tooling;
- ADR-0009: нет integration ports и T-Invest адаптера.

## Локальное окружение после перезагрузки

Канонический запуск `gradlew.bat` первоначально не работал, потому что Java и
Node.js из игнорируемого каталога `.tools` не были доступны через окружение.
После явного подключения локальных Java 25 и Node.js 24 сборка прошла.

Это не самостоятельное нарушение ADR, но окружение разработки пока нельзя
считать восстановленным и документированным для обычного запуска после
перезагрузки.

### Статус исправления

После аудита Windows Wrapper получил fallback на подготовленные локальные Java 25
и Node.js 24. Для Codex-среды `GRADLE_USER_HOME` также направляется в доступный
workspace-каталог. Команда `./gradlew build` после исправления завершилась
успешно без ручной настройки переменных окружения.
