#!/bin/bash
# Однократное исправление: Nginx и systemd так, чтобы /java-api/login отдавала админка, а не фронт.
# Запуск на сервере: bash /opt/astramicro/deploy/fix-java-api-routes.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR_PATH="$APP_DIR/astrakhan-admin/target/history-admin-1.0.0.jar"

echo "=== Исправление маршрутов /java-api ==="

NGINX_SITE="/etc/nginx/sites-available/astramicro"
if [ -f "$APP_DIR/deploy/nginx-astramicro.conf" ]; then
  cp "$APP_DIR/deploy/nginx-astramicro.conf" "$NGINX_SITE"
  echo "[1] Nginx: конфиг скопирован из deploy/nginx-astramicro.conf."
else
  echo "[1] Предупреждение: deploy/nginx-astramicro.conf не найден, правки sed."
fi

# Убрать ошибочный rewrite (prod: context-path=/java-api, путь должен оставаться /java-api/...)
if grep -q 'rewrite \^/java-api' "$NGINX_SITE" 2>/dev/null; then
  sed -i '/rewrite \^\/java-api/d' "$NGINX_SITE"
  echo "[1b] Nginx: удалён rewrite /java-api (несовместим с Spring prod)."
fi

# proxy_pass без завершающего слэша — полный URI /java-api/...
if grep -q 'proxy_pass http://127.0.0.1:8080/;' "$NGINX_SITE" 2>/dev/null; then
  sed -i 's|proxy_pass http://127.0.0.1:8080/;|proxy_pass http://127.0.0.1:8080;|' "$NGINX_SITE"
  echo "[1c] Nginx: proxy_pass без слэша."
fi

# 2. Systemd: добавить профиль prod в ExecStart
if ! grep -q 'spring.profiles.active=prod' /etc/systemd/system/astrakhan-admin.service 2>/dev/null; then
  sed -i 's|ExecStart=/usr/bin/java -Xmx1024m -jar |ExecStart=/usr/bin/java -Xmx1024m -Dspring.profiles.active=prod -jar |' /etc/systemd/system/astrakhan-admin.service
  echo "[2] Systemd: добавлен -Dspring.profiles.active=prod."
else
  echo "[2] Systemd: профиль prod уже задан."
fi

# 3. Пересобрать JAR (чтобы в нём был application-prod.properties)
echo "[3] Пересборка бэкенда..."
cd "$APP_DIR/astrakhan-admin"
mvn -q package -DskipTests
cd "$APP_DIR"

# 4. Перезагрузить конфиги и сервисы
systemctl daemon-reload
nginx -t && systemctl reload nginx
systemctl restart astrakhan-admin

echo ""
echo "Готово. Через 20–30 сек проверьте: http://193.233.49.59/workflow/login"
echo "Логи: journalctl -u astrakhan-admin -n 30 -f"
echo ""
