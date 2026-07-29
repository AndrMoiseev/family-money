# ADR-0008: Развёртывание и резервное копирование

Статус: Принято  
Дата: 29 июля 2026 года

## Контекст

Pet-проект должен минимизировать дополнительные расходы. Уже доступен Vultr VPS
с `1 vCPU / 1 GB RAM`. Managed PostgreSQL и постоянно работающие дополнительные
облачные ресурсы увеличили бы стоимость, однако единственный VPS требует
независимых резервных копий и строгого контроля памяти.

## Решение

Основной production target — существующий Vultr VPS. Приложение и PostgreSQL
работают отдельными Docker Compose-сервисами; PostgreSQL использует persistent
volume, независимый от container lifecycle. Vultr Managed PostgreSQL по цене
около `$0.02/час` для MVP не используется.

Начальный профиль:

- на host настраивается 1 GB swap для кратковременной защиты, не как замена RAM;
- container приложения ограничивается примерно 600 MiB;
- JVM heap ограничивается примерно 350–400 MiB container-aware настройкой
  `MaxRAMPercentage`;
- включается `-XX:+ExitOnOutOfMemoryError`;
- одновременно выполняется не более одного Timefold solve;
- application connection pool содержит не более пяти соединений;
- PostgreSQL: `shared_buffers=64MB`, `work_mem=2MB`,
  `maintenance_work_mem=32MB`, `max_connections=10`;
- Docker logs ротируются;
- backup и test restore выполняются вне пользовательской активности.

Наблюдаются RAM, swap, OOM/restarts и свободное место. Переход на 2 GB RAM
выполняется при регулярном swap, устойчивом потреблении более 85% RAM,
повторяющихся OOM/restarts или нехватке памяти solver.

Резервное копирование:

- ежедневно создаётся `pg_dump` в custom format;
- последняя успешная копия остаётся локально;
- client-side encrypted копия отправляется в private Backblaze B2 bucket;
- credentials и encryption key не хранятся в репозитории;
- сохраняются 14 ежедневных и 12 ежемесячных копий;
- upload и целостность архива проверяются автоматически;
- не реже раза в месяц выполняется настоящее восстановление в отдельную БД с
  проверкой доступности и Flyway schema version;
- VPS snapshot может быть дополнительной защитой, но не заменяет off-site dump.

Резервный путь миграции — Google Cloud Run с scale-to-zero и IAP, Neon на
поддерживаемой стабильной версии PostgreSQL и независимые object-storage
backups. Это не первоначальный deployment; провайдеры, цены и free-tier
ограничения повторно проверяются перед миграцией.

## Последствия

- Первоначальная дополнительная стоимость hosting близка к нулю.
- Один VPS остаётся single point of runtime failure, но не потери всех копий.
- Лимиты ресурсов являются частью production configuration и требуют метрик.
- Развёртывание в cloud возможно без смены СУБД или container model.

## Рассмотренные альтернативы

- Немедленный переход на VPS с 2 GB отложен до появления метрик.
- Vultr Managed PostgreSQL отклонён для MVP из-за фиксированной стоимости.
- Cloud Run + Neon оставлены резервом: они снижают server administration, но
  добавляют cold starts, двух провайдеров и deployment complexity.
- Только локальные dumps и только VPS snapshots отклонены из-за общей failure
  domain с production.

