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
          <span :class="['tag', route.isPaid ? (isLocked ? 'tag-locked' : '') : 'tag-accent']">
            <template v-if="!route.isPaid">Бесплатно</template>
            <template v-else-if="isLocked">
              <svg class="tag-lock-icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="14" height="14" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"/></svg>
              {{ route.price }} ₽
            </template>
            <template v-else>Разблокирован</template>
          </span>
        </div>
      </div>

      <!-- Main Content with Sidebar -->
      <div class="route-body container">
        <!-- Scroll Progress Sidebar (desktop) -->
        <aside class="route-progress-nav" aria-label="Прогресс маршрута" v-if="route.stops?.length">
          <div class="route-progress-nav__track">
            <div class="route-progress-nav__fill" :style="{ transform: `scaleY(${progressPercent / 100})` }" />
          </div>
          <div class="route-progress-nav__dots">
            <button
              v-for="(stop, i) in route.stops"
              :key="'nav-' + i"
              type="button"
              :class="['route-progress-nav__dot', { active: activeStopIndex === i, visited: i < activeStopIndex }]"
              :aria-label="`Остановка ${i + 1}: ${stop.name}`"
              :title="stop.name"
              @click="scrollToStop(i)"
            >
              <span class="route-progress-nav__dot-num">{{ i + 1 }}</span>
              <span class="route-progress-nav__dot-label" aria-hidden="true">{{ stop.name }}</span>
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

          <!-- ═══ Paywall Banner (locked paid routes) ═══ -->
          <section v-if="isLocked" class="route-paywall">
            <div class="route-paywall__icon" aria-hidden="true">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="48" height="48"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"/></svg>
            </div>
            <h2 class="route-paywall__title">Платный маршрут</h2>
            <p class="route-paywall__text">
              Этот маршрут доступен за <strong>{{ route.price }} ₽</strong> или по награде.
              Разблокируйте, чтобы увидеть подробные описания, фото, аудио-гиды и видео каждой остановки.
            </p>
            <div class="route-paywall__actions">
              <button type="button" class="btn btn-primary btn-lg" @click="onPaidRoute">
                Разблокировать маршрут
              </button>
              <p class="route-paywall__hint">
                Пройдите 2 платных и 3 бесплатных маршрута, чтобы заработать награду.
              </p>
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
                :class="[
                  'route-stop-card',
                  { 'route-stop-card--active': activeStopIndex === i },
                  { 'route-stop-card--locked': isLocked }
                ]"
                :data-stop-index="i"
              >
                <div class="route-stop-card__connector" aria-hidden="true">
                  <div class="route-stop-card__connector-line" />
                  <div :class="['route-stop-card__connector-dot', { active: activeStopIndex >= i }]">
                    {{ i + 1 }}
                  </div>
                </div>

                <div class="route-stop-card__body">
                  <div
                    v-if="stopHeroImage(stop) && !isLocked"
                    class="route-stop-card__hero"
                  >
                    <img
                      :src="stopHeroImage(stop)"
                      :alt="stop.name"
                      loading="lazy"
                      class="route-stop-card__hero-img"
                    />
                  </div>

                  <div class="route-stop-card__content">
                    <div class="route-stop-card__header">
                      <h3 class="route-stop-card__name">{{ stop.name }}</h3>
                      <div class="route-stop-card__header-meta">
                        <span v-if="stop.durationMinutes && !isLocked" class="route-stop-card__duration">
                          <span class="meta-icon">⏱</span> {{ stop.durationMinutes }} мин
                        </span>
                      </div>
                      <div v-if="!isLocked" class="route-stop-card__badges">
                        <router-link
                          v-if="stop.poiId && !stop.isCustom"
                          :to="{ name: 'poi-details', params: { id: stop.poiId } }"
                          class="route-stop-card__poi-link"
                        >
                          Перейти к месту →
                        </router-link>
                        <span v-if="stop.isCustom" class="route-stop-card__custom-badge">Особая точка</span>
                        <span v-if="stop.address" class="route-stop-card__address">{{ stop.address }}</span>
                      </div>
                    </div>

                    <template v-if="isLocked">
                      <div class="route-stop-card__locked-stub">
                        <div class="route-stop-card__locked-blur" aria-hidden="true">
                          <div class="locked-blur-line" style="width: 85%"></div>
                          <div class="locked-blur-line" style="width: 70%"></div>
                          <div class="locked-blur-line" style="width: 60%"></div>
                        </div>
                        <div class="route-stop-card__locked-badge">
                          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"/></svg>
                          Контент скрыт
                        </div>
                      </div>
                    </template>

                    <template v-else>
                      <div v-if="stopThematicParagraphs(stop).length" class="route-stop-card__text">
                        <p v-for="(par, pi) in stopThematicParagraphs(stop)" :key="'t-' + pi">{{ par }}</p>
                      </div>
                      <p v-if="stop.description" class="route-stop-card__desc">{{ stop.description }}</p>

                      <div
                        v-if="stopVideoEmbeds(stop).length || stopAudioUrls(stop).length"
                        class="route-stop-card__media-block"
                      >
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
                          <span class="route-stop-card__media-label text-mono">Аудио-гид</span>
                          <div class="route-stop-card__audio-list">
                            <audio
                              v-for="(url, ai) in stopAudioUrls(stop)"
                              :key="'sa-' + ai"
                              :src="url"
                              controls
                              preload="none"
                            />
                          </div>
                        </div>
                      </div>
                    </template>
                  </div>
                </div>

                <!-- Walking time between stops -->
                <div
                  v-if="i < route.stops.length - 1"
                  class="route-stop-card__walk-time"
                  aria-hidden="true"
                >
                  <svg class="walk-icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="14" height="14"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z"/></svg>
                  <span>~{{ estimateWalkMinutes(i) }} мин</span>
                </div>
              </article>
            </div>
          </section>

          <!-- Actions -->
          <section class="route-actions" aria-label="Действия с маршрутом">
            <template v-if="isLocked">
              <button type="button" class="btn btn-primary btn-lg" @click="onPaidRoute">
                Разблокировать за {{ route.price }} ₽ или наградой
              </button>
            </template>
            <template v-else>
              <button v-if="route.isPaid" type="button" class="btn btn-accent btn-lg" @click="onStartRoute">
                Начать маршрут на карте
              </button>
              <button v-else type="button" class="btn btn-accent btn-lg" @click="onStartRoute">
                Начать маршрут на карте
              </button>
              <button type="button" class="btn btn-ghost" @click="markCompleted">
                Отметить как пройденный
              </button>
            </template>
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
          <div class="route-mobile-progress__fill" :style="{ transform: `scaleX(${progressPercent / 100})` }" />
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
import { isClientLoggedIn } from '@/auth/clientAuth.js'
import { useMapStore, useToastStore } from '@/store/index.js'
import { useGuestProgress } from '@/composables/useGuestProgress.js'
import RouteCoverImage from '@/components/routes/RouteCoverImage.vue'
import { normalizeRouteMedia, pickRouteCoverSource, resolveApiMediaUrl } from '@/utils/routeMedia.js'
import { normalizeUrlList, resolveVideoEmbed } from '@/utils/mediaEmbed.js'

const vueRoute = useRoute()
const router = useRouter()
const mapStore = useMapStore()
const toastStore = useToastStore()
const { markRouteCompleted, consumeReward } = useGuestProgress()

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
let stopObserver = null

const shareUrl = computed(() => {
  if (typeof window === 'undefined' || !route.value?.id) return ''
  return `${window.location.origin}/routes/${route.value.id}`
})

const hasCover = computed(() => !!pickRouteCoverSource(route.value))

const isLocked = computed(() => {
  const r = route.value
  if (!r) return false
  return r.isPaid && !r.accessGranted
})

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

function stopHeroImage(stop) {
  return resolveApiMediaUrl(stop?.imageUrl || stop?.poiImage || '') || ''
}

function estimateWalkMinutes(fromIndex) {
  const nextStop = route.value?.stops?.[fromIndex + 1]
  if (nextStop?.durationMinutes) return nextStop.durationMinutes
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
    accessGranted: r.accessGranted !== false,
    stops: Array.isArray(r.stops) ? r.stops : [],
    pois,
    thematicDescription: r.thematicDescription || '',
    videoUrls: normalizeUrlList(r.videoUrls),
    audioUrls: normalizeUrlList(r.audioUrls),
  })
}

function extractPoiIdsFromRoute(r) {
  if (Array.isArray(r.stops) && r.stops.length) {
    const fromStops = r.stops
      .filter((s) => s && !s.isCustom && s.poiId != null)
      .map((s) => Number(s.poiId))
      .filter(Number.isFinite)
    if (fromStops.length) return fromStops
  }
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
  if (heroRef.value) {
    const heroRect = heroRef.value.getBoundingClientRect()
    isMobileProgressVisible.value = heroRect.bottom < 0
  }
}

function setupStopObserver() {
  if (typeof IntersectionObserver === 'undefined') return
  if (stopObserver) stopObserver.disconnect()

  stopObserver = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        const idx = Number(entry.target.dataset.stopIndex)
        if (Number.isFinite(idx)) {
          activeStopIndex.value = idx
          const total = route.value?.stops?.length || 1
          progressPercent.value = Math.round((idx / Math.max(total - 1, 1)) * 100)
        }
      }
    },
    { rootMargin: '-40% 0px -55% 0px', threshold: 0 }
  )

  nextTick(() => {
    for (const el of stopRefs.value) {
      if (el) stopObserver.observe(el)
    }
  })
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
    setupStopObserver()
  }
}

function onClientProfileUpdated() {
  void refreshProgress()
  const id = vueRoute.params.id
  if (id) void load(id)
}

onMounted(() => {
  load(vueRoute.params.id)
  refreshProgress()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('astra:client-profile-updated', onClientProfileUpdated)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('astra:client-profile-updated', onClientProfileUpdated)
  if (revealObserver) revealObserver.disconnect()
  if (stopObserver) stopObserver.disconnect()
})

watch(() => vueRoute.params.id, (id) => load(id))

async function refreshProgress() {
  if (!isClientLoggedIn()) return
  try {
    const stats = await javaApi.userProgress.getConfirmed()
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

function requireProfileAuth(message) {
  if (isClientLoggedIn()) return true
  toastStore.push(message, 'info')
  router.push({ path: '/profile/auth', query: { redirect: vueRoute.fullPath } })
  return false
}

async function markCompleted() {
  const r = route.value
  if (!r?.id) return
  if (!requireProfileAuth('Для фиксации прохождения войдите в профиль.')) return
  try {
    const stats = await javaApi.routes.markCompleted(r.id)
    confirmedProgress.value = { ...confirmedProgress.value, ...stats }
    markRouteCompleted(r.id, { paid: !!r.isPaid })
    toastStore.push('Маршрут отмечен как пройденный', 'success')
  } catch (e) {
    toastStore.push(e?.message || 'Не удалось сохранить прогресс', 'error')
  }
}

async function onPaidRoute() {
  const r = route.value
  if (!r?.id) return
  if (!requireProfileAuth('Для разблокировки маршрута войдите в профиль.')) return
  try {
    await refreshProgress()
    if (Number(confirmedProgress.value.availableRewards || 0) > 0) {
      await javaApi.rewards.redeem(r.id)
      toastStore.push('Награда применена — маршрут разблокирован!', 'success')
      consumeReward(r.id)
      await load(r.id)
      return
    }
    toastStore.push('Нет доступных наград. Пройдите 2 платных + 3 бесплатных маршрута.', 'info')
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
  grid-template-columns: minmax(0, 11rem) minmax(0, 1fr);
  gap: clamp(var(--spacing-md), 3vw, var(--spacing-xl));
  max-width: 1100px;
  padding-top: var(--spacing-2xl);
  padding-bottom: var(--spacing-2xl);
  align-items: start;
}

/* ===== Progress Sidebar ===== */
.route-progress-nav {
  --nav-dot-size: 28px;
  --nav-rail-w: 3px;
  --nav-rail-x: calc(var(--nav-dot-size) / 2);

  position: sticky;
  top: calc(var(--nav-h) + var(--spacing-lg));
  height: fit-content;
  max-height: calc(100dvh - var(--nav-h) - 3rem);
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--gray-600) transparent;
  min-width: 0;
  width: 100%;
  padding-right: 2px;
  isolation: isolate;
}

.route-progress-nav::-webkit-scrollbar {
  display: none;
}

/* Вертикальная линия — под точками, по центру кружков */
.route-progress-nav__track {
  position: absolute;
  left: var(--nav-rail-x);
  top: calc(var(--nav-dot-size) / 2 + var(--spacing-xs));
  bottom: calc(var(--nav-dot-size) / 2 + var(--spacing-xs));
  width: var(--nav-rail-w);
  transform: translateX(-50%);
  background: var(--gray-700);
  border-radius: 2px;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}

.route-progress-nav__fill {
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, var(--accent), var(--accent-dark));
  border-radius: 2px;
  transform-origin: top center;
  transition: transform 400ms var(--ease-spring);
}

.route-progress-nav__dots {
  position: relative;
  z-index: 1;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding: var(--spacing-xs) 0;
}

.route-progress-nav__dot {
  position: relative;
  display: grid;
  grid-template-columns: var(--nav-dot-size) minmax(0, 1fr);
  align-items: center;
  column-gap: var(--spacing-xs);
  background: none;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  padding: 0.35rem 0.25rem 0.35rem 0;
  text-align: left;
  min-width: 0;
  min-height: 44px;
  width: 100%;
  transition: color 200ms var(--ease-spring), background 200ms var(--ease-spring);
}

.route-progress-nav__dot:hover {
  background: rgba(212, 184, 150, 0.06);
}

.route-progress-nav__dot:focus {
  outline: none;
}

.route-progress-nav__dot:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.route-progress-nav__dot-num {
  box-sizing: border-box;
  width: var(--nav-dot-size);
  height: var(--nav-dot-size);
  margin: 0;
  justify-self: center;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--gray-800);
  border: 2px solid var(--gray-600);
  color: var(--gray-400);
  font-family: var(--font-mono);
  font-size: 0.65rem;
  line-height: 1;
  transition: border-color 300ms var(--ease-spring), color 300ms var(--ease-spring), background 300ms var(--ease-spring), box-shadow 300ms var(--ease-spring);
}

.route-progress-nav__dot.active .route-progress-nav__dot-num {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(212, 184, 150, 0.12);
  box-shadow: 0 0 10px rgba(212, 184, 150, 0.2);
}

.route-progress-nav__dot.visited .route-progress-nav__dot-num {
  border-color: var(--accent-dark);
  color: var(--accent-dark);
  background: rgba(143, 111, 69, 0.1);
}

.route-progress-nav__dot-label {
  display: none;
  grid-column: 2;
  font-size: 0.68rem;
  line-height: 1.3;
  color: var(--gray-500);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

/* Только активная остановка — подпись внутри колонки, без налезания на контент */
.route-progress-nav__dot.active .route-progress-nav__dot-label {
  display: block;
  color: var(--cream);
  font-weight: 500;
}

.route-progress-nav__dot:hover .route-progress-nav__dot-label,
.route-progress-nav__dot:focus-visible .route-progress-nav__dot-label {
  display: block;
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
  color: var(--paper);
  opacity: 0.82;
  line-height: 1.8;
  margin-bottom: var(--spacing-md);
}

.route-section__lead {
  color: var(--paper);
  opacity: 0.82;
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
  opacity: 0.7;
  transform: translateY(8px);
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
  transition: border-color 400ms var(--ease-spring), color 400ms var(--ease-spring), background 400ms var(--ease-spring), box-shadow 400ms var(--ease-spring);
}

.route-stop-card__connector-dot.active {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(212, 184, 150, 0.08);
  box-shadow: 0 0 16px rgba(212, 184, 150, 0.2);
}

/* Card body */
.route-stop-card__body {
  container-type: inline-size;
  container-name: stop-card;
  padding: var(--spacing-lg);
  border: 1px solid var(--gray-700);
  border-radius: var(--radius-md);
  background: var(--accent-soft);
  margin: var(--spacing-sm) 0;
  transition: border-color 400ms var(--ease-spring), background 400ms var(--ease-spring);
}

.route-stop-card--active .route-stop-card__body {
  border-color: var(--accent);
  background: var(--accent-glow);
}

.route-stop-card__hero {
  margin: calc(-1 * var(--spacing-lg)) calc(-1 * var(--spacing-lg)) var(--spacing-md);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  max-height: 240px;
  background: var(--gray-800);
}

.route-stop-card__hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.route-stop-card__content {
  min-width: 0;
}

.route-stop-card__header {
  margin-bottom: var(--spacing-sm);
}

.route-stop-card__name {
  font-family: var(--font-display);
  font-size: clamp(1rem, 2.5cqi + 0.75rem, 1.15rem);
  margin: 0 0 0.25rem;
  color: var(--cream);
  text-transform: uppercase;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.route-stop-card__header-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: 0.15rem;
}

.route-stop-card__poi-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  font-size: 0.8rem;
  color: var(--accent);
  text-decoration: none;
  font-weight: 500;
  min-height: 44px;
  padding: 0.5rem 1rem;
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  margin-top: var(--spacing-sm);
  cursor: pointer;
  transition: background 200ms var(--ease-spring), color 200ms var(--ease-spring), border-color 200ms var(--ease-spring);
}

.route-stop-card__poi-link:hover {
  background: var(--accent);
  color: var(--gray-900);
  text-decoration: none;
}

.route-stop-card__poi-link:focus {
  outline: none;
}

.route-stop-card__poi-link:focus-visible {
  outline: 2px solid var(--cream);
  outline-offset: 2px;
}

.route-stop-card__text p {
  color: var(--paper);
  opacity: 0.82;
  line-height: 1.75;
  margin-bottom: var(--spacing-sm);
}

.route-stop-card__desc {
  color: var(--paper);
  opacity: 0.82;
  line-height: 1.65;
  font-size: 0.9rem;
  margin: 0 0 var(--spacing-md);
}

.route-stop-card__media-block {
  margin-top: var(--spacing-md);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.route-stop-card__media {
  margin-top: 0;
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

.route-stop-card__audio-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.route-stop-card__audio-list audio {
  width: 100%;
}

.route-stop-card__duration {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--gray-500);
  display: flex;
  align-items: center;
  gap: 0.25rem;
  white-space: nowrap;
}

.route-stop-card__badges {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
  margin-top: 0.35rem;
}

.route-stop-card__custom-badge {
  font-size: 0.65rem;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(74, 158, 255, 0.15);
  color: #6ab0ff;
  font-weight: 500;
}

.route-stop-card__address {
  font-size: 0.72rem;
  color: var(--gray-500);
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
  flex-shrink: 0;
  opacity: 0.75;
  color: var(--gray-400);
}

.tag-lock-icon {
  vertical-align: -0.15em;
  margin-right: 0.2rem;
}

/* ===== Paywall ===== */
.route-paywall {
  text-align: center;
  padding: var(--spacing-2xl) var(--spacing-xl);
  margin-bottom: var(--spacing-2xl);
  border: 2px dashed var(--accent);
  border-radius: var(--radius-md);
  background: linear-gradient(
    135deg,
    rgba(212, 184, 150, 0.04) 0%,
    rgba(212, 184, 150, 0.01) 100%
  );
}

.route-paywall__icon {
  color: var(--accent);
  margin-bottom: var(--spacing-md);
  opacity: 0.8;
}

.route-paywall__title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  color: var(--cream);
  margin-bottom: var(--spacing-sm);
}

.route-paywall__text {
  color: var(--gray-300);
  font-size: 0.9rem;
  line-height: 1.7;
  max-width: 480px;
  margin: 0 auto var(--spacing-lg);
}

.route-paywall__actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
}

.route-paywall__hint {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin: 0;
}

/* ===== Locked stop card ===== */
.route-stop-card--locked .route-stop-card__body {
  border-style: dashed;
  opacity: 0.75;
}

.route-stop-card__locked-stub {
  position: relative;
  padding: var(--spacing-sm) 0;
}

.route-stop-card__locked-blur {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.locked-blur-line {
  height: 12px;
  border-radius: 6px;
  background: var(--gray-700);
  opacity: 0.4;
}

.route-stop-card__locked-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: var(--spacing-sm);
  font-size: 0.72rem;
  color: var(--gray-500);
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--gray-700);
  background: rgba(0, 0, 0, 0.2);
}

.tag-locked {
  background: rgba(212, 184, 150, 0.12);
  color: var(--accent);
  border: 1px solid rgba(212, 184, 150, 0.3);
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
  background: var(--header-bg-solid, rgba(42, 35, 30, 0.96));
  backdrop-filter: blur(8px);
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
  width: 100%;
  background: var(--accent);
  border-radius: 2px;
  transform-origin: left;
  transition: transform 400ms var(--ease-spring);
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

/* ===== Light theme overrides ===== */
:global(:root.app-light) .route-stop-card__body {
  border-color: var(--gray-200);
  background: var(--gray-50);
}

:global(:root.app-light) .route-stop-card--active .route-stop-card__body {
  border-color: var(--accent);
  background: var(--accent-soft);
}

:global(:root.app-light) .route-progress-nav__track {
  background: var(--gray-200);
}

:global(:root.app-light) .route-progress-nav__dot-num {
  background: var(--gray-100);
  border-color: var(--gray-300);
  color: var(--gray-600);
}

:global(:root.app-light) .route-stop-card__connector-line {
  background: var(--gray-200);
}

:global(:root.app-light) .route-stop-card__connector-dot {
  background: var(--white);
  border-color: var(--gray-300);
  color: var(--gray-600);
}

:global(:root.app-light) .route-stop-card__connector-dot.active {
  border-color: var(--accent);
  color: var(--accent-dark);
}

:global(:root.app-light) .route-mobile-progress {
  background: rgba(255, 255, 255, 0.95);
  border-color: var(--gray-200);
}

:global(:root.app-light) .route-mobile-progress__bar {
  background: var(--gray-200);
}

:global(:root.app-light) .route-mobile-progress__label {
  color: var(--gray-700);
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
