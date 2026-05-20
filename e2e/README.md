# E2E и визуальные тесты (Playwright)

Локальные автотесты со **скриншотами по разделам** для фиксации внешнего вида сайта.

| Модуль | Что снимаем | Нужен сервер |
|--------|-------------|--------------|
| **module2** | Главная, музей, карта, маршруты, магазин, профиль, заказы и др. | Vite поднимается автоматически |
| **astrakhan-admin** | `/login` | Spring Boot `:8080` |
| **module3** | `/login` | Flask `:5000` |

Java API для module2 **мокается** в тестах — Spring Boot для скриншотов module2 не обязателен.

## Быстрый старт

```bash
cd e2e
npm install
npm run install:browsers
npm run test:module2
```

Первый прогон создаёт эталонные PNG в `e2e/module2/__screenshots__/module2-desktop/`.  
При изменении вёрстки обновите эталоны:

```bash
npm run test:update:module2
```

Интерактивный режим:

```bash
npm run test:ui
```

## Все модули

```bash
# module2 (по умолчанию поднимает npm run dev в ../module2)
npm run test:module2

# admin + module3 (пропускаются, если сервер не отвечает)
cd ../astrakhan-admin && mvn spring-boot:run
cd ../module3 && flask run
cd ../e2e && npm test
```

Переменные окружения:

| Переменная | По умолчанию | Назначение |
|------------|--------------|------------|
| `MODULE2_BASE_URL` | `http://127.0.0.1:5173` | URL Vite |
| `SKIP_MODULE2_SERVER` | — | `1` — не запускать dev-сервер (уже запущен вручную) |
| `ADMIN_BASE_URL` | `http://127.0.0.1:8080` | Spring Admin |
| `MODULE3_BASE_URL` | `http://127.0.0.1:5000` | Flask |

## Карта и внешние ресурсы

- **Карта** (`/map`): область `.map-wrapper` маскируется (тайлы 2GIS нестабильны).
- **Внешние изображения** (Wikimedia и т.п.) блокируются для стабильности pixel-diff.
- Для главной допускается чуть больший `maxDiffPixelRatio` (0.04).

## CI

Workflow `.github/workflows/e2e-screenshots.yml` — только module2 (без Java/Flask).
