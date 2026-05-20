#!/bin/bash
# Быстрый перезапуск Java без пересборки. Запуск: sudo bash deploy/restart-java.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$APP_DIR/astrakhan-admin/target/history-admin-1.0.0.jar"

if [ ! -f "$JAR" ]; then
  echo "JAR не найден: $JAR"
  echo "Соберите: cd $APP_DIR/astrakhan-admin && mvn clean package -DskipTests"
  exit 1
fi

systemctl daemon-reload
systemctl restart astrakhan-admin

echo "Ожидание старта Spring Boot (до 45 сек)..."
for i in $(seq 1 15); do
  if curl -sf -o /dev/null "http://127.0.0.1:8080/java-api/login" 2>/dev/null; then
    echo "OK: Java отвечает на :8080/java-api/login (попытка $i)"
    exit 0
  fi
  if ! systemctl is-active --quiet astrakhan-admin; then
    echo "Сервис astrakhan-admin не active. Логи:"
    journalctl -u astrakhan-admin -n 40 --no-pager
    exit 1
  fi
  sleep 3
done

echo "Таймаут: порт 8080 не ответил. Логи:"
journalctl -u astrakhan-admin -n 50 --no-pager
exit 1
