# Unit test cases по модулям

## module2 (Vue frontend)

Основные кейсы:

1. Корректный подсчет прогресса:
   - инкремент paid/free счетчиков только при уникальном `routeId`;
   - повторное прохождение одного маршрута не увеличивает счетчики.
2. Начисление наград:
   - при достижении порога `2 paid + 3 free` выдается 1 награда;
   - прогресс награды (`eligible`, `progressPercent`) соответствует счетчикам.
3. Списание наград:
   - `consumeReward(routeId)` переносит награду из `available` в `history`;
   - фиксируется `usedForRouteId`.

Реализованные юнит-тесты:

- `module2/src/composables/useGuestProgress.test.js`
- `module2/src/api/backend.test.js`

## astrakhan-admin (Spring Boot backend)

Основные кейсы:

1. `RouteProgressService`:
   - `markCompleted` сохраняет событие только для нового прохождения;
   - `redeemReward` запрещен для бесплатного маршрута;
   - `redeemReward` разрешен при наличии доступных наград.
2. `UserProgressService`:
   - неизвестный пользователь возвращает пустой snapshot;
   - поврежденный JSON snapshot не ломает ответ (graceful fallback);
   - `upsertSnapshot` нормализует email и сохраняет JSON;
   - невалидный email отклоняется с ошибкой.
3. `GuestOrderLookupService`:
   - код не отправляется, если заказов по email нет;
   - повторный запрос кода чаще 1 раза/мин отклоняется;
   - успешная верификация кода возвращает список заказов;
   - неверный код отклоняется с `LookupAuthException`.

Реализованные юнит-тесты:

- `astrakhan-admin/src/test/java/ru/astrakhan/admin/service/RouteProgressServiceTest.java`
- `astrakhan-admin/src/test/java/ru/astrakhan/admin/service/UserProgressServiceTest.java`
- `astrakhan-admin/src/test/java/ru/astrakhan/admin/service/GuestOrderLookupServiceTest.java`

## module3 (Flask analytics)

Основные кейсы:

1. Парсинг latency:
   - приоритет поля `"duration_ms"` из JSON payload;
   - fallback на шаблон `NNN ms`;
   - отсутствие тайминга возвращает `None`.
2. Расчет перцентилей:
   - пустой набор значений возвращает `None`;
   - интерполяция p50/p95 выполняется корректно.
3. Telegram-уведомления (утилиты форматирования):
   - корректный маппинг внутренних статусов в человекочитаемые подписи;
   - формат сообщения содержит номер заказа, статус, трек и сумму.

Реализованные юнит-тесты:

- `module3/test_metrics_utils.py`
- `module3/test_telegram_utils.py`

## Команды запуска

- `module2`: `npm run test:run`
- `astrakhan-admin`: `mvn test`
- `module3`: `pytest`
