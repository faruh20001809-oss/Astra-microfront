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

echo "[1/7] Миграция БД routes (priority, status)..."
if [ -f "$APP_DIR/deploy/migrate-routes-db.sh" ]; then
  bash "$APP_DIR/deploy/migrate-routes-db.sh" || {
    echo "  ВНИМАНИЕ: SQL-миграция routes не выполнена. Вручную:"
    echo "    sudo bash $APP_DIR/deploy/migrate-routes-db.sh"
  }
else
  echo "  migrate-routes-db.sh не найден — пропуск"
fi

echo "[2/7] Сборка бэкенда (Maven)..."
if ! command -v java >/dev/null 2>&1; then
  echo "  ОШИБКА: java не найдена. Установите JDK 17: apt install -y openjdk-17-jdk"
  exit 1
fi
JAVA_MAJOR="$(java -version 2>&1 | head -1 | sed -n 's/.*version "\([0-9]*\).*/\1/p')"
if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt 17 ] 2>/dev/null; then
  echo "  ОШИБКА: нужен JDK 17+, сейчас: $(java -version 2>&1 | head -1)"
  echo "  apt install -y openjdk-17-jdk && update-alternatives --config java"
  exit 1
fi
cd "$APP_DIR/astrakhan-admin"
if [ ! -f "src/main/java/ru/astrakhan/admin/AstrakhanAdminApplication.java" ]; then
  echo "  ОШИБКА: нет исходников Spring Boot в astrakhan-admin/src/main/java"
  exit 1
fi
if ! mvn clean package -DskipTests; then
  echo "  ОШИБКА Maven. Частые причины: JDK < 17, нет сети для зависимостей."
  echo "  Проверка: ls -la target/classes/ru/astrakhan/admin/AstrakhanAdminApplication.class"
  exit 1
fi
if [ ! -f "target/history-admin-1.0.0.jar" ]; then
  echo "  ОШИБКА: JAR не собран: target/history-admin-1.0.0.jar"
  exit 1
fi

echo "[3/7] Сборка фронта (npm)..."
cd "$APP_DIR/module2"
npm ci
npm run build

echo "[4/7] Nginx (актуальный конфиг без rewrite /java-api)..."
if [ -f "$APP_DIR/deploy/nginx-astramicro.conf" ]; then
  cp "$APP_DIR/deploy/nginx-astramicro.conf" /etc/nginx/sites-available/astramicro
  sed -i '/rewrite \^\/java-api/d' /etc/nginx/sites-available/astramicro 2>/dev/null || true
  nginx -t && systemctl reload nginx
fi

echo "[5/7] Перезапуск astrakhan-admin (Java)..."
systemctl restart astrakhan-admin

echo "[6/7] Перезапуск astramicro-admin (Flask)..."
# Сначала остановка, пауза (чтобы порт 5000 успел освободиться), затем старт — иначе «Address already in use»
systemctl stop astramicro-admin 2>/dev/null || true
sleep 2
systemctl start astramicro-admin

echo "[7/7] Перезапуск astramicro-node (Node API /api, если unit есть)..."
if [ -f /etc/systemd/system/astramicro-node.service ]; then
  systemctl restart astramicro-node
  echo "  astramicro-node перезапущен."
else
  echo "  unit не установлен — пропуск (см. README-DEPLOY п. 5.1)."
fi

echo "Готово. Статика из module2/dist, Nginx перезагружать не нужно."
