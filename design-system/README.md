# Единый стиль «Астрахань — Живая История»

Файл **astra.css** — общая дизайн-система для всего приложения:

- **module2** (Vue) — стили уже в `src/assets/styles/main.css`; токены совпадают с astra.css.
- **astrakhan-admin** (Java/Thymeleaf) — подключается `static/css/astra.css` + шрифты; body с классом `astra-app`.
- **module3** (Flask) — подключается `static/css/astra.css`; в `templates/base.html` body с классом `astra-app`.

При изменении цветов, шрифтов или компонентов править **design-system/astra.css**, затем скопировать в:

- `astrakhan-admin/src/main/resources/static/css/astra.css`
- `module2` не копирует — там свой main.css, синхронизировать переменные вручную при необходимости.

Шрифты: **Playfair Display** (заголовки), **IBM Plex Mono** (текст).  
Цвета: тёмный фон `--ink`, светлый текст `--paper`, акцент `--accent`.
