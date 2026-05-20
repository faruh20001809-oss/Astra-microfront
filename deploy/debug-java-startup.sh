#!/bin/bash
# Почему не слушается :8080. root: bash /opt/astramicro/deploy/debug-java-startup.sh

set +e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$APP_DIR/astrakhan-admin/target/history-admin-1.0.0.jar"
LOG="/tmp/astrakhan-admin-manual.log"

echo "=============================================="
echo " Диагностика запуска astrakhan-admin"
echo "=============================================="

echo ""
echo "--- 1. Java и JAR ---"
java -version 2>&1 | head -3
if [ -f "$JAR" ]; then
  echo "JAR: $JAR ($(du -h "$JAR" | cut -f1))"
else
  echo "JAR ОТСУТСТВУЕТ: $JAR"
  echo "  cd $APP_DIR/astrakhan-admin && mvn clean package -DskipTests"
fi

echo ""
echo "--- 2. systemd ---"
systemctl status astrakhan-admin --no-pager -l 2>&1 | head -20

echo ""
echo "--- 3. Порт 8080 ---"
ss -tlnp | grep 8080 || echo "  :8080 НЕ слушается"

echo ""
echo "--- 4. PostgreSQL ---"
if systemctl list-unit-files postgresql.service >/dev/null 2>&1; then
  systemctl start postgresql 2>/dev/null
  echo "postgresql: $(systemctl is-active postgresql)"
else
  echo "postgresql unit не найден"
fi

if [ -f /etc/astra/admin.env ]; then
  echo "есть /etc/astra/admin.env"
  grep -E '^SPRING_DATASOURCE_' /etc/astra/admin.env 2>/dev/null | sed 's/PASSWORD=.*/PASSWORD=***/'
elif [ -f /etc/astrakhan-admin.env ]; then
  echo "есть /etc/astrakhan-admin.env"
  grep -E '^SPRING_DATASOURCE_' /etc/astrakhan-admin.env 2>/dev/null | sed 's/PASSWORD=.*/PASSWORD=***/'
else
  echo "НЕТ /etc/astra/admin.env — используются дефолты из application-prod (postgres/root)"
fi

if command -v psql >/dev/null 2>&1; then
  set -a
  [ -f /etc/astra/admin.env ] && . /etc/astra/admin.env
  [ -f /etc/astrakhan-admin.env ] && . /etc/astrakhan-admin.env
  set +a
  PGPASSWORD="${SPRING_DATASOURCE_PASSWORD:-root}"
  U="${SPRING_DATASOURCE_USERNAME:-postgres}"
  if psql -h localhost -U "$U" -d museum_user -c 'SELECT 1' >/dev/null 2>&1; then
    echo "psql museum_user: OK"
  else
    echo "psql museum_user: ОШИБКА — Java не стартует без БД"
    echo "  Создать БД:"
    echo "    sudo -u postgres psql -c \"CREATE DATABASE museum_user;\" 2>/dev/null"
    echo "    sudo -u postgres psql -c \"ALTER USER postgres PASSWORD 'root';\""
  fi
fi

echo ""
echo "--- 5. Последние 80 строк journalctl ---"
journalctl -u astrakhan-admin -n 80 --no-pager 2>/dev/null

echo ""
echo "--- 6. Ручной старт (45 сек, лог: $LOG) ---"
if [ ! -f "$JAR" ]; then
  echo "Пропуск: нет JAR"
  exit 1
fi

systemctl stop astrakhan-admin 2>/dev/null
sleep 2

: > "$LOG"
cd "$APP_DIR/astrakhan-admin"
timeout 45 java -Xmx1024m -Dspring.profiles.active=prod -jar "$JAR" >>"$LOG" 2>&1 &
JPID=$!
echo "PID $JPID, ждём Started или ошибку..."
for i in $(seq 1 15); do
  sleep 3
  if curl -sf -o /dev/null "http://127.0.0.1:8080/java-api/login" 2>/dev/null; then
    echo "OK: приложение поднялось за ~$((i*3)) сек"
    kill "$JPID" 2>/dev/null
    wait "$JPID" 2>/dev/null
    echo "Запустите: systemctl start astrakhan-admin"
    exit 0
  fi
  if ! kill -0 "$JPID" 2>/dev/null; then
    echo "Процесс завершился досрочно (ошибка при старте)"
    break
  fi
done
kill "$JPID" 2>/dev/null
wait "$JPID" 2>/dev/null

echo ""
echo "--- Ошибки из $LOG (ключевые строки) ---"
grep -iE 'error|exception|failed|refused|password|database|fatal|caused by' "$LOG" | tail -40
echo ""
echo "Полный лог: cat $LOG"
echo ""
echo "Частые решения:"
echo "  1) PostgreSQL: bash deploy/check-postgres.sh"
echo "  2) Пароль БД в /etc/astra/admin.env"
echo "  3) Пересборка: cd astrakhan-admin && mvn clean package -DskipTests"
echo "  4) systemctl restart astrakhan-admin && bash deploy/test-api.sh"
