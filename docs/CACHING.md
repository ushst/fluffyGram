# Кеширование в Build Pipeline / Caching in Build Pipeline

## Обзор / Overview

В проекте FluffyGram реализована комплексная система кеширования для ускорения сборки в GitHub Actions.  
FluffyGram project implements a comprehensive caching system to speed up builds in GitHub Actions.

## Типы кеша / Cache Types

### 1. Gradle Cache (Кеш Gradle)

**Расположение:** `~/.gradle/caches`  
**Управление:** `actions/setup-java@v4` с параметром `cache: 'gradle'`  
**Назначение:** Кеширование зависимостей Gradle и метаданных

**Что кешируется:**
- Скачанные зависимости (dependencies)
- Плагины Gradle
- Метаданные сборки
- Build cache

**Проверка статуса:**
```bash
ls -la ~/.gradle/caches
du -sh ~/.gradle/caches
```

### 2. NDK Cache (Кеш Android NDK)

**Расположение:** `/usr/local/lib/android/sdk/ndk/${{ env.NDK_VERSION }}`  
**Ключ:** `ndk-${{ env.NDK_VERSION }}-${{ runner.os }}`  
**Назначение:** Кеширование установленного Android Native Development Kit

**Что кешируется:**
- Компиляторы для нативного кода (C/C++)
- Инструменты сборки NDK
- Заголовочные файлы и библиотеки

**Преимущество:** Позволяет избежать повторной установки NDK (~1-2 GB), экономя 2-5 минут на каждой сборке.

### 3. ccache (Compiler Cache)

**Управление:** `hendrikmuhs/ccache-action@v1.2`  
**Ключ:** `${{ runner.os }}-ccache`  
**Назначение:** Кеширование результатов компиляции C/C++ кода

**Что кешируется:**
- Результаты компиляции `.cpp`, `.c` файлов
- Объектные файлы (`.o`)

**Метрики:**
- Cache hit rate - процент попаданий в кеш
- При первой сборке: низкий hit rate (кеш создается)
- При последующих сборках: высокий hit rate (используется кеш)

**Проверка статистики:**
```bash
ccache -s
```

### 4. CMake Build Cache (Кеш сборки CMake)

**Расположение:**
- `TMessagesProj/.cxx`
- `TMessagesProj_App/.cxx`

**Ключ:** `${{ runner.os }}-cmake-${{ hashFiles(...) }}`  
**Назначение:** Кеширование промежуточных файлов сборки CMake

**Что кешируется:**
- Сгенерированные файлы сборки CMake
- Промежуточные объектные файлы
- Метаданные конфигурации

**Инвалидация кеша:** Происходит автоматически при изменении:
- `*.cpp` файлов в `TMessagesProj/jni/`
- `*.c` файлов в `TMessagesProj/jni/`
- `*.h` файлов в `TMessagesProj/jni/`
- `CMakeLists.txt` файлов

## Мониторинг работы кеша / Cache Monitoring

### В логах GitHub Actions можно найти:

1. **Cache Verification Report** - показывает статус восстановления кешей:
   - `NDK Cache Hit: true/false`
   - `CMake Cache Hit: true/false`

2. **Gradle Cache Information** - информация о кеше Gradle:
   - Существует ли директория кеша
   - Размер кеша

3. **ccache Statistics** - статистика компилятора кеша:
   - Количество попаданий в кеш
   - Процент эффективности

## Ожидаемые результаты / Expected Results

### Первая сборка (First Build):
- NDK Cache Hit: `false`
- CMake Cache Hit: `false`
- ccache hit rate: 0-10%
- Время сборки: ~30-45 минут

### Последующие сборки без изменений (Subsequent Builds - No Changes):
- NDK Cache Hit: `true`
- CMake Cache Hit: `true`
- ccache hit rate: 90-100%
- Время сборки: ~10-15 минут

### Сборки с изменениями кода (Builds with Code Changes):
- NDK Cache Hit: `true`
- CMake Cache Hit: `false` или частичное попадание
- ccache hit rate: 60-90% (зависит от объема изменений)
- Время сборки: ~15-25 минут

## Отладка проблем с кешем / Cache Troubleshooting

### Если кеш не работает:

1. **Проверьте ключи кеша:**
   - Убедитесь, что ключи генерируются корректно
   - Проверьте, что `hashFiles()` возвращает правильные хеши

2. **Проверьте размер кеша:**
   - GitHub Actions ограничивает размер кеша до 10 GB
   - Старые кеши автоматически удаляются

3. **Проверьте логи:**
   - Ищите сообщения об ошибках восстановления кеша
   - Проверьте, что пути к директориям кеша корректны

4. **Принудительная очистка:**
   - Можно удалить кеши вручную в настройках репозитория
   - Settings → Actions → Caches

## Оптимизация / Optimization

### Рекомендации:
1. Регулярно проверяйте статистику ccache
2. Следите за размером кеша Gradle
3. Обновляйте версии actions для улучшения кеширования
4. Используйте `restore-keys` для гибкости восстановления кеша

## Проверка работы кеша / Testing Cache Functionality

Для проверки работы кеша:

1. Запустите workflow вручную через GitHub UI (workflow_dispatch)
2. Проверьте секцию "Cache Verification Report" в логах
3. Посмотрите на время выполнения сборки
4. Запустите workflow повторно без изменений
5. Сравните время выполнения и статус кешей

## Связанные PR / Related PRs

- #22 - Добавлено кеширование для сборок
