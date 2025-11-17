# GitHub Actions для FluffyGram

## Настройка автоматической сборки и релизов

Этот workflow автоматически собирает APK файлы FluffyGram и публикует их в GitHub Releases.

### Необходимые GitHub Secrets

Для работы GitHub Action необходимо добавить следующие секреты в настройках репозитория:

1. **KEYSTORE_BASE64** - ваш keystore файл, закодированный в base64
2. **KEYSTORE_PASSWORD** - пароль от keystore
3. **KEY_ALIAS** - алиас ключа
4. **KEY_PASSWORD** - пароль ключа

### Как создать и добавить secrets

#### 1. Конвертировать keystore в base64

В Linux/Mac:
```bash
base64 -i fluffyGram_dev/ushastoe-release.keystore | tr -d '\n' > keystore.txt
```

В Windows (PowerShell):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("fluffyGram_dev\ushastoe-release.keystore")) | Out-File -FilePath keystore.txt -NoNewline
```

#### 2. Добавить secrets в GitHub

1. Перейдите в настройки репозитория: `Settings` → `Secrets and variables` → `Actions`
2. Нажмите `New repository secret`
3. Добавьте каждый секрет:
   - **Name:** `KEYSTORE_BASE64`, **Value:** содержимое файла `keystore.txt`
   - **Name:** `KEYSTORE_PASSWORD`, **Value:** ваш пароль от keystore
   - **Name:** `KEY_ALIAS`, **Value:** ваш key alias
   - **Name:** `KEY_PASSWORD`, **Value:** ваш пароль от ключа

### Как запустить сборку

#### Автоматически
Workflow автоматически запускается при создании тега версии:
```bash
git tag v12.1.1
git push origin v12.1.1
```

#### Вручную
1. Перейдите в раздел `Actions` в вашем репозитории
2. Выберите workflow "Build and Release FluffyGram"
3. Нажмите `Run workflow`
4. Выберите ветку
5. Введите имя тега для релиза (например, `v12.1.1`)
6. При необходимости отключите создание релиза, сняв галочку "Create a GitHub Release"
7. Нажмите зеленую кнопку `Run workflow`

**Примечание:** При ручном запуске вам нужно указать имя тега, которое будет использоваться для создания релиза. Тег будет создан автоматически при публикации релиза.

### Что делает workflow

1. ✅ Проверяет код из репозитория
2. ✅ Устанавливает JDK 21 и Android SDK
3. ✅ Устанавливает NDK 21.4.7075529
4. ✅ Создает keystore файл из secrets
5. ✅ Собирает Release APK файлы для всех вариантов
6. ✅ Переименовывает APK в удобный формат (например, `FluffyGram-v12.1.1-arm64-v8a.apk`)
7. ✅ Создает GitHub Release с APK файлами
8. ✅ Сохраняет APK как артефакты (доступны 30 дней)

### Результаты сборки

После успешной сборки вы найдете:
- **Releases**: APK файлы доступны в разделе Releases вашего репозитория
- **Artifacts**: APK файлы доступны в разделе Actions → конкретный workflow run → Artifacts

### Собираемые варианты APK

- `FluffyGram-v{VERSION}-arm64-v8a.apk` - для Android 5.0+ (API 21+)
- `FluffyGram-v{VERSION}-arm64-v8a-sdk23.apk` - для Android 6.0+ (API 23+)

### Troubleshooting

**Проблема:** Сборка падает с ошибкой "Keystore was tampered with"
- **Решение:** Проверьте, что KEYSTORE_BASE64 правильно закодирован без переносов строк

**Проблема:** Gradle не находит NDK
- **Решение:** Проверьте версию NDK в `TMessagesProj_App/build.gradle` (должна быть 21.4.7075529)

**Проблема:** Недостаточно памяти при сборке
- **Решение:** Workflow уже настроен на использование кэша Gradle

### Дополнительная информация

- Workflow использует `ubuntu-latest` runner
- Кэширование Gradle ускоряет повторные сборки
- APK файлы автоматически подписываются вашим keystore
