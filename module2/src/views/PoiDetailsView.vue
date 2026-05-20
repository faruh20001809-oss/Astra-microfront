<template>
  <div class="page-wrapper poi-details-page">
    <div class="container poi-details__inner">

      <div v-if="loading" class="poi-details-state" role="status" aria-live="polite">
        <div class="spinner" aria-hidden="true" />
        <span>Загружаем точку…</span>
      </div>

      <div v-else-if="error" class="poi-details-state poi-details-state--error">
        <p class="poi-details-state__label text-mono">Ошибка</p>
        <p>{{ error }}</p>
        <div class="poi-details-state__actions">
          <button type="button" class="btn btn-ghost btn-sm" @click="load">Попробовать снова</button>
          <router-link class="btn btn-ghost btn-sm" to="/virtual-museum">К виртуальному музею</router-link>
        </div>
      </div>

      <article v-else-if="poi" class="poi-details" aria-labelledby="poi-title">

        <nav class="poi-breadcrumbs" aria-label="Путь">
          <router-link to="/">Главная</router-link>
          <span aria-hidden="true">/</span>
          <router-link to="/virtual-museum">Виртуальный музей</router-link>
          <span aria-hidden="true">/</span>
          <span class="poi-breadcrumbs__current">{{ poi.name }}</span>
        </nav>

        <header class="poi-hero motion-reveal">
          <div class="poi-hero__media">
            <img
              v-if="poi.image"
              :src="poi.image"
              :alt="poi.name"
              loading="eager"
            />
            <div v-else class="poi-hero__placeholder" aria-hidden="true">◻</div>
          </div>
          <div class="poi-hero__copy">
            <p v-if="poi.category" class="poi-eyebrow text-mono">{{ poi.category }}</p>
            <h1 id="poi-title" class="poi-title">{{ poi.name }}</h1>
            <p v-if="poi.shortDescription" class="poi-short">{{ poi.shortDescription }}</p>
            <dl v-if="hasFacts" class="poi-facts">
              <div v-if="poi.year" class="poi-fact">
                <dt>Год</dt><dd>{{ poi.year }}</dd>
              </div>
              <div v-if="poi.architect" class="poi-fact">
                <dt>Архитектор</dt><dd>{{ poi.architect }}</dd>
              </div>
              <div v-if="poi.style" class="poi-fact">
                <dt>Стиль</dt><dd>{{ poi.style }}</dd>
              </div>
              <div v-if="poi.address" class="poi-fact">
                <dt>Адрес</dt><dd>{{ poi.address }}</dd>
              </div>
            </dl>
            <div class="poi-actions">
              <button
                v-if="hasCoords"
                type="button"
                class="btn btn-primary btn-sm"
                @click="openOnMap"
              >
                Открыть на карте →
              </button>
              <a
                v-if="hasCoords"
                :href="`https://yandex.ru/maps/?pt=${poi.lng},${poi.lat}&z=17`"
                target="_blank"
                rel="noopener"
                class="btn btn-ghost btn-sm"
              >
                Маршрут в Яндекс.Картах
              </a>
            </div>
          </div>
        </header>

        <section v-if="poi.detailText" class="poi-section poi-article" aria-labelledby="poi-text-title">
          <h2 id="poi-text-title" class="poi-section__title">Подробнее о точке</h2>
          <div class="poi-article__body">
            <p v-for="(par, i) in detailParagraphs" :key="i">{{ par }}</p>
          </div>
        </section>

        <section
          v-else
          class="poi-section poi-stub"
          aria-labelledby="poi-stub-title"
        >
          <h2 id="poi-stub-title" class="poi-section__title">Полное описание</h2>
          <p class="poi-stub__lead">
            Раздел в разработке: здесь появится расширенный текст, исторические справки,
            дополнительные фотографии и материалы из MAX.
          </p>
          <p v-if="poi.shortDescription || poi.description" class="poi-stub__preview">
            {{ poi.shortDescription || poi.description }}
          </p>
        </section>

        <section v-if="hasMaxMedia" class="poi-section poi-max" aria-labelledby="poi-max-title">
          <header class="poi-max__head">
            <p class="poi-max__eyebrow text-mono">MAX · Медиа</p>
            <h2 id="poi-max-title" class="poi-section__title">Плееры и записи</h2>
          </header>

          <div class="poi-max__grid">
            <div v-if="poi.maxAudioUrl" class="poi-max-card poi-max-card--audio">
              <p class="poi-max-card__label text-mono">Аудио</p>
              <audio :src="poi.maxAudioUrl" controls preload="none" class="poi-max-card__player">
                Ваш браузер не поддерживает аудио-плеер.
              </audio>
              <a :href="poi.maxAudioUrl" target="_blank" rel="noopener" class="poi-max-card__fallback">
                Скачать / открыть в MAX →
              </a>
            </div>

            <div v-if="poi.maxVideoUrl" class="poi-max-card poi-max-card--video">
              <p class="poi-max-card__label text-mono">Видео</p>
              <video
                :src="poi.maxVideoUrl"
                controls
                preload="metadata"
                class="poi-max-card__player"
                playsinline
              >
                Ваш браузер не поддерживает видео-плеер.
              </video>
              <a :href="poi.maxVideoUrl" target="_blank" rel="noopener" class="poi-max-card__fallback">
                Открыть видео в новой вкладке →
              </a>
            </div>

            <div v-if="poi.maxPlaylistUrl" class="poi-max-card poi-max-card--playlist">
              <p class="poi-max-card__label text-mono">Плейлист</p>
              <a
                :href="poi.maxPlaylistUrl"
                target="_blank"
                rel="noopener"
                class="poi-max-card__playlist-link"
              >
                <span class="poi-max-card__playlist-title">Перейти к плейлисту MAX →</span>
                <span class="poi-max-card__playlist-hint">
                  Полная подборка материалов о точке в внешнем плеере.
                </span>
              </a>
            </div>
          </div>
        </section>

        <section v-if="hasCoords" class="poi-section poi-geo" aria-labelledby="poi-geo-title">
          <h2 id="poi-geo-title" class="poi-section__title">Где находится</h2>
          <div class="poi-geo__row">
            <div class="poi-geo__coords text-mono" aria-label="Координаты">
              <span>{{ formatCoord(poi.lat) }}</span>
              <span>{{ formatCoord(poi.lng) }}</span>
            </div>
            <button
              type="button"
              class="btn btn-ghost btn-sm poi-geo__copy-btn"
              :aria-pressed="coordsCopied"
              :aria-label="coordsCopied ? 'Координаты скопированы в буфер обмена' : 'Скопировать координаты в буфер обмена'"
              @click="copyCoords"
            >
              <span aria-hidden="true">{{ coordsCopied ? '✓ Скопировано' : 'Скопировать координаты' }}</span>
            </button>
            <!-- Скрытый live-region: screen-reader озвучит "Скопировано" даже если фокус не на кнопке. -->
            <span class="sr-only" role="status" aria-live="polite">
              {{ coordsCopied ? 'Координаты скопированы' : '' }}
            </span>
          </div>
        </section>

      </article>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { javaApi } from '@/api/backend'
import { normalizePoi, useMapStore, useToastStore } from '@/store/index.js'

/**
 * PoiDetailsView — отдельная страница `/pois/:id` (ТЗ, раздел 4.2).
 * Показывает заголовок, краткое описание, расширенный текст (detailText),
 * блок MAX-плееров (audio/video/playlist) и гео-блок с координатами.
 * Все секции с fallback: если поля нет — секция скрывается.
 */
const route = useRoute()
const router = useRouter()
const mapStore = useMapStore()
const toastStore = useToastStore()

const poi = ref(null)
const loading = ref(false)
const error = ref('')
const coordsCopied = ref(false)

const hasFacts = computed(
  () => poi.value && (poi.value.year || poi.value.architect || poi.value.style || poi.value.address)
)
const hasCoords = computed(
  () => poi.value && Number.isFinite(Number(poi.value.lat)) && Number.isFinite(Number(poi.value.lng))
        && (poi.value.lat !== 0 || poi.value.lng !== 0)
)
const hasMaxMedia = computed(
  () => poi.value && (poi.value.maxAudioUrl || poi.value.maxVideoUrl || poi.value.maxPlaylistUrl)
)
const detailParagraphs = computed(() => {
  if (!poi.value?.detailText) return []
  return poi.value.detailText
    .split(/\n{2,}|\r\n{2,}/)
    .map(s => s.trim())
    .filter(Boolean)
})

async function load() {
  const idRaw = route.params.id
  const id = Number(idRaw)
  if (!Number.isFinite(id) || id <= 0) {
    error.value = 'Некорректный идентификатор точки.'
    poi.value = null
    return
  }
  loading.value = true
  error.value = ''
  try {
    const raw = await javaApi.pois.getById(id)
    const normalized = normalizePoi(raw)
    if (!normalized || !normalized.id) {
      throw new Error('Точка не найдена')
    }
    poi.value = normalized
    document.title = `${normalized.name} — Астрахань. Живая История`
  } catch (e) {
    console.error('PoiDetailsView: load failed', e)
    error.value = e?.message || 'Не удалось загрузить точку. Попробуйте позже.'
    poi.value = null
  } finally {
    loading.value = false
  }
}

function openOnMap() {
  if (!poi.value) return
  mapStore.setSelected(poi.value)
  router.push({
    path: '/virtual-museum',
    hash: '#museum-map',
    query: { poi: String(poi.value.id) },
  })
}

function formatCoord(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(6) : '—'
}

async function copyCoords() {
  if (!hasCoords.value) return
  const text = `${formatCoord(poi.value.lat)}, ${formatCoord(poi.value.lng)}`
  try {
    await navigator.clipboard.writeText(text)
    coordsCopied.value = true
    toastStore.push('Координаты скопированы', 'success', 2500)
    setTimeout(() => { coordsCopied.value = false }, 2200)
  } catch (_) {
    toastStore.push('Не удалось скопировать координаты', 'error')
  }
}

onMounted(load)
watch(() => route.params.id, (next, prev) => {
  if (next && next !== prev) load()
})
</script>

<style scoped>
.poi-details-page {
  padding-bottom: var(--spacing-2xl);
  container-type: inline-size;
  container-name: poi;
}

.poi-details__inner {
  max-width: 1100px;
}

.poi-details-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  padding: clamp(2rem, 6cqi, 4rem) var(--spacing-md);
  text-align: center;
  color: var(--gray-400);
}

.poi-details-state--error {
  border: 1px solid rgba(184, 74, 60, 0.4);
  background: rgba(184, 74, 60, 0.08);
  border-radius: var(--radius-md);
}

.poi-details-state__label {
  color: var(--danger);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.poi-details-state__actions {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
  justify-content: center;
}

.poi-breadcrumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem 0.35rem;
  align-items: center;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  letter-spacing: 0.04em;
  color: var(--gray-400);
  text-transform: uppercase;
  padding: clamp(0.5rem, 2cqi, 0.85rem) 0;
}

/**
 * Тач-ареал ссылки крошек: minimum 36×36 на десктопе, ≥44 на мобильных
 * (см. responsive-design skill). Padding выбран так, чтобы визуально
 * крошки оставались компактными, а сама кликабельная область — крупной.
 */
.poi-breadcrumbs a {
  color: var(--gray-400);
  text-decoration: none;
  padding: 0.5rem 0.6rem;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  transition: color 0.15s ease, background 0.15s ease;
}

.poi-breadcrumbs a:hover {
  color: var(--accent);
  background: rgba(212, 184, 150, 0.08);
}

.poi-breadcrumbs a:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.poi-breadcrumbs__current {
  color: var(--cream, #f5ead0);
  padding: 0.5rem 0.6rem;
}

@media (max-width: 480px) {
  .poi-breadcrumbs a,
  .poi-breadcrumbs__current {
    min-height: 44px;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

.poi-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: clamp(1rem, 3cqi, 2rem);
  margin: clamp(0.5rem, 2cqi, 1rem) 0 clamp(1.5rem, 4cqi, 2.5rem);
}

@container poi (min-width: 720px) {
  .poi-hero {
    grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
    align-items: start;
  }
}

.poi-hero__media {
  border-radius: var(--radius-md);
  border: 1px solid rgba(212, 184, 150, 0.16);
  overflow: hidden;
  background: rgba(20, 16, 13, 0.65);
  aspect-ratio: 16 / 10;
  display: flex;
  align-items: center;
  justify-content: center;
}

.poi-hero__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.poi-hero__placeholder {
  font-size: 4rem;
  color: var(--gray-600);
}

.poi-hero__copy {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  min-width: 0;
}

.poi-eyebrow {
  color: var(--accent);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: clamp(0.65rem, 0.4cqi + 0.55rem, 0.78rem);
  margin: 0;
}

.poi-title {
  font-family: var(--font-display);
  font-size: clamp(1.75rem, 3.5cqi + 1rem, 2.6rem);
  margin: 0;
  line-height: 1.1;
  color: var(--cream, #f5ead0);
}

.poi-short {
  margin: 0;
  color: var(--gray-300, #d2c7b3);
  font-size: clamp(0.95rem, 0.4cqi + 0.85rem, 1.1rem);
  line-height: 1.55;
  max-width: 56ch;
}

.poi-facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(45%, 12rem), 1fr));
  gap: var(--spacing-sm) var(--spacing-md);
  margin: 0;
  padding: var(--spacing-md);
  border: 1px solid rgba(212, 184, 150, 0.16);
  border-radius: var(--radius-sm);
  background: rgba(20, 16, 13, 0.45);
}

.poi-fact {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.poi-fact dt {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gray-400);
  margin-bottom: 2px;
}

.poi-fact dd {
  margin: 0;
  color: var(--paper, #f0e5cd);
  font-size: 0.95rem;
  line-height: 1.4;
}

.poi-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.poi-actions .btn {
  min-height: 44px;
}

.poi-section {
  margin-top: clamp(1.5rem, 4cqi, 2.5rem);
  padding-top: clamp(1.25rem, 3cqi, 1.75rem);
  border-top: 1px solid rgba(212, 184, 150, 0.12);
}

.poi-section__title {
  font-family: var(--font-display);
  font-size: clamp(1.2rem, 2cqi + 0.85rem, 1.6rem);
  margin: 0 0 var(--spacing-md);
  color: var(--cream, #f5ead0);
}

.poi-article__body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  max-width: 70ch;
  color: var(--paper, #f0e5cd);
  font-size: clamp(0.95rem, 0.3cqi + 0.9rem, 1.05rem);
  line-height: 1.75;
}

.poi-article__body p {
  margin: 0;
}

.poi-stub__lead {
  margin: 0 0 var(--spacing-md);
  max-width: 62ch;
  color: var(--gray-300, #d2c7b3);
  font-size: 1rem;
  line-height: 1.6;
}

.poi-stub__preview {
  margin: 0;
  max-width: 70ch;
  padding: var(--spacing-md);
  border-left: 3px solid var(--accent);
  color: var(--paper, #f0e5cd);
  font-size: 0.95rem;
  line-height: 1.65;
  background: rgba(20, 16, 13, 0.35);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

:root.app-light .poi-stub__preview {
  background: #faf7f3;
  color: #1d1d1b;
}

.poi-max__head {
  margin-bottom: var(--spacing-md);
}

.poi-max__eyebrow {
  color: var(--accent);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.65rem;
  margin: 0 0 var(--spacing-xs);
}

.poi-max__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 280px), 1fr));
  gap: clamp(0.85rem, 2cqi, 1.25rem);
}

.poi-max-card {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(212, 184, 150, 0.18);
  background: rgba(20, 16, 13, 0.55);
}

.poi-max-card__label {
  margin: 0;
  color: var(--accent);
  font-size: 0.65rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.poi-max-card__player {
  width: 100%;
  display: block;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.45);
}

.poi-max-card--video .poi-max-card__player {
  aspect-ratio: 16 / 9;
  height: auto;
}

.poi-max-card__fallback {
  font-family: var(--font-mono);
  font-size: 0.75rem;
  color: var(--gray-400);
  text-decoration: none;
  letter-spacing: 0.04em;
  margin-top: auto;
  padding-top: var(--spacing-xs);
  border-top: 1px dashed rgba(212, 184, 150, 0.15);
}

.poi-max-card__fallback:hover {
  color: var(--accent);
}

.poi-max-card__fallback:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.poi-max-card__playlist-link {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  text-decoration: none;
  padding: var(--spacing-sm);
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  color: var(--accent);
  background: rgba(200, 169, 110, 0.08);
  min-height: 88px;
  transition: background 0.15s ease;
}

.poi-max-card__playlist-link:hover {
  background: rgba(200, 169, 110, 0.16);
}

.poi-max-card__playlist-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.poi-max-card__playlist-title {
  font-family: var(--font-display);
  font-size: 1rem;
}

.poi-max-card__playlist-hint {
  font-size: 0.78rem;
  color: var(--paper);
  line-height: 1.4;
}

.poi-geo__row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.poi-geo__coords {
  display: flex;
  gap: var(--spacing-md);
  font-size: 0.95rem;
  color: var(--paper);
  letter-spacing: 0.05em;
}

@container poi (max-width: 540px) {
  .poi-hero__media {
    aspect-ratio: 4 / 3;
  }
  .poi-actions .btn {
    flex: 1 1 0;
  }
}

/**
 * Light-тема: переопределяем тёмные хардкод-rgba фоны.
 * Без этого `color: var(--paper)` (= #0d0d0d на light) на тёмном фоне
 * становится нечитаемым.
 */
:root.app-light .poi-hero__media {
  background: #faf7f3;
  border-color: rgba(0, 0, 0, 0.1);
}

:root.app-light .poi-facts,
:root.app-light .poi-max-card {
  background: #faf7f3;
  border-color: rgba(0, 0, 0, 0.08);
}

:root.app-light .poi-max-card__player {
  background: rgba(0, 0, 0, 0.05);
}

:root.app-light .poi-max-card__playlist-link {
  background: rgba(184, 148, 94, 0.1);
}

:root.app-light .poi-max-card__playlist-link:hover {
  background: rgba(184, 148, 94, 0.2);
}

:root.app-light .poi-details-state--error {
  background: rgba(192, 57, 43, 0.06);
  border-color: rgba(192, 57, 43, 0.35);
}

@media (prefers-reduced-motion: reduce) {
  .poi-max-card__playlist-link,
  .poi-breadcrumbs a {
    transition: none;
  }
}
</style>
