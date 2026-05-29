<template>
  <div class="page-wrapper routes-page">
    <div class="container routes-page__inner">

      <!-- Hero -->
      <section class="routes-hero motion-reveal" aria-labelledby="routes-hero-title">
        <div class="routes-hero__accent" aria-hidden="true" />
        <p class="routes-eyebrow text-mono">Исследование города</p>
        <h1 id="routes-hero-title" class="routes-hero-title">Онлайн-маршруты</h1>
        <p class="routes-subtitle">
          Испытайте атмосферу города так, как это делают местные жители, и откройте для себя уникальные
          тайны, которые остаются за кадром стандартных экскурсий.
        </p>
      </section>

      <!-- Filter bar -->
      <div class="routes-filter-bar" role="region" aria-label="Фильтры маршрутов">
        <div class="routes-filter-bar__row routes-filter-bar__row--scroll">
          <div class="filter-group motion-stagger-children">
            <button
              v-for="cat in categories"
              :key="cat"
              type="button"
              :class="['filter-chip', { active: activeCategory === cat }]"
              @click="activeCategory = activeCategory === cat ? null : cat"
            >
              {{ cat }}
            </button>
          </div>
        </div>
        <div class="routes-filter-bar__row">
          <div class="filter-group filter-group--toggles motion-stagger-children">
            <button
              type="button"
              :class="['filter-chip', { active: showFree }]"
              @click="showFree = !showFree"
            >
              Бесплатные
            </button>
            <button
              type="button"
              :class="['filter-chip', { active: showPaid }]"
              @click="showPaid = !showPaid"
            >
              Платные
            </button>
            <button
              type="button"
              :class="['filter-chip', { active: showFavoritesOnly }]"
              @click="showFavoritesOnly = !showFavoritesOnly"
            >
              Только избранные
            </button>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="isLoading" class="routes-loading">
        <div class="spinner" />
        <span>Загрузка маршрутов…</span>
      </div>

      <section v-if="!isLoading && featuredRoute" class="route-feature" aria-label="Главный маршрут">
        <div class="route-feature__media">
          <RouteCoverImage
            v-if="featuredHasCover"
            :route="featuredRoute"
            :alt="featuredRoute.title"
          />
          <div v-else class="route-feature__placeholder" aria-hidden="true"></div>
        </div>
        <div class="route-feature__content">
          <h2>{{ featuredRoute.title }}</h2>
          <p>{{ featuredRoute.description }}</p>
          <div class="route-feature__actions">
            <span>{{ featuredRoute.isPaid ? `${featuredRoute.price} ₽` : 'Бесплатно' }}</span>
            <button type="button" class="btn btn-primary btn-sm" @click="openRoute(featuredRoute)">
              Подробнее →
            </button>
          </div>
        </div>
      </section>

      <h2 v-if="!isLoading && filteredRoutes.length" class="reference-section-title">Другие маршруты</h2>

      <!-- Routes grid -->
      <transition-group v-if="!isLoading && filteredRoutes.length" name="slide-up" tag="div" class="routes-grid">
        <RouteCard
          v-for="r in filteredRoutes"
          :key="r.id"
          :route="r"
          :favorite="isRouteFavorite(r.id)"
          @open="openRoute(r)"
          @toggle-favorite="toggleRoute(r.id)"
          @share="openShare(r)"
        />
      </transition-group>

      <div v-if="!isLoading && !filteredRoutes.length" class="empty-state">
        <p>Маршруты не найдены</p>
      </div>

    </div>

    <transition name="fade">
      <div
        v-if="shareModalOpen"
        class="modal-backdrop"
        role="presentation"
        @click.self="closeShareModalFromBackdrop"
      >
        <div class="modal-box route-modal route-modal--share" role="dialog" aria-modal="true" aria-labelledby="share-route-title">
          <button type="button" class="modal-close-btn" aria-label="Закрыть" @click="shareModalOpen = false">✕</button>
          <h2 id="share-route-title" class="route-modal__title route-modal__title--sm">Поделиться маршрутом</h2>
          <input
            :value="shareUrl"
            readonly
            class="form-input route-share-input"
            aria-label="Ссылка на маршрут"
            @focus="$event.target.select()"
          />
          <div class="route-modal-actions">
            <button type="button" class="btn btn-primary" @click="copyShareUrl">Скопировать ссылку</button>
            <button type="button" class="btn btn-ghost" @click="qrOpen = true">Показать QR</button>
          </div>
        </div>
      </div>
    </transition>

    <QrModal v-model="qrOpen" :text="shareUrl" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import RouteCard from '@/components/routes/RouteCard.vue'
import RouteCoverImage from '@/components/routes/RouteCoverImage.vue'
import { normalizeRouteMedia, pickRouteCoverSource } from '@/utils/routeMedia.js'
import QrModal from '@/components/common/QrModal.vue'
import { useToastStore } from '@/store/index.js'
import { useFavorites } from '@/composables/useFavorites.js'
import { javaApi, unwrapJavaList } from '@/api/backend.js'

const toastStore = useToastStore()
const router = useRouter()
const { isRouteFavorite, toggleRoute, favoriteRoutesList } = useFavorites()

function normalizeRouteFromApi(r) {
  if (!r || typeof r !== 'object') return r
  const pois = Array.isArray(r.pois) ? r.pois.map(Number).filter(Number.isFinite) : []
  return normalizeRouteMedia({
    ...r,
    title: r.title || r.name || 'Маршрут',
    isPaid: !!(r.isPaid ?? r.paid),
    priority: Number(r.priority) || 0,
    stops: Array.isArray(r.stops) ? r.stops : [],
    pois,
  })
}

function compareRoutesByPriority(a, b) {
  const pa = Number(a?.priority) || 0
  const pb = Number(b?.priority) || 0
  if (pb !== pa) return pb - pa
  return (Number(b?.id) || 0) - (Number(a?.id) || 0)
}

const routes = ref([])
const isLoading = ref(false)
const activeCategory = ref(null)
const showFree = ref(false)
const showPaid = ref(false)
const showFavoritesOnly = ref(false)

const shareModalOpen = ref(false)
const shareUrl = ref('')
const qrOpen = ref(false)
/** Игнорировать закрытие по backdrop сразу после открытия (ghost click / тач). */
let shareModalIgnoreBackdropUntilMs = 0

function closeShareModalFromBackdrop() {
  if (Date.now() < shareModalIgnoreBackdropUntilMs) return
  shareModalOpen.value = false
}

// 🔹 categories: защита от не-массива
const categories = computed(() => {
  if (!Array.isArray(routes.value)) return []
  return [...new Set(routes.value.map(r => r.category).filter(Boolean))]
})

// 🔹 filteredRoutes: защита от не-массива
const favoriteIdSet = computed(() => new Set(favoriteRoutesList.value))

const filteredRoutes = computed(() => {
  if (!Array.isArray(routes.value)) return []

  let list = routes.value
  if (activeCategory.value) list = list.filter(r => r.category === activeCategory.value)
  if (showFree.value && !showPaid.value) list = list.filter(r => !r.isPaid)
  if (showPaid.value && !showFree.value) list = list.filter(r => r.isPaid)
  if (showFavoritesOnly.value) list = list.filter(r => favoriteIdSet.value.has(r.id))
  return [...list].sort(compareRoutesByPriority)
})

const featuredRoute = computed(() => filteredRoutes.value[0] || null)
const featuredHasCover = computed(() => !!pickRouteCoverSource(featuredRoute.value))

function buildRouteShareUrl(route) {
  const base = window.location.origin + (import.meta.env.BASE_URL || '/').replace(/\/?$/, '/')
  return `${base}routes/${route.id}?utm_source=share&utm_medium=route&utm_campaign=route_${route.id}`
}

async function openShare(route) {
  const url = buildRouteShareUrl(route)
  const title = route?.title || route?.name || 'Маршрут'

  // Web Share API только в безопасном контексте (HTTPS / localhost). На http://IP шаринг часто «молча» не работает — сразу показываем модалку.
  const secureContext =
    typeof window !== 'undefined' &&
    (window.isSecureContext ||
      window.location.hostname === 'localhost' ||
      window.location.hostname === '127.0.0.1')

  const canNativeShare =
    secureContext &&
    typeof navigator !== 'undefined' &&
    typeof navigator.share === 'function'

  if (canNativeShare) {
    try {
      const payload = { title, text: title, url }
      if (typeof navigator.canShare === 'function' && !navigator.canShare(payload)) {
        throw new Error('canShare')
      }
      await navigator.share(payload)
      return
    } catch (e) {
      if (e?.name === 'AbortError') return
    }
  }

  shareUrl.value = url
  // Открываем после завершения текущего клика — иначе тот же жест попадает на backdrop и @click.self закрывает окно.
  await nextTick()
  requestAnimationFrame(() => {
    shareModalIgnoreBackdropUntilMs = Date.now() + 450
    shareModalOpen.value = true
  })
}

async function copyShareUrl() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    toastStore.push('Ссылка скопирована', 'success')
  } catch {
    toastStore.push('Не удалось скопировать', 'error')
  }
}

onMounted(async () => {
  isLoading.value = true
  try {
    const data = await javaApi.routes.getList({ published: 'true' })
    routes.value = unwrapJavaList(data).map(normalizeRouteFromApi).sort(compareRoutesByPriority)
    if (!routes.value.length) {
      routes.value = getMockRoutes().map(normalizeRouteFromApi)
    }
  } catch (err) {
    if (import.meta.env.DEV) console.warn('Java API failed:', err)
    routes.value = getMockRoutes().map(normalizeRouteFromApi)
  } finally {
    isLoading.value = false
  }
})

function openRoute(r) {
  if (r?.id == null) return
  router.push({ name: 'route-details', params: { id: r.id } })
}

function getMockRoutes() {
  return [
    {
      id: 1,
      title: 'Кремль и его окрестности',
      category: 'Архитектура',
      description: 'Прогулка по историческому центру Астрахани: Кремль, Успенский и Троицкий соборы, Пречистенские ворота. Узнайте о многовековой истории главной цитадели Поволжья.',
      duration: '2–3 часа',
      distance: '2.5 км',
      isPaid: false,
      price: null,
      coverImage: null,
      rating: 4.8,
      pois: [1, 2, 3, 4],
      stops: [
        { name: 'Астраханский Кремль', description: 'Главная достопримечательность города' },
        { name: 'Успенский собор', description: 'Шедевр русского барокко XVII в.' },
        { name: 'Троицкий собор', description: 'Один из старейших храмов края' },
        { name: 'Пречистенские ворота', description: 'Главный въезд в кремль' }
      ]
    },
    {
      id: 2,
      title: 'Купеческая Астрахань',
      category: 'История',
      description: 'Маршрут по особнякам купцов XIX века, армянским и индийским торговым рядам. Откройте для себя Астрахань как перекрёсток торговых путей Востока и Запада.',
      duration: '3–4 часа',
      distance: '4 км',
      isPaid: true,
      price: 450,
      coverImage: null,
      rating: 4.6,
      stops: [
        { name: 'Гостиный двор', description: 'Центр торговой жизни XIX века' },
        { name: 'Индийское подворье', description: 'След Великого Шёлкового пути' },
        { name: 'Армянский квартал', description: 'Диаспора купцов-меценатов' },
        { name: 'Особняк Губина', description: 'Жемчужина купеческого модерна' }
      ]
    },
    {
      id: 3,
      title: 'Зодчество Астраханского края',
      category: 'Архитектура',
      description: 'Специализированный маршрут для ценителей архитектуры: деревянное зодчество, каменные палаты, советский конструктивизм и постройки эпохи эклектики.',
      duration: '4–5 часов',
      distance: '5.5 км',
      isPaid: true,
      price: 800,
      coverImage: null,
      rating: 4.9,
      stops: [
        { name: 'Деревянные дома Криуши', description: 'Уникальная резьба XIX века' },
        { name: 'Здание Белого города', description: 'Образец советского ар-деко' },
        { name: 'Дом Тетюшинова', description: 'Памятник деревянного зодчества' }
      ]
    },
    {
      id: 4,
      title: 'Природа дельты Волги',
      category: 'Природа',
      description: 'Пешеходно-водная прогулка по уникальной экосистеме дельты Волги — лотосовые поля, птичьи колонии и рыбацкие сёла.',
      duration: '5–6 часов',
      distance: '8 км',
      isPaid: true,
      price: 1200,
      coverImage: null,
      rating: 4.7,
      stops: [
        { name: 'Лотосовые поля', description: 'Крупнейшие в Европе заросли лотоса' },
        { name: 'Птичья колония', description: 'Более 200 видов птиц' },
        { name: 'Рыбацкое село', description: 'Традиционный уклад жизни' }
      ]
    },
    {
      id: 5,
      title: 'Советская Астрахань',
      category: 'История',
      description: 'Конструктивизм, монументальная скульптура и градостроительство советской эпохи в исторической перспективе.',
      duration: '2 часа',
      distance: '3 км',
      isPaid: false,
      price: null,
      coverImage: null,
      rating: 4.3,
      pois: [1, 3, 5],
      stops: [
        { name: 'Площадь Ленина', description: 'Центр советского города' },
        { name: 'Здание ОГПУ', description: 'Конструктивизм 1930-х' },
        { name: 'Речной вокзал', description: 'Ворота Каспия' }
      ]
    },
    {
      id: 6,
      title: 'Религиозное многообразие',
      category: 'Культура',
      description: 'Мечети, православные храмы, армянская церковь, буддийский хурул и синагога в одном городе — Астрахань как символ межконфессионального диалога.',
      duration: '3 часа',
      distance: '3.5 км',
      isPaid: false,
      price: null,
      coverImage: null,
      rating: 4.5,
      pois: [2, 3, 4],
      stops: [
        { name: 'Белая мечеть', description: 'Главная мечеть Астрахани' },
        { name: 'Армянская церковь', description: 'XVIII век' },
        { name: 'Буддийский хурул', description: 'Единственный в Поволжье' }
      ]
    }
  ]
}
</script>

<style scoped>
.routes-page {
  padding-bottom: var(--spacing-2xl);
  container-type: inline-size;
  container-name: routes;
}

.routes-page__inner {
  max-width: 1200px;
}

.routes-hero {
  position: relative;
  padding: clamp(1.5rem, 4vw, 2.5rem) 0 clamp(1.25rem, 3vw, 2rem);
  max-width: 40rem;
}

.routes-hero__accent {
  position: absolute;
  left: 0;
  top: 0;
  width: 3rem;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), transparent);
  border-radius: 2px;
}

.routes-eyebrow {
  color: var(--accent);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: clamp(0.65rem, 0.5cqi + 0.55rem, 0.75rem);
  margin: 0 0 var(--spacing-sm);
}

.routes-hero-title {
  font-family: var(--font-display);
  font-size: clamp(1.85rem, 3.5cqi + 1rem, 2.65rem);
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: -0.02em;
  margin: 0;
  color: var(--cream);
}

.routes-subtitle {
  margin-top: var(--spacing-md);
  color: var(--gray-400);
  font-size: clamp(0.875rem, 0.8rem + 0.35vw, 0.95rem);
  line-height: 1.75;
  max-width: 48ch;
}

/* Filter bar */
.routes-filter-bar {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) 0;
  border-top: 1px solid rgba(212, 184, 150, 0.12);
  border-bottom: 1px solid rgba(212, 184, 150, 0.12);
  margin-bottom: var(--spacing-xl);
}

.routes-filter-bar__row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-sm);
}

.routes-filter-bar__row--scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 2px;
  scrollbar-width: thin;
  mask-image: linear-gradient(to right, black calc(100% - 1.5rem), transparent);
}

@container routes (min-width: 640px) {
  .routes-filter-bar__row--scroll {
    mask-image: none;
    overflow: visible;
  }
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  min-width: min-content;
}

.filter-group--toggles {
  width: 100%;
}

.filter-chip {
  padding: 0.4rem 0.85rem;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.06em;
  border: 1px solid var(--gray-600);
  border-radius: 999px;
  background: rgba(20, 16, 13, 0.35);
  color: var(--gray-400);
  cursor: pointer;
  transition: border-color var(--transition), color var(--transition), background var(--transition);
  white-space: nowrap;
}

.filter-chip:hover {
  border-color: var(--gray-400);
  color: var(--paper);
}

.filter-chip:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.filter-chip.active {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(200, 169, 110, 0.1);
}

/* Grid */
.routes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr));
  gap: clamp(1rem, 2cqi, 1.5rem);
}

.routes-loading {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  color: var(--gray-400);
  padding: var(--spacing-xl) 0;
}

.empty-state {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--gray-400);
}

/* Route modal */
.route-modal {
  max-width: min(92vw, 720px);
  position: relative;
  border-radius: var(--radius-md);
  border: 1px solid rgba(212, 184, 150, 0.14);
  box-shadow: var(--shadow-modal);
  background: linear-gradient(165deg, rgba(42, 35, 30, 0.98) 0%, rgba(20, 16, 13, 0.99) 100%);
  backdrop-filter: blur(12px);
}

.route-modal--share {
  max-width: 420px;
}

.route-modal__title {
  font-family: var(--font-display);
  font-size: clamp(1.35rem, 2vw + 0.5rem, 1.75rem);
  margin: 0.75rem 0 0.5rem;
  line-height: 1.2;
  color: var(--cream);
}

.route-modal__title--sm {
  font-size: 1.15rem;
  margin: 0 0 0.75rem;
}

.route-share-input {
  margin-bottom: 1rem;
  font-family: var(--font-mono);
  font-size: 0.8rem;
}

.modal-close-btn {
  position: absolute;
  top: var(--spacing-md);
  right: var(--spacing-md);
  z-index: 2;
  background: rgba(20, 16, 13, 0.75);
  border: 1px solid var(--gray-600);
  color: var(--paper);
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 999px;
  transition: border-color var(--transition), background var(--transition);
}

.modal-close-btn:hover {
  border-color: var(--paper);
}

.modal-close-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.route-meta-row {
  display: flex;
  gap: var(--spacing-sm);
  color: var(--gray-400);
  font-size: 0.75rem;
  flex-wrap: wrap;
}

.route-desc {
  color: var(--gray-200);
  line-height: 1.8;
  font-size: 0.875rem;
  margin-bottom: var(--spacing-lg);
}

/* Stops timeline */
.stops-list {
  margin-bottom: var(--spacing-xl);
}

.stops-list__label {
  color: var(--gray-400);
  margin-bottom: 0.75rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  font-size: 0.65rem;
}

.stops-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  position: relative;
  padding-left: 0.25rem;
}

.stops-timeline::before {
  content: '';
  position: absolute;
  left: 13px;
  top: 0.5rem;
  bottom: 0.5rem;
  width: 2px;
  background: linear-gradient(
    to bottom,
    rgba(212, 184, 150, 0.15),
    rgba(212, 184, 150, 0.45),
    rgba(212, 184, 150, 0.15)
  );
  border-radius: 1px;
}

.stop-item {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid rgba(212, 184, 150, 0.08);
  position: relative;
}

.stop-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--gray-900);
  border: 2px solid rgba(212, 184, 150, 0.55);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: 0.65rem;
  flex-shrink: 0;
  box-shadow: 0 0 0 4px rgba(20, 16, 13, 0.9);
}

.stop-name { font-weight: 500; font-size: 0.875rem; margin-bottom: 2px; }
.stop-desc { font-size: 0.75rem; color: var(--gray-400); }

.route-modal-actions {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
  margin-top: var(--spacing-lg);
}

@media (max-width: 768px) {
  .routes-grid { grid-template-columns: 1fr; }
  .routes-filter-bar { gap: var(--spacing-md); }
  .route-modal { max-width: 100%; margin: var(--spacing-md); padding: var(--spacing-lg); }
  .filter-group .filter-chip { min-height: 44px; padding: 0.5rem 1rem; }
}

@media (max-width: 480px) {
  .routes-hero { padding: var(--spacing-lg) 0 var(--spacing-md); }
  .route-card { padding: var(--spacing-lg); }
  .route-modal { margin: var(--spacing-sm); padding: var(--spacing-md); }
  .stops-timeline .stop-item { padding: var(--spacing-sm) 0; }
}

@media (prefers-reduced-motion: reduce) {
  .filter-chip {
    transition: none;
  }
}
</style>
