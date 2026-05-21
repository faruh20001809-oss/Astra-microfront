#!/bin/bash
# Добавляет колонки priority, status, outdated_reason в routes (PostgreSQL).
# Вызывается из update.sh / recover-all.sh или вручную:
#   sudo bash /opt/astramicro/deploy/migrate-routes-db.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$APP_DIR/deploy/sql/migrate-routes-lifecycle.sql"

if [ ! -f "$SQL_FILE" ]; then
  echo "ОШИБКА: нет файла $SQL_FILE"
  exit 1
fi

DB_NAME="museum_user"
DB_USER="postgres"
DB_HOST="localhost"

for env_file in /etc/astrakhan-admin.env /etc/astra/admin.env; do
  if [ -f "$env_file" ]; then
    # shellcheck disable=SC1090
    set -a
    source "$env_file"
    set +a
  fi
done

if [ -n "${SPRING_DATASOURCE_URL:-}" ]; then
  DB_NAME="$(echo "$SPRING_DATASOURCE_URL" | sed -n 's|.*://[^/]*/\([^?]*\).*|\1|p')"
fi
DB_USER="${SPRING_DATASOURCE_USERNAME:-postgres}"
export PGPASSWORD="${SPRING_DATASOURCE_PASSWORD:-}"

echo "=== Миграция routes: БД=$DB_NAME пользователь=$DB_USER ==="

if ! systemctl is-active --quiet postgresql 2>/dev/null; then
  echo "PostgreSQL не запущен. Запустите: systemctl start postgresql"
  exit 1
fi

run_psql() {
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
}

if run_psql -f "$SQL_FILE"; then
  echo "OK: колонки routes (priority, status, outdated_reason)"
  run_psql -c "SELECT column_name FROM information_schema.columns WHERE table_name='routes' AND column_name IN ('priority','status','outdated_reason') ORDER BY 1;"
  exit 0
fi

echo "Повтор через sudo -u postgres..."
sudo -u postgres psql -d "$DB_NAME" -v ON_ERROR_STOP=1 -f "$SQL_FILE"
echo "OK (postgres)"
sudo -u postgres psql -d "$DB_NAME" -c "SELECT column_name FROM information_schema.columns WHERE table_name='routes' AND column_name IN ('priority','status','outdated_reason') ORDER BY 1;"
