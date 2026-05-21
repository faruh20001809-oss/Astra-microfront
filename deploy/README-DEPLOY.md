# Astra-microfront: сборка и деплой на сервер

Адрес: **http://193.233.49.59** (пока по IP; после покупки домена замените в конфигах).  
Маршруты: `/` — фронт, `/workflow/` — Java (сотрудники), `/admin/` — module3 (Flask), `/java-api/` — API.

---

## 1. Сборка локально (перед пушем в Git)

### Бэкенд (Spring Boot)

```bash
cd astrakhan-admin
mvn clean package -DskipTests
```

JAR: `astrakhan-admin/target/history-admin-1.0.0.jar`

### Фронт (module2, Vue + Vite)

```bash
cd module2
npm ci
npm run build
```

Статика: `module2/dist/`

### Админка (module3, Flask)

Сборки нет — интерпретируемый Python. Для продакшена нужен запуск через gunicorn (см. ниже).

---

## 2. Первая установка на сервер (Debian 12)

Подключитесь по SSH, затем:

```bash
# Клонирование и первичная настройка (Java, Nginx, Node, сборка, systemd)
export GIT_REPO="https://github.com/SileverTM/Astra-microfront.git"
# по SSH: export GIT_REPO="git@github.com:SileverTM/Astra-microfront.git"
export APP_DIR="/opt/astramicro"
export BRANCH="main"

# Опционально: ключ для AI (OpenRouter) и домен
# export VITE_AI_API_KEY="sk-or-v1-..."
# export APP_DOMAIN="193.233.49.59"

sudo bash -c 'cd /opt && git clone -b main "$GIT_REPO" astramicro 2>/dev/null || true'
cd /opt/astramicro
sudo bash deploy/setup.sh
```

Если репозиторий уже клонирован в `/opt/astramicro`:

```bash
cd /opt/astramicro
sudo bash deploy/setup.sh
```

Одной строкой — между `cd` и следующей командой нужен **`&&`**, иначе shell выдаст `cd: too many arguments`:

```bash
cd /opt/astramicro && sudo bash deploy/setup.sh
```

**Почему на сервере «не как в репозитории»:** до правки `setup.sh` повторный запуск на уже клонированном каталоге **не подтягивал** коммиты с GitHub — дерево оставалось на старом `HEAD`. Сейчас `setup.sh` при наличии `.git` делает `fetch` и жёсткий сброс к `origin/main` (как `deploy/update.sh`). Дополнительно: каждые 5 минут cron может вызывать `auto-update.sh` и снова выравнивать код с `origin`; вне Git остаются артефакты сборки (`module2/dist`, JAR), секреты (`/etc/astrakhan-admin.env`, при необходимости `module2/.env`), сгенерированный Nginx-фрагмент `deploy/nginx-ai-key.conf`.

После установки:
- Бэкенд: systemd-сервис `astrakhan-admin` (порт 8080).
- Nginx: раздаёт `module2/dist`, проксирует `/workflow/`, `/admin/`, `/java-api/`.
- Module3 (админка на Flask) **нужно запустить отдельно** (см. п. 4).

---

## 3. Обновление на сервере (после git push)

Из каталога репозитория на сервере (под root; если вы root, `sudo` не нужен):

```bash
cd /opt/astramicro
bash deploy/update.sh
```

Если при запуске `./deploy/update.sh` появляется **Permission denied**, вызывайте именно **`bash deploy/update.sh`** (или обновите репозиторий — в `deploy/*.sh` в Git выставлен исполняемый бит).

Скрипт делает: `git fetch` и сброс к `origin/main`, сборка astrakhan-admin, сборка module2, перезапуск Java, Flask и (если установлен) **astramicro-node**.  
Nginx перезагружать не нужно (статику берёт из обновлённого `module2/dist`).

**Не используйте на сервере связку `git pull && …`.** `git pull` делает merge и может остановиться с ошибкой «local changes would be overwritten» из‑за артефактов сборки (`astrakhan-admin/target/`), если они когда‑то попали в репозиторий или пересобирались локально. `update.sh` использует **`git reset --hard origin/main`**, а не merge — обновление проходит стабильно.

### Если «всё равно не совпадает» с тем, что у вас локально или в GitHub

1. **Коммит не запушен** — на сервере будет старый `main`. Локально: `git push origin main`, затем на сервере `bash deploy/update.sh`.
2. **Сверка хеша** — на ПК и на сервере должны совпасть:
   ```bash
   git rev-parse HEAD
   ```
   После пуша и `update.sh` на сервере выведите то же в `/opt/astramicro`.
3. **Кэш браузера** — жёсткое обновление: Ctrl+Shift+R (или очистка кэша для сайта). Старый `index.html` может подтягивать старые чанки с другими именами файлов.
4. **Node API** — карта/AI идут через `astramicro-node` (порт 3001). `update.sh` перезапускает его, если есть unit `astramicro-node.service`; иначе вручную: `systemctl restart astramicro-node`.
5. **Проверка на сервере:** `sudo bash deploy/diagnose.sh` — блок **«0. Git»** покажет, совпадает ли `HEAD` с `origin/main`.

Ручной вариант по шагам (аналог `update.sh`, **без `git pull`**):

```bash
cd /opt/astramicro
git fetch origin
git reset --hard origin/main

cd /opt/astramicro/astrakhan-admin
sudo mvn -q clean package -DskipTests

cd /opt/astramicro/module2
sudo npm ci
sudo npm run build

sudo systemctl restart astrakhan-admin
sudo systemctl restart astramicro-admin 2>/dev/null || true
sudo systemctl restart astramicro-node 2>/dev/null || true
```

---

## 4. PostgreSQL на сервере (для Java и module3)

Бэкенд (astrakhan-admin) и админка (module3) используют одну БД PostgreSQL. Если её ещё нет:

```bash
apt-get install -y postgresql postgresql-contrib
systemctl start postgresql
systemctl enable postgresql
```

Создать пользователя и базу (пароль `root` — замените в prod на свой):

```bash
sudo -u postgres psql -c "CREATE USER postgres WITH PASSWORD 'root';" 2>/dev/null || true
sudo -u postgres psql -c "CREATE DATABASE museum_user OWNER postgres;" 2>/dev/null || true
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'root';"
```

Если пользователь `postgres` уже есть, достаточно создать базу:

```bash
sudo -u postgres createdb -O postgres museum_user
```

Проверка: `psql -U postgres -h localhost -d museum_user -c "SELECT 1;"`

---

## 4.1. Почта (SMTP) для Java-бэкенда (`astrakhan-admin`)

Письма уходят из Spring Boot: подтверждения заказов, код для страницы «Мои заказы» и т.д. Нужны **реальные SMTP-данные** (часто — **пароль приложения**, не пароль от входа в почту).

### Шаг 1 — провайдер

| Провайдер | Хост | Порт | Примечание |
|-----------|------|------|------------|
| **Yandex** | `smtp.yandex.ru` | `465` | В аккаунте: Пароли и авторизация → пароли приложений |
| **Mail.ru** | `smtp.mail.ru` | `465` | Пароль для внешних приложений |
| **Gmail** | `smtp.gmail.com` | `587` | Пароль приложения; на сервере нужен STARTTLS (см. шаг 3) |

В профиле **prod** по умолчанию ожидается схема **465 + SSL** (как у Yandex/Mail.ru). Базовые ключи те же, что в `application.properties`:

- `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
- `APP_MAIL_FROM` — адрес «От кого» (часто совпадает с `USERNAME`)
- `APP_MAIL_ENABLED=true` — отправка включена; при `false` письма не шлются, код для «Мои заказы» пишется **в лог** (`MAIL DISABLED`)

### Шаг 2 — файл с секретами (рекомендуется)

Не храните пароль в Git. На сервере:

```bash
sudo cp /opt/astramicro/deploy/astrakhan-admin.mail.env.example /etc/astrakhan-admin.env
sudo nano /etc/astrakhan-admin.env   # подставьте хост, логин, пароль приложения, APP_MAIL_FROM
sudo chmod 600 /etc/astrakhan-admin.env
```

В unit **`astrakhan-admin.service`** должна быть строка (в репозитории и в `setup.sh` она уже добавлена):

```ini
EnvironmentFile=-/etc/astrakhan-admin.env
```

Префикс `-` значит: файла может ещё не быть — сервис всё равно стартует.

Если unit создавали вручную **до** появления этой строки — добавьте её в секцию `[Service]`, затем:

```bash
sudo systemctl daemon-reload
sudo systemctl restart astrakhan-admin
```

### Шаг 3 — если нужен порт 587 (STARTTLS)

В `/etc/astrakhan-admin.env` добавьте (или раскомментируйте в примере):

```bash
SPRING_MAIL_PORT=587
SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE=false
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

### Шаг 4 — проверка

```bash
sudo journalctl -u astrakhan-admin -n 80 --no-pager
```

Ошибки SMTP обычно видны при первой отправке. Быстрая проверка с сайта: **«Мои заказы»** → ввести email с заказом → **«Получить код»** → письмо или строка в логе при `APP_MAIL_ENABLED=false`.

Подробные комментарии к переменным: `astrakhan-admin/src/main/resources/application-mail.example.properties`.

---

## 5. Запуск админки (module3, Flask) на сервере

Админка отдаётся по `http://193.233.49.59/admin/` и проксируется на порт 5000.

На Debian 12 нельзя ставить пакеты в системный Python (PEP 668), поэтому используем **виртуальное окружение (venv)**.

### Установка venv и зависимостей (один раз)

```bash
apt-get install -y python3-venv
cd /opt/astramicro/module3
bash setup_venv.sh
```

Скрипт создаёт каталог `venv/` и ставит в него все пакеты из `requirements.txt`.

### Однократный запуск (для проверки)

```bash
cd /opt/astramicro/module3
venv/bin/python app.py
# Или: source venv/bin/activate && python app.py
# Переменные для БД (если не по умолчанию):
# export DATABASE_URL="postgresql://user:pass@localhost:5432/museum_user"
```

Остановка: Ctrl+C.

### Постоянно через systemd (рекомендуется)

Сервис должен запускать gunicorn из venv:

```bash
tee /etc/systemd/system/astramicro-admin.service << 'EOF'
[Unit]
Description=Astra-microfront admin (Flask module3)
After=network.target postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/astramicro/module3
ExecStart=/opt/astramicro/module3/venv/bin/gunicorn -w 1 -b 127.0.0.1:5000 app:app
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

Включите и запустите (сначала выполните `setup_venv.sh`, чтобы был каталог venv):

```bash
systemctl daemon-reload
systemctl enable astramicro-admin
systemctl start astramicro-admin
systemctl status astramicro-admin
```

Дальше при обновлении кода module3 перезапускайте сервис:

```bash
systemctl restart astramicro-admin
```

---

## 5.1. Node API (module2: карта, AI, TTS)

Фронт module2 ходит на `/api/` (озвучка, генерация текста). Nginx проксирует эти запросы на Node (порт 3001). Сервис должен быть запущен.

### Запуск через systemd (рекомендуется)

```bash
cp /opt/astramicro/deploy/astramicro-node.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable astramicro-node
systemctl start astramicro-node
systemctl status astramicro-node
```

Дальше при обновлении:

```bash
systemctl restart astramicro-node
```

Логи: `journalctl -u astramicro-node -n 100 -f`

### Yandex TTS (озвучка голосом)

На Debian 12 (PEP 668) пакеты в системный Python ставить нельзя — нужен **venv**. Выполните на сервере **один раз**:

```bash
apt-get update
apt-get install -y ffmpeg python3-venv python3-pip

cd /opt/astramicro/scripts
python3 -m venv venv
./venv/bin/pip install yandex-tts-free
```

Скрипт уже в репозитории: `/opt/astramicro/scripts/yandex_tts.py`. Node сам подхватит Python из `scripts/venv/bin/python3`, если venv есть.

Перезапуск Node API:

```bash
systemctl restart astramicro-node
```

Проверка: в логах не должно быть `[yandex-tts] Install: pip install yandex-tts-free`. Если видите это — venv не создан или пакет не установлен в venv.

### Перезагрузка Nginx после изменения конфига

Если вы под **root**, `sudo` не нужен:

```bash
nginx -t && systemctl reload nginx
```

Если используете sudo: `sudo nginx -t && sudo systemctl reload nginx`

---

## 5.2. Если после обновления ничего не работает

**Шаг 1 — диагностика на сервере:**

```bash
cd /opt/astramicro
sudo bash deploy/diagnose.sh
```

Скрипт покажет: какие сервисы не запущены, есть ли JAR и статика, слушаются ли порты 8080/5000, последние строки логов.

**Шаг 2 — по результатам:**

| Проблема | Что сделать |
|----------|-------------|
| `astrakhan-admin` не running | `sudo journalctl -u astrakhan-admin -n 50` — смотреть причину падения. Часто: нет БД, нет JAR. Затем `sudo systemctl start astrakhan-admin`. |
| JAR не найден | Собрать заново: `cd /opt/astramicro/astrakhan-admin && sudo mvn clean package -DskipTests`, затем `sudo systemctl restart astrakhan-admin`. |
| Ошибка БД (PostgreSQL) | Проверить, что PostgreSQL запущен: `systemctl status postgresql`. Поднять при необходимости: `systemctl start postgresql`. Проверить строку в `application-prod.properties` и переменные `SPRING_DATASOURCE_*`. |
| `/java-api/admin` или `/admin/routes` — «Что-то пошло не так» (logId в HTML) | В логах: `journalctl -u astrakhan-admin -n 200 \| grep <logId>`. Частая причина: в таблице `routes` нет колонок `priority`, `status`, `outdated_reason`. После `git pull` пересоберите JAR и `systemctl restart astrakhan-admin` (миграция при старте — `DatabaseSchemaMigration`). Вручную: `psql … -f deploy/sql/migrate-routes-lifecycle.sql`. |
| Module2 не видит маршруты/товары | `curl -sS http://127.0.0.1:8080/java-api/api/v1/routes?published=true` — должен быть JSON `status: success`. Иначе та же проблема с БД или сервис `astrakhan-admin` не запущен. |
| `astramicro-admin` не running | `sudo journalctl -u astramicro-admin -n 30`. Если нет venv: `cd /opt/astramicro/module3 && bash setup_venv.sh`. Затем `sudo systemctl start astramicro-admin`. |
| Сайт не открывается | `nginx -t && sudo systemctl status nginx`. При изменении конфига Nginx: `sudo systemctl reload nginx`. |

**Шаг 3 — быстрый перезапуск всего:**

```bash
sudo systemctl restart astrakhan-admin
sudo systemctl restart astramicro-admin
sudo systemctl restart astramicro-node
sudo systemctl reload nginx
```

---

## 6. Полезные команды на сервере

| Действие | Команда |
|----------|--------|
| Логи бэкенда (Java) | `journalctl -u astrakhan-admin -n 100 -f` |
| Логи админки (Flask) | `journalctl -u astramicro-admin -n 100 -f` |
| Логи Node API (TTS/AI) | `journalctl -u astramicro-node -n 100 -f` |
| Перезапуск Nginx | `nginx -t && systemctl reload nginx` |
| Проверка конфига Nginx | `nginx -t` |
| Статус сервисов | `systemctl status astrakhan-admin astramicro-admin astramicro-node nginx` |
| Лог автообновления (cron) | `tail -f /var/log/astramicro-auto-update.log` |

---

## 7. Переменные окружения (продакшен)

- **APP_MODULE2_URL** — публичный URL основного фронта (письма, редиректы). По умолчанию: `http://193.233.49.59`. Задаётся в systemd для `astrakhan-admin`.
- **APP_ADMIN_URL** — URL админки (module3). По умолчанию: `http://193.233.49.59/admin`. В `application-prod.properties`.
- **SPRING_DATASOURCE_*** — БД для Java (по умолчанию PostgreSQL `museum_user` на localhost).
- **VITE_AI_API_KEY** — в `module2/.env` для сборки и (при необходимости) для Nginx-прокси к OpenRouter.

---

## 8. Краткая шпаргалка

```text
# Локально: сборка перед пушем
cd astrakhan-admin && mvn clean package -DskipTests
cd module2 && npm ci && npm run build

# Сервер: первая установка
cd /opt/astramicro && sudo bash deploy/setup.sh

# Сервер: обновление после git push
cd /opt/astramicro && sudo bash deploy/update.sh

# Сервер: админка (module3) — первый запуск через systemd
sudo systemctl start astramicro-admin
```
