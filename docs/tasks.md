# Отложенные и отдельные задачи

## Массовая загрузка точек и маршрутов (реализовано)

В админке есть кнопка **«Массовая загрузка точек/маршрутов»** на странице списка точек интереса (`/admin/poi`).

- **Страница:** `/admin/poi/bulk`
- **Формат JSON:** `{ "pois": [ { "name", "category", "latitude", "longitude", "description?", "address?", "foundedYear?", "architect?" } ], "routes": [ { "name", "description", "category", "poiOrder": [0, 1, 2] } ] }`
- Точки создаются как черновики **без картинок**; маршруты — с `poiOrder` (индексы в массиве `pois`). После загрузки картинки добавляются вручную в карточках точек.
- **Код:** `astrakhan-admin/.../PoiAdminController.java` (методы `bulkForm`, `bulkUpload`), шаблон `poi/bulk.html`.

---

## Редизайн корзины + пошаговая миграция на TypeScript

**Цель:** улучшить UI/UX корзины и перевести фронт (module2) на TypeScript пошагово, по файлам.

**Что входит:**
- Полный редизайн корзины: визуал, пустое состояние, карточки товаров, форма оформления, итоги и кнопка «Оформить заказ».
- Пошаговая миграция на TypeScript:
  1. Добавить `tsconfig.json` и поддержку `.ts`/`.vue` с типами в module2 (Vite уже поддерживает TS).
  2. Переводить по одному модулю: например сначала `store/index.js` → `store/index.ts` (типы для cart, map, toast), затем `api/backend.js` → `api/backend.ts`, потом компоненты (например `CartDrawer.vue` с `<script setup lang="ts">`).
  3. Включить строгую проверку типов по мере перевода файлов, не ломая текущую сборку.

**Не входит в текущий спринт:** задача вынесена отдельно; корзина уже доработана (persist в localStorage, текущий UI сохранён).

**Где код:** `module2/src/store/index.js` (cart), `module2/src/components/shop/CartDrawer.vue`, `module2/src/api/backend.js`.
