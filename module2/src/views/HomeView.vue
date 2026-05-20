<template>
  <div class="museum-home-page">

    <section class="museum-hero" aria-labelledby="museum-hero-title">
      <img
        class="museum-hero__image"
        src="https://commons.wikimedia.org/wiki/Special:FilePath/%D0%90%D1%81%D1%82%D1%80%D0%B0%D1%85%D0%B0%D0%BD%D1%8C._%D0%94%D0%BE%D0%BC_%D0%A2%D0%B5%D1%82%D1%8E%D1%88%D0%B8%D0%BD%D0%BE%D0%B2%D0%B0._%D0%A4%D0%B0%D1%81%D0%B0%D0%B4.JPG?width=1400"
        alt="Деревянная архитектура Астраханской области"
      />
      <div class="museum-hero__content">
        <h1 id="museum-hero-title">Виртуальный музей деревянной архитектуры Астраханской области</h1>
        <router-link class="museum-pill-link" to="/virtual-museum#museum-map">
          Перейти к карте →
        </router-link>
      </div>
    </section>

    <section class="museum-about" aria-labelledby="museum-about-title">
      <h2 id="museum-about-title">Почему деревяшки?</h2>
      <div class="museum-about__copy">
        <p>
          Астраханская область — уникальный регион, где сохранились редкие образцы деревянной архитектуры.
          Эти дома и их резные элементы рассказывают живую историю края.
        </p>
        <p>
          Сегодня деревянные дома массово сносятся в рамках программы переселения из аварийного жилья,
          и с каждым годом исчезают части архитектурного наследия.
        </p>
        <p>
          Поэтому был создан этот проект, чтобы сохранить уникальное деревянное зодчество региона
          в виртуальном и общедоступном формате.
        </p>
      </div>
    </section>

    <section class="museum-preview-section" aria-labelledby="museum-routes-title">
      <div class="museum-section-head">
        <h2 id="museum-routes-title">Онлайн-маршруты по городу</h2>
        <router-link class="museum-outline-link" to="/routes">Больше маршрутов →</router-link>
      </div>
      <div class="museum-card-row museum-card-row--routes">
        <article
          v-for="route in previewRoutes"
          :key="route.id ?? route.title"
          class="museum-preview-card museum-preview-card--dark"
        >
          <div class="museum-card-media">
            <img
              v-if="route.coverImage"
              :src="route.coverImage"
              :alt="route.title"
              loading="lazy"
            />
          </div>
          <p>{{ route.title }}</p>
          <div class="museum-card-foot">
            <span>{{ route.price }}</span>
            <router-link to="/routes">Подробнее →</router-link>
          </div>
        </article>
      </div>
    </section>

    <section class="museum-preview-section" aria-labelledby="museum-shop-title">
      <div class="museum-section-head">
        <h2 id="museum-shop-title">Наш мерч</h2>
        <router-link class="museum-outline-link" to="/shop">Больше товаров →</router-link>
      </div>
      <div class="museum-card-row">
        <article
          v-for="item in previewProducts"
          :key="item.id ?? item.title"
          class="museum-preview-card museum-preview-card--shop"
        >
          <div class="museum-card-media">
            <img
              v-if="item.image"
              :src="item.image"
              :alt="item.title"
              loading="lazy"
            />
          </div>
          <p>{{ item.title }}</p>
          <div class="museum-card-foot">
            <span>{{ item.price }}</span>
            <router-link to="/shop">Купить →</router-link>
          </div>
        </article>
      </div>
    </section>

  </div>
</template>

<script setup>
/**
 * HomeView — главная страница `/` (ТЗ §4.1).
 *
 * Содержание:
 *   - museum-hero — большое фото и заголовок «Виртуальный музей деревянной архитектуры»
 *     + CTA «Перейти к карте →» (ведёт на /virtual-museum);
 *   - «Почему деревяшки?» — манифест проекта;
 *   - превью маршрутов и мерча.
 *
 * Карты на этой странице нет: она живёт на отдельном маршруте /virtual-museum
 * (MapView). Это удовлетворяет требование ТЗ 4.1 «разделить страницы».
 */

import { ref, onMounted } from 'vue'
import { javaApi } from '@/api/backend.js'

const PREVIEW_LIMIT = 3

const previewRoutes = ref([])
const previewProducts = ref([])

function formatRoutePrice(route) {
  const paid = !!(route.isPaid ?? route.paid)
  if (!paid) return 'Бесплатно'
  const price = route.price
  if (price == null || price === '') return 'Бесплатно'
  return `${Number(price).toLocaleString('ru-RU')} ₽`
}

function normalizeRoute(r) {
  return {
    id: r.id,
    title: r.title || r.name || 'Маршрут',
    coverImage: r.coverImage || r.image || null,
    price: formatRoutePrice(r),
  }
}

function normalizeProduct(p) {
  const price = p.price
  return {
    id: p.id,
    title: p.name || p.title || 'Товар',
    image: p.image || null,
    price: price != null && price !== '' ? `${Number(price).toLocaleString('ru-RU')} ₽` : '',
  }
}

onMounted(async () => {
  try {
    const data = await javaApi.routes.getList({ published: 'true' })
    const list = Array.isArray(data) ? data : []
    previewRoutes.value = list.slice(0, PREVIEW_LIMIT).map(normalizeRoute)
  } catch (err) {
    if (import.meta.env.DEV) console.warn('Home routes preview:', err)
  }

  try {
    const data = await javaApi.products.getList()
    const list = Array.isArray(data) ? data : []
    previewProducts.value = list.slice(0, PREVIEW_LIMIT).map(normalizeProduct)
  } catch (err) {
    if (import.meta.env.DEV) console.warn('Home products preview:', err)
  }
})
</script>

<style scoped>
/* Основной фон страницы — белый, как на эталоне. Шрифт — sans-serif. */
.museum-home-page {
  width: 100%;
  flex: 1 0 auto;
  background: #fff;
  color: #1d1d1b;
  padding: calc(var(--nav-h, 64px) + 1rem) max(1.5rem, calc((100vw - 1180px) / 2)) 2.5rem;
  font-family: Arial, Helvetica, sans-serif;
}

/* Лендинг главной — всегда светлый, как на референсе */
#app-root.app-dark .museum-home-page,
body.app-dark-theme .museum-home-page {
  background: #fff;
  color: #1d1d1b;
}

.museum-hero {
  position: relative;
  min-height: min(62vw, 520px);
  overflow: hidden;
  background: #d9d9d9;
}

.museum-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0.66) 34%, rgba(255, 255, 255, 0.08) 72%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.museum-hero__image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 50% 42%;
  filter: saturate(0.82) contrast(0.95) brightness(0.98);
}

.museum-hero__content {
  position: relative;
  z-index: 1;
  width: min(680px, 88%);
  padding: clamp(1.35rem, 3vw, 2.75rem);
}

.museum-hero h1 {
  max-width: 11.8em;
  margin: 0 0 0.95rem;
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
  font-size: clamp(2.6rem, 4.4vw, 5.35rem);
  font-weight: 900;
  line-height: 0.86;
  letter-spacing: 0;
  text-wrap: balance;
  text-shadow:
    0 1px 0 #fff,
    0 0 18px rgba(255, 255, 255, 0.96),
    0 0 36px rgba(255, 255, 255, 0.82);
}

.museum-pill-link,
.museum-outline-link,
.museum-card-foot a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.65rem;
  padding: 0 1.3rem;
  border-radius: 999px;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.museum-pill-link {
  background: #1d1d1b;
  color: #fff;
}

.museum-pill-link:hover {
  background: #000;
}

.museum-pill-link:focus-visible,
.museum-outline-link:focus-visible,
.museum-card-foot a:focus-visible {
  outline: 2px solid #1d1d1b;
  outline-offset: 3px;
}

.museum-about {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(320px, 1fr);
  gap: clamp(2rem, 8vw, 10rem);
  min-height: 470px;
  padding: 1.5rem 0 2.5rem;
}

.museum-about h2,
.museum-preview-section h2 {
  margin: 0;
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
  font-size: clamp(2rem, 2vw + 1rem, 3.4rem);
  font-weight: 900;
  line-height: 0.98;
  letter-spacing: 0;
}

.museum-about__copy {
  display: grid;
  align-content: start;
  gap: 1.35rem;
  padding-top: 1.8rem;
  font-size: clamp(0.95rem, 0.22vw + 0.9rem, 1.05rem);
  line-height: 1.22;
}

.museum-preview-section {
  margin: 0 0 2rem;
}

.museum-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.museum-outline-link {
  border: 2px solid #1d1d1b;
  color: #1d1d1b;
  background: #fff;
}

.museum-outline-link:hover {
  background: #1d1d1b;
  color: #fff;
}

.museum-card-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1.25rem;
}

.museum-preview-card {
  min-width: 0;
}

.museum-preview-card--dark {
  padding: 1rem;
  background: #191716;
  color: #fff;
}

.museum-preview-card--shop {
  border: 2px solid #5f5f5f;
  background: #fff;
  color: #1d1d1b;
}

.museum-card-media {
  aspect-ratio: 1.55 / 1;
  background: #c9c9c9;
  overflow: hidden;
}

.museum-card-media img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.museum-preview-card p {
  margin: 0.75rem 0 0;
  min-height: 2.1em;
  font-size: 0.9rem;
  font-weight: 700;
  line-height: 1.05;
}

.museum-card-foot {
  display: grid;
  grid-template-columns: auto minmax(7rem, 9rem);
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.65rem;
}

.museum-card-foot span {
  font-size: 1.2rem;
}

.museum-card-foot a {
  min-height: 2.2rem;
  padding: 0 1rem;
  background: #1d1d1b;
  color: #fff;
  font-size: 0.78rem;
}

.museum-card-foot a:hover {
  background: #000;
}

.museum-preview-card--dark .museum-card-foot a {
  background: #fff;
  color: #1d1d1b;
}

.museum-preview-card--dark .museum-card-foot a:hover {
  background: #d4d4d4;
}

.museum-preview-card--dark .museum-card-foot span {
  color: #fff;
}

@media (max-width: 900px) {
  .museum-home-page {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .museum-about {
    grid-template-columns: 1fr;
    gap: 1rem;
    min-height: 0;
  }

  .museum-card-row {
    grid-template-columns: 1fr;
  }

  .museum-section-head {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .museum-home-page {
    padding-top: calc(var(--nav-h, 56px) + 0.75rem);
  }

  .museum-hero {
    min-height: 420px;
  }

  .museum-hero__content {
    width: 100%;
  }

  .museum-hero h1 {
    font-size: clamp(2.25rem, 13vw, 3.6rem);
    line-height: 0.9;
  }

  .museum-card-foot {
    grid-template-columns: 1fr;
  }

  .museum-pill-link,
  .museum-outline-link {
    min-height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .museum-pill-link,
  .museum-outline-link,
  .museum-card-foot a {
    transition: none;
  }
}
</style>
