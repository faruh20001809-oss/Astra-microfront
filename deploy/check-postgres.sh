#!/bin/bash
# Проверка PostgreSQL для Java (museum_user). root: bash deploy/check-postgres.sh

set -e

URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/museum_user}"
USER="${SPRING_DATASOURCE_USERNAME:-postgres}"
PASS="${SPRING_DATASOURCE_PASSWORD:-root}"

if [ -f /etc/astra/admin.env ]; then
  # shellcheck disable=SC1091
  set -a && source /etc/astra/admin.env && set +a
fi
if [ -f /etc/astrakhan-admin.env ]; then
  # shellcheck disable=SC1091
  set -a && source /etc/astrakhan-admin.env && set +a
fi

echo "=== PostgreSQL для astrakhan-admin ==="
systemctl start postgresql 2>/dev/null || true
echo "postgresql: $(systemctl is-active postgresql 2>/dev/null || echo '?')"

if command -v psql >/dev/null 2>&1; then
  export PGPASSWORD="${SPRING_DATASOURCE_PASSWORD:-$PASS}"
  DB_USER="${SPRING_DATASOURCE_USERNAME:-$USER}"
  if psql -h localhost -U "$DB_USER" -d museum_user -c 'SELECT 1' >/dev/null 2>&1; then
    echo "psql: подключение к museum_user — OK"
  else
    echo "psql: ОШИБКА подключения (логин/пароль/БД)"
    echo "  Проверьте /etc/astra/admin.env:"
    echo "    SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD"
    echo "  Создание БД (если нет):"
    echo "    sudo -u postgres psql -c \"CREATE DATABASE museum_user;\" 2>/dev/null || true"
    exit 1
  fi
else
  echo "psql не установлен — пропуск прямой проверки"
fi

echo "Дальше: bash deploy/start-java.sh"
