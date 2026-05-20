#!/bin/bash
# Проверка цепочки API: Java :8080 → nginx → JSON маршруты/товары.
# root: bash /opt/astramicro/deploy/test-api.sh

set +e

HOST="${1:-127.0.0.1}"
PUBLIC="${2:-http://193.233.49.59}"

echo "=============================================="
echo " Тест API (маршруты + товары)"
echo "=============================================="

check() {
  local label="$1"
  local url="$2"
  local code body
  body=$(curl -sS -m 15 -w "\n%{http_code}" "$url" 2>/dev/null)
  code=$(echo "$body" | tail -n1)
  body=$(echo "$body" | sed '$d')
  echo ""
  echo "--- $label ---"
  echo "URL: $url"
  echo "HTTP: $code"
  if [ "$code" = "200" ]; then
    if echo "$body" | grep -q '"status"[[:space:]]*:[[:space:]]*"success"'; then
      echo "OK: JSON success"
    elif echo "$body" | head -c 1 | grep -q '{'; then
      status=$(echo "$body" | sed -n 's/.*"status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
      echo "status в JSON: ${status:-?}"
      if echo "$body" | grep -q '"data"'; then
        count=$(echo "$body" | grep -o '"id"' | wc -l)
        echo "элементов (по полю id): ~$count"
      fi
      echo "начало ответа: $(echo "$body" | head -c 120)..."
    else
      echo "ВНИМАНИЕ: не JSON (HTML/ошибка nginx?)"
      echo "$(echo "$body" | head -c 200)"
    fi
  elif [ "$code" = "502" ] || [ "$code" = "000" ]; then
    echo "ОШИБКА: Java не отвечает (:8080 не слушается)."
    echo "  bash deploy/debug-java-startup.sh   ← причина в логах"
    echo "  bash deploy/start-java.sh"
  elif [ "$code" = "404" ]; then
    echo "ОШИБКА: 404 — неверный путь. Prod: context-path=/java-api, URL должен быть /java-api/api/v1/..."
    echo "  nginx: без rewrite префикса /java-api"
  else
    echo "тело: $(echo "$body" | head -c 200)"
  fi
}

echo ""
echo "[0] Порты"
ss -tlnp 2>/dev/null | grep -E ':8080|:80 ' || echo "  (ss недоступен)"

check "Java login" "http://${HOST}:8080/java-api/login"
check "Java routes (напрямую)" "http://${HOST}:8080/java-api/api/v1/routes?published=true"
check "Java products (напрямую)" "http://${HOST}:8080/java-api/api/v1/products"
check "Nginx routes" "http://${HOST}/java-api/api/v1/routes?published=true"
check "Nginx products" "http://${HOST}/java-api/api/v1/products"

if [ "$PUBLIC" != "skip" ]; then
  check "Публичный IP routes" "${PUBLIC}/java-api/api/v1/routes?published=true"
fi

echo ""
echo "Ожидаемая цепочка:"
echo "  Браузер → GET /java-api/api/v1/routes?published=true"
echo "  nginx   → proxy_pass http://127.0.0.1:8080 (путь без изменений)"
echo "  Spring  → @GetMapping /api/v1/routes + context-path /java-api"
echo "  Ответ   → {\"status\":\"success\",\"data\":[...]}"
