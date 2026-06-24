# Доступ из интернета (не только локальная сеть)

Три варианта — от быстрого демо до постоянного сервера.

---

## Вариант 1 — Cloudflare Tunnel (быстро, бесплатно)

**ПК должен быть включён.** Телефон может быть на **любой сети** (мобильный интернет).

```powershell
cd c:\Users\Crankshaft\Documents\telemedicine-vkr
.\scripts\start-public.ps1
```

Скрипт:
1. Запускает backend
2. Создаёт публичный HTTPS-адрес вида `https://xxxx.trycloudflare.com`
3. Сохраняет URL в `.public-url`

Собрать APK с этим адресом:

```powershell
.\scripts\build-apk.ps1 -UsePublicUrl
```

Установите `MedConnect.apk` на телефон — работает **из любой точки мира**, пока запущен `start-public.ps1`.

**Скачать без USB:** откройте на телефоне `https://xxxx.trycloudflare.com/download/`  
**Или GitHub (постоянная ссылка):** см. [GITHUB-RELEASE.md](GITHUB-RELEASE.md)

| Плюсы | Минусы |
|-------|--------|
| 5 минут, HTTPS | ПК должен быть online |
| Не нужен белый IP | URL меняется при каждом запуске |

---

## Вариант 2 — VPS (постоянный сервер)

Арендуйте VPS (Timeweb, Selectel, Hetzner, DigitalOcean ~300–500 ₽/мес).

### На сервере (Ubuntu):

```bash
git clone <your-repo> telemedicine-vkr
cd telemedicine-vkr
chmod +x deploy/vps-install.sh
sudo ./deploy/vps-install.sh
```

Откройте **порт 8000** в firewall облака.

### На ПК — собрать APK:

```powershell
.\scripts\build-apk.ps1 -ApiUrl "http://ВАШ_IP_VPS:8000/"
```

Приложение работает **24/7** без вашего ПК.

---

## Вариант 3 — Render.com (облако PaaS)

1. Зарегистрируйтесь на [render.com](https://render.com)
2. New → Blueprint → подключите репозиторий с `render.yaml`
3. Получите URL вида `https://medconnect-api.onrender.com`

```powershell
.\scripts\build-apk.ps1 -ApiUrl "https://medconnect-api.onrender.com/"
```

> На free tier сервер «засыпает» — первый запрос может быть медленным.

---

## Сравнение

| Способ | Доступ из интернета | ПК нужен | HTTPS |
|--------|---------------------|----------|-------|
| Локальная сеть | Только Wi-Fi дома | Да | Нет |
| Cloudflare Tunnel | Да | Да | Да |
| VPS | Да | Нет | Настраивается |
| Render | Да | Нет | Да |

---

## Проверка

После деплоя откройте на **телефоне через мобильный интернет** (не Wi-Fi):

```
https://ваш-адрес/health
```

Должно вернуть: `{"status":"ok",...}`

Затем установите APK и войдите: `user@test.ru` / `user123`

---

## Локальная сеть (как было)

```powershell
.\scripts\start-network.ps1
.\scripts\build-apk.ps1
```

Только когда телефон и ПК в одной Wi-Fi.
