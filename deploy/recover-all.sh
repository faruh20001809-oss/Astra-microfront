#!/bin/bash
# Поднять весь стек после падения: PostgreSQL → nginx → Java → Flask → Node.
# Запуск на сервере: sudo bash /opt/astramicro/deploy/recover-all.sh
# Полная пересборка: sudo bash deploy/recover-all.sh --full

set -e

FULL=0
[ "${1:-}" = "--full" ] && FULL=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$APP_DIR/astrakhan-admin/target/history-admin-1.0.0.jar"
NGINX_SITE="/etc/nginx/sites-available/astramicro"

echo "=============================================="
echo " Восстановление Astra-microfront"
echo " Каталог: $APP_DIR"
echo "=============================================="

# --- PostgreSQL (общая БД для Java и Flask) ---
if systemctl list-unit-files postgresql.service >/dev/null 2>&1; then
  echo "[1] PostgreSQL..."
  systemctl start postgresql 2>/dev/null || true
  sleep 2
  if systemctl is-active --quiet postgresql; then
    echo "  postgresql: RUNNING"
  else
    echo "  postgresql: не запущен (проверьте вручную)"
  fi
else
  echo "[1] unit postgresql не найден — пропуск"
fi

# --- Systemd units из репозитория ---
echo "[2] Systemd units..."
for unit in astrakhan-admin astramicro-admin astramicro-node; do
  if [ -f "$APP_DIR/deploy/${unit}.service" ]; then
    cp "$APP_DIR/deploy/${unit}.service" "/etc/systemd/system/${unit}.service"
    echo "  установлен ${unit}.service"
  fi
done
# prod-профиль в Java unit
if [ -f /etc/systemd/system/astrakhan-admin.service ] && \
   ! grep -q 'spring.profiles.active=prod' /etc/systemd/system/astrakhan-admin.service; then
  sed -i 's|ExecStart=/usr/bin/java -Xmx1024m -jar |ExecStart=/usr/bin/java -Xmx1024m -Dspring.profiles.active=prod -jar |' \
    /etc/systemd/system/astrakhan-admin.service
  echo "  astrakhan-admin: добавлен -Dspring.profiles.active=prod"
fi
systemctl daemon-reload

# --- Nginx ---
echo "[3] Nginx..."
if [ -f "$APP_DIR/deploy/nginx-astramicro.conf" ]; then
  cp "$APP_DIR/deploy/nginx-astramicro.conf" "$NGINX_SITE"
  ln -sf "$NGINX_SITE" /etc/nginx/sites-enabled/astramicro 2>/dev/null || true
  rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true
  sed -i '/rewrite \^\/java-api/d' "$NGINX_SITE" 2>/dev/null || true
  echo "  конфиг: deploy/nginx-astramicro.conf"
else
  echo "  предупреждение: nginx-astramicro.conf не найден"
fi
nginx -t
systemctl enable nginx 2>/dev/null || true
systemctl restart nginx
echo "  nginx: RUNNING"

# --- Сборка Java ---
echo "[4] Spring Boot (Java)..."
if [ "$FULL" = 1 ] || [ ! -f "$JAR" ]; then
  if ! command -v mvn >/dev/null 2>&1; then
    echo "  ОШИБКА: установите Maven: apt install -y maven"
    exit 1
  fi
  cd "$APP_DIR/astrakhan-admin"
  mvn -q clean package -DskipTests
  cd "$APP_DIR"
fi
if [ ! -f "$JAR" ]; then
  echo "  ОШИБКА: нет JAR: $JAR"
  exit 1
fi
echo "  JAR: OK"

# --- Сборка фронта (если нет dist или --full) ---
if [ "$FULL" = 1 ] || [ ! -f "$APP_DIR/module2/dist/index.html" ]; then
  echo "[5] Фронт module2..."
  if command -v npm >/dev/null 2>&1; then
    cd "$APP_DIR/module2"
    npm ci
    npm run build
    cd "$APP_DIR"
    echo "  module2/dist: собран"
  else
    echo "  предупреждение: npm нет — фронт не пересобран"
  fi
else
  echo "[5] module2/dist: уже есть"
fi

# Права для nginx (www-data): иначе 403 Forbidden на /
echo "[5b] Права на static (dist)..."
chmod 755 /opt "$APP_DIR" "$APP_DIR/module2" 2>/dev/null || true
if [ -d "$APP_DIR/module2/dist" ]; then
  find "$APP_DIR/module2/dist" -type d -exec chmod 755 {} \;
  find "$APP_DIR/module2/dist" -type f -exec chmod 644 {} \;
fi

# --- Flask venv ---
echo "[6] Flask (module3)..."
if [ ! -x "$APP_DIR/module3/venv/bin/gunicorn" ]; then
  echo "  создание venv..."
  cd "$APP_DIR/module3"
  python3 -m venv venv
  ./venv/bin/pip install -q -r requirements.txt gunicorn
  cd "$APP_DIR"
fi

# --- Перезапуск сервисов ---
echo "[7] Перезапуск сервисов..."
systemctl enable astrakhan-admin 2>/dev/null || true
systemctl restart astrakhan-admin

systemctl stop astramicro-admin 2>/dev/null || true
sleep 2
systemctl enable astramicro-admin 2>/dev/null || true
systemctl start astramicro-admin

if [ -f /etc/systemd/system/astramicro-node.service ]; then
  systemctl enable astramicro-node 2>/dev/null || true
  systemctl restart astramicro-node 2>/dev/null || true
fi

# --- Ожидание Java ---
echo "[8] Проверка Java (до 60 сек)..."
JAVA_OK=0
for i in $(seq 1 20); do
  if curl -sf -o /dev/null "http://127.0.0.1:8080/java-api/login" 2>/dev/null; then
    JAVA_OK=1
    break
  fi
  if ! systemctl is-active --quiet astrakhan-admin; then
    echo "  astrakhan-admin не active (попытка $i)"
    break
  fi
  sleep 3
done

echo ""
echo "=============================================="
echo " Результат"
echo "=============================================="
for s in nginx astrakhan-admin astramicro-admin astramicro-node; do
  if systemctl is-active --quiet "$s" 2>/dev/null; then
    echo "  $s: RUNNING"
  elif [ -f "/etc/systemd/system/${s}.service" ]; then
    echo "  $s: FAILED"
  fi
done
for port in 80 8080 5000 3001; do
  if ss -tlnp 2>/dev/null | grep -q ":$port "; then
    echo "  порт $port: слушается"
  else
    echo "  порт $port: НЕ слушается"
  fi
done

if [ "$JAVA_OK" = 1 ]; then
  curl -s -o /dev/null -w "  Java /java-api/login: HTTP %{http_code}\n" "http://127.0.0.1:8080/java-api/login" || true
  curl -s -o /dev/null -w "  API routes: HTTP %{http_code}\n" \
    "http://127.0.0.1:8080/java-api/api/v1/routes?published=true" || true
  echo ""
  echo "Сайт: http://193.233.49.59/"
  echo "Вход сотрудников: http://193.233.49.59/workflow/login"
  echo "Админка: http://193.233.49.59/admin/"
else
  echo ""
  echo "ОШИБКА: Java не отвечает. Логи:"
  journalctl -u astrakhan-admin -n 50 --no-pager
  echo ""
  echo "Частые причины:"
  echo "  • PostgreSQL не запущен или неверный пароль в /etc/astrakhan-admin.env"
  echo "  • Нет места на диске (df -h)"
  echo "Диагностика: sudo bash $APP_DIR/deploy/diagnose.sh"
  exit 1
fi
