#!/bin/bash
# Диагностика после обновления. Запуск: sudo bash deploy/diagnose.sh
# Или: cd /opt/astramicro && sudo bash deploy/diagnose.sh

set +e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=============================================="
echo " Диагностика Astra-microfront ($APP_DIR)"
echo "=============================================="

echo ""
echo "--- 1. Сервисы ---"
for s in astrakhan-admin astramicro-admin astramicro-node nginx; do
  if systemctl is-active --quiet "$s" 2>/dev/null; then
    echo "  $s: RUNNING"
  else
    echo "  $s: FAILED / inactive"
  fi
done

echo ""
echo "--- 2. JAR и статика ---"
JAR="$APP_DIR/astrakhan-admin/target/history-admin-1.0.0.jar"
if [ -f "$JAR" ]; then
  echo "  JAR: $JAR (OK, $(du -h "$JAR" | cut -f1))"
else
  echo "  JAR: НЕТ ($JAR)"
fi
if [ -d "$APP_DIR/module2/dist" ]; then
  echo "  module2/dist: есть"
else
  echo "  module2/dist: НЕТ (соберите: cd module2 && npm run build)"
fi

echo ""
echo "--- 3. Порты ---"
for port in 8080 5000 3001; do
  if ss -tlnp 2>/dev/null | grep -q ":$port "; then
    echo "  :$port — слушается"
  else
    echo "  :$port — не слушается"
  fi
done

echo ""
echo "--- 4. Последние ошибки astrakhan-admin (Java) ---"
journalctl -u astrakhan-admin -n 15 --no-pager 2>/dev/null || echo "  (сервис не найден или нет логов)"

echo ""
echo "--- 5. Последние ошибки astramicro-admin (Flask) ---"
journalctl -u astramicro-admin -n 15 --no-pager 2>/dev/null || echo "  (сервис не найден или нет логов)"

echo ""
echo "--- 6. Nginx ---"
nginx -t 2>&1 || true

echo ""
echo "=============================================="
echo " Рекомендации:"
echo "  • Перезапуск Java:  systemctl restart astrakhan-admin"
echo "  • Логи в реальном времени:  journalctl -u astrakhan-admin -f"
echo "  • Пересборка JAR:  cd $APP_DIR/astrakhan-admin && mvn clean package -DskipTests"
echo "=============================================="
