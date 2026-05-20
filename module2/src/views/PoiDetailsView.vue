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
        <header class="poi-details-head" aria-labelledby="poi-page-title">
          <h1 id="poi-page-title">Виртуальный музей</h1>
          <p class="poi-details-intro">
            Проект сохраняет уникальное деревянное зодчество Астраханской области в виртуальном
            и общедоступном формате. Ниже — полная карточка объекта с интерактивной картой 2ГИС.
          </p>
        </header>

        <article class="poi-detail-card" aria-labelledby="poi-card-name">
          <div class="poi-detail-card__gallery">
            <div class="poi-gallery__viewport">
              <img
                v-if="currentSlide?.type === 'photo'"
                :src="currentSlide.url"
                :alt="currentSlide.caption || poi.name"
                class="poi-gallery__img"
              />
              <div v-else class="poi-gallery__placeholder" aria-hidden="true" />
            </div>
            <button
              v-if="gallerySlides.length > 1"
              type="button"
              class="poi-gallery__nav poi-gallery__nav--prev"
              aria-label="Предыдущее фото"
              @click="prevSlide"
            >
              ‹
            </button>
            <button
              v-if="gallerySlides.length > 1"
              type="button"
              class="poi-gallery__nav poi-gallery__nav--next"
              aria-label="Следующее фото"
              @click="nextSlide"
            >
              ›
            </button>
          </div>

          <div class="poi-detail-card__info">
            <h2 id="poi-card-name" class="poi-detail-card__title">{{ poi.name }}</h2>
            <p class="poi-detail-card__line">
              <strong>Адрес:</strong> {{ addressLine }}
            </p>
            <p v-if="poi.style" class="poi-detail-card__line">
              <strong>Стиль:</strong> {{ poi.style }}
            </p>
            <p class="poi-detail-card__short">{{ briefText }}</p>

            <PoiMiniMap
              v-if="hasCoords"
              class="poi-detail-card__map"
              :lat="Number(poi.lat)"
              :lng="Number(poi.lng)"
              :aria-label="`Карта: ${poi.name}`"
            />

            <router-link
              class="poi-detail-back"
              :to="backToMapRoute"
            >
              ← Назад к карте
            </router-link>
          </div>
        </article>

        <div v-if="articleParagraphs.length" class="poi-detail-article">
          <p v-for="(par, i) in articleParagraphs" :key="i">{{ par }}</p>
        </div>
        <div v-else-if="briefText" class="poi-detail-article">
          <p>{{ briefText }}</p>
        </div>

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
      </template>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMapStore } from '@/store/index.js'
import PoiMiniMap from '@/components/poi/PoiMiniMap.vue'

const route = useRoute()
const mapStore = useMapStore()

const poi = ref(null)
const loading = ref(false)
const error = ref('')
const slideIndex = ref(0)

const hasCoords = computed(
  () => poi.value
    && Number.isFinite(Number(poi.value.lat))
    && Number.isFinite(Number(poi.value.lng))
    && (poi.value.lat !== 0 || poi.value.lng !== 0),
)

const hasMaxMedia = computed(
  () => poi.value && (poi.value.maxAudioUrl || poi.value.maxVideoUrl || poi.value.maxPlaylistUrl),
)

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

const gallerySlides = computed(() => {
  if (!poi.value) return []
  const slides = []
  if (Array.isArray(poi.value.photos)) {
    for (const ph of poi.value.photos) {
      const url = ph?.url || ph?.src
      if (url) slides.push({ type: 'photo', url, caption: ph.caption || poi.value.name })
    }
  }
  if (!slides.length) {
    slides.push({ type: 'empty', caption: poi.value.name })
  }
  return slides
})

const currentSlide = computed(() => gallerySlides.value[slideIndex.value] || gallerySlides.value[0])

const articleParagraphs = computed(() => {
  const text = poi.value?.detailText || ''
  if (!text.trim()) return []
  return text
    .split(/\n{2,}|\r\n{2,}/)
    .map((s) => s.trim())
    .filter(Boolean)
})

function prevSlide() {
  const n = gallerySlides.value.length
  if (n < 2) return
  slideIndex.value = (slideIndex.value - 1 + n) % n
}

function nextSlide() {
  const n = gallerySlides.value.length
  if (n < 2) return
  slideIndex.value = (slideIndex.value + 1) % n
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
watch(gallerySlides, () => {
  slideIndex.value = 0
})
</script>

<style scoped>
.poi-details-page {
  flex: 1 0 auto;
  background: #fff;
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
  padding: calc(var(--nav-h, 64px) + 1rem) max(1.5rem, calc((100vw - 1180px) / 2)) 2.5rem;
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

.poi-details-head {
  margin-bottom: 1.5rem;
}

.poi-details-head h1 {
  margin: 0 0 0.75rem;
  font-size: clamp(2rem, 2vw + 1rem, 3.4rem);
  font-weight: 900;
  line-height: 0.98;
}

.poi-details-intro {
  margin: 0;
  max-width: 52em;
  font-size: clamp(0.95rem, 0.22vw + 0.9rem, 1.05rem);
  line-height: 1.22;
}

.poi-detail-card {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(280px, 0.95fr);
  gap: 0;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 2rem;
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

.poi-gallery__img,
.poi-gallery__map {
  width: 100%;
  height: 100%;
  min-height: 320px;
  display: block;
}

.poi-gallery__img {
  object-fit: cover;
}

.poi-gallery__map :deep(.poi-static-map-preview) {
  min-height: 320px;
  object-fit: cover;
}

.poi-gallery__placeholder {
  width: 100%;
  min-height: 320px;
  background: linear-gradient(135deg, #e5e5e5, #c4c4c4);
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

.poi-detail-card__info {
  display: flex;
  flex-direction: column;
  padding: 1.35rem 1.5rem 1.5rem;
  border-left: 1px solid rgba(0, 0, 0, 0.08);
}

.poi-detail-card__title {
  margin: 0 0 0.65rem;
  font-size: 1.35rem;
  font-weight: 900;
  line-height: 1.05;
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

.poi-detail-card__map {
  margin-bottom: 1rem;
}

.poi-detail-back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
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

.poi-detail-article {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 100%;
  font-size: clamp(1.02rem, 0.35vw + 0.95rem, 1.12rem);
  line-height: 1.08;
  text-align: justify;
}

.poi-detail-article p {
  margin: 0;
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
  margin-top: 2rem;
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
  .poi-gallery__img,
  .poi-gallery__placeholder {
    min-height: 260px;
  }
}

@media (max-width: 480px) {
  .poi-gallery__nav {
    width: 2.75rem;
    height: 2.75rem;
  }

  .poi-detail-back {
    width: 100%;
    justify-content: center;
  }
}
</style>
