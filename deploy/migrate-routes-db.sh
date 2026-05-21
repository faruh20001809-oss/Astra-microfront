#!/bin/bash
# Добавляет колонки priority, status, outdated_reason в routes (PostgreSQL).
# Запуск (root на сервере): bash /opt/astramicro/deploy/migrate-routes-db.sh
# На Debian peer-auth: подключение от системного пользователя postgres (без пароля).

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$APP_DIR/deploy/sql/migrate-routes-lifecycle.sql"

if [ ! -f "$SQL_FILE" ]; then
  echo "ОШИБКА: нет файла $SQL_FILE"
  exit 1
fi

DB_NAME="museum_user"

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

echo "=== Миграция routes: БД=$DB_NAME ==="

if ! systemctl is-active --quiet postgresql 2>/dev/null; then
  echo "PostgreSQL не запущен. Запустите: systemctl start postgresql"
  exit 1
fi

verify_columns() {
  psql -d "$DB_NAME" -v ON_ERROR_STOP=1 -c \
    "SELECT column_name FROM information_schema.columns WHERE table_name='routes' AND column_name IN ('priority','status','outdated_reason') ORDER BY 1;"
}

# 1) Системный пользователь postgres (peer, без пароля) — типично для root на Debian
run_as_os_postgres() {
  if ! id postgres &>/dev/null 2>&1; then
    return 1
  fi
  # su без «-» — тот же стиль, что «sudo -u postgres»; на сервере sudo часто отсутствует
  if command -v runuser &>/dev/null; then
    runuser -u postgres -- psql -d "$DB_NAME" -v ON_ERROR_STOP=1 -f "$SQL_FILE"
  else
    su postgres -s /bin/sh -c "psql -d '$DB_NAME' -v ON_ERROR_STOP=1 -f '$SQL_FILE'"
  fi
}

verify_as_os_postgres() {
  if command -v runuser &>/dev/null; then
    runuser -u postgres -- psql -d "$DB_NAME" -v ON_ERROR_STOP=1 -c \
      "SELECT column_name FROM information_schema.columns WHERE table_name='routes' AND column_name IN ('priority','status','outdated_reason') ORDER BY 1;"
  else
    su postgres -s /bin/sh -c "psql -d '$DB_NAME' -v ON_ERROR_STOP=1 -c \"SELECT column_name FROM information_schema.columns WHERE table_name='routes' AND column_name IN ('priority','status','outdated_reason') ORDER BY 1;\""
  fi
}

# 2) TCP + пароль из env (как Spring Boot)
run_with_env_password() {
  local db_user="${SPRING_DATASOURCE_USERNAME:-postgres}"
  export PGPASSWORD="${SPRING_DATASOURCE_PASSWORD:-}"
  if [ -z "$PGPASSWORD" ]; then
    return 1
  fi
  psql -h localhost -U "$db_user" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f "$SQL_FILE"
}

verify_with_env_password() {
  local db_user="${SPRING_DATASOURCE_USERNAME:-postgres}"
  export PGPASSWORD="${SPRING_DATASOURCE_PASSWORD:-}"
  psql -h localhost -U "$db_user" -d "$DB_NAME" -v ON_ERROR_STOP=1 -c \
    "SELECT column_name FROM information_schema.columns WHERE table_name='routes' AND column_name IN ('priority','status','outdated_reason') ORDER BY 1;"
}

if run_as_os_postgres; then
  echo "OK: миграция через пользователя ОС postgres (peer)"
  verify_as_os_postgres
  exit 0
fi

echo "Peer (postgres): не удалось, пробуем пароль из SPRING_DATASOURCE_*..."
if run_with_env_password; then
  echo "OK: миграция через TCP (логин из env)"
  verify_with_env_password
  exit 0
fi

echo ""
echo "ОШИБКА: не удалось применить $SQL_FILE"
echo "  Вручную от пользователя postgres (без sudo):"
echo "    su postgres -c \"psql -d $DB_NAME -f $SQL_FILE\""
echo "  Или укажите верный пароль в /etc/astrakhan-admin.env:"
echo "    SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD"
exit 1
