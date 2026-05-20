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

# 3. Пересобрать JAR
echo "[3] Пересборка бэкенда..."
if ! command -v mvn >/dev/null 2>&1; then
  echo "ОШИБКА: mvn не установлен. apt install maven"
  exit 1
fi
cd "$APP_DIR/astrakhan-admin"
mvn -q package -DskipTests
cd "$APP_DIR"
if [ ! -f "$JAR_PATH" ]; then
  echo "ОШИБКА: JAR не собран: $JAR_PATH"
  exit 1
fi
echo "  JAR: OK ($(du -h "$JAR_PATH" | cut -f1))"

# 4. Перезагрузить конфиги и сервисы
systemctl daemon-reload
nginx -t && systemctl reload nginx
systemctl restart astrakhan-admin

echo "[4] Ожидание Spring Boot..."
OK=0
for i in $(seq 1 15); do
  if curl -sf -o /dev/null "http://127.0.0.1:8080/java-api/login" 2>/dev/null; then
    OK=1
    break
  fi
  if ! systemctl is-active --quiet astrakhan-admin; then
    echo "  astrakhan-admin упал на попытке $i"
    break
  fi
  sleep 3
done

echo ""
if [ "$OK" = 1 ]; then
  echo "Готово. Java отвечает."
  curl -s -o /dev/null -w "  localhost routes API: HTTP %{http_code}\n" \
    "http://127.0.0.1:8080/java-api/api/v1/routes?published=true" || true
  echo "  Страница входа: http://193.233.49.59/workflow/login"
else
  echo "ОШИБКА: Java не поднялся за ~45 сек. Логи:"
  journalctl -u astrakhan-admin -n 60 --no-pager
  echo ""
  echo "Часто: нет PostgreSQL, неверный пароль БД, нет JAR."
  echo "Диагностика: sudo bash $APP_DIR/deploy/diagnose.sh"
  exit 1
fi
echo ""
