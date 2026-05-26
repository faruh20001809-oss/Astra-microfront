import { ref, computed, watch } from 'vue'

const STORAGE_KEY = 'astra_favorites_v1'

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { routes: new Set(), pois: new Set() }
    const parsed = JSON.parse(raw)
    const routes = new Set(Array.isArray(parsed.routes) ? parsed.routes.map(Number) : [])
    const pois = new Set(Array.isArray(parsed.pois) ? parsed.pois.map(Number) : [])
    return { routes, pois }
  } catch {
    return { routes: new Set(), pois: new Set() }
  }
}

function saveState(routesSet, poisSet) {
  const payload = {
    routes: [...routesSet],
    pois: [...poisSet],
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
}

const favoriteRoutes = ref(new Set())
const favoritePois = ref(new Set())
let initialized = false
let persistWatchStarted = false
let favSaveTimer = null

function scheduleFavSave() {
  if (favSaveTimer) return
  favSaveTimer = setTimeout(() => {
    favSaveTimer = null
    saveState(favoriteRoutes.value, favoritePois.value)
  }, 200)
}

function ensureInit() {
  if (initialized) return
  const { routes, pois } = loadState()
  favoriteRoutes.value = routes
  favoritePois.value = pois
  initialized = true
}

export function useFavorites() {
  ensureInit()

  if (!persistWatchStarted) {
    persistWatchStarted = true
    watch(
      [favoriteRoutes, favoritePois],
      scheduleFavSave,
      { deep: true },
    )
  }

  const favoriteRoutesList = computed(() => [...favoriteRoutes.value])
  const favoritePoisList = computed(() => [...favoritePois.value])

  function isRouteFavorite(id) {
    ensureInit()
    return favoriteRoutes.value.has(Number(id))
  }

  function toggleRoute(id) {
    ensureInit()
    const n = Number(id)
    const next = new Set(favoriteRoutes.value)
    if (next.has(n)) next.delete(n)
    else next.add(n)
    favoriteRoutes.value = next
  }

  function isPoiFavorite(id) {
    ensureInit()
    return favoritePois.value.has(Number(id))
  }

  function togglePoi(id) {
    ensureInit()
    const n = Number(id)
    const next = new Set(favoritePois.value)
    if (next.has(n)) next.delete(n)
    else next.add(n)
    favoritePois.value = next
  }

  return {
    isRouteFavorite,
    toggleRoute,
    isPoiFavorite,
    togglePoi,
    favoriteRoutesList,
    favoritePoisList,
  }
}
