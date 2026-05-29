import { defineStore } from 'pinia'
import { javaApi } from '@/api/backend.js'
import { CACHE_KEYS, cacheGet, cacheSet } from '@/utils/appCache.js'
import { resolveApiMediaUrl } from '@/utils/routeMedia.js'

/* ─────────────────────────────────────────
   Cart Store
───────────────────────────────────────── */
const CART_STORAGE_KEY = 'astra-cart'
const THEME_STORAGE_KEY = 'astra-theme'
const VALID_THEMES = ['dark', 'light']

function loadCartFromStorage() {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY)
    if (raw) {
      const arr = JSON.parse(raw)
      return Array.isArray(arr) ? arr : []
    }
  } catch (_) {}
  return []
}

function saveCartToStorage(items) {
  try {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items))
  } catch (_) {}
}

/** По умолчанию — светлая тема; переключение в UI и `localStorage['astra-theme']`. */
function detectSystemTheme() {
  return 'light'
}

function loadThemeFromStorage() {
  try {
    const raw = localStorage.getItem(THEME_STORAGE_KEY)
    if (VALID_THEMES.includes(raw)) return raw
  } catch (_) {}
  return 'light'
}

function saveThemeToStorage(theme) {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme)
  } catch (_) {}
}

function applyThemeToDom(theme) {
  if (typeof document === 'undefined') return

  const isDark = theme === 'dark'
  document.documentElement.classList.toggle('app-dark', isDark)
  document.documentElement.classList.toggle('app-light', !isDark)
  document.body?.classList.toggle('app-dark-theme', isDark)
  document.body?.classList.toggle('app-light-theme', !isDark)
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: loadCartFromStorage(),
    isOpen: false
  }),
  getters: {
    totalCount: (state) => state.items.reduce((sum, i) => sum + (i.qty || 1), 0),
    totalPrice: (state) => state.items.reduce((sum, i) => sum + (i.price || 0) * (i.qty || 1), 0)
  },
  actions: {
    addItem(product) {
      const found = this.items.find(i => i.id === product.id)
      if (found) {
        found.qty = (found.qty || 1) + 1
      } else {
        this.items.push({ ...product, qty: 1 })
      }
      saveCartToStorage(this.items)
    },
    removeItem(id) {
      this.items = this.items.filter(i => i.id !== id)
      saveCartToStorage(this.items)
    },
    updateQty(id, qty) {
      const item = this.items.find(i => i.id === id)
      if (item) {
        if (qty <= 0) this.removeItem(id)
        else { item.qty = qty; saveCartToStorage(this.items) }
      }
    },
    clearCart() {
      this.items = []
      saveCartToStorage(this.items)
    },
    toggleCart() {
      this.isOpen = !this.isOpen
    }
  }
})

/* ─────────────────────────────────────────
   UI Store
───────────────────────────────────────── */
export const useUiStore = defineStore('ui', {
  state: () => ({
    theme: loadThemeFromStorage(),
  }),
  getters: {
    isDark: (state) => state.theme === 'dark',
  },
  actions: {
    initTheme() {
      const nextTheme = this.theme && VALID_THEMES.includes(this.theme)
        ? this.theme
        : detectSystemTheme()
      this.theme = nextTheme
      applyThemeToDom(nextTheme)
      saveThemeToStorage(nextTheme)
    },
    setTheme(theme) {
      if (!VALID_THEMES.includes(theme)) return
      this.theme = theme
      applyThemeToDom(theme)
      saveThemeToStorage(theme)
    },
    toggleTheme() {
      this.setTheme(this.theme === 'dark' ? 'light' : 'dark')
    },
  },
})

/* ─────────────────────────────────────────
   Map Store
───────────────────────────────────────── */
export const useMapStore = defineStore('map', {
  state: () => ({
    pois: [],
    selectedPoi: null,
    activeFilters: [],
    userLocation: null,
    isLoadingPois: false,
    nearbyPoi: null,
    /** Активный маршрут с карты: { id, title, poiIds } — задаётся со страницы маршрутов перед переходом на «/». */
    activeFollowRoute: null,
  }),


  getters: {
    filteredPois: (state) => {
      if (!Array.isArray(state.pois)) return []
      if (state.activeFilters.length === 0) return state.pois
      return state.pois.filter(poi => state.activeFilters.includes(poi.category))
    },

    categories: (state) => {
      if (!Array.isArray(state.pois)) return []
      return [...new Set(state.pois.map(p => p.category).filter(Boolean))]
    },

    /** Ближайшая точка к пользователю и расстояние в метрах (для GPS). */
    nearestPoiWithDistance: (state) => {
      if (!state.userLocation || !Array.isArray(state.pois) || state.pois.length === 0) return null
      const { lat, lng } = state.userLocation
      let nearest = null
      let minDist = Infinity
      for (const poi of state.pois) {
        const d = haversine(lat, lng, poi.lat, poi.lng)
        if (d < minDist) {
          minDist = d
          nearest = poi
        }
      }
      return nearest ? { poi: nearest, distanceMetres: Math.round(minDist) } : null
    }
  },

  actions: {
    async fetchPois({ force = false } = {}) {
      if (!force) {
        const cached = cacheGet(CACHE_KEYS.poisPublished)
        if (Array.isArray(cached) && cached.length) {
          this.pois = cached
          return
        }
      }

      this.isLoadingPois = true
      try {
        const data = await javaApi.pois.getList({ status: 'PUBLISHED' })
        const raw = Array.isArray(data) ? data : []
        if (!raw.length) {
          this.pois = getMockPois()
        } else {
          this.pois = raw.map((p) => normalizePoi(p))
        }
        cacheSet(CACHE_KEYS.poisPublished, this.pois)
      } catch (err) {
        console.error('Failed to fetch POIs:', err)
        this.pois = getMockPois()
      } finally {
        this.isLoadingPois = false
      }
    },

    /** Одна точка для страницы /pois/:id (кэш в sessionStorage). */
    async fetchPoiById(id, { force = false } = {}) {
      const numId = Number(id)
      if (!Number.isFinite(numId) || numId <= 0) return null

      const cacheKey = CACHE_KEYS.poiDetail(numId)
      if (!force) {
        const cached = cacheGet(cacheKey)
        if (cached?.id) return cached
      }

      try {
        const raw = await javaApi.pois.getById(numId)
        const normalized = normalizePoi(raw)
        if (normalized?.id) {
          cacheSet(cacheKey, normalized)
          return normalized
        }
      } catch (err) {
        console.error('Failed to fetch POI by id:', err)
      }

      // Фолбэк: берём точку из уже загруженного каталога или из мок-данных,
      // чтобы страница работала при недоступном backend (как fetchPois).
      const fromCatalog = (Array.isArray(this.pois) ? this.pois : [])
        .find((p) => Number(p?.id) === numId)
      if (fromCatalog?.id) return fromCatalog

      const fromMock = getMockPois()
        .map((p) => normalizePoi(p))
        .find((p) => Number(p?.id) === numId)
      return fromMock?.id ? fromMock : null
    },

    setSelected(poi) {
      this.selectedPoi = poi
    },

    clearSelected() {
      this.selectedPoi = null
    },

    setUserLocation(coords) {
      this.userLocation = coords
      this.checkGeoTriggers()
    },

    toggleFilter(category) {
      const idx = this.activeFilters.indexOf(category)
      if (idx === -1) this.activeFilters.push(category)
      else this.activeFilters.splice(idx, 1)
    },

    setActiveFollowRoute(payload) {
      if (!payload || payload.id == null) {
        this.activeFollowRoute = null
        return
      }
      const poiIds = Array.isArray(payload.poiIds)
        ? payload.poiIds.map(Number).filter((n) => Number.isFinite(n))
        : []
      this.activeFollowRoute = {
        id: Number(payload.id),
        title: payload.title || 'Маршрут',
        poiIds,
      }
    },

    clearActiveFollowRoute() {
      this.activeFollowRoute = null
    },

    /** Определяет, рядом ли пользователь с какой-либо точкой (в радиусе 150 м). */
    checkGeoTriggers() {
      if (!this.userLocation || !Array.isArray(this.pois)) return
      const { lat, lng } = this.userLocation
      const TRIGGER_RADIUS_M = 150

      for (const poi of this.pois) {
        const dist = haversine(lat, lng, poi.lat, poi.lng)
        if (dist <= TRIGGER_RADIUS_M) {
          if (!this.nearbyPoi || this.nearbyPoi.id !== poi.id) {
            this.nearbyPoi = poi
          }
          return
        }
      }
      this.nearbyPoi = null
    }
  }
})

/* ─────────────────────────────────────────
   Toast Store
───────────────────────────────────────── */
export const useToastStore = defineStore('toast', {
  state: () => ({ toasts: [] }),
  actions: {
    push(msg, type = 'info', duration = 3500) {
      const id = Date.now()
      this.toasts.push({ id, msg, type })
      setTimeout(() => this.remove(id), duration)
    },
    /** Всплывашка «достижение» — фиксированный угол, см. App.vue + .toast--achievement */
    pushAchievement(achievementTitle, duration = 5200) {
      const id = Date.now() + Math.random()
      this.toasts.push({
        id,
        type: 'achievement',
        achievementTitle: String(achievementTitle || ''),
        msg: '',
      })
      setTimeout(() => this.remove(id), duration)
    },
    remove(id) {
      this.toasts = this.toasts.filter(t => t.id !== id)
    }
  }
})

/* ─────────────────────────────────────────
   Helpers
───────────────────────────────────────── */

/**
 * Нормализует POI из Java-API в форму, удобную для UI (карты, карточки, деталки).
 * Поддерживает новые поля: detailText, maxAudioUrl, maxVideoUrl, maxPlaylistUrl
 * (см. `docs/route-points-preorder-tech-analysis.md`, разделы 2 и 4.2).
 */
export function normalizePoi(p) {
  if (!p || typeof p !== 'object') return null
  const extended = p.extendedInfo || {}
  const shortDescription =
    p.shortDescription ||
    p.summary ||
    p.cardDescription ||
    extended.shortDescription ||
    extended.summary ||
    p.description ||
    ''
  const detailText =
    p.detailText ||
    p.fullDescription ||
    p.articleText ||
    p.historyText ||
    p.longDescription ||
    extended.detailText ||
    extended.fullDescription ||
    extended.articleText ||
    extended.historyText ||
    extended.longDescription ||
    p.description ||
    ''
  return {
    id: p.id,
    name: p.name,
    description: shortDescription,
    shortDescription,
    mapDescription: shortDescription,
    mapLabel: p.mapLabel || p.label || extended.mapLabel || p.name,
    detailText,
    fullDescription: detailText,
    articleText: detailText,
    category: p.category,
    lat: p.latitude ?? p.coordinates?.latitude ?? 0,
    lng: p.longitude ?? p.coordinates?.longitude ?? 0,
    address: p.address,
    style: p.style || extended.style || null,
    year: extended.foundedYear ?? p.year ?? p.foundedYear ?? null,
    architect: extended.architect ?? p.architect ?? null,
    image: resolveApiMediaUrl(p.image || p.imageUrl || null),
    photos: (Array.isArray(p.photos) ? p.photos : [])
      .map((ph) => {
        const url = resolveApiMediaUrl(ph?.url || ph?.src)
        if (!url) return null
        return { ...ph, url, src: url }
      })
      .filter(Boolean),
    tags: p.tags || [],
    maxAudioUrl: p.maxAudioUrl || extended.maxAudioUrl || null,
    maxVideoUrl: p.maxVideoUrl || extended.maxVideoUrl || null,
    maxPlaylistUrl: p.maxPlaylistUrl || extended.maxPlaylistUrl || null,
  }
}

function haversine(lat1, lon1, lat2, lon2) {
  const R = 6371000
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function getMockPois() {
  return [
    {
      id: 1, lat: 46.3497, lng: 48.0408,
      name: 'Астраханский Кремль',
      category: 'архитектура',
      description: 'Астраханский кремль — выдающийся памятник русского оборонного зодчества XVI–XVII веков.',
      shortDescription: 'Выдающийся памятник русского оборонного зодчества XVI–XVII веков.',
      fullDescription: 'История этого места уходит глубоко в прошлое и хранит множество городских легенд. Кремль стал главной цитаделью Астрахани и точкой, вокруг которой складывался исторический центр.\n\nСтены, башни и соборы формировали силуэт города на протяжении столетий. Каждый элемент ансамбля рассказывает о ремесле мастеров, оборонительной архитектуре и культурной памяти региона.',
      year: 1582,
      architect: 'Михаил Вельяминов',
      photos: []
    },
    {
      id: 2, lat: 46.3453, lng: 48.0331,
      name: 'Успенский собор',
      category: 'архитектура',
      description: 'Главный православный храм Астраханской епархии, построенный в стиле русского барокко.',
      shortDescription: 'Главный православный храм Астраханской епархии в стиле русского барокко.',
      fullDescription: 'Собор занимает важное место в архитектурном ансамбле города. Его объем, декоративные детали и исторический контекст связывают религиозную жизнь Астрахани с развитием городской среды.\n\nСегодня это один из ключевых объектов, через который можно читать историю региона: от строительных традиций до изменений в облике исторического центра.',
      year: 1710,
      architect: 'Дорофей Мякишев',
      photos: []
    },
    {
      id: 3, lat: 46.3512, lng: 48.0452,
      name: 'Астраханский государственный музей-заповедник',
      category: 'музеи',
      description: 'Один из крупнейших музеев Поволжья с богатой коллекцией по истории и культуре края.',
      year: 1837,
      architect: null,
      photos: []
    },
    {
      id: 4, lat: 46.3479, lng: 48.0387,
      name: 'Дом-музей Ульяновых',
      category: 'музеи',
      description: 'Исторический дом, связанный с семьёй Ульяновых.',
      year: 1924,
      architect: null,
      photos: []
    },
    {
      id: 5, lat: 46.3530, lng: 48.0310,
      name: 'Братский сад',
      category: 'парки',
      description: 'Старейший парк Астрахани, разбитый в честь воинов, павших в Первую мировую войну.',
      year: 1915,
      architect: null,
      photos: []
    }
  ]
}
