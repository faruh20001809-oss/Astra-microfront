#!/bin/bash
# Автообновление сборки на сервере: при появлении новых коммитов на origin запускает update.sh
# Cron (каждые 5 мин): */5 * * * * root /opt/astramicro/deploy/auto-update.sh >> /var/log/astramicro-auto-update.log 2>&1
# Переменные: BRANCH=main, APP_DIR задаётся относительно скрипта

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BRANCH="${BRANCH:-main}"
LOG_TAG="[astramicro-auto-update]"

cd "$APP_DIR" || exit 0

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "$LOG_TAG $(date -Iseconds) Не git-репозиторий, выход." >&2
  exit 0
fi

git fetch origin 2>/dev/null || exit 0

LOCAL=$(git rev-parse HEAD 2>/dev/null)
REMOTE=$(git rev-parse "origin/$BRANCH" 2>/dev/null)

if [ -z "$LOCAL" ] || [ -z "$REMOTE" ]; then
  exit 0
fi

if [ "$LOCAL" = "$REMOTE" ]; then
  exit 0
fi

echo "$LOG_TAG $(date -Iseconds) Найдены новые коммиты (origin/$BRANCH), запуск update.sh..."
if "$SCRIPT_DIR/update.sh"; then
  echo "$LOG_TAG $(date -Iseconds) Обновление завершено успешно."
else
  echo "$LOG_TAG $(date -Iseconds) Обновление завершилось с ошибкой (код $?)." >&2
  exit 1
fi
