#!/bin/bash
# Обновление приложения на сервере: синхронизация с origin, пересборка, перезапуск
# Запуск: sudo bash /opt/astramicro/deploy/update.sh
# Переменные: BRANCH=main (по умолчанию)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BRANCH="${BRANCH:-main}"

echo "=== Обновление Astra-microfront в $APP_DIR (ветка $BRANCH) ==="

cd "$APP_DIR"
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Ошибка: $APP_DIR не является git-репозиторием."
  exit 1
fi

# Всегда берём состояние с сервера — локальные правки на сервере не сохраняем
git fetch origin
if ! git rev-parse "origin/$BRANCH" >/dev/null 2>&1; then
  echo "Ошибка: ветка origin/$BRANCH не найдена."
  exit 1
fi

# Жёсткий сброс к origin: никакие локальные изменения (target/, dist/, и т.д.) не блокируют обновление
git reset --hard "origin/$BRANCH"
git checkout -B "$BRANCH" "origin/$BRANCH"
git clean -fd astrakhan-admin/target module2/dist 2>/dev/null || true

echo "[1/5] Сборка бэкенда (Maven)..."
cd "$APP_DIR/astrakhan-admin"
mvn -q clean package -DskipTests

echo "[2/5] Сборка фронта (npm)..."
cd "$APP_DIR/module2"
npm ci
npm run build

echo "[3/5] Перезапуск astrakhan-admin (Java)..."
systemctl restart astrakhan-admin

echo "[4/5] Перезапуск astramicro-admin (Flask)..."
# Сначала остановка, пауза (чтобы порт 5000 успел освободиться), затем старт — иначе «Address already in use»
systemctl stop astramicro-admin 2>/dev/null || true
sleep 2
systemctl start astramicro-admin

echo "[5/5] Перезапуск astramicro-node (Node API /api, если unit есть)..."
if [ -f /etc/systemd/system/astramicro-node.service ]; then
  systemctl restart astramicro-node
  echo "  astramicro-node перезапущен."
else
  echo "  unit не установлен — пропуск (см. README-DEPLOY п. 5.1)."
fi

echo "Готово. Статика из module2/dist, Nginx перезагружать не нужно."
