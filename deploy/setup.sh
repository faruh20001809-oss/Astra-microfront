#!/bin/bash
# Первичная настройка сервера Debian 12 для Astra-microfront
# Запуск: sudo bash setup.sh
# Перед запуском: задайте GIT_REPO (URL репозитория) и при необходимости APP_DIR

set -e

# --- Настройки (измените под себя) ---
GIT_REPO="${GIT_REPO:-https://github.com/YOUR_USER/Astra-microfront.git}"
APP_DIR="${APP_DIR:-/opt/astramicro}"
BRANCH="${BRANCH:-main}"

echo "=== Astra-microfront: установка на сервер ==="
echo "  Репозиторий: $GIT_REPO"
echo "  Каталог приложения: $APP_DIR"
echo "  Ветка: $BRANCH"
echo ""

# 1. Обновление системы и установка зависимостей
echo "[1/6] Обновление системы и установка пакетов..."
apt-get update
apt-get install -y \
  openjdk-17-jdk \
  nginx \
  git \
  maven \
  curl

# Node.js 20 LTS (для сборки фронта)
if ! command -v node &>/dev/null; then
  echo "[1.1/6] Установка Node.js 20..."
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y nodejs
fi

# 2. Каталог приложения
echo "[2/6] Каталог приложения: $APP_DIR"
mkdir -p "$APP_DIR"
cd "$APP_DIR"

if [ -d ".git" ]; then
  echo "  Репозиторий уже клонирован, пропуск clone."
else
  echo "  Клонирование репозитория..."
  git clone -b "$BRANCH" "$GIT_REPO" .
fi

# 3. Сборка бэкенда
echo "[3/6] Сборка Spring Boot (astrakhan-admin)..."
cd "$APP_DIR/astrakhan-admin"
mvn -q package -DskipTests
cd "$APP_DIR"

# 4. Сборка фронта
echo "[4/6] Сборка фронта (module2)..."
cd "$APP_DIR/module2"
npm ci
npm run build
cd "$APP_DIR"

# 5. Systemd: автозапуск бэкенда
echo "[5/6] Настройка systemd..."
JAR_PATH="$APP_DIR/astrakhan-admin/target/history-admin-1.0.0.jar"
cat > /etc/systemd/system/astrakhan-admin.service << EOF
[Unit]
Description=Astrakhan Admin (Spring Boot)
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$APP_DIR/astrakhan-admin
ExecStart=/usr/bin/java -Xmx1024m -Dspring.profiles.active=prod -jar $JAR_PATH
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable astrakhan-admin
systemctl start astrakhan-admin
echo "  Сервис astrakhan-admin: включён и запущен."

# 6. Nginx: статика + прокси к API
echo "[6/6] Настройка Nginx..."
FRONTEND_ROOT="$APP_DIR/module2/dist"
cat > /etc/nginx/sites-available/astramicro << EOF
server {
    listen 80 default_server;
    server_name _;
    root $FRONTEND_ROOT;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /java-api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

ln -sf /etc/nginx/sites-available/astramicro /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx
echo "  Nginx: конфиг применён."

# Опционально: cron для автообновления при новых коммитах (каждые 5 мин)
if [ -z "$NO_CRON" ]; then
  CRON_LINE="*/5 * * * * root $APP_DIR/deploy/auto-update.sh >> /var/log/astramicro-auto-update.log 2>&1"
  if ! grep -q "astramicro/auto-update.sh" /etc/crontab 2>/dev/null; then
    echo "$CRON_LINE" >> /etc/crontab
    echo "  Cron: автообновление каждые 5 мин (проверка Git). Лог: /var/log/astramicro-auto-update.log"
  fi
fi

echo ""
echo "=== Готово. ==="
echo "  Фронт: http://$(curl -s ifconfig.me 2>/dev/null || echo 'IP')/"
echo "  API:   http://127.0.0.1:8080 (прокси /java-api)"
echo "  Обновление: $APP_DIR/deploy/update.sh"
echo ""
