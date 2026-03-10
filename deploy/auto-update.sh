#!/bin/bash
# Проверяет, есть ли новые коммиты на origin. Если да — запускает update.sh (pull, build, restart).
# Для cron: */5 * * * * root /opt/astramicro/deploy/auto-update.sh >> /var/log/astramicro-auto-update.log 2>&1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BRANCH="${BRANCH:-main}"
LOG_TAG="[astramicro-auto-update]"

cd "$APP_DIR" || exit 0

# Обновить ссылки без слияния
git fetch origin 2>/dev/null || exit 0

LOCAL=$(git rev-parse HEAD 2>/dev/null)
REMOTE=$(git rev-parse "origin/$BRANCH" 2>/dev/null)

if [ -z "$LOCAL" ] || [ -z "$REMOTE" ]; then
  exit 0
fi

if [ "$LOCAL" = "$REMOTE" ]; then
  # Нет новых коммитов
  exit 0
fi

echo "$LOG_TAG $(date -Iseconds) Обнаружены новые коммиты (origin/$BRANCH), запуск обновления..."
"$SCRIPT_DIR/update.sh"
echo "$LOG_TAG $(date -Iseconds) Обновление завершено."
