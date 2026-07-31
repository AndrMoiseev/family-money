# family-money
Управление семейными финансами

## Сборка

Требуются Java 25 и Node.js 24. На Windows используйте корневой Gradle Wrapper:

```powershell
./gradlew build
```

Если системные `JAVA_HOME` и `PATH` не настроены, Windows Wrapper автоматически
использует подготовленные локальные toolchains из `.tools/jdk25` и
`.tools/node-v24*-win-x64`, когда они доступны. Каталог `.tools` является
локальным и не хранится в Git.

## Локальный запуск

Все команды выполняются из корня репозитория. Перед первым запуском проверьте
полную сборку:

```powershell
./gradlew build
```

Запустите приложение через Gradle Wrapper:

```powershell
./gradlew run
```

На Windows ту же команду можно вызвать явно как `./gradlew.bat run`.

После запуска доступны:

- приложение — <http://localhost:8080/>;
- проверка API — <http://localhost:8080/api/v1/system/status>;
- readiness probe — <http://localhost:8085/system/readiness>;
- liveness probe — <http://localhost:8085/system/liveness>.

Frontend собирается и обслуживается backend-приложением, поэтому отдельный Vite
server для обычного локального запуска не требуется. Текущему каркасу также пока
не требуется PostgreSQL.

Для остановки приложения нажмите `Ctrl+C` в терминале, где выполняется Gradle.
