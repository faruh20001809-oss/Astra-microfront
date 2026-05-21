# Astra-microfront

[![Unit Tests](https://github.com/SileverTM/Astra-microfront/actions/workflows/unit-tests.yml/badge.svg)](https://github.com/SileverTM/Astra-microfront/actions/workflows/unit-tests.yml)
[![Lint](https://github.com/SileverTM/Astra-microfront/actions/workflows/lint.yml/badge.svg)](https://github.com/SileverTM/Astra-microfront/actions/workflows/lint.yml)

Цифровая экосистема историко-туристического сервиса с пользовательским контуром, административной панелью и технической аналитикой.

Проект объединяет:

- клиентский путь пользователя (карта, POI, маршруты, магазин, заказы, личный кабинет, геймификация);
- операционный контур управления контентом и заказами;
- эксплуатационный контур мониторинга, метрик и алертинга.

## Архитектура

### `module2` — пользовательский фронтенд (Vue)

- интерактивная карта и каталог точек интереса (POI);
- маршруты (бесплатные/платные), карточки и прохождение;
- корзина/checkout и оформление заказа;
- вход в кабинет заказов по `email + одноразовый код`;
- прогресс, достижения и награды (геймификация), синхронизация с backend.

### `astrakhan-admin` — Java/Spring backend + админка

- CRUD для POI/маршрутов/товаров;
- обработка заказов и управление статусами;
- публичный API для фронтенда (`/api/v1`);
- серверный учет прогресса/наград и событий прохождения маршрутов;
- интеграции email/Telegram и аналитические события.

### `module3` — Flask тех-админка и аналитика

- управление пользователями и ролями;
- эксплуатационные метрики (`error rate`, `latency p50/p95/p99`, интеграции);
- бизнес-метрики (`funnel`) и контроль качества данных;
- алерты и история срезов.

## Ключевой функционал

- карта и карточки POI с обновлением каталога;
- витрина маршрутов и сценарии прохождения;
- магазин и заказ с трекингом статусов;
- кабинет гостя с историей заказов и таймлайнами;
- Telegram-привязка для уведомлений по заказам;
- геймификация: прогресс, достижения, награды;
- тех-дашборд с метриками и алертингом.

## API (укрупненно)

### Публичный API (`astrakhan-admin`, `/api/v1`)

- `POI`: `/pois`, `/pois/{id}`, `/pois/since`, `/pois/{id}/reviews`
- `Routes`: `/routes`, `/routes/{id}`, `/routes/{id}/complete`
- `Products`: `/products`, `/products/{id}`
- `Orders`: `/orders`, `/orders/{orderId}`, `/orders/lookup/*`, `/orders/{orderId}/payment*`
- `Progress/Rewards`: `/user-progress*`, `/rewards/redeem`
- `Integrations`: `/telegram/link/*`, `/feedback`, `/newsletter/*`, `/analytics/events`

### Технический API (`module3`, `/api`)

- auth/users/roles;
- staff management: `/api/staff` (создание профилей сотрудников тех-админом);
- metrics: `/api/metrics`, `/api/metrics/activity`, `/api/metrics/ops`, `/api/metrics/funnel`, `/api/metrics/data-quality`, `/api/metrics/alerts`.

## Единый источник пользователей

- единый источник профилей — общая таблица `users` для модулей;
- клиентские профили синхронизируются автоматически из клиентского контура (`module2` + `astrakhan-admin`) по email;
- профили сотрудников создаются только тех-администратором через `module3` endpoint `POST /api/staff`.

## Технологии

- frontend: `Vue 3`, `Vite`, `Pinia`, `PrimeVue`
- backend: `Java 17`, `Spring Boot`, `Thymeleaf`, `JPA`
- analytics/admin: `Python`, `Flask`, `SQLAlchemy`
- database: `PostgreSQL`
- integrations: `SMTP`, `Telegram Bot`

## Тесты и CI

Локальный запуск:

- `module2`: `npm run test:run` (Vitest, API-клиент)
- `astrakhan-admin`: `mvn test`
- `module3`: `python -m pytest -q`
- **E2E + скриншоты UI** (Playwright): [`e2e/README.md`](e2e/README.md)
  - витрина: `npm run test:module2`
  - **админка (ВКР):** `npm run test:admin` — скриншоты для диплома: `e2e/astrakhan-admin/__screenshots__/diploma/`

CI в GitHub Actions:

- `unit-tests.yml` — юнит-тесты всех модулей;
- `lint.yml` — проверки сборки/компиляции/синтаксиса;
- `e2e-screenshots.yml` — визуальные регрессионные тесты пользовательского сайта (module2).

## Документация проекта

- Полное описание архитектуры, БД, API, DFD/UML, сценариев и результатов: `diplom.md`
- Дополнительные рабочие спецификации и планы: директория `docs/`
