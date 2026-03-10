#!/bin/bash
# Обновление приложения: git pull, пересборка, перезапуск
# Запуск: из каталога репозитория или: sudo bash /opt/astramicro/deploy/update.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BRANCH="${BRANCH:-main}"

echo "=== Обновление Astra-microfront в $APP_DIR ==="

cd "$APP_DIR"
git fetch origin
git checkout "$BRANCH"
git pull origin "$BRANCH"

echo "[1/3] Сборка бэкенда..."
cd "$APP_DIR/astrakhan-admin"
mvn -q package -DskipTests

echo "[2/3] Сборка фронта..."
cd "$APP_DIR/module2"
npm ci
npm run build

echo "[3/3] Перезапуск сервиса..."
systemctl restart astrakhan-admin

echo "Готово. Nginx отдаёт статику из module2/dist, перезагрузка не нужна."
