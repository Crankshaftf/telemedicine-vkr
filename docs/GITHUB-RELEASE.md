# Скачивание APK через GitHub Releases

Постоянная ссылка для телефона — **не нужен USB** и **не нужен включённый ПК** (только сервер API).

---

## Быстрый старт

### 1. Установите GitHub CLI

```powershell
winget install GitHub.cli
gh auth login
```

### 2. Создайте репозиторий и загрузите проект

```powershell
cd c:\Users\Crankshaft\Documents\telemedicine-vkr
.\scripts\setup-github.ps1
```

Или вручную на [github.com/new](https://github.com/new), затем:

```powershell
git init
git add .
git commit -m "MedConnect VKR"
git remote add origin https://github.com/YOUR_USER/telemedicine-vkr.git
git push -u origin main
```

### 3. Опубликуйте APK

Укажите **постоянный адрес вашего сервера** (VPS, Render, или текущий туннель):

```powershell
.\scripts\publish-github.ps1 -ApiUrl "https://your-server.com/"
```

Скрипт:
- соберёт APK с этим API URL;
- загрузит в **GitHub Releases**;
- сохранит ссылку в `github-release.url`.

### 4. Отправьте ссылку на телефон

Файл `github-release.url` будет содержать что-то вроде:

```
https://github.com/YOUR_USER/telemedicine-vkr/releases/latest/download/MedConnect.apk
```

Эту ссылку можно:
- отправить в мессенджер;
- открыть в браузере телефона;
- указать в пояснительной записке ВКР;
- показать на странице `/download/` вашего сервера.

---

## Обновление приложения

При смене сервера или новой версии:

```powershell
.\scripts\publish-github.ps1 -Tag v1.1 -ApiUrl "https://new-server.com/"
```

Ссылка `releases/latest/download/...` **не меняется** — всегда отдаёт последний релиз.

---

## Сборка в облаке (GitHub Actions)

Альтернатива локальной сборке:

1. GitHub → репозиторий → **Settings → Secrets → Actions**
2. Добавьте секрет `MEDCONNECT_API_URL` = `https://your-server.com/`
3. **Actions → Build and Release APK → Run workflow**
4. Укажите `api_url` и тег `v1.0`

Или создайте git-тег:

```powershell
git tag v1.0
git push origin v1.0
```

---

## Важно про API URL

APK «зашивает» адрес сервера при сборке. Для GitHub Releases лучше использовать **постоянный** адрес:

| Сервер | Подходит для GitHub APK |
|--------|-------------------------|
| VPS / Render | ✅ Да |
| Cloudflare Tunnel | ⚠️ URL меняется — пересобирайте APK |

---

## Страница на сервере

После публикации на GitHub страница `/download/` автоматически показывает кнопку **«Скачать с GitHub»**, если заполнен файл `github-release.url`.

Можно также задать через `.env`:

```
GITHUB_RELEASE_URL=https://github.com/YOUR_USER/telemedicine-vkr/releases/latest/download/MedConnect.apk
```

---

## Тестовый вход

После установки: `user@test.ru` / `user123`
