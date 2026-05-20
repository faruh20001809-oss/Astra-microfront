#!/bin/bash
# 403 на главной: нет dist/index.html или nginx (www-data) не может читать каталог.
# Запуск: sudo bash /opt/astramicro/deploy/fix-static-403.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DIST="$APP_DIR/module2/dist"

echo "=== Исправление статики фронта (403 Forbidden) ==="

# 1. Сборка, если нет index.html
if [ ! -f "$DIST/index.html" ]; then
  echo "[1] Сборка module2 (нет $DIST/index.html)..."
  if ! command -v npm >/dev/null 2>&1; then
    echo "ОШИБКА: npm не установлен. apt install -y nodejs"
    exit 1
  fi
  cd "$APP_DIR/module2"
  npm ci
  npm run build
  cd "$APP_DIR"
else
  echo "[1] $DIST/index.html — есть"
fi

# 2. Права: www-data должен проходить по пути и читать файлы
echo "[2] Права на каталоги и dist..."
chmod 755 /opt 2>/dev/null || true
chmod 755 "$APP_DIR" 2>/dev/null || true
chmod 755 "$APP_DIR/module2" 2>/dev/null || true
if [ ! -d "$DIST" ] || [ ! -f "$DIST/index.html" ]; then
  echo "ОШИБКА: каталог $DIST не создан после сборки"
  exit 1
fi
find "$DIST" -type d -exec chmod 755 {} \;
find "$DIST" -type f -exec chmod 644 {} \;
echo "  dist: $(find "$DIST" -type f | wc -l) файлов"
if [ -f "$APP_DIR/deploy/favicon.svg" ]; then
  cp "$APP_DIR/deploy/favicon.svg" "$DIST/favicon.svg"
  cp "$APP_DIR/deploy/favicon.svg" "$DIST/favicon.ico"
  cp "$APP_DIR/deploy/favicon.svg" "$APP_DIR/module2/public/favicon.svg" 2>/dev/null || true
  echo "  favicon: скопирован в dist"
fi

# 3. Nginx root в конфиге
NGINX_SITE="/etc/nginx/sites-available/astramicro"
if [ -f "$APP_DIR/deploy/nginx-astramicro.conf" ]; then
  cp "$APP_DIR/deploy/nginx-astramicro.conf" "$NGINX_SITE"
  ln -sf "$NGINX_SITE" /etc/nginx/sites-enabled/astramicro 2>/dev/null || true
fi
if grep -q 'root ' "$NGINX_SITE" 2>/dev/null; then
  echo "[3] nginx root: $(grep -m1 'root ' "$NGINX_SITE")"
fi
nginx -t
systemctl reload nginx

# 4. Проверка от имени nginx (если есть sudo -u www-data)
echo "[4] Проверка..."
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1/" 2>/dev/null || echo "000")
echo "  GET http://127.0.0.1/ → HTTP $HTTP"
if [ "$HTTP" = "200" ]; then
  echo "Готово. Откройте http://193.233.49.59/"
else
  echo "Всё ещё не 200. Проверьте:"
  echo "  ls -la $DIST/index.html"
  echo "  namei -l $DIST/index.html"
  echo "  tail -20 /var/log/nginx/error.log"
  exit 1
fi
