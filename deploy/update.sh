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
# target/ — артефакт Maven; dist/ не трогаем до успешной сборки фронта (иначе 403 на /)
git clean -fd astrakhan-admin/target 2>/dev/null || true

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
if ! npm ci; then
  echo "  ОШИБКА: npm ci. Проверьте node/npm: node -v && npm -v"
  exit 1
fi
if ! npm run build; then
  echo "  ОШИБКА: npm run build. Статика module2/dist не обновлена."
  if [ ! -f "$APP_DIR/module2/dist/index.html" ]; then
    echo "  На главной будет 403 — восстановите: cd $APP_DIR/module2 && npm run build"
    echo "  или: sudo bash $APP_DIR/deploy/fix-static-403.sh"
  fi
  exit 1
fi
# Права для nginx (www-data): без этого часто 403 Forbidden на /
echo "  Права на module2/dist..."
chmod 755 /opt "$APP_DIR" "$APP_DIR/module2" 2>/dev/null || true
find "$APP_DIR/module2/dist" -type d -exec chmod 755 {} \;
find "$APP_DIR/module2/dist" -type f -exec chmod 644 {} \;
if [ -f "$APP_DIR/deploy/favicon.svg" ]; then
  cp "$APP_DIR/deploy/favicon.svg" "$APP_DIR/module2/dist/favicon.svg" 2>/dev/null || true
  cp "$APP_DIR/deploy/favicon.svg" "$APP_DIR/module2/dist/favicon.ico" 2>/dev/null || true
fi

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

echo ""
echo "=== Проверка после обновления ==="
if [ -f "$APP_DIR/module2/dist/index.html" ]; then
  echo "  module2/dist/index.html: OK"
else
  echo "  ВНИМАНИЕ: нет module2/dist/index.html → sudo bash $APP_DIR/deploy/fix-static-403.sh"
fi
for port in 8080 5000 3001; do
  if ss -tlnp 2>/dev/null | grep -q ":$port "; then
    echo "  :$port — слушается"
  else
    echo "  :$port — не слушается (см. systemctl / journalctl)"
  fi
done
CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1/java-api/api/v1/routes?published=true" 2>/dev/null || echo "000")
echo "  GET /java-api/api/v1/routes (через nginx): HTTP $CODE"
echo ""
echo "Готово. При проблемах: sudo bash $APP_DIR/deploy/diagnose.sh"
