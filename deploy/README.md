# Деплой Astra-microfront на сервер (Debian 12)

## Быстрый старт

1. На сервере задайте переменные и запустите первичную установку:

```bash
export GIT_REPO="https://github.com/YOUR_USER/Astra-microfront.git"
sudo bash -c 'curl -sL https://raw.githubusercontent.com/YOUR_USER/Astra-microfront/main/deploy/setup.sh | bash'
```

Или склонируйте репозиторий вручную и запустите из папки проекта:

```bash
git clone https://github.com/YOUR_USER/Astra-microfront.git /opt/astramicro
cd /opt/astramicro
sudo GIT_REPO="" APP_DIR=/opt/astramicro bash deploy/setup.sh
```

Перед запуском отредактируйте `deploy/setup.sh`: укажите свой `GIT_REPO` (URL репозитория).

## Обновление из Git

**Вручную** — после правок в репозитории на сервере:

```bash
sudo /opt/astramicro/deploy/update.sh
```

Скрипт делает: `git pull`, сборку бэкенда (Maven), сборку фронта (npm run build), перезапуск `astrakhan-admin.service`. Статика уже в `dist/`, Nginx перезагружать не нужно.

**Автоматически** — при первом запуске `setup.sh` в cron добавляется задача: каждые 5 минут проверяется наличие новых коммитов на `origin/main`; если есть — выполняется `update.sh`. Лог: `/var/log/astramicro-auto-update.log`. Отключить при установке: `NO_CRON=1 bash deploy/setup.sh`.

Включить автообновление вручную (если ставили до этого изменения):

```bash
echo '*/5 * * * * root /opt/astramicro/deploy/auto-update.sh >> /var/log/astramicro-auto-update.log 2>&1' | sudo tee -a /etc/crontab
```

## Автозапуск

Сервис **astrakhan-admin** (Spring Boot) регистрируется в systemd и включается в автозагрузку:

- Статус: `sudo systemctl status astrakhan-admin`
- Логи: `sudo journalctl -u astrakhan-admin -f`
- Перезапуск: `sudo systemctl restart astrakhan-admin`

## 502 Bad Gateway на /admin/ или /admin/users

По конфигу Nginx путь **/admin/** проксируется на **порт 5000** — там должна работать админка **module3 (Flask)**:
пользователи, роли, метрики, отчёты. Если module3 не запущен, при открытии `http://ваш-сервер/admin/` или `/admin/users` будет **502 Bad Gateway**.

**Что сделать на сервере:**

1. Установить зависимости и запустить Flask (один раз):
   ```bash
   cd /opt/astramicro/module3
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   # Настроить .env или переменные (БД и т.д. — см. module3 и deploy/README-DEPLOY.md)
   gunicorn -w 1 -b 127.0.0.1:5000 app:app
   ```
2. Либо завести systemd-сервис для автозапуска (пример в `deploy/README-DEPLOY.md`, раздел «Запуск админки (module3, Flask)»).

После запуска приложения на порту 5000 страницы `/admin/`, `/admin/users` и т.д. начнут открываться. Если админка на Flask не нужна, пункт меню «Пользователи» в Java-админке можно скрыть или изменить (редирект ведёт на /admin/users).

## Файлы в deploy/

| Файл | Назначение |
|------|------------|
| `setup.sh` | Первичная установка: пакеты, клон, сборка, systemd, nginx |
| `update.sh` | Обновление: git pull, сборка, перезапуск сервиса |
| `astrakhan-admin.service` | Пример unit для systemd |
| `nginx-astramicro.conf` | Пример конфига Nginx |
| `auto-update.sh` | Проверка Git и запуск `update.sh` при новых коммитах (для cron) |

## Требования на сервере

- Debian 12
- Доступ в интернет (git, Maven, npm)
- Порт 80 для Nginx (и при необходимости 443 для HTTPS)

## Почта (письма о заказах)

Чтобы клиентам уходили письма (подтверждение заказа, трек-номер), настройте SMTP — переменными окружения на сервере или в `application-local.properties`:

- `SPRING_MAIL_HOST` — хост SMTP (например `smtp.yandex.ru`, `smtp.mail.ru`)
- `SPRING_MAIL_PORT` — порт (587 или 465)
- `SPRING_MAIL_USERNAME` и `SPRING_MAIL_PASSWORD` — логин и пароль (для Yandex/Mail.ru часто нужен «пароль приложения»)
- `APP_MAIL_FROM` — адрес отправителя (должен быть разрешён у провайдера)
- `APP_MAIL_ENABLED=true` — включить отправку (по умолчанию true; при `false` письма не отправляются, только пишется в лог)

Пример для systemd (в `/etc/systemd/system/astrakhan-admin.service` в секции `[Service]`):

```ini
Environment="SPRING_MAIL_HOST=smtp.yandex.ru"
Environment="SPRING_MAIL_PORT=465"
Environment="SPRING_MAIL_USERNAME=your@yandex.ru"
Environment="SPRING_MAIL_PASSWORD=your-app-password"
Environment="APP_MAIL_FROM=your@yandex.ru"
```

Подробнее см. `astrakhan-admin/src/main/resources/application-mail.example.properties`.

## ИИ (генерация текста)

Функция «ИИ для генерации текста» использует OpenRouter; на сервере должен быть задан API-ключ. Без него запросы к `/api/ai/` не проксируются на OpenRouter и генерация не работает.

**Если сервер уже установлен и ключа не было** — выполните на сервере (подставьте свой ключ и при необходимости путь к проекту):

```bash
# 1) Создать module2/.env (для единообразия и будущих сборок)
echo 'VITE_AI_API_KEY=sk-or-v1-ВАШ_КЛЮЧ_OPENROUTER' | sudo tee /opt/astramicro/module2/.env

# 2) Файл с ключом для Nginx (прокси /api/ai/)
echo 'set $vite_ai_api_key "sk-or-v1-ВАШ_КЛЮЧ_OPENROUTER";' | sudo tee /opt/astramicro/deploy/nginx-ai-key.conf

# 3) Добавить в Nginx проксирование /api/ai/ и перезагрузить Nginx
# (если при установке VITE_AI_API_KEY не задавали — конфиг без location /api/ai/)
# Вставьте в server { } в /etc/nginx/sites-available/astramicro после "root ...;":
#     include /opt/astramicro/deploy/nginx-ai-key.conf;
#     location /api/ai/ {
#         rewrite ^/api/ai/chat/(.*)$ /api/v1/chat/$1 break;
#         proxy_pass https://openrouter.ai;
#         proxy_http_version 1.1;
#         proxy_set_header Host openrouter.ai;
#         proxy_set_header Authorization "Bearer $vite_ai_api_key";
#         proxy_set_header X-Real-IP $remote_addr;
#         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
#         proxy_set_header X-Forwarded-Proto $scheme;
#         proxy_ssl_server_name on;
#     }
# Затем: sudo nginx -t && sudo systemctl reload nginx
```

**При первичной установке** можно сразу передать ключ — тогда `setup.sh` создаст `module2/.env` и настроит Nginx с проксированием AI:

```bash
export VITE_AI_API_KEY="sk-or-v1-ВАШ_КЛЮЧ_OPENROUTER"
sudo GIT_REPO="" APP_DIR=/opt/astramicro bash deploy/setup.sh
```

### Картинки маршрутов и API

В `nginx-astramicro.conf` для `location /java-api/` должен быть **rewrite**: `rewrite ^/java-api(.*)$ $1 break;`, чтобы бэкенд получал путь `/api/v1/...` (без префикса `/java-api`). Иначе запросы к картинкам маршрутов и товаров вернут 404.

### TTS (озвучка POI)

По умолчанию озвучка идёт через Web Speech API в браузере или через Groq (Node). Чтобы использовать **Yandex TTS** (пакет `yandex-tts-free`):

1. На сервере: `pip install yandex-tts-free`, в системе должен быть **ffmpeg**.
2. В проекте есть скрипт `scripts/yandex_tts.py` (читает текст из stdin, выводит MP3 в stdout).
3. При запуске Node-сервера задайте переменную: `YANDEX_TTS_SCRIPT=/opt/astramicro/scripts/yandex_tts.py` (или полный путь к скрипту). Тогда запросы к `/api/node/ai/tts` будут использовать Yandex TTS; при ошибке — fallback на Groq/Web Speech.
