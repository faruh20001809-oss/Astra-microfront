# План перехода с email на Telegram для подтверждения и статусов заказа

Этот файл является **детальной спецификацией** к разделу `2.7` документа [thesis-declared-features-completion.md](./thesis-declared-features-completion.md).

Документ описывает **только проектные изменения** (без внесения кода), чтобы заменить/дополнить неработающую почту:

1) подтверждение аккаунта через Telegram-бота;  
2) отправка статусов заказа через Telegram.

---

## 1. Почему это нужно сейчас

- В текущей реализации `astrakhan-admin` подтверждение доступа к заказам (`/orders/lookup/request-code`) завязано на email.
- Уведомления по заказу (`accepted`, `tracking`) тоже отправляются через `OrderEmailService`.
- Если SMTP недоступен, цепочка уведомлений ломается полностью.

Telegram-канал можно использовать как основной или резервный канал доставки.

---

## 2. Что уже есть в проекте

## `module3/telegram_bot.py`

- Уже есть рабочий бот на `pyTelegramBotAPI`.
- Есть доступ к PostgreSQL, команды и инфраструктура polling.
- Сейчас бот заточен под отчеты/метрики, но его можно расширить под auth + order notifications.

## `astrakhan-admin`

- Есть сервисы и API для email-кодов и email-уведомлений:
  - `GuestOrderLookupService`
  - `OrderEmailService`
  - `ApiController` (`/orders/lookup/request-code`, `/orders/lookup/verify`)
  - `OrderAdminController` (смена статуса заказа)
- В `Order` уже есть `phone`, `email`, `orderId`, `status`, `trackingNumber`.

---

## 3. Целевая архитектура (рекомендуемая)

## 3.1. Основная идея

Разделить роли:

- **Java (`astrakhan-admin`)** — источник бизнес-событий, хранит токены привязки и подписки, решает кому/когда отправлять.
- **Telegram bot (`module3`)** — канал взаимодействия с пользователем и фактическая отправка сообщений.

## 3.2. Интеграционный слой

Ввести один внутренний контракт:

- Java вызывает endpoint бота (или Python-service), например:
  - `POST /internal/telegram/send`
  - `POST /internal/telegram/send-order-status`
- Бот проверяет service-token и отправляет сообщение в Telegram API.

Это избавляет от прямого HTTP в Telegram из Java и упрощает мониторинг доставки.

---

## 4. Подтверждение аккаунта через Telegram

## 4.1. Бизнес-сценарий

1. Пользователь в `module2` выбирает «Подтвердить через Telegram».  
2. Фронт запрашивает у Java одноразовый `linkCode` (или deep-link token).  
3. Пользователь нажимает ссылку вида:
   `https://t.me/<bot_username>?start=link_<token>`  
4. Бот получает `/start link_<token>`, валидирует токен через Java API, связывает `telegram_chat_id` с аккаунтом/контактом.
5. Java помечает канал Telegram как подтвержденный.

## 4.2. Что нужно хранить

Новая таблица (пример): `user_telegram_links`

- `id`
- `user_id` (nullable, если гостевой сценарий)
- `email` (nullable)
- `phone` (nullable)
- `telegram_chat_id` (bigint, unique)
- `telegram_username`
- `is_verified`
- `linked_at`
- `last_used_at`

Таблица токенов привязки: `telegram_link_tokens`

- `token_hash`
- `target_user_id/email/phone`
- `expires_at`
- `used_at`
- `created_at`

## 4.3. API в Java (план)

- `POST /api/v1/telegram/link/request`
  - вход: email/phone/user context
  - выход: `botDeepLink`, `expiresInSec`

- `POST /api/v1/internal/telegram/link/confirm`
  - вызывается ботом с `token`, `chatId`, `username`
  - Java валидирует и активирует связь

- `GET /api/v1/telegram/link/status`
  - фронт проверяет, привязан ли Telegram

## 4.4. Изменения на фронте `module2`

- В `OrdersView.vue` добавить альтернативный блок:
  - «Войти через Telegram»
  - кнопка «Открыть бота»
  - индикатор статуса привязки
- В `backend.js` добавить `javaApi.telegram.*` методы.

---

## 5. Уведомления о статусе заказа в Telegram

## 5.1. События отправки

Минимально:

- Заказ создан (`PROCESSING/CONFIRMED`)
- Заказ отправлен (`SHIPPED`) + трек-номер
- Заказ доставлен (`DELIVERED`)
- Заказ отменен (`CANCELLED`)

## 5.2. Где триггерить

- В `OrderAdminController.updateStatus(...)` после `orderService.save(order)`:
  - дергать `OrderNotificationService`, который:
    1) ищет привязанный `telegram_chat_id`,
    2) отправляет через bot-integration endpoint,
    3) логирует результат.

Дополнительно:

- При создании заказа (`ApiController.createOrder`) отправлять «Заказ принят».

## 5.3. Формат сообщений

- Кратко, структурно, с эмодзи:
  - `📦 Заказ ORD-XXXX`
  - `Статус: Отправлен`
  - `Трек-номер: 123...`
  - `Сумма: ...`
  - `Время: ...`

Ссылки:

- Ссылка на страницу заказа в `module2` (если доступна).

---

## 6. Что изменить в `module3/telegram_bot.py`

## 6.1. Новые команды/обработчики

- `/start link_<token>` — сценарий привязки.
- `/unlink` — отвязать чат от аккаунта.
- `/my_orders` (опционально) — быстрый просмотр последних статусов.

## 6.2. Входящие callback-и от Java

Добавить защищенный endpoint (если бот будет поднят как web service, а не только polling script):

- `POST /internal/telegram/send-order-status`

Если остается polling-only скрипт:

- вынести «sender service» в отдельный python http-модуль,
- `telegram_bot.py` оставить для пользовательских команд.

## 6.3. Обязательная безопасность

- Убрать хардкод токена из файла.
- Перенести:
  - `TELEGRAM_BOT_TOKEN`
  - DB URI
  - internal service token
  в переменные окружения.

---

## 7. Поэтапный план внедрения (синхронизировано с roadmap в thesis-документе)

## Этап 1 (быстрый, 1-2 дня)

- Добавить в Java таблицу связки `chat_id <-> email/phone`.
- Реализовать deep-link привязку через `/start link_<token>`.
- Реализовать отправку статуса `SHIPPED` в Telegram при наличии связки.

## Этап 2

- Добавить все статусы + шаблоны сообщений.
- Добавить UI в `module2` для привязки Telegram и статуса канала уведомлений.
- Добавить fallback: если Telegram не привязан, использовать email (если доступен).

## Этап 3

- История уведомлений и ретраи.
- Отписка/настройки уведомлений в ЛК.
- Мониторинг доставки и админ-диагностика.

---

## 8. Риски и как закрыть

- **Утечка bot token**: токен уже присутствует в коде — срочно ротировать через BotFather.
- **Фейковая привязка**: использовать одноразовые токены + TTL + hash-хранение.
- **Спам/флуд**: лимитировать частоту отправок и команд.
- **Неполная доставка**: логировать статусы отправки и retries.

---

## 9. Минимальный результат (Definition of Done)

- Пользователь может привязать Telegram в 2 клика через deep-link.
- При смене статуса заказа в админке приходит Telegram-уведомление.
- Почта становится необязательной для критичных уведомлений.
- Все чувствительные ключи вынесены в env, токен бота ротирован.

