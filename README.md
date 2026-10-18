# Pancreatitis

Мобильное Android-приложение для поддержки врачебной диагностики панкреатита: сбор характеристик пациента по анкетам, локальное и облачное хранение данных, шифрование и предсказание диагноза на основе модели сходства (similarity-based model).

## Основные возможности

- **Анкеты пациентов** — структурированный ввод клинических характеристик (числовые и нечисловые значения) с весами, диапазонами и историей заполнений.
- **Предсказание диагноза** — модель на основе сходства (`SimilarityBasedModel`), обучаемая на тренировочных данных из текстового файла; выдаёт вероятность одного из целевых диагнозов.
- **Регистрация и авторизация врачей** — аккаунты защищены криптографически: ключ шифрования данных выводится из пароля, подпись обновлений проверяется по публичному ключу Ed25519.
- **Локальное хранилище** — база данных SQLite (`mobile.db`), поставляемая в assets приложения.
- **Облачное хранилище** — синхронизация анкет и данных пользователя через Yandex Cloud API с проверкой подписи (Ed25519, BouncyCastle).
- **Безопасность** — модуль `SafetyModule`: симметричное шифрование строк и ключей, дешифрование ключа по паролю.
- **Справка** — встроенная HTML-помощь (`assets/help.html`).

## Технологии

| Компонент | Используется |
|---|---|
| Платформа | Android (Java 11), `minSdk 26`, `targetSdk/compileSdk 36` |
| Сборка | Gradle (Kotlin DSL), version catalogs (`libs.versions.toml`) |
| UI | AppCompat, Material Components, ConstraintLayout, Navigation (fragments + ViewBinding) |
| Состояние | Lifecycle ViewModel / LiveData |
| Криптография | BouncyCastle (`bcprov-jdk18on`): AES, Ed25519; BIP-39 (мнемоники) |
| Данные | Jackson (`jackson-databind`) для JSON, SQLite для локальной БД |
| Облако | REST-запросы к `cloud-api.yandex.net` (HttpURLConnection) |

## Структура проекта

```
app/src/main/java/com/pancreatitis/
├── MainActivity.java              # Точка входа, навигация
├── modules/
│   ├── prediction/                # Модель предсказания диагноза и парсер тренировочных данных
│   ├── registration/              # Регистрация врача
│   ├── authorization/             # Авторизация
│   ├── database/                  # Работа с локальной SQLite-базой
│   ├── bip39/                     # Генерация/декодирование мнемоник BIP-39
│   ├── cloudstorage/              # Синхронизация с Yandex Cloud, проверка Ed25519-подписей
│   ├── localstorage/              # Локальное хранилище
│   ├── safety/                    # Шифрование/дешифрование данных и ключей
│   └── questionnairemanager/      # Управление анкетами
├── adapters/                      # Адаптеры списков (анкеты, диагнозы, характеристики)
├── ui/                            # Fragments: home, auth, register, activate, profile,
│                                  # questionnaires, characteristic, helps и др.
└── models/                        # Модели предметной области (Patient, Doctor, Questionnaire,
                                   # Characteristic, DiagnosisItem, Update и т.д.)

app/src/main/assets/
├── mobile.db                      # Локальная база данных SQLite
├── algorithm_*.txt                # Тренировочные данные модели
├── wordlist_en.txt                # Список слов BIP-39
├── ed25519_public.key             # Публичный ключ проверки подписи обновлений
└── help.html                      # Встроенная справка
```

## Сборка и запуск

### Требования

- JDK 11+
- Android Gradle Plugin 8.13.0
- Android SDK (compileSdk 36)
- Android Studio (рекомендуется) или чистый Gradle

### Шаги

```bash
# Клонировать репозиторий
git clone <url-репозитория> Pancreatitis
cd Pancreatitis

# Сборка debug APK
./gradlew assembleDebug

# APK будет в app/build/outputs/apk/debug/
# Готовая debug-сборка также лежит в app/debug/

# Установка на подключённое устройство
./gradlew installDebug
```

Текущая версия: **1.1.10** (versionCode 12).

## Конфигурация

- `app/build.gradle.kts` — ID приложения, версии, зависимости; поле `API_KEY` (BuildConfig) для доступа к облачному хранилищу.
- `gradle/libs.versions.toml` — версии плагинов и библиотек AndroidX.
- `gradle.properties` — параметры JVM и Gradle.

> ⚠️ Не публикуйте реальный `API_KEY` и приватные ключи в открытом доступе. В проекте используется только публичный Ed25519-ключ (`assets/ed25519_public.key`).

## Тестирование

```bash
# Unit-тесты
./gradlew test

# Инструментальные тесты (нужно устройство/эмулятор)
./gradlew connectedAndroidTest
```
