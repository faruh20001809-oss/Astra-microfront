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
export GIT_REPO="https://github.com/YOUR_USER/Astra-microfront.git"
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

Скрипт делает: `git pull`, сборка astrakhan-admin, сборка module2, перезапуск `astrakhan-admin`.  
Nginx перезагружать не нужно (статику берёт из обновлённого `module2/dist`).

Ручной вариант по шагам:

```bash
cd /opt/astramicro
git fetch origin
git pull origin main

# Сборка бэкенда
cd /opt/astramicro/astrakhan-admin
sudo mvn -q package -DskipTests

# Сборка фронта (при необходимости задать VITE_AI_API_KEY в module2/.env)
cd /opt/astramicro/module2
sudo npm ci
sudo npm run build

# Перезапуск Java
sudo systemctl restart astrakhan-admin
```

---

## 4. Запуск админки (module3, Flask) на сервере

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

## 5. Полезные команды на сервере

| Действие | Команда |
|----------|--------|
| Логи бэкенда (Java) | `sudo journalctl -u astrakhan-admin -n 100 -f` |
| Логи админки (Flask) | `sudo journalctl -u astramicro-admin -n 100 -f` |
| Перезапуск Nginx | `sudo nginx -t && sudo systemctl reload nginx` |
| Проверка конфига Nginx | `sudo nginx -t` |
| Статус сервисов | `sudo systemctl status astrakhan-admin astramicro-admin nginx` |
| Лог автообновления (cron) | `sudo tail -f /var/log/astramicro-auto-update.log` |

---

## 6. Переменные окружения (продакшен)

- **APP_MODULE2_URL** — публичный URL основного фронта (письма, редиректы). По умолчанию: `http://193.233.49.59`. Задаётся в systemd для `astrakhan-admin`.
- **APP_ADMIN_URL** — URL админки (module3). По умолчанию: `http://193.233.49.59/admin`. В `application-prod.properties`.
- **SPRING_DATASOURCE_*** — БД для Java (по умолчанию PostgreSQL `museum_user` на localhost).
- **VITE_AI_API_KEY** — в `module2/.env` для сборки и (при необходимости) для Nginx-прокси к OpenRouter.

---

## 7. Краткая шпаргалка

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
