import { ref, computed, watch } from 'vue'

const STORAGE_KEY = 'astra_guest_progress_v1'

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { visitedPois: new Set(), completedRoutes: new Set() }
    const parsed = JSON.parse(raw)
    const visitedPois = new Set(Array.isArray(parsed.visitedPois) ? parsed.visitedPois.map(Number) : [])
    const completedRoutes = new Set(
      Array.isArray(parsed.completedRoutes) ? parsed.completedRoutes.map(Number) : [],
    )
    return { visitedPois, completedRoutes }
  } catch {
    return { visitedPois: new Set(), completedRoutes: new Set() }
  }
}

function saveState(visited, completed) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      visitedPois: [...visited],
      completedRoutes: [...completed],
    }),
  )
}

const visitedPois = ref(new Set())
const completedRoutes = ref(new Set())
let initialized = false
let persistWatchStarted = false

function ensureInit() {
  if (initialized) return
  const s = loadState()
  visitedPois.value = s.visitedPois
  completedRoutes.value = s.completedRoutes
  initialized = true
}

export function useGuestProgress() {
  ensureInit()

  if (!persistWatchStarted) {
    persistWatchStarted = true
    watch(
      [visitedPois, completedRoutes],
      () => {
        saveState(visitedPois.value, completedRoutes.value)
      },
      { deep: true },
    )
  }

  const visitedPoisCount = computed(() => visitedPois.value.size)
  const completedRoutesCount = computed(() => completedRoutes.value.size)

  function markPoiVisited(id) {
    ensureInit()
    const n = Number(id)
    if (!Number.isFinite(n)) return
    const next = new Set(visitedPois.value)
    next.add(n)
    visitedPois.value = next
  }

  function markRouteCompleted(id) {
    ensureInit()
    const n = Number(id)
    if (!Number.isFinite(n)) return
    const next = new Set(completedRoutes.value)
    next.add(n)
    completedRoutes.value = next
  }

  /** Уровень по маршрутам: bronze | silver | gold | null */
  const routeTier = computed(() => {
    const n = completedRoutesCount.value
    if (n >= 10) return 'gold'
    if (n >= 5) return 'silver'
    if (n >= 1) return 'bronze'
    return null
  })

  return {
    markPoiVisited,
    markRouteCompleted,
    visitedPoisCount,
    completedRoutesCount,
    routeTier,
  }
}
