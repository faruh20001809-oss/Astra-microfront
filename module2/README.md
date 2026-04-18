# Модуль 2 — Астрахань. Живая История
**Vue 3 + Node.js frontend/backend**

**Тема оформления:** по умолчанию **светлая** (`astra-theme` в `localStorage`, при отсутствии ключа — светлая). Переключатель в шапке (и в бургер-меню на мобильных). Тот же ключ `astra-theme` используют module3 и Spring Admin.

## Архитектура

```
module2/
├── index.html
├── package.json
├── vite.config.js
├── public/
│   └── favicon.svg
├── src/
│   ├── main.js                             # Точка входа Vue
│   ├── App.vue                             # Корневой компонент + тосты
│   ├── api/
│   │   ├── groq.js                         # Groq LLM + TTS клиент
│   │   └── backend.js                      # API-хелперы (Java + Node)
│   ├── assets/
│   │   └── styles/
│   │       └── main.css                    # Design system (ч/б палитра)
│   ├── router/
│   │   └── index.js                        # Vue Router (4 раздела)
│   ├── store/
│   │   └── index.js                        # Pinia: корзина, карта, тосты
│   ├── views/
│   │   ├── MapView.vue                     # ★ Карта 2GIS + GPS + AI + TTS
│   │   ├── RoutesView.vue                  # Онлайн-маршруты
│   │   ├── ShopView.vue                    # Магазин мерча
│   │   └── ContactView.vue                 # Контакты + донейшн
│   └── components/
│       ├── layout/
│       │   └── AppHeader.vue               # Навигация + бургер-меню
│       ├── routes/
│       │   └── RouteCard.vue               # Карточка маршрута
│       └── shop/
│           ├── ProductCard.vue             # Карточка товара
│           └── CartDrawer.vue              # Слайд-корзина
└── server/
    ├── package.json
    └── index.js                            # Express: Groq proxy + REST API
```

## Запуск

### 1. Установка зависимостей
```bash
# Фронтенд
npm install

# Сервер
cd server && npm install && cd ..
```

### 2. Переменные окружения (опционально)
```bash
# server/.env
GROQ_API_KEY=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
PORT=3001
```

Фронт (корень `module2/`): скопируйте **`.env.example` → `.env.local`** и заполните ключи (`.env.local` в `.gitignore` не попадает в репозиторий).

| Переменная | Назначение |
|------------|------------|
| `VITE_DGIS_MAP` | JSON `{"key":"…","style":"…"}` — ключ MapGL и стиль карты 2GIS |
| `VITE_DGIS_ROUTING_KEY` | Ключ **Routing / Directions API** 2GIS (если не задан — подставляется `key` из `VITE_DGIS_MAP`) |
| `VITE_DGIS_ROUTING_BASE` | Базовый URL API маршрутизации (по умолчанию `https://routing.api.2gis.com`). При CORS в dev: `VITE_DGIS_ROUTING_BASE=/api/dgis-routing` — запрос пойдёт через прокси Vite |
| `VITE_DGIS_ROUTING_TRANSPORT` | `walking` (по умолчанию) или `driving` и др. — см. документацию 2GIS Routing |

Маршрут с каталога (`?route=id`) на карте строится по **цепочке POI**: запросы к Routing API по сегментам между соседними точками, линия по дорогам/тропам; при ошибке — прямая линия между точками.

### 3. Запуск в dev-режиме (два терминала)
```bash
# Терминал 1 — Node.js сервер
cd server && npm run dev

# Терминал 2 — Vue фронтенд
npm run dev
```

Фронтенд: http://localhost:5173  
API сервер: http://localhost:3001

### 4. Сборка для продакшна
```bash
npm run build
```

## Интеграция с Java (Модуль 1)

Vite proxy в `vite.config.js` настроен перенаправлять `/java-api/*` → `http://localhost:8080/*`

Ожидаемые эндпоинты Java:

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/api/v1/pois?status=PUBLISHED` | Точки интереса для карты |
| GET | `/api/v1/routes?published=true` | Маршруты |
| GET | `/api/v1/products` | Товары магазина |
| POST | `/api/v1/orders` | Создание заказа |
| POST | `/api/v1/feedback` | Контактная форма |

При недоступности Java API — автоматически используются mock-данные.

## Функциональность

### 🗺 Карта (MapView)
- Интерактивная карта 2GIS (MapGL API)
- Кастомные маркеры по категориям
- Фильтрация точек интереса
- Детальная карточка POI с вкладками
- **GPS-трекинг в реальном времени** — `navigator.geolocation.watchPosition`
- **Гео-триггеры** — автоматическое уведомление при приближении (150 м)
- **AI-генерация текста** (Groq LLM) в трёх стилях: обычный, детский, академический
- **TTS-озвучка** сгенерированного текста (Groq Audio)

### 🚶 Маршруты (RoutesView)
- Получение от Java или mock-данные
- Фильтры по категории и цене
- Модальное окно с остановками и таймлайном

### 🛒 Магазин (ShopView)
- Каталог товаров с фильтрами
- Корзина (Pinia store) — добавление, редактирование кол-ва
- Slide-out Drawer корзины
- Оформление заказа с выбором доставки

### 📬 Контакты (ContactView)
- Контактная форма → Node.js → Java (fallback mock)
- Блок донейшна с выбором суммы

## Технологии

| Слой | Стек |
|------|------|
| Фреймворк | Vue 3 (Composition API) |
| Стейт | Pinia |
| Маршрутизация | Vue Router 4 |
| Карты | 2GIS MapGL API |
| AI/LLM | Groq (Llama 4) |
| TTS | Groq Audio (PlayAI) |
| Сервер | Node.js + Express |
| HTTP-клиент | Fetch API (native) |
| Сборка | Vite 5 |
