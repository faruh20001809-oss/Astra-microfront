<template>
  <div class="poi-details-page">
    <div class="poi-details__inner">

      <div v-if="loading" class="poi-details-state" role="status" aria-live="polite">
        <div class="spinner" aria-hidden="true" />
        <span>Загружаем точку…</span>
      </div>

      <div v-else-if="error" class="poi-details-state poi-details-state--error">
        <p>{{ error }}</p>
        <div class="poi-details-state__actions">
          <button type="button" class="poi-detail-back" @click="load">Попробовать снова</button>
          <router-link class="poi-detail-back" to="/virtual-museum">← Назад к карте</router-link>
        </div>
      </div>

      <template v-else-if="poi">
        <nav class="poi-breadcrumb" aria-label="Хлебные крошки">
          <router-link class="poi-breadcrumb__link" :to="backToMapRoute">Карта</router-link>
          <span class="poi-breadcrumb__sep" aria-hidden="true">—</span>
          <span class="poi-breadcrumb__current">{{ categoryLabel }}</span>
        </nav>

        <article class="poi-detail-card" aria-labelledby="poi-page-title">
          <div v-if="hasGallery" class="poi-detail-card__gallery">
            <div class="poi-gallery__viewport">
              <img
                v-if="currentSlide?.type === 'photo'"
                :src="currentSlide.url"
                :alt="currentSlide.caption || poi.name"
                class="poi-gallery__img"
              />
            </div>
            <button
              v-if="photoSlides.length > 1"
              type="button"
              class="poi-gallery__nav poi-gallery__nav--prev"
              aria-label="Предыдущее фото"
              @click="prevSlide"
            >
              ‹
            </button>
            <button
              v-if="photoSlides.length > 1"
              type="button"
              class="poi-gallery__nav poi-gallery__nav--next"
              aria-label="Следующее фото"
              @click="nextSlide"
            >
              ›
            </button>
            <span v-if="photoSlides.length > 1" class="poi-gallery__counter">
              {{ slideIndex + 1 }} / {{ photoSlides.length }}
            </span>
          </div>

          <div class="poi-detail-card__info">
            <p class="poi-detail-card__eyebrow">{{ categoryLabel }}</p>
            <div class="poi-detail-card__title-row">
              <h1 id="poi-page-title" class="poi-detail-card__title">{{ poi.name }}</h1>
              <button
                type="button"
                class="poi-fav"
                :class="{ 'poi-fav--active': isFavorite }"
                :aria-pressed="isFavorite"
                :aria-label="isFavorite ? 'Убрать из избранного' : 'Добавить в избранное'"
                @click="toggleFavorite"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                  <path
                    d="M12 21s-6.7-4.3-9.3-8.1C.9 10.3 1.6 6.9 4.4 5.7c2-0.9 4.2-0.1 5.4 1.6l.2.3.2-.3c1.2-1.7 3.4-2.5 5.4-1.6 2.8 1.2 3.5 4.6 1.7 7.2C18.7 16.7 12 21 12 21z"
                    :fill="isFavorite ? 'currentColor' : 'none'"
                    stroke="currentColor"
                    stroke-width="1.8"
                  />
                </svg>
              </button>
            </div>
            <p class="poi-detail-card__line">
              <strong>Адрес:</strong> {{ addressLine }}
            </p>
            <p v-if="poi.style" class="poi-detail-card__line">
              <strong>Стиль:</strong> {{ poi.style }}
            </p>
            <p v-if="briefText && !articleParagraphs.length" class="poi-detail-card__short">{{ briefText }}</p>

            <div class="poi-detail-card__actions">
              <router-link class="poi-detail-back" :to="backToMapRoute">← Назад к карте</router-link>
              <button type="button" class="poi-detail-share" @click="share">{{ shareLabel }}</button>
            </div>
          </div>
        </article>

        <div v-if="articleParagraphs.length" class="poi-detail-article">
          <p v-for="(par, i) in articleParagraphs" :key="i">{{ par }}</p>
        </div>

        <section
          v-if="hasCoords"
          class="poi-detail-map-section"
          aria-labelledby="poi-map-section-title"
        >
          <h2 id="poi-map-section-title" class="poi-detail-section-title">Расположение на карте</h2>
          <p class="poi-detail-map-section__hint">
            Приближайте и перемещайте карту 2ГИС, чтобы изучить окрестности объекта.
          </p>
          <PoiInteractiveMap2gis
            large
            :lat="Number(poi.lat)"
            :lng="Number(poi.lng)"
            :zoom="17"
            :aria-label="`Интерактивная карта: ${poi.name}`"
          />
          <a
            class="poi-detail-map-section__link"
            :href="dgisWebUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            Открыть в 2ГИС →
          </a>
        </section>

        <section class="poi-contacts" aria-labelledby="poi-contacts-title">
          <h2 id="poi-contacts-title" class="poi-detail-section-title">Контакты</h2>
          <dl class="poi-contacts__list">
            <div class="poi-contacts__row">
              <dt>Адрес</dt>
              <dd>{{ addressLine }}</dd>
            </div>
            <div v-if="poi.category" class="poi-contacts__row">
              <dt>Категория</dt>
              <dd>{{ categoryLabel }}</dd>
            </div>
            <div v-if="poi.year" class="poi-contacts__row">
              <dt>Год</dt>
              <dd>{{ poi.year }}</dd>
            </div>
            <div v-if="poi.architect" class="poi-contacts__row">
              <dt>Архитектор</dt>
              <dd>{{ poi.architect }}</dd>
            </div>
          </dl>
        </section>

        <section v-if="hasMaxMedia" class="poi-detail-extra" aria-label="Медиа MAX">
          <p class="poi-detail-extra__label text-mono">MAX · Медиа</p>
          <div class="poi-detail-extra__grid">
            <audio v-if="poi.maxAudioUrl" :src="poi.maxAudioUrl" controls preload="none" class="poi-detail-extra__player" />
            <video
              v-if="poi.maxVideoUrl"
              :src="poi.maxVideoUrl"
              controls
              preload="metadata"
              playsinline
              class="poi-detail-extra__player poi-detail-extra__player--video"
            />
            <a
              v-if="poi.maxPlaylistUrl"
              :href="poi.maxPlaylistUrl"
              target="_blank"
              rel="noopener"
              class="poi-detail-extra__link"
            >
              Плейлист MAX →
            </a>
          </div>
        </section>

        <section v-if="recommendations.length" class="poi-recos" aria-labelledby="poi-recos-title">
          <div class="poi-recos__head">
            <h2 id="poi-recos-title" class="poi-detail-section-title">Также рекомендуем</h2>
            <router-link class="poi-recos__all" :to="{ path: '/virtual-museum', hash: '#museum-map' }">
              Все точки →
            </router-link>
          </div>
          <div class="poi-recos__grid">
            <article v-for="item in recommendations" :key="item.id" class="poi-reco-card">
              <router-link class="poi-reco-card__media" :to="{ name: 'poi-details', params: { id: item.id } }">
                <img
                  v-if="item.thumb"
                  :src="item.thumb"
                  :alt="item.name"
                  class="poi-reco-card__img"
                  loading="lazy"
                />
                <div v-else class="poi-reco-card__media-fallback" aria-hidden="true" />
              </router-link>
              <div class="poi-reco-card__body">
                <h3>
                  <router-link :to="{ name: 'poi-details', params: { id: item.id } }">{{ item.name }}</router-link>
                </h3>
                <p>{{ item.address?.trim() || 'Адрес не указан' }}</p>
                <router-link
                  class="poi-reco-card__cta"
                  :to="{ name: 'poi-details', params: { id: item.id } }"
                >
                  Подробнее →
                </router-link>
              </div>
            </article>
          </div>
        </section>
      </template>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMapStore } from '@/store/index.js'
import { useFavorites } from '@/composables/useFavorites.js'
import { get2gisWebUrl, poiHasValidCoords } from '@/utils/dgisLinks.js'
import PoiInteractiveMap2gis from '@/components/poi/PoiInteractiveMap2gis.vue'

const route = useRoute()
const mapStore = useMapStore()
const { isPoiFavorite, togglePoi } = useFavorites()

const poi = ref(null)
const loading = ref(false)
const error = ref('')
const slideIndex = ref(0)
const shareLabel = ref('Поделиться')

const hasCoords = computed(() => poi.value && poiHasValidCoords(poi.value.lat, poi.value.lng))
const hasGallery = computed(() => photoSlides.value.length > 0)

const hasMaxMedia = computed(
  () => poi.value && (poi.value.maxAudioUrl || poi.value.maxVideoUrl || poi.value.maxPlaylistUrl),
)

const categoryLabel = computed(() => poi.value?.category || 'Точка интереса')
const isFavorite = computed(() => Boolean(poi.value) && isPoiFavorite(poi.value.id))
const dgisWebUrl = computed(() => (poi.value ? get2gisWebUrl(poi.value.lat, poi.value.lng) : ''))

const backToMapRoute = computed(() => ({
  path: '/virtual-museum',
  hash: '#museum-map',
  query: poi.value?.id ? { poi: String(poi.value.id) } : {},
}))

const briefText = computed(
  () => poi.value?.shortDescription || poi.value?.description || '',
)

const addressLine = computed(() => {
  if (!poi.value) return '—'
  const addr = poi.value.address?.trim() || '—'
  if (!hasCoords.value) return addr
  const lat = Number(poi.value.lat).toFixed(6)
  const lng = Number(poi.value.lng).toFixed(6)
  return `${addr}; ${lat}, ${lng}`
})

const photoSlides = computed(() => {
  if (!poi.value || !Array.isArray(poi.value.photos)) return []
  const slides = []
  for (const ph of poi.value.photos) {
    const url = ph?.url || ph?.src
    if (url) slides.push({ type: 'photo', url, caption: ph.caption || poi.value.name })
  }
  return slides
})

const currentSlide = computed(() => photoSlides.value[slideIndex.value] || photoSlides.value[0] || null)

const articleParagraphs = computed(() => {
  const text = poi.value?.detailText || ''
  if (!text.trim()) return []
  return text
    .split(/\n{2,}|\r\n{2,}/)
    .map((s) => s.trim())
    .filter(Boolean)
})

function poiThumb(item) {
  if (item?.image) return item.image
  if (Array.isArray(item?.photos)) {
    const first = item.photos.find((ph) => ph?.url || ph?.src)
    if (first) return first.url || first.src
  }
  return ''
}

const recommendations = computed(() => {
  if (!poi.value) return []
  const all = Array.isArray(mapStore.pois) ? mapStore.pois : []
  const others = all.filter((p) => p && Number(p.id) !== Number(poi.value.id))
  const sameCategory = others.filter((p) => p.category && p.category === poi.value.category)
  const rest = others.filter((p) => !sameCategory.includes(p))
  return [...sameCategory, ...rest]
    .slice(0, 3)
    .map((p) => ({ ...p, thumb: poiThumb(p) }))
})

function prevSlide() {
  const n = photoSlides.value.length
  if (n < 2) return
  slideIndex.value = (slideIndex.value - 1 + n) % n
}

function nextSlide() {
  const n = photoSlides.value.length
  if (n < 2) return
  slideIndex.value = (slideIndex.value + 1) % n
}

function toggleFavorite() {
  if (poi.value?.id) togglePoi(poi.value.id)
}

async function share() {
  const url = window.location.href
  const title = poi.value?.name ? `${poi.value.name} — Виртуальный музей` : 'Виртуальный музей'
  try {
    if (navigator.share) {
      await navigator.share({ title, url })
      return
    }
    await navigator.clipboard.writeText(url)
    shareLabel.value = 'Ссылка скопирована'
    setTimeout(() => { shareLabel.value = 'Поделиться' }, 2000)
  } catch {
    /* отмена шаринга */
  }
}

async function load() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    error.value = 'Некорректный идентификатор точки.'
    poi.value = null
    return
  }
  loading.value = true
  error.value = ''
  slideIndex.value = 0
  try {
    if (!Array.isArray(mapStore.pois) || mapStore.pois.length === 0) {
      await mapStore.fetchPois().catch(() => {})
    }
    const normalized = await mapStore.fetchPoiById(id)
    if (!normalized?.id) throw new Error('Точка не найдена')
    poi.value = normalized
    document.title = `${normalized.name} — Виртуальный музей`
  } catch (e) {
    error.value = e?.message || 'Не удалось загрузить точку.'
    poi.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, (next, prev) => {
  if (next && next !== prev) load()
})
watch(photoSlides, () => {
  slideIndex.value = 0
})
</script>

<style scoped>
/* Белая «витрина» как на /virtual-museum, шрифты — из main.css */
.poi-details-page {
  flex: 1 0 auto;
  background: #fff;
  color: #1d1d1b;
  padding: calc(var(--nav-h, 64px) + 1rem) max(1.5rem, calc((100vw - 1180px) / 2)) 2.5rem;
  font-family: inherit;
}

#app-root.app-dark .poi-details-page,
body.app-dark-theme .poi-details-page {
  background: #fff;
  color: #1d1d1b;
}

.poi-details__inner {
  max-width: 1180px;
  margin: 0 auto;
}

.poi-breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #5f5f5f;
}

.poi-breadcrumb__link {
  color: #5f5f5f;
  text-decoration: none;
}

.poi-breadcrumb__link:hover {
  color: #1d1d1b;
}

.poi-breadcrumb__current {
  color: #1d1d1b;
}

/* Карточка: галерея + инфо (без карты в шапке) */
.poi-detail-card {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(280px, 0.95fr);
  gap: 0;
  background: #fff;
  border-radius: 2px;
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 2rem;
}

.poi-detail-card:not(:has(.poi-detail-card__gallery)) {
  grid-template-columns: 1fr;
}

.poi-detail-card__gallery {
  position: relative;
  background: #d9d9d9;
  min-height: 320px;
}

.poi-gallery__viewport {
  width: 100%;
  height: 100%;
  min-height: 320px;
}

.poi-gallery__img {
  width: 100%;
  height: 100%;
  min-height: 320px;
  display: block;
  object-fit: cover;
}

.poi-gallery__nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 2.5rem;
  height: 2.5rem;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #1d1d1b;
  font-size: 1.75rem;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.poi-gallery__nav:hover {
  background: #fff;
}

.poi-gallery__nav--prev {
  left: 0.75rem;
}

.poi-gallery__nav--next {
  right: 0.75rem;
}

.poi-gallery__counter {
  position: absolute;
  right: 0.75rem;
  bottom: 0.75rem;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 700;
}

.poi-detail-card__info {
  display: flex;
  flex-direction: column;
  padding: 1.35rem 1.5rem 1.5rem;
  border-left: 1px solid rgba(0, 0, 0, 0.08);
}

.poi-detail-card:not(:has(.poi-detail-card__gallery)) .poi-detail-card__info {
  border-left: 0;
}

.poi-detail-card__eyebrow {
  margin: 0 0 0.35rem;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #5f5f5f;
}

.poi-detail-card__title-row {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  margin-bottom: 0.5rem;
}

.poi-detail-card__title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 900;
  line-height: 1.05;
}

.poi-fav {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  margin-top: 0.1rem;
  border: 2px solid #1d1d1b;
  border-radius: 50%;
  background: #fff;
  color: #1d1d1b;
  cursor: pointer;
}

.poi-fav:hover {
  background: #1d1d1b;
  color: #fff;
}

.poi-fav--active {
  background: #1d1d1b;
  color: #fff;
}

.poi-detail-card__line {
  margin: 0 0 0.4rem;
  font-size: 0.88rem;
  line-height: 1.25;
}

.poi-detail-card__line strong {
  font-weight: 700;
}

.poi-detail-card__short {
  margin: 0.65rem 0 1rem;
  font-size: 0.9rem;
  line-height: 1.22;
  flex: 1 1 auto;
}

.poi-detail-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-top: auto;
  padding-top: 1rem;
}

.poi-detail-section-title {
  margin: 0 0 0.75rem;
  font-size: clamp(1.5rem, 1.2vw + 1rem, 2rem);
  font-weight: 900;
  line-height: 0.98;
}

.poi-detail-article {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  font-size: clamp(1.02rem, 0.35vw + 0.95rem, 1.12rem);
  line-height: 1.22;
  text-align: justify;
}

.poi-detail-article p {
  margin: 0;
}

.poi-detail-map-section {
  margin-bottom: 2rem;
}

.poi-detail-map-section__hint {
  margin: 0 0 0.75rem;
  font-size: 0.88rem;
  color: #5f5f5f;
  line-height: 1.3;
}

.poi-detail-map-section__link {
  display: inline-flex;
  margin-top: 0.75rem;
  font-size: 0.88rem;
  font-weight: 700;
  color: #1d1d1b;
  text-decoration: underline;
}

.poi-detail-map-section__link:hover {
  color: #000;
}

.poi-contacts {
  margin-bottom: 2rem;
  padding: 1.25rem 1.35rem;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 2px;
}

.poi-contacts__list {
  margin: 0;
  display: grid;
  gap: 0.55rem;
}

.poi-contacts__row {
  display: grid;
  grid-template-columns: 7.5rem minmax(0, 1fr);
  gap: 0.5rem 1rem;
  align-items: baseline;
}

.poi-contacts__row dt {
  margin: 0;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #5f5f5f;
}

.poi-contacts__row dd {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.3;
}

.poi-detail-back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.65rem;
  padding: 0 1.35rem;
  border-radius: 999px;
  background: #1d1d1b;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 700;
  text-decoration: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
}

.poi-detail-back:hover {
  background: #000;
  color: #fff;
}

.poi-detail-share {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.65rem;
  padding: 0 1.35rem;
  border: 2px solid #1d1d1b;
  border-radius: 999px;
  background: #fff;
  color: #1d1d1b;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}

.poi-detail-share:hover {
  background: #1d1d1b;
  color: #fff;
}

.poi-details-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 3rem 1rem;
  text-align: center;
  color: #5f5f5f;
}

.poi-details-state--error {
  border: 1px solid rgba(192, 57, 43, 0.35);
  border-radius: 8px;
  padding: 2rem;
}

.poi-details-state__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
}

.poi-detail-extra {
  margin-bottom: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.poi-detail-extra__label {
  margin: 0 0 0.75rem;
  font-size: 0.7rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #5f5f5f;
}

.poi-detail-extra__grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-width: 480px;
}

.poi-detail-extra__player {
  width: 100%;
}

.poi-detail-extra__player--video {
  aspect-ratio: 16 / 9;
  height: auto;
}

.poi-detail-extra__link {
  color: #1d1d1b;
  font-weight: 700;
}

/* Рекомендации — как карточки на /virtual-museum */
.poi-recos {
  margin-top: 0.5rem;
}

.poi-recos__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.poi-recos__head .poi-detail-section-title {
  margin: 0;
}

.poi-recos__all {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 1rem;
  border-radius: 999px;
  background: #1d1d1b;
  color: #fff;
  font-size: 0.78rem;
  font-weight: 700;
  text-decoration: none;
}

.poi-recos__all:hover {
  background: #000;
}

.poi-recos__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
}

.poi-reco-card {
  display: grid;
  min-width: 0;
  background: #191716;
  color: #fff;
  padding: 1rem;
}

.poi-reco-card__media {
  display: block;
  aspect-ratio: 2.3 / 1;
  background: #c9c9c9;
  overflow: hidden;
  text-decoration: none;
}

.poi-reco-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.poi-reco-card__media-fallback {
  width: 100%;
  height: 100%;
  min-height: 120px;
  background: linear-gradient(160deg, #ececec, #d4d4d4);
}

.poi-reco-card__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 0.3rem 1rem;
  margin-top: 0.75rem;
}

.poi-reco-card__body h3,
.poi-reco-card__body p {
  grid-column: 1 / 2;
  margin: 0;
}

.poi-reco-card__body h3 {
  font-size: 1rem;
  line-height: 1.05;
  font-weight: 700;
}

.poi-reco-card__body h3 a {
  color: inherit;
  text-decoration: none;
}

.poi-reco-card__body h3 a:hover {
  text-decoration: underline;
}

.poi-reco-card__body p {
  font-size: 0.9rem;
  opacity: 0.9;
}

.poi-reco-card__cta {
  grid-column: 2 / 3;
  grid-row: 1 / 3;
  align-self: end;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2rem;
  padding: 0 1rem;
  border-radius: 999px;
  background: #fff;
  color: #1d1d1b;
  font-size: 0.78rem;
  font-weight: 700;
  text-decoration: none;
}

.poi-reco-card__cta:hover {
  background: #d4d4d4;
}

@media (max-width: 900px) {
  .poi-details-page {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .poi-detail-card {
    grid-template-columns: 1fr;
  }

  .poi-detail-card__info {
    border-left: 0;
    border-top: 1px solid rgba(0, 0, 0, 0.08);
  }

  .poi-detail-card__gallery,
  .poi-gallery__viewport,
  .poi-gallery__img {
    min-height: 260px;
  }

  .poi-recos__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .poi-contacts__row {
    grid-template-columns: 1fr;
    gap: 0.15rem;
  }
}

@media (max-width: 480px) {
  .poi-detail-card__actions .poi-detail-back,
  .poi-detail-card__actions .poi-detail-share {
    flex: 1 1 auto;
    justify-content: center;
  }
}
</style>
