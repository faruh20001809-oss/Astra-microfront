<template>
  <div class="page-wrapper route-details-page" ref="pageRef">
    <div v-if="loading" class="route-details-state" role="status">
      <div class="spinner" aria-hidden="true" />
      <span>Загрузка маршрута…</span>
    </div>

    <div v-else-if="error" class="route-details-state route-details-state--error">
      <p>{{ error }}</p>
      <router-link class="btn btn-ghost" to="/routes">← К списку маршрутов</router-link>
    </div>

    <template v-else-if="route">
      <!-- Parallax Hero -->
      <div class="route-hero-parallax" ref="heroRef">
        <div class="route-hero-parallax__bg" :style="heroParallaxStyle">
          <RouteCoverImage
            v-if="hasCover"
            :route="route"
            :alt="route.title"
            loading="eager"
          />
          <div v-else class="route-hero-parallax__gradient" aria-hidden="true" />
        </div>
        <div class="route-hero-parallax__overlay" />
        <div class="route-hero-parallax__content container">
          <nav class="route-details-breadcrumb" aria-label="Навигация">
            <router-link to="/routes">Маршруты</router-link>
            <span aria-hidden="true">/</span>
            <span>{{ route.title }}</span>
          </nav>
          <p class="route-hero-eyebrow text-mono">{{ route.category || 'Маршрут' }}</p>
          <h1 class="route-hero-title">{{ route.title }}</h1>
          <div class="route-hero-meta">
            <span class="route-hero-meta__item"><span class="meta-icon">⏱</span> {{ route.duration }}</span>
            <span class="route-hero-meta__item"><span class="meta-icon">⇢</span> {{ route.distance }}</span>
            <span class="route-hero-meta__item"><span class="meta-icon">◎</span> {{ route.stops?.length || 0 }} остановок</span>
          </div>
          <span :class="['tag', route.isPaid ? '' : 'tag-accent']">
            {{ route.isPaid ? `${route.price} ₽` : 'Бесплатно' }}
          </span>
        </div>
      </div>

      <!-- Main Content with Sidebar -->
      <div class="route-body container">
        <!-- Scroll Progress Sidebar (desktop) -->
        <aside class="route-progress-nav" aria-label="Прогресс маршрута" v-if="route.stops?.length">
          <div class="route-progress-nav__track">
            <div class="route-progress-nav__fill" :style="{ height: progressPercent + '%' }" />
          </div>
          <div class="route-progress-nav__dots">
            <button
              v-for="(stop, i) in route.stops"
              :key="'nav-' + i"
              type="button"
              :class="['route-progress-nav__dot', { active: activeStopIndex === i, visited: i < activeStopIndex }]"
              :aria-label="`Остановка ${i + 1}: ${stop.name}`"
              @click="scrollToStop(i)"
            >
              <span class="route-progress-nav__dot-num">{{ i + 1 }}</span>
              <span class="route-progress-nav__dot-label">{{ stop.name }}</span>
            </button>
          </div>
        </aside>

        <!-- Content Column -->
        <div class="route-content-col">
          <!-- About section -->
          <section
            v-if="thematicParagraphs.length"
            class="route-section route-section--reveal"
            :class="{ revealed: sectionsRevealed.about }"
            ref="aboutRef"
          >
            <h2 class="route-section__title">О маршруте</h2>
            <div class="route-section__article">
              <p v-for="(par, i) in thematicParagraphs" :key="i">{{ par }}</p>
            </div>
          </section>

          <!-- Route description -->
          <section
            v-if="route.description && !thematicParagraphs.length"
            class="route-section route-section--reveal"
            :class="{ revealed: sectionsRevealed.about }"
            ref="aboutRef"
          >
            <h2 class="route-section__title">О маршруте</h2>
            <p class="route-section__lead">{{ route.description }}</p>
          </section>

          <!-- Video section -->
          <section
            v-if="videoEmbeds.length"
            class="route-section route-section--reveal"
            :class="{ revealed: sectionsRevealed.video }"
            ref="videoRef"
          >
            <h2 class="route-section__title">Видео</h2>
            <div class="route-media-grid">
              <div v-for="(item, i) in videoEmbeds" :key="'v-' + i" class="route-media-item">
                <iframe
                  v-if="item.type === 'iframe'"
                  :src="item.src"
                  :title="`Видео ${i + 1}: ${route.title}`"
                  loading="lazy"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowfullscreen
                />
                <video v-else :src="item.src" controls playsinline preload="metadata" />
              </div>
            </div>
          </section>

          <!-- Audio section -->
          <section
            v-if="audioUrls.length"
            class="route-section route-section--reveal"
            :class="{ revealed: sectionsRevealed.audio }"
            ref="audioRef"
          >
            <h2 class="route-section__title">Аудио-гид</h2>
            <div class="route-audio-list">
              <audio
                v-for="(url, i) in audioUrls"
                :key="'a-' + i"
                :src="url"
                controls
                preload="none"
              />
            </div>
          </section>

          <!-- Stops Section (the main "фишка") -->
          <section
            v-if="route.stops?.length"
            class="route-section"
            aria-labelledby="route-stops-heading"
          >
            <h2 id="route-stops-heading" class="route-section__title">Остановки маршрута</h2>
            <p class="route-section__subtitle">
              Прокрутите вниз — навигатор слева покажет ваш прогресс.
            </p>

            <div class="route-stops-timeline">
              <article
                v-for="(stop, i) in route.stops"
                :key="stop.poiId ?? i"
                :ref="el => { if (el) stopRefs[i] = el }"
                :class="['route-stop-card', { 'route-stop-card--active': activeStopIndex === i }]"
                :data-stop-index="i"
              >
                <div class="route-stop-card__connector" aria-hidden="true">
                  <div class="route-stop-card__connector-line" />
                  <div :class="['route-stop-card__connector-dot', { active: activeStopIndex >= i }]">
                    {{ i + 1 }}
                  </div>
                </div>

                <div class="route-stop-card__body">
                  <div class="route-stop-card__header">
                    <h3 class="route-stop-card__name">{{ stop.name }}</h3>
                    <router-link
                      v-if="stop.poiId"
                      :to="{ name: 'poi-details', params: { id: stop.poiId } }"
                      class="route-stop-card__poi-link"
                    >
                      Карточка в музее →
                    </router-link>
                  </div>

                  <div v-if="stopThematicParagraphs(stop).length" class="route-stop-card__text">
                    <p v-for="(par, pi) in stopThematicParagraphs(stop)" :key="'t-' + pi">{{ par }}</p>
                    <p v-if="stop.description" class="route-stop-card__ref">
                      {{ stop.description }}
                    </p>
                  </div>
                  <p v-else-if="stop.description" class="route-stop-card__desc">{{ stop.description }}</p>

                  <!-- Stop media -->
                  <div v-if="stopVideoEmbeds(stop).length" class="route-stop-card__media">
                    <span class="route-stop-card__media-label text-mono">Видео</span>
                    <div class="route-media-grid route-media-grid--sm">
                      <div v-for="(item, vi) in stopVideoEmbeds(stop)" :key="'sv-' + vi" class="route-media-item">
                        <iframe
                          v-if="item.type === 'iframe'"
                          :src="item.src"
                          :title="`Видео: ${stop.name}`"
                          loading="lazy"
                          allowfullscreen
                        />
                        <video v-else :src="item.src" controls playsinline preload="metadata" />
                      </div>
                    </div>
                  </div>

                  <div v-if="stopAudioUrls(stop).length" class="route-stop-card__media">
                    <span class="route-stop-card__media-label text-mono">Аудио</span>
                    <audio
                      v-for="(url, ai) in stopAudioUrls(stop)"
                      :key="'sa-' + ai"
                      :src="url"
                      controls
                      preload="none"
                    />
                  </div>
                </div>

                <!-- Walking time between stops -->
                <div
                  v-if="i < route.stops.length - 1"
                  class="route-stop-card__walk-time"
                  aria-hidden="true"
                >
                  <span class="walk-icon">🚶</span>
                  <span>~{{ estimateWalkMinutes(i) }} мин</span>
                </div>
              </article>
            </div>
          </section>

          <!-- Actions -->
          <section class="route-actions" aria-label="Действия с маршрутом">
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
        </div>
      </div>

      <!-- Mobile Floating Progress (visible only on mobile) -->
      <div
        v-if="route.stops?.length && isMobileProgressVisible"
        class="route-mobile-progress"
        @click="toggleMobileNav"
      >
        <div class="route-mobile-progress__bar">
          <div class="route-mobile-progress__fill" :style="{ width: progressPercent + '%' }" />
        </div>
        <span class="route-mobile-progress__label">
          {{ activeStopIndex + 1 }} / {{ route.stops.length }}
        </span>
      </div>

      <!-- Mobile Nav Popup -->
      <transition name="slide-up-mobile">
        <div v-if="mobileNavOpen" class="route-mobile-nav" @click.self="mobileNavOpen = false">
          <div class="route-mobile-nav__sheet">
            <div class="route-mobile-nav__handle" aria-hidden="true" />
            <h3 class="route-mobile-nav__title">Остановки</h3>
            <div class="route-mobile-nav__list">
              <button
                v-for="(stop, i) in route.stops"
                :key="'mnav-' + i"
                type="button"
                :class="['route-mobile-nav__item', { active: activeStopIndex === i }]"
                @click="scrollToStop(i); mobileNavOpen = false"
              >
                <span class="route-mobile-nav__num">{{ i + 1 }}</span>
                <span>{{ stop.name }}</span>
              </button>
            </div>
          </div>
        </div>
      </transition>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick, reactive } from 'vue'
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

// Parallax
const pageRef = ref(null)
const heroRef = ref(null)
const heroScrollY = ref(0)
const heroParallaxStyle = computed(() => ({
  transform: `translateY(${heroScrollY.value * 0.35}px) scale(${1 + heroScrollY.value * 0.0003})`,
}))

// Scroll progress tracking
const activeStopIndex = ref(0)
const progressPercent = ref(0)
const stopRefs = ref([])
const isMobileProgressVisible = ref(false)
const mobileNavOpen = ref(false)

// Section reveal tracking
const sectionsRevealed = reactive({
  about: false,
  video: false,
  audio: false,
})
const aboutRef = ref(null)
const videoRef = ref(null)
const audioRef = ref(null)

let scrollRAF = null
let revealObserver = null

const shareUrl = computed(() => {
  if (typeof window === 'undefined' || !route.value?.id) return ''
  return `${window.location.origin}/routes/${route.value.id}`
})

const hasCover = computed(() => !!pickRouteCoverSource(route.value))

const thematicParagraphs = computed(() => {
  const text = route.value?.thematicDescription || ''
  return text.split(/\n{2,}/).map(p => p.trim()).filter(Boolean)
})

const videoEmbeds = computed(() => {
  const urls = normalizeUrlList(route.value?.videoUrls)
  return urls.map(resolveVideoEmbed).filter(Boolean)
})

const audioUrls = computed(() => normalizeUrlList(route.value?.audioUrls))

function stopThematicParagraphs(stop) {
  const text = stop?.thematicDescription || ''
  return text.split(/\n{2,}/).map(p => p.trim()).filter(Boolean)
}

function stopVideoEmbeds(stop) {
  return normalizeUrlList(stop?.videoUrls).map(resolveVideoEmbed).filter(Boolean)
}

function stopAudioUrls(stop) {
  return normalizeUrlList(stop?.audioUrls)
}

function estimateWalkMinutes(fromIndex) {
  const totalStops = route.value?.stops?.length || 1
  const totalDistStr = route.value?.distance || ''
  const totalDistKm = parseFloat(totalDistStr) || 2
  const perSegmentKm = totalDistKm / Math.max(totalStops - 1, 1)
  const walkSpeedKmH = 4.5
  return Math.max(2, Math.round((perSegmentKm / walkSpeedKmH) * 60))
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

// Scroll handling
function onScroll() {
  if (scrollRAF) return
  scrollRAF = requestAnimationFrame(() => {
    scrollRAF = null
    updateParallax()
    updateStopProgress()
  })
}

function updateParallax() {
  if (!heroRef.value) return
  const rect = heroRef.value.getBoundingClientRect()
  heroScrollY.value = Math.max(0, -rect.top)
}

function updateStopProgress() {
  const stops = stopRefs.value
  if (!stops.length) return

  const viewportMid = window.innerHeight * 0.4
  let closest = 0
  let minDist = Infinity

  for (let i = 0; i < stops.length; i++) {
    if (!stops[i]) continue
    const rect = stops[i].getBoundingClientRect()
    const dist = Math.abs(rect.top - viewportMid)
    if (dist < minDist) {
      minDist = dist
      closest = i
    }
  }

  activeStopIndex.value = closest
  progressPercent.value = Math.round((closest / Math.max(stops.length - 1, 1)) * 100)

  // Show mobile progress when past hero
  if (heroRef.value) {
    const heroRect = heroRef.value.getBoundingClientRect()
    isMobileProgressVisible.value = heroRect.bottom < 0
  }
}

function scrollToStop(index) {
  const el = stopRefs.value[index]
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function toggleMobileNav() {
  mobileNavOpen.value = !mobileNavOpen.value
}

function setupRevealObserver() {
  if (typeof IntersectionObserver === 'undefined') {
    sectionsRevealed.about = true
    sectionsRevealed.video = true
    sectionsRevealed.audio = true
    return
  }

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        const el = entry.target
        if (el === aboutRef.value) sectionsRevealed.about = true
        else if (el === videoRef.value) sectionsRevealed.video = true
        else if (el === audioRef.value) sectionsRevealed.audio = true
      })
    },
    { threshold: 0.15 }
  )

  nextTick(() => {
    if (aboutRef.value) revealObserver.observe(aboutRef.value)
    if (videoRef.value) revealObserver.observe(videoRef.value)
    if (audioRef.value) revealObserver.observe(audioRef.value)
  })
}

// Data loading
async function load(id) {
  loading.value = true
  error.value = ''
  route.value = null
  stopRefs.value = []
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
    await nextTick()
    setupRevealObserver()
  }
}

onMounted(() => {
  load(vueRoute.params.id)
  refreshProgress()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  if (revealObserver) revealObserver.disconnect()
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
/* ===== Hero Parallax ===== */
.route-hero-parallax {
  position: relative;
  min-height: 55vh;
  max-height: 600px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.route-hero-parallax__bg {
  position: absolute;
  inset: -20% 0 0;
  z-index: 0;
  will-change: transform;
  transition: transform 0ms linear;
}

.route-hero-parallax__bg :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.route-hero-parallax__gradient {
  width: 100%;
  height: 100%;
  background: linear-gradient(
    135deg,
    var(--gray-900) 0%,
    var(--gray-800) 40%,
    rgba(212, 184, 150, 0.08) 100%
  );
}

.route-hero-parallax__overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(
    to top,
    var(--ink) 0%,
    rgba(20, 16, 13, 0.7) 40%,
    rgba(20, 16, 13, 0.2) 100%
  );
}

.route-hero-parallax__content {
  position: relative;
  z-index: 2;
  padding: var(--spacing-2xl) 0;
  max-width: 720px;
}

.route-details-breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.5rem;
  font-size: 0.75rem;
  color: var(--gray-400);
  margin-bottom: var(--spacing-lg);
}

.route-details-breadcrumb a {
  color: var(--accent);
  text-decoration: none;
}

.route-hero-eyebrow {
  font-size: 0.7rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--accent);
  margin-bottom: var(--spacing-sm);
}

.route-hero-title {
  font-family: var(--font-display);
  font-size: clamp(2rem, 5vw, 3rem);
  line-height: 1.1;
  margin-bottom: var(--spacing-md);
  color: var(--cream);
}

.route-hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.route-hero-meta__item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: var(--gray-300);
}

.meta-icon {
  color: var(--accent);
}

/* ===== Body Layout ===== */
.route-body {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: var(--spacing-xl);
  max-width: 1100px;
  padding-top: var(--spacing-2xl);
  padding-bottom: var(--spacing-2xl);
}

/* ===== Progress Sidebar ===== */
.route-progress-nav {
  position: sticky;
  top: calc(var(--nav-h) + var(--spacing-lg));
  height: fit-content;
  max-height: calc(100vh - var(--nav-h) - 3rem);
  overflow-y: auto;
  scrollbar-width: none;
}

.route-progress-nav::-webkit-scrollbar {
  display: none;
}

.route-progress-nav__track {
  position: absolute;
  left: 14px;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--gray-700);
  border-radius: 2px;
  overflow: hidden;
}

.route-progress-nav__fill {
  width: 100%;
  background: linear-gradient(to bottom, var(--accent), var(--accent-dark));
  border-radius: 2px;
  transition: height 400ms var(--ease-spring);
}

.route-progress-nav__dots {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  padding: var(--spacing-sm) 0;
}

.route-progress-nav__dot {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--spacing-xs) 0;
  text-align: left;
  transition: all 300ms var(--ease-spring);
}

.route-progress-nav__dot-num {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--gray-800);
  border: 2px solid var(--gray-600);
  color: var(--gray-400);
  font-family: var(--font-mono);
  font-size: 0.7rem;
  transition: all 300ms var(--ease-spring);
}

.route-progress-nav__dot.active .route-progress-nav__dot-num {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(212, 184, 150, 0.12);
  box-shadow: 0 0 12px rgba(212, 184, 150, 0.25);
  transform: scale(1.15);
}

.route-progress-nav__dot.visited .route-progress-nav__dot-num {
  border-color: var(--accent-dark);
  color: var(--accent-dark);
  background: rgba(143, 111, 69, 0.1);
}

.route-progress-nav__dot-label {
  font-size: 0.72rem;
  color: var(--gray-500);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 130px;
  transition: color 300ms var(--ease-spring);
}

.route-progress-nav__dot.active .route-progress-nav__dot-label {
  color: var(--cream);
  font-weight: 500;
}

/* ===== Content Column ===== */
.route-content-col {
  min-width: 0;
}

/* ===== Sections with reveal animation ===== */
.route-section {
  margin-bottom: var(--spacing-2xl);
}

.route-section--reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 600ms var(--ease-spring), transform 600ms var(--ease-spring);
}

.route-section--reveal.revealed {
  opacity: 1;
  transform: translateY(0);
}

.route-section__title {
  font-family: var(--font-display);
  font-size: 1.4rem;
  margin-bottom: var(--spacing-md);
  color: var(--cream);
}

.route-section__subtitle {
  font-size: 0.8rem;
  color: var(--gray-500);
  margin: -0.5rem 0 var(--spacing-lg);
}

.route-section__article p {
  color: var(--gray-300);
  line-height: 1.8;
  margin-bottom: var(--spacing-md);
}

.route-section__lead {
  color: var(--gray-300);
  line-height: 1.8;
  font-size: 1rem;
}

/* ===== Media ===== */
.route-media-grid {
  display: grid;
  gap: var(--spacing-lg);
}

.route-media-grid--sm .route-media-item {
  max-height: 280px;
}

.route-media-item {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: var(--gray-900);
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--gray-700);
}

.route-media-item iframe,
.route-media-item video {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}

.route-audio-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.route-audio-list audio {
  width: 100%;
}

/* ===== Stops Timeline ===== */
.route-stops-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.route-stop-card {
  position: relative;
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 0;
  opacity: 0.6;
  transform: translateY(12px);
  transition: opacity 500ms var(--ease-spring), transform 500ms var(--ease-spring);
}

.route-stop-card--active {
  opacity: 1;
  transform: translateY(0);
}

/* Connector (vertical line + dot) */
.route-stop-card__connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.route-stop-card__connector-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--gray-700);
}

.route-stop-card:first-child .route-stop-card__connector-line {
  top: 50%;
}

.route-stop-card:last-child .route-stop-card__connector-line {
  bottom: 50%;
}

.route-stop-card__connector-dot {
  position: relative;
  z-index: 1;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--gray-900);
  border: 2px solid var(--gray-600);
  color: var(--gray-400);
  font-family: var(--font-mono);
  font-size: 0.75rem;
  margin-top: var(--spacing-lg);
  transition: all 400ms var(--ease-spring);
}

.route-stop-card__connector-dot.active {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(212, 184, 150, 0.08);
  box-shadow: 0 0 16px rgba(212, 184, 150, 0.2);
}

/* Card body */
.route-stop-card__body {
  padding: var(--spacing-lg);
  border: 1px solid var(--gray-700);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.015);
  margin: var(--spacing-sm) 0;
  transition: border-color 400ms var(--ease-spring), background 400ms var(--ease-spring);
}

.route-stop-card--active .route-stop-card__body {
  border-color: rgba(212, 184, 150, 0.25);
  background: rgba(212, 184, 150, 0.03);
}

.route-stop-card__header {
  margin-bottom: var(--spacing-sm);
}

.route-stop-card__name {
  font-family: var(--font-display);
  font-size: 1.15rem;
  margin: 0 0 0.25rem;
  color: var(--cream);
}

.route-stop-card__poi-link {
  font-size: 0.72rem;
  color: var(--accent);
  text-decoration: none;
}

.route-stop-card__poi-link:hover {
  text-decoration: underline;
}

.route-stop-card__text p {
  color: var(--gray-300);
  line-height: 1.75;
  margin-bottom: var(--spacing-sm);
}

.route-stop-card__ref {
  font-size: 0.82rem;
  color: var(--gray-500);
  font-style: italic;
}

.route-stop-card__desc {
  color: var(--gray-400);
  line-height: 1.65;
  font-size: 0.9rem;
}

.route-stop-card__media {
  margin-top: var(--spacing-md);
}

.route-stop-card__media-label {
  display: block;
  font-size: 0.62rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gray-500);
  margin-bottom: var(--spacing-sm);
}

.route-stop-card__media audio {
  width: 100%;
}

/* Walking time indicator between stops */
.route-stop-card__walk-time {
  position: absolute;
  bottom: -20px;
  left: 0;
  width: 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 0.62rem;
  color: var(--gray-500);
  font-family: var(--font-mono);
  z-index: 2;
}

.walk-icon {
  font-size: 0.9rem;
  opacity: 0.7;
}

/* ===== Actions ===== */
.route-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  padding-top: var(--spacing-xl);
  border-top: 1px solid var(--gray-700);
}

/* ===== Mobile Floating Progress ===== */
.route-mobile-progress {
  display: none;
  position: fixed;
  bottom: var(--spacing-lg);
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  background: rgba(42, 35, 30, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid var(--gray-600);
  border-radius: 999px;
  padding: 0.6rem 1.2rem;
  cursor: pointer;
  gap: var(--spacing-sm);
  align-items: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  transition: transform 300ms var(--ease-spring);
}

.route-mobile-progress:active {
  transform: translateX(-50%) scale(0.96);
}

.route-mobile-progress__bar {
  width: 60px;
  height: 3px;
  background: var(--gray-700);
  border-radius: 2px;
  overflow: hidden;
}

.route-mobile-progress__fill {
  height: 100%;
  background: var(--accent);
  border-radius: 2px;
  transition: width 400ms var(--ease-spring);
}

.route-mobile-progress__label {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--gray-300);
}

/* ===== Mobile Nav Sheet ===== */
.route-mobile-nav {
  display: none;
  position: fixed;
  inset: 0;
  z-index: var(--z-modal);
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  align-items: flex-end;
}

.route-mobile-nav__sheet {
  width: 100%;
  max-height: 70vh;
  background: var(--gray-900);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  border-top: 1px solid var(--gray-700);
  padding: var(--spacing-lg);
  overflow-y: auto;
}

.route-mobile-nav__handle {
  width: 40px;
  height: 4px;
  background: var(--gray-600);
  border-radius: 2px;
  margin: 0 auto var(--spacing-md);
}

.route-mobile-nav__title {
  font-family: var(--font-display);
  font-size: 1.1rem;
  margin-bottom: var(--spacing-md);
  color: var(--cream);
}

.route-mobile-nav__list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.route-mobile-nav__item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-sm);
  background: none;
  border: none;
  color: var(--gray-400);
  font-size: 0.85rem;
  cursor: pointer;
  text-align: left;
  transition: background 200ms, color 200ms;
}

.route-mobile-nav__item:hover {
  background: rgba(212, 184, 150, 0.06);
}

.route-mobile-nav__item.active {
  color: var(--accent);
  background: rgba(212, 184, 150, 0.1);
}

.route-mobile-nav__num {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--gray-800);
  border: 1px solid var(--gray-600);
  font-family: var(--font-mono);
  font-size: 0.65rem;
  flex-shrink: 0;
}

.route-mobile-nav__item.active .route-mobile-nav__num {
  border-color: var(--accent);
  color: var(--accent);
}

/* ===== States ===== */
.route-details-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-2xl) 0;
  color: var(--gray-400);
  min-height: 60vh;
  justify-content: center;
}

/* ===== Transitions ===== */
.slide-up-mobile-enter-active,
.slide-up-mobile-leave-active {
  transition: opacity 300ms var(--ease-spring);
}

.slide-up-mobile-enter-active .route-mobile-nav__sheet,
.slide-up-mobile-leave-active .route-mobile-nav__sheet {
  transition: transform 350ms var(--ease-spring);
}

.slide-up-mobile-enter-from,
.slide-up-mobile-leave-to {
  opacity: 0;
}

.slide-up-mobile-enter-from .route-mobile-nav__sheet,
.slide-up-mobile-leave-to .route-mobile-nav__sheet {
  transform: translateY(100%);
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .route-body {
    grid-template-columns: 1fr;
  }

  .route-progress-nav {
    display: none;
  }

  .route-mobile-progress {
    display: flex;
  }

  .route-mobile-nav {
    display: flex;
  }
}

@media (max-width: 600px) {
  .route-hero-parallax {
    min-height: 45vh;
  }

  .route-hero-title {
    font-size: clamp(1.6rem, 6vw, 2.2rem);
  }

  .route-stop-card {
    grid-template-columns: 36px 1fr;
  }

  .route-stop-card__connector-dot {
    width: 28px;
    height: 28px;
    font-size: 0.65rem;
  }

  .route-stop-card__body {
    padding: var(--spacing-md);
  }

  .route-stop-card__walk-time {
    width: 36px;
    font-size: 0.55rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .route-section--reveal {
    opacity: 1;
    transform: none;
    transition: none;
  }

  .route-stop-card {
    opacity: 1;
    transform: none;
    transition: none;
  }

  .route-hero-parallax__bg {
    transform: none !important;
  }
}
</style>
