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
echo "--- 0. Git (совпадает ли с GitHub) ---"
cd "$APP_DIR" || true
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  BRANCH="$(git branch --show-current 2>/dev/null || echo main)"
  echo "  origin: $(git remote get-url origin 2>/dev/null || echo '?')"
  echo "  ветка:  $BRANCH"
  echo "  HEAD:   $(git rev-parse --short HEAD) — $(git log -1 --format=%s 2>/dev/null)"
  if git fetch origin -q 2>/dev/null && git rev-parse "origin/$BRANCH" >/dev/null 2>&1; then
    L="$(git rev-parse HEAD)"
    R="$(git rev-parse "origin/$BRANCH")"
    if [ "$L" = "$R" ]; then
      echo "  с origin/$BRANCH: ОК (тот же коммит, что на GitHub после fetch)"
    else
      echo "  ВНИМАНИЕ: HEAD != origin/$BRANCH — на сервере другой код, чем на GitHub."
      echo "  origin/$BRANCH: $(git rev-parse --short "$R")"
      echo "  Выполните: cd $APP_DIR && bash deploy/update.sh"
    fi
  else
    echo "  (не удалось сравнить с origin — сеть или нет origin/$BRANCH)"
  fi
else
  echo "  $APP_DIR не git-репозиторий"
fi

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
if [ -f "$APP_DIR/module2/dist/index.html" ]; then
  echo "  module2/dist/index.html: есть"
  if namei -l "$APP_DIR/module2/dist/index.html" 2>/dev/null | tail -1 | grep -q 'www-data\| o '; then
    :
  fi
  PERM_OK=1
  for d in /opt "$APP_DIR" "$APP_DIR/module2" "$APP_DIR/module2/dist"; do
    if [ -d "$d" ] && [ ! -r "$d" ]; then PERM_OK=0; fi
    if [ -d "$d" ] && [ ! -x "$d" ]; then PERM_OK=0; fi
  done
  if [ "$PERM_OK" = 0 ]; then
    echo "  ВНИМАНИЕ: nginx может отдать 403 — выполните: sudo bash $APP_DIR/deploy/fix-static-403.sh"
  fi
else
  echo "  module2/dist/index.html: НЕТ → 403 на главной. sudo bash $APP_DIR/deploy/fix-static-403.sh"
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
