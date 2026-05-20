#!/bin/bash
# Запуск/диагностика Java API. root: bash /opt/astramicro/deploy/start-java.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$APP_DIR/astrakhan-admin/target/history-admin-1.0.0.jar"

echo "=== Java API (astrakhan-admin) ==="

# PostgreSQL
if systemctl list-unit-files postgresql.service >/dev/null 2>&1; then
  systemctl start postgresql 2>/dev/null || true
  echo "postgresql: $(systemctl is-active postgresql 2>/dev/null || echo '?')"
fi

# JAR
if [ ! -f "$JAR" ]; then
  echo "Сборка JAR..."
  cd "$APP_DIR/astrakhan-admin"
  mvn -q clean package -DskipTests
  cd "$APP_DIR"
fi
[ -f "$JAR" ] || { echo "Нет JAR: $JAR"; exit 1; }
echo "JAR: OK"

# unit + prod
if [ -f "$APP_DIR/deploy/astrakhan-admin.service" ]; then
  cp "$APP_DIR/deploy/astrakhan-admin.service" /etc/systemd/system/astrakhan-admin.service
fi
if ! grep -q 'spring.profiles.active=prod' /etc/systemd/system/astrakhan-admin.service 2>/dev/null; then
  sed -i 's|ExecStart=/usr/bin/java -Xmx1024m -jar |ExecStart=/usr/bin/java -Xmx1024m -Dspring.profiles.active=prod -jar |' \
    /etc/systemd/system/astrakhan-admin.service
fi
systemctl daemon-reload
systemctl enable astrakhan-admin 2>/dev/null || true
systemctl restart astrakhan-admin

echo "Ожидание :8080 (до 90 сек)..."
for i in $(seq 1 30); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:8080/java-api/login" 2>/dev/null || echo "000")
  if [ "$CODE" = "200" ] || [ "$CODE" = "302" ]; then
    echo "OK: /java-api/login → HTTP $CODE"
    curl -s -o /dev/null -w "  routes API: HTTP %{http_code}\n" \
      "http://127.0.0.1:8080/java-api/api/v1/routes?published=true" || true
    curl -s -o /dev/null -w "  products API: HTTP %{http_code}\n" \
      "http://127.0.0.1:8080/java-api/api/v1/products" || true
    curl -s -o /dev/null -w "  через nginx: HTTP %{http_code}\n" \
      "http://127.0.0.1/java-api/api/v1/routes?published=true" || true
    exit 0
  fi
  if ! systemctl is-active --quiet astrakhan-admin; then
    echo "Сервис упал на попытке $i"
    break
  fi
  sleep 3
done

echo ""
echo "Java не поднялся. Последние логи:"
journalctl -u astrakhan-admin -n 60 --no-pager
echo ""
echo "Ручной запуск (увидите ошибку в консоли, Ctrl+C):"
echo "  cd $APP_DIR/astrakhan-admin"
echo "  java -Xmx1024m -Dspring.profiles.active=prod -jar $JAR"
exit 1
