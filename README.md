# adminPMR — Android APK

Android-приложение для работы с сервером PMR (185.221.154.39).

Версия: **V0.1-apk**

## Сборка через GitHub Actions

1. Создать репозиторий на GitHub.
2. Загрузить содержимое папки `D:\PMRapk` в репозиторий.
3. Перейти во вкладку **Actions** → workflow **Build adminPMR APK**.
4. Дождаться окончания сборки.
5. Скачать `adminPMR-debug.apk` из **Artifacts**.

## Установка на Android

1. Скачать `adminPMR-debug.apk` на телефон.
2. Разрешить установку из неизвестных источников.
3. Установить APK.
4. При первом запуске разрешить:
   - Интернет
   - Уведомления
   - Работу в фоне (Foreground Service)

## Функционал

- Список абонентов в канале PMR
- Активность абонентов (кто говорит)
- Локальный список расшифровок (list.txt)
- Терминал команд: `rename`, `delete`, `list`, `l`, `b<ID>`, `260`, `261`, `exit`
- Управление сервисом
- Подсказка по командам

## Структура

- `app/src/main/java/com/pmr/admin/` — Java-классы
- `app/src/main/res/layout/` — разметки вкладок
- `app/src/main/res/values/` — строки, цвета, темы
- `app/src/main/res/raw/list.txt` — встроенный список по умолчанию
- `.github/workflows/build.yml` — сборка APK через GitHub Actions

## Примечания

- Порт UDP приёма: **5322** (не 5321 — 5321 занят системой Android).
- Сервер PMR: **185.221.154.39**.
- Все настройки PMR (`MyMailIndex`, `MyPChannel`, `Priznak_pmr`) — в `PmrSocket.java`.