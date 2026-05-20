# Окружение на продакшене (Debian): единая схема для всех приложений

Один каталог конфигурации на сервере, одинаковые правила прав и подключения. Код приложений по-прежнему лежит в `/var/www/…` или `/opt/…`, а **секреты и env — только в `/etc/astra/`** (не в git).

Ниже команды **без `sudo`** — рассчитаны на сессию **root**. Если вы не root, добавьте **`sudo`** перед записями в `/etc`, вызовами `systemctl`, `nginx` и т.п.

## Принципы (общие для всех)

| Правило | Зачем |
|--------|--------|
| Каталог **`/etc/astra/`** | Одно место для всех env-файлов стека Astra |
| Права **`640`** или **`600`**, группа **`astra`** (или пользователь деплоя) | Чтение только сервисами, не мир |
| **systemd:** `EnvironmentFile=-/etc/astra/…`** | Одинаковый способ подать переменные Node, Flask, Spring |
| **Vite:** переменные **`VITE_*` только на этапе сборки** | Их нельзя «подменить» одним runtime-файлом для уже собранного `dist/` — см. ниже |

Создание каталога и группы (один раз, от root):

```bash
groupadd -f astra
mkdir -p /etc/astra
chown root:astra /etc/astra
chmod 750 /etc/astra
# пользователей сервисов добавьте в группу astra при необходимости:
# usermod -aG astra deploy
```

Дальше **все секреты правьте в `/etc/astra/`**, а приложения подключайте симлинками или через `EnvironmentFile` (см. таблицу).

## Файлы в `/etc/astra/` — что за что отвечает

| Файл (каноническое имя) | Приложение | Как подключается |
|-------------------------|------------|------------------|
| `module2.vite.env` | Vue / Vite (сборка `module2`) | Симлинк → `module2/.env.production` **или** копирование перед `npm run build` |
| `module2.server.env` | Node (`module2/server`) | `EnvironmentFile=/etc/astra/module2.server.env` в systemd |
| `module3.env` | Flask (`module3`) | `EnvironmentFile=/etc/astra/module3.env` в gunicorn/systemd |
| `admin.env` | Spring Boot (astrakhan-admin) | `EnvironmentFile=/etc/astra/admin.env` в systemd **или** `--spring.config.additional-location` на `.properties` — см. ниже |

Имена файлов можно не менять — так проще искать в бэкапах и документации.

### Пример наполнения (подставьте свои значения)

Ниже для каждого файла одинаковый шаблон: **создать файл** → **правка** → **содержимое**. От root:

```bash
install -m 640 -g astra /dev/null /etc/astra/ИМЯ_ФАЙЛА
nano /etc/astra/ИМЯ_ФАЙЛА
```

(Если не root: `sudo install …`, `sudo nano …`.)

---

**`/etc/astra/module2.vite.env`** (Vite, только `KEY=VALUE` и `#`):

```bash
install -m 640 -g astra /dev/null /etc/astra/module2.vite.env
nano /etc/astra/module2.vite.env
```

```dotenv
VITE_JAVA_API_BASE=/java-api/api/v1
# Тестовый стенд (MapGL + Static): key e9f7375c-7ad1-4258-854a-399d99eb65cb, style ниже
VITE_DGIS_MAP={"key":"e9f7375c-7ad1-4258-854a-399d99eb65cb","style":"0651ff51-79b6-409c-8b90-37a9be2e97ad"}
# Прод: подставьте свой ключ(и); при отдельном Static — добавьте "staticKey":"…"
# либо отдельно: VITE_DGIS_STATIC_KEY=ВАШ_STATIC_KEY
# Ключ MapGL без продукта «Static API» даёт 403 на static.maps.2gis.com — превью в /virtual-museum не загрузятся
VITE_DGIS_ROUTING_KEY=ВАШ_ROUTING_KEY
# VITE_DGIS_ROUTING_BASE=/api/dgis-routing
# VITE_DGIS_STATIC_BASE=/api/dgis-static
VITE_AI_API_KEY=ВАШ_LLM_ИЛИ_OPENROUTER_КЛЮЧ
```

---

**`/etc/astra/module2.server.env`** (Node `module2/server`):

```bash
install -m 640 -g astra /dev/null /etc/astra/module2.server.env
nano /etc/astra/module2.server.env
```

```dotenv
PORT=3001
GROQ_API_KEY=ВАШ_GROQ_API_KEY
```

---

**`/etc/astra/module3.env`** (Flask; `DATABASE_URL` и `SECRET_KEY` читает `module3/app.py`):

```bash
install -m 640 -g astra /dev/null /etc/astra/module3.env
nano /etc/astra/module3.env
```

```dotenv
FLASK_ENV=production
SECRET_KEY=СГЕНЕРИРУЙТЕ_openssl_rand_-hex_32
DATABASE_URL=postgresql://postgres:ПАРОЛЬ@localhost:5432/museum_user?client_encoding=UTF8&connect_timeout=10
```

Сгенерировать `SECRET_KEY` на сервере: `openssl rand -hex 32` (вставьте строку в файл вручную, не через историю команд с паролем).

---

**`/etc/astra/admin.env`** (Spring Boot, профиль `prod`; см. `application-prod.properties`). Готовый шаблон без секретов: **`deploy/astra-admin.env.example`** → скопируйте в `/etc/astra/admin.env` и заполните. Если на сервере файла нет (`cannot stat`), обновите репозиторий (`git pull`) или создайте **`/etc/astra/admin.env`** через `nano` и перенесите переменные из примера вручную.

```bash
install -m 640 -g astra /dev/null /etc/astra/admin.env
nano /etc/astra/admin.env
```

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/museum_user
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=ПАРОЛЬ_ОТ_POSTGRES
DGIS_MAP_KEY=e9f7375c-7ad1-4258-854a-399d99eb65cb
APP_MODULE2_URL=https://ваш-домен
APP_API_PUBLIC_BASE_URL=https://ваш-домен/java-api
APP_ADMIN_URL=https://ваш-домен/admin
```

Имена `SPRING_*` и `DGIS_MAP_KEY` соответствуют `application-prod.properties` / `application.properties` в `astrakhan-admin`. Дополнительно: почта (`SPRING_MAIL_*`), Telegram (`APP_TELEGRAM_*`, `TELEGRAM_INTERNAL_TOKEN`) — по необходимости см. тот же файл.

Если нужны только свойства в формате `.properties`, см. альтернативу ниже и [relaxed binding](https://docs.spring.io/spring-boot/reference/features/external-config.html) Spring Boot.

Альтернатива для Java только: **`/etc/astra/admin-production.properties`** (тот же смысл, что и раньше `application-production.properties`), тогда в unit:

```ini
ExecStart=/usr/bin/java -jar /opt/astra-admin/app.jar --spring.profiles.active=prod --spring.config.additional-location=file:/etc/astra/admin-production.properties
```

## Связка с каталогами приложений (симлинки)

Корень деплоя примера: `/var/www/astra`. Подстройте под себя.

```bash
# Vite: один источник — /etc/astra/module2.vite.env
ln -sf /etc/astra/module2.vite.env /var/www/astra/module2/.env.production
```

Сборка фронта — **только из каталога `module2`**, не из `/root` и не из корня репозитория:

```bash
cd /var/www/astra/module2   # подставьте реальный путь к module2 на сервере
pwd
ls package.json package-lock.json
npm ci && npm run build
```

- **`npm ci`** требует **`package-lock.json`** в этом каталоге. Если на сервере его нет (скопировали без lockfile), либо добавьте файл из репозитория, либо один раз: **`npm install`** и затем **`npm run build`**.
- Ошибка `Could not read package.json` в `/root` значит, что **`cd` в `module2` не выполняли** — сначала перейдите в каталог проекта.

После **любого** изменения `module2.vite.env` нужна **повторная** `npm run build`.

### Карта не работает, хотя `module2.vite.env` заполнен

Переменные **`VITE_*` не читаются браузером с сервера в рантайме** — они **вшиваются в JS при `npm run build`**. Пока вы не пересобрали фронт и nginx не отдаёт **новый** `dist/`, правки только в `/etc/astra/module2.vite.env` **никак не влияют** на сайт.

**Сделайте по порядку:**

1. **Симлинк** из каталога сборки на общий env-файл (путь к `module2` замените на свой):

   ```bash
   ln -sf /etc/astra/module2.vite.env /var/www/astra/module2/.env.production
   ls -la /var/www/astra/module2/.env.production
   ```

   Должно указывать на `/etc/astra/module2.vite.env`. Если собираете не из `/var/www/astra/module2`, симлинк и `npm run build` нужно выполнять **в том же каталоге**, откуда реально собираете.

2. **Сборка в режиме production** (из этого же `module2`):

   ```bash
   cd /var/www/astra/module2
   npm ci
   npm run build
   ```

3. **Nginx** должен отдавать статику из **`…/module2/dist`** именно этой сборки (не старый каталог и не кэш другого релиза).

4. **Формат `VITE_DGIS_MAP`:** одна строка, валидный JSON **без** обёртки в одинарные кавычки целиком. Пример:

   `VITE_DGIS_MAP={"key":"ВАШ_UUID","style":"0651ff51-79b6-409c-8b90-37a9be2e97ad"}`  

   Если JSON невалиден, в коде сработает запасной ключ по умолчанию — карта может вести себя непредсказуемо. Проверка: после сборки в минифицированном бандле должна встречаться подстрока вашего ключа (поиск по `dist/assets/*.js`).

5. **Маршруты на карте** (построение пути) используют отдельный ключ **`VITE_DGIS_ROUTING_KEY`** и при CORS в проде часто нужен прокси **`VITE_DGIS_ROUTING_BASE=/api/dgis-routing`** в том же файле **и снова пересборка**; в nginx должен быть `location` с проксированием на `https://routing.api.2gis.com` (как в комментариях к `vite.config.js`).

6. В браузере: жёсткое обновление (Ctrl+F5) или очистка кэша — чтобы не подтягивался старый `index.html` / chunk.

### После билда карта всё равно не работает

Разделите проблему на три проверки: **бандл**, **nginx**, **2GIS / браузер**.

**1) Убедиться, что ключ из env реально попал в сборку** (на сервере, из каталога `module2` после `npm run build`):

```bash
# подставьте несколько первых символов своего ключа MapGL (из VITE_DGIS_MAP), без публикации полного ключа:
grep -l 'ВАШ_ПРЕФИКС_КЛЮЧА' dist/assets/*.js
```

Если совпадений **нет** — сборка шла **без** актуального `.env.production` (нет симлинка, другой каталог `cd`, или env не читается). Исправьте симлинк и пересоберите.

**2) Nginx отдаёт именно эту папку `dist`**

- `root` в `server` должен указывать на каталог, где лежит свежий `dist` после сборки.
- После копирования/сборки выполните `nginx -t && systemctl reload nginx`.
- Для SPA нужен fallback на `index.html`, например: `location / { try_files $uri $uri/ /index.html; }` (иначе при прямом заходе на вложенный URL может быть 404, но главная с картой обычно открывается).

**3) Сайт открывается не с корня домена** (`https://example.com/app/` вместо `https://example.com/`)

Сборка по умолчанию кладёт ассеты в `/assets/...`. Если приложение живёт **под префиксом**, в `module2/vite.config.js` нужно задать `base: '/app/'` (ваш префикс), затем снова **`npm run build`** и в nginx — тот же префикс. Иначе бандл не загрузится (пустой экран или ошибки в консоли).

**4) Ключ 2GIS и домен продакшена**

В [личном кабинете 2GIS / настройках ключа MapGL](https://docs.2gis.com/) ключ часто **привязан к списку доменов или origin**. Если прод открывается по **новому домену или по IP**, а в кабинете указан только `localhost` или другой хост — тайлы или инициализация карты могут **не работать**, хотя локально всё ок. Добавьте в разрешённые **фактический домен** (и при необходимости IP) и сохраните настройки.

**5) Браузер: вкладка «Сеть» и «Консоль»**

- Запрос к `https://mapgl.2gis.com/api/js/v1` должен быть **успешным** (не блокируется расширением, корпоративным прокси, жёстким CSP в nginx).
- В консоли ищите `Failed to init 2GIS map` — по тексту ошибки видно, ключ неверен, квота и т.д.

Node, Flask и Spring **не** требуют симлинков, если в unit-файлах указан прямой путь `EnvironmentFile=/etc/astra/…`.

## Единый фрагмент systemd (шаблон)

Под каждый сервис — свой unit, но блок `[Service]` одинаковый по стилю:

```ini
[Service]
User=deploy
Group=astra
UMask=027
EnvironmentFile=-/etc/astra/ИМЯ_ФАЙЛА.env
WorkingDirectory=/путь/к/приложению
```

Примеры имён файлов: `module2.server.env`, `module3.env`, `admin.env`.

Применение:

```bash
systemctl daemon-reload
systemctl restart astra-node astra-flask astra-admin   # ваши имена юнитов
```

## Общие секреты (один ключ в нескольких местах)

Ключ карты 2GIS и пароль БД часто нужны и фронту (Vite), и бэкенду (Spring). В **одном** физическом файле их держать нельзя в разном формате (JSON в `VITE_DGIS_MAP` vs `APP_DGIS_MAP_KEY`), поэтому:

- правило **«истина в `/etc/astra/`»**: вы копируете **одно и то же значение** в `module2.vite.env` и в `admin.env` при смене ключа;
- либо автоматизируете выкладку скриптом деплоя, который подставляет значения из одного внутреннего шаблона (не храните этот шаблон с секретами в git).

## Смена ключа 2GIS (чеклист)

После выпуска **нового** ключа в кабинете 2GIS обновите **все** места — иначе в URL/карте останется старый UUID:

1. **`/etc/astra/module2.vite.env`** — `VITE_DGIS_MAP`, при необходимости `VITE_DGIS_STATIC_KEY` / `staticKey` в JSON → **`npm run build`** и выкладка `module2/dist`.
2. **`/etc/astrakhan-admin.env`** — `DGIS_MAP_KEY=…` → **`systemctl restart astrakhan-admin`** (пересборка фронта не нужна).
3. **Браузер** — очистить sessionStorage (ключи `astra-cache-static-*`, `astra-cache-pois-*`) или жёсткое обновление, иначе превью карт хранят старые URL с `key=` 24 ч.

В собранном `dist/assets/*.js` можно проверить, какой ключ попал в бандл: `grep -o 'key=[a-f0-9-]*' dist/assets/*.js` (не публикуйте вывод).

## Astrakhan Admin (Spring): ключ карты `DGIS_MAP_KEY`

Фронт берёт ключ из **`VITE_DGIS_MAP`** (после сборки). **Java-админка** использует **отдельную** переменную окружения **`DGIS_MAP_KEY`** (один UUID, как поле `key` в JSON на фронте). Это не Vite: **пересборка не нужна**, нужен **рестарт** процесса Java после смены env.

В `application.properties` / `application-prod.properties`: `app.dgis.map-key=${DGIS_MAP_KEY:…}`.

**Частая ошибка:** unit systemd из репозитория (`deploy/astrakhan-admin.service`) по умолчанию подключает **`/etc/astrakhan-admin.env`**. Если вы заполнили только **`/etc/astra/admin.env`**, Spring **не увидит** переменные, пока не сделаете одно из:

- добавьте в unit вторую строку `EnvironmentFile=-/etc/astra/admin.env` и `daemon-reload` + `restart` (актуальный шаблон в `deploy/astrakhan-admin.service` уже содержит оба файла); или  
- продублируйте строку `DGIS_MAP_KEY=…` в **`/etc/astrakhan-admin.env`**.

Проверка, что ключ попал в процесс (после рестарта):

```bash
systemctl show astrakhan-admin -p Environment --value | tr ' ' '\n' | grep DGIS
# или:
pid=$(pgrep -f history-admin); tr '\0' '\n' < /proc/$pid/environ | grep DGIS_MAP_KEY
```

## Nginx

`root` → собранный `module2/dist`; префиксы `/java-api` и при необходимости `/api/dgis-routing` — как в вашей dev-конфигурации.

## Проверка

```bash
ls -la /etc/astra/
# ожидается: доступ у root и группы astra, без world-readable для секретов
```

Чеклист после выкладки:

- [ ] Карта и маршруты 2GIS после сборки с актуальным `module2.vite.env`
- [ ] Java API доступен по пути из `VITE_JAVA_API_BASE`
- [ ] `systemctl is-active` для всех юнитов стека

## Тема в UI

Тема (светлая по умолчанию) задаётся в браузере (`localStorage['astra-theme']`), не через env на сервере. На **одном origin** настройки совпадают между фронтами.
