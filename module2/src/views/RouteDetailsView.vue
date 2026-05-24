<template>
  <div class="page-wrapper route-details-page">
    <div class="container route-details__inner">

      <div v-if="loading" class="route-details-state" role="status">
        <div class="spinner" aria-hidden="true" />
        <span>Загрузка маршрута…</span>
      </div>

      <div v-else-if="error" class="route-details-state route-details-state--error">
        <p>{{ error }}</p>
        <router-link class="btn btn-ghost" to="/routes">← К списку маршрутов</router-link>
      </div>

      <template v-else-if="route">
        <nav class="route-details-breadcrumb" aria-label="Навигация">
          <router-link to="/routes">Маршруты</router-link>
          <span aria-hidden="true">/</span>
          <span>{{ route.title }}</span>
        </nav>

        <header class="route-details-hero">
          <div class="route-details-hero__media">
            <RouteCoverImage
              v-if="hasCover"
              :route="route"
              :alt="route.title"
              loading="eager"
            />
            <div v-else class="route-details-hero__placeholder" aria-hidden="true">◎</div>
          </div>
          <div class="route-details-hero__body">
            <p class="route-details-eyebrow text-mono">{{ route.category || 'Маршрут' }}</p>
            <h1 id="route-page-title" class="route-details__title">{{ route.title }}</h1>
            <div class="route-details-meta">
              <span>{{ route.duration }}</span>
              <span>·</span>
              <span>{{ route.distance }}</span>
              <span>·</span>
              <span>{{ route.stops?.length || 0 }} остановок</span>
            </div>
            <p v-if="route.description" class="route-details-lead">{{ route.description }}</p>
            <span :class="['tag', route.isPaid ? '' : 'tag-accent']">
              {{ route.isPaid ? `${route.price} ₽` : 'Бесплатно' }}
            </span>
          </div>
        </header>

        <section
          v-if="thematicParagraphs.length"
          class="route-details-section"
          aria-labelledby="route-thematic-title"
        >
          <h2 id="route-thematic-title" class="route-details-section__title">О маршруте</h2>
          <div class="route-details-article">
            <p v-for="(par, i) in thematicParagraphs" :key="i">{{ par }}</p>
          </div>
        </section>

        <section
          v-if="videoEmbeds.length"
          class="route-details-section"
          aria-labelledby="route-video-title"
        >
          <h2 id="route-video-title" class="route-details-section__title">Видео</h2>
          <div class="route-details-media-grid">
            <div v-for="(item, i) in videoEmbeds" :key="'v-' + i" class="route-details-media-item">
              <iframe
                v-if="item.type === 'iframe'"
                :src="item.src"
                :title="`Видео ${i + 1}: ${route.title}`"
                loading="lazy"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen
                class="route-details-media-item__iframe"
              />
              <video
                v-else
                :src="item.src"
                controls
                playsinline
                preload="metadata"
                class="route-details-media-item__video"
              />
            </div>
          </div>
        </section>

        <section
          v-if="audioUrls.length"
          class="route-details-section"
          aria-labelledby="route-audio-title"
        >
          <h2 id="route-audio-title" class="route-details-section__title">Аудио</h2>
          <div class="route-details-audio-list">
            <audio
              v-for="(url, i) in audioUrls"
              :key="'a-' + i"
              :src="url"
              controls
              preload="none"
              class="route-details-audio-list__player"
            />
          </div>
        </section>

        <section
          v-if="route.stops?.length"
          class="route-details-section"
          aria-labelledby="route-stops-title"
        >
          <h2 id="route-stops-title" class="route-details-section__title">Остановки маршрута</h2>
          <p class="route-details-section__hint">
            У каждой точки — своё тематическое описание в контексте этого маршрута.
          </p>
          <div class="route-details-stops">
            <article
              v-for="(stop, i) in route.stops"
              :key="stop.poiId ?? i"
              class="route-details-stop"
            >
              <div class="route-details-stop__head">
                <div class="route-details-stop__num">{{ i + 1 }}</div>
                <div class="route-details-stop__head-text">
                  <h3 class="route-details-stop__name">{{ stop.name }}</h3>
                  <router-link
                    v-if="stop.poiId"
                    :to="{ name: 'poi-details', params: { id: stop.poiId } }"
                    class="route-details-stop__poi-link"
                  >
                    Карточка в музее →
                  </router-link>
                </div>
              </div>

              <div v-if="stopThematicParagraphs(stop).length" class="route-details-stop__thematic">
                <p
                  v-for="(par, pi) in stopThematicParagraphs(stop)"
                  :key="'t-' + pi"
                >{{ par }}</p>
                <p v-if="stop.description" class="route-details-stop__poi-ref">
                  Справочник: {{ stop.description }}
                </p>
              </div>
              <p v-else-if="stop.description" class="route-details-stop__desc">{{ stop.description }}</p>

              <div v-if="stopVideoEmbeds(stop).length" class="route-details-stop__media">
                <p class="route-details-stop__media-label text-mono">Видео</p>
                <div class="route-details-media-grid">
                  <div
                    v-for="(item, vi) in stopVideoEmbeds(stop)"
                    :key="'sv-' + vi"
                    class="route-details-media-item route-details-media-item--sm"
                  >
                    <iframe
                      v-if="item.type === 'iframe'"
                      :src="item.src"
                      :title="`Видео: ${stop.name}`"
                      loading="lazy"
                      allowfullscreen
                      class="route-details-media-item__iframe"
                    />
                    <video
                      v-else
                      :src="item.src"
                      controls
                      playsinline
                      preload="metadata"
                      class="route-details-media-item__video"
                    />
                  </div>
                </div>
              </div>

              <div v-if="stopAudioUrls(stop).length" class="route-details-stop__media">
                <p class="route-details-stop__media-label text-mono">Аудио</p>
                <audio
                  v-for="(url, ai) in stopAudioUrls(stop)"
                  :key="'sa-' + ai"
                  :src="url"
                  controls
                  preload="none"
                  class="route-details-audio-list__player"
                />
              </div>
            </article>
          </div>
        </section>

        <section class="route-details-actions" aria-label="Действия с маршрутом">
          <button
            v-if="route.isPaid"
            type="button"
            class="btn btn-primary btn-lg"
            @click="onPaidRoute"
          >
            Использовать награду / Купить за {{ route.price }} ₽
          </button>
          <button v-else type="button" class="btn btn-accent btn-lg" @click="onStartRoute">
            Начать маршрут на карте
          </button>
          <button type="button" class="btn btn-ghost" @click="markCompleted">
            Отметить как пройденный
          </button>
          <button type="button" class="btn btn-ghost" @click="copyShareLink">
            Поделиться ссылкой
          </button>
          <router-link to="/routes" class="btn btn-ghost">← Все маршруты</router-link>
        </section>
      </template>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { javaApi } from '@/api/backend.js'
import { useMapStore, useToastStore } from '@/store/index.js'
import { useGuestProgress } from '@/composables/useGuestProgress.js'
import RouteCoverImage from '@/components/routes/RouteCoverImage.vue'
import { normalizeRouteMedia, pickRouteCoverSource } from '@/utils/routeMedia.js'
import { normalizeUrlList, resolveVideoEmbed } from '@/utils/mediaEmbed.js'

const vueRoute = useRoute()
const router = useRouter()
const mapStore = useMapStore()
const toastStore = useToastStore()
const { markRouteCompleted } = useGuestProgress()

const route = ref(null)
const loading = ref(true)
const error = ref('')
const guestEmail = ref(localStorage.getItem('astra_guest_email') || '')
const confirmedProgress = ref({ availableRewards: 0 })

const shareUrl = computed(() => {
  if (typeof window === 'undefined' || !route.value?.id) return ''
  return `${window.location.origin}/routes/${route.value.id}`
})

const hasCover = computed(() => !!pickRouteCoverSource(route.value))

const thematicParagraphs = computed(() => {
  const text = route.value?.thematicDescription || ''
  return text
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean)
})

const videoEmbeds = computed(() => {
  const urls = normalizeUrlList(route.value?.videoUrls)
  return urls.map(resolveVideoEmbed).filter(Boolean)
})

const audioUrls = computed(() => normalizeUrlList(route.value?.audioUrls))

function stopThematicParagraphs(stop) {
  const text = stop?.thematicDescription || ''
  return text.split(/\n{2,}/).map((p) => p.trim()).filter(Boolean)
}

function stopVideoEmbeds(stop) {
  return normalizeUrlList(stop?.videoUrls).map(resolveVideoEmbed).filter(Boolean)
}

function stopAudioUrls(stop) {
  return normalizeUrlList(stop?.audioUrls)
}

function normalizeRouteFromApi(r) {
  if (!r || typeof r !== 'object') return r
  const pois = Array.isArray(r.pois) ? r.pois.map(Number).filter(Number.isFinite) : []
  return normalizeRouteMedia({
    ...r,
    title: r.title || r.name || 'Маршрут',
    isPaid: !!(r.isPaid ?? r.paid),
    stops: Array.isArray(r.stops) ? r.stops : [],
    pois,
    thematicDescription: r.thematicDescription || '',
    videoUrls: normalizeUrlList(r.videoUrls),
    audioUrls: normalizeUrlList(r.audioUrls),
  })
}

function extractPoiIdsFromRoute(r) {
  if (Array.isArray(r.pois) && r.pois.length) {
    return r.pois.map(Number).filter(Number.isFinite)
  }
  return []
}

async function load(id) {
  loading.value = true
  error.value = ''
  route.value = null
  const num = Number(id)
  if (!Number.isFinite(num)) {
    error.value = 'Некорректная ссылка на маршрут'
    loading.value = false
    return
  }
  try {
    const data = await javaApi.routes.getById(num)
    if (data && (data.id != null || data.title || data.name)) {
      route.value = normalizeRouteFromApi(data)
    } else {
      error.value = 'Маршрут не найден или недоступен'
    }
  } catch {
    error.value = 'Не удалось загрузить маршрут'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load(vueRoute.params.id)
  refreshProgress()
})
watch(() => vueRoute.params.id, (id) => load(id))

async function refreshProgress() {
  const email = guestEmail.value.trim()
  if (!email) return
  try {
    const stats = await javaApi.userProgress.getConfirmedByEmail(email)
    confirmedProgress.value = { ...confirmedProgress.value, ...stats }
  } catch {}
}

function onStartRoute() {
  const r = route.value
  if (!r?.id) return
  const id = Number(r.id)
  const poiIds = extractPoiIdsFromRoute(r)
  mapStore.setActiveFollowRoute({ id, title: r.title, poiIds })
  router.push({ path: '/', query: { route: String(id) } })
  toastStore.push('Открываем маршрут на карте…', 'success')
}

async function markCompleted() {
  const r = route.value
  const email = guestEmail.value.trim()
  if (!r?.id) return
  if (!email) {
    toastStore.push('Для фиксации прохождения войдите в кабинет (email + код).', 'info')
    return
  }
  try {
    const stats = await javaApi.routes.markCompleted(r.id, email)
    confirmedProgress.value = { ...confirmedProgress.value, ...stats }
    markRouteCompleted(r.id, { paid: !!r.isPaid })
    toastStore.push('Маршрут отмечен как пройденный', 'success')
  } catch (e) {
    toastStore.push(e?.message || 'Не удалось сохранить прогресс', 'error')
  }
}

async function onPaidRoute() {
  const r = route.value
  const email = guestEmail.value.trim()
  if (!r?.id) return
  if (!email) {
    toastStore.push('Покупка маршрута доступна после входа в кабинет заказов', 'info')
    return
  }
  try {
    await refreshProgress()
    if (Number(confirmedProgress.value.availableRewards || 0) > 0) {
      await javaApi.rewards.redeemByEmail(email, r.id)
      toastStore.push('Награда применена: маршрут открыт бесплатно', 'success')
      onStartRoute()
      return
    }
    toastStore.push('Нет доступных наград. Выполните условие 2 платных + 3 бесплатных.', 'info')
  } catch (e) {
    toastStore.push(e?.message || 'Не удалось проверить награды', 'error')
  }
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    toastStore.push('Ссылка скопирована', 'success')
  } catch {
    toastStore.push('Не удалось скопировать', 'error')
  }
}
</script>

<style scoped>
.route-details-page {
  padding-bottom: var(--spacing-2xl);
}

.route-details__inner {
  max-width: 920px;
  margin: 0 auto;
}

.route-details-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-2xl) 0;
  color: var(--gray-400);
}

.route-details-breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.5rem;
  font-size: 0.75rem;
  color: var(--gray-500);
  margin-bottom: var(--spacing-lg);
}

.route-details-breadcrumb a {
  color: var(--accent);
  text-decoration: none;
}

.route-details-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: var(--spacing-xl);
  margin-bottom: var(--spacing-2xl);
}

.route-details-hero__media {
  border-radius: var(--radius-md);
  overflow: hidden;
  min-height: 220px;
  background: var(--gray-800);
}

.route-details-hero__media :deep(img) {
  width: 100%;
  height: 100%;
  min-height: 220px;
  object-fit: cover;
  display: block;
}

.route-details-hero__placeholder {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  color: var(--gray-600);
}

.route-details-eyebrow {
  font-size: 0.7rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--accent);
  margin-bottom: var(--spacing-sm);
}

.route-details__title {
  font-family: var(--font-display);
  font-size: clamp(1.6rem, 4vw, 2.4rem);
  line-height: 1.15;
  margin-bottom: var(--spacing-sm);
}

.route-details-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  font-size: 0.8rem;
  color: var(--gray-400);
  margin-bottom: var(--spacing-md);
}

.route-details-lead {
  color: var(--gray-300);
  line-height: 1.65;
  margin-bottom: var(--spacing-md);
}

.route-details-section {
  margin-bottom: var(--spacing-2xl);
}

.route-details-section__title {
  font-family: var(--font-display);
  font-size: 1.35rem;
  margin-bottom: var(--spacing-md);
}

.route-details-article p {
  color: var(--gray-300);
  line-height: 1.75;
  margin-bottom: var(--spacing-md);
}

.route-details-media-grid {
  display: grid;
  gap: var(--spacing-lg);
}

.route-details-media-item {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: var(--gray-900);
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--gray-700);
}

.route-details-media-item__iframe,
.route-details-media-item__video {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}

.route-details-audio-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.route-details-audio-list__player {
  width: 100%;
}

.route-details-section__hint {
  font-size: 0.85rem;
  color: var(--gray-500);
  margin: -0.5rem 0 var(--spacing-lg);
}

.route-details-stops {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.route-details-stop {
  padding: var(--spacing-lg);
  border: 1px solid var(--gray-700);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.02);
}

.route-details-stop__head {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.route-details-stop__head-text {
  flex: 1;
  min-width: 0;
}

.route-details-stop__num {
  width: 2rem;
  height: 2rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--gray-800);
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 0.75rem;
}

.route-details-stop__name {
  font-family: var(--font-display);
  font-size: 1.15rem;
  margin: 0 0 0.35rem;
}

.route-details-stop__poi-link {
  font-size: 0.75rem;
  color: var(--accent);
  text-decoration: none;
}

.route-details-stop__poi-link:hover {
  text-decoration: underline;
}

.route-details-stop__thematic p {
  color: var(--gray-300);
  line-height: 1.7;
  margin-bottom: var(--spacing-sm);
}

.route-details-stop__desc {
  font-size: 0.9rem;
  color: var(--gray-400);
  line-height: 1.6;
}

.route-details-stop__poi-ref {
  font-size: 0.8rem;
  color: var(--gray-500);
  margin-top: var(--spacing-sm);
  font-style: italic;
}

.route-details-stop__media {
  margin-top: var(--spacing-md);
}

.route-details-stop__media-label {
  font-size: 0.65rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gray-500);
  margin-bottom: var(--spacing-sm);
}

.route-details-media-item--sm {
  aspect-ratio: 16 / 9;
  max-height: 280px;
}

.route-details-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--gray-700);
}

@media (max-width: 768px) {
  .route-details-hero {
    grid-template-columns: 1fr;
  }
}
</style>
