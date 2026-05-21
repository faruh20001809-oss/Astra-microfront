# E2E и визуальные тесты (Playwright)

| Модуль | Документация | Сервер в тестах |
|--------|--------------|-----------------|
| **module2** (витрина) | ниже | Vite `dev:e2e` |
| **astrakhan-admin** (ВКР, инструменты сотрудника) | [astrakhan-admin/README.md](astrakhan-admin/README.md) | Spring Boot профиль `e2e` |
| **module3** | ниже | Flask (опционально) |

## module2 — пользовательский сайт

```bash
cd e2e
npm install && npm run install:browsers
npm run test:module2
npm run test:update:module2   # обновить эталоны
```

Скриншоты: `e2e/module2/__screenshots__/module2-desktop/*.png`

## astrakhan-admin — для диплома (Косоротикова)

```bash
npm run test:admin
npm run test:update:admin
```

**Скриншоты для вставки в ВКР:** `e2e/astrakhan-admin/__screenshots__/diploma/`

Полный набор UI: `e2e/astrakhan-admin/__screenshots__/desktop/`

## module3

Тест `/login` — только если Flask на `:5000`.

## Переменные окружения

| Переменная | По умолчанию |
|------------|--------------|
| `MODULE2_BASE_URL` | `http://127.0.0.1:5173` |
| `ADMIN_BASE_URL` | `http://127.0.0.1:8080` |
| `SKIP_MODULE2_SERVER` | `1` — не поднимать Vite |
| `SKIP_ADMIN_SERVER` | `1` — не поднимать Spring |
| `ADMIN_E2E_USER` / `ADMIN_E2E_PASSWORD` | `admin` / `admin` |

## CI

- `e2e-screenshots.yml` — module2
- Для admin в CI нужен Java 17 + Maven (локально: `npm run test:admin`)
