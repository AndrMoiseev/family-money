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
