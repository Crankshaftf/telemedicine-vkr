# МедКоннект — телемедицинская платформа (ВКР)

Демонстрационный прототип Android-приложения телемедицины для медицинской организации.

## Структура проекта

```
telemedicine-vkr/
├── backend/          # FastAPI + PostgreSQL
├── android/          # (этап 3) Kotlin + Jetpack Compose
├── admin-web/        # (этап 4) панель врача/администратора
└── docker-compose.yml
```

## Этап 1 — Backend (текущий)

### Требования

- Docker Desktop
- Python 3.11+

### Быстрый старт

```powershell
# 1. Запустить PostgreSQL
cd c:\Users\Crankshaft\Documents\telemedicine-vkr
docker compose up -d

# 2. Установить зависимости
cd backend
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env

# 3. Заполнить тестовыми данными
python scripts\seed.py

# 4. Запустить API-сервер
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Документация API

После запуска сервера:

- Swagger UI: http://localhost:8000/docs
- Health check: http://localhost:8000/health

### Тестовые пользователи

| Роль | Логин | Пароль | Назначение |
|------|-------|--------|------------|
| **Обычный пользователь** | user@test.ru | user123 | Android, полный сценарий без истории |
| Демо-пациент | patient@test.ru | patient123 | Готовая консультация для скриншотов |
| Врач | doctor@test.ru | doctor123 | Веб-панель |
| Админ | admin@test.ru | admin123 | Веб-панель |

Подробнее: [docs/ACCOUNTS.md](docs/ACCOUNTS.md)

### Основные эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| POST | /auth/register | Регистрация пациента |
| POST | /auth/login | Вход |
| GET | /profile | Профиль |
| GET | /specializations | Специализации |
| GET | /doctors | Список врачей |
| GET | /schedule/{doctorId} | Свободные слоты |
| POST | /appointments | Создать запись |
| GET | /appointments/history | История пациента |
| POST | /files | Загрузить файл |
| GET | /consultations/{appointmentId} | Консультация + чат |
| POST | /consultations/{id}/result | Заключение врача |
| GET | /notifications | Уведомления |

Авторизация: заголовок `Authorization: Bearer <token>`.

## Этап 2 — Android-приложение (текущий)

### Стек

- Kotlin, Jetpack Compose, MVVM
- Retrofit + Kotlin Serialization
- DataStore (токен авторизации)
- Navigation Compose

### Экраны (15)

1. Стартовый (Splash)
2. Регистрация
3. Авторизация
4. Главное меню
5. Профиль
6. Выбор специализации
7. Список врачей
8. Выбор даты/времени
9. Создание заявки + загрузка файла
10. Подтверждение записи
11. Уведомления
12. Консультация (чат)
13. История обращений
14. Детали обращения
15. Заключение врача (в деталях обращения)

### Запуск Android

1. Откройте папку `android/` в **Android Studio** (Ladybug или новее)
2. Дождитесь синхронизации Gradle
3. Запустите backend (см. выше) и PostgreSQL
4. Запустите эмулятор (API 29+) или подключите телефон
5. Run → `app`

**URL API:** `http://10.0.2.2:8000/` — для эмулятора (localhost ПК).
На реальном устройстве замените в `app/build.gradle.kts` на IP вашего компьютера в локальной сети.

### Тестовый вход в приложении

**Обычный пользователь (рекомендуется):**
- Email: `user@test.ru`
- Пароль: `user123`

**Демо с готовой консультацией:** `patient@test.ru` / `patient123`

## Этап 3 — Веб-панель врача/администратора ✅

Панель доступна по адресу: **http://localhost:8000/panel/** (при запущенном backend).

### Возможности

**Врач** (`doctor@test.ru` / `doctor123`):
- Просмотр записанных пациентов
- Чат консультации
- Заполнение и сохранение заключения

**Администратор** (`admin@test.ru` / `admin123`):
- Просмотр всех заявок, изменение статуса
- Создание слотов расписания
- Блокировка/разблокировка слотов
- Просмотр статистики

### Новые API-методы

| Метод | Путь | Описание |
|-------|------|----------|
| GET | /admin/appointments | Все заявки |
| PUT | /admin/appointments/{id}/status | Изменить статус |
| GET | /admin/schedule/{doctorId} | Все слоты врача |
| POST | /admin/slots | Создать слот |
| PUT | /admin/slots/{id} | Изменить статус слота |

## Этап 4 — Тестирование и материалы для ВКР ✅

| Документ | Содержание |
|----------|------------|
| [docs/ACCOUNTS.md](docs/ACCOUNTS.md) | Все учётные записи, включая обычного пользователя |
| [docs/TESTING.md](docs/TESTING.md) | Чеклист контрольного сценария |
| [docs/SCREENSHOTS.md](docs/SCREENSHOTS.md) | 15 скриншотов для диплома |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Описание архитектуры для текста работы |
| [docs/screenshots/](docs/screenshots/) | Папка для сохранения скриншотов |

### Аккаунт обычного пользователя

```
Email:    user@test.ru
Пароль:   user123
ФИО:      Сидорова Мария Александровна
```

Чистый аккаунт без истории — для демонстрации записи на консультацию на защите.

Обновить/создать аккаунты в БД:

```powershell
cd backend
python scripts\seed.py
```

Скрипт можно запускать повторно — добавит `user@test.ru`, не удаляя существующие данные.

---

## Скачивание APK на телефон (GitHub)

Постоянная ссылка — без USB и без вашего ПК.

См. **[docs/GITHUB-RELEASE.md](docs/GITHUB-RELEASE.md)**

```powershell
# Один раз: репозиторий на GitHub
.\scripts\setup-github.ps1

# Опубликовать APK (укажите URL вашего сервера)
.\scripts\publish-github.ps1 -ApiUrl "https://your-server.com/"
```

Ссылка для телефона сохранится в `github-release.url`:
`https://github.com/USER/telemedicine-vkr/releases/latest/download/MedConnect.apk`

## Доступ из интернета (не только Wi-Fi)

См. **[docs/DEPLOY-PUBLIC.md](docs/DEPLOY-PUBLIC.md)**

```powershell
# Публичный HTTPS-туннель (телефон на мобильном интернете)
.\scripts\start-public.ps1
.\scripts\build-apk.ps1 -UsePublicUrl

# Или свой сервер / VPS
.\scripts\build-apk.ps1 -ApiUrl "https://your-server.com/"
```

## Развёртывание в локальной сети

См. **[docs/DEPLOY.md](docs/DEPLOY.md)**

```powershell
# Сервер в Wi-Fi
.\scripts\start-network.ps1

# Сборка APK
.\scripts\build-apk.ps1

# Скачать на телефон (без USB): http://IP_ПК:8000/download/
# Или USB: .\scripts\install-apk.ps1
```

Страница загрузки: **`/download/`** — откройте в браузере телефона, нажмите «Скачать APK».

APK также лежит в корне: `MedConnect.apk`.

---

## Итог проекта

| Этап | Статус |
|------|--------|
| 1. Backend + PostgreSQL | ✅ |
| 2. Android-приложение | ✅ |
| 3. Веб-панель врача/админа | ✅ |
| 4. Тестирование + материалы ВКР | ✅ |
