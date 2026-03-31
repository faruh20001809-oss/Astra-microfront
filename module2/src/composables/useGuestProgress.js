import { ref, computed, watch } from 'vue'

const STORAGE_KEY = 'astra_guest_progress_v1'
const ACHIEVEMENTS_KEY = 'astra_user_achievements_v1'
const REWARDS_KEY = 'astra_user_rewards_v1'

function nowIso() {
  return new Date().toISOString()
}

function safeJsonParse(raw, fallback) {
  try {
    return JSON.parse(raw)
  } catch {
    return fallback
  }
}

function loadState() {
  const parsed = safeJsonParse(localStorage.getItem(STORAGE_KEY), null)
  if (!parsed) {
    return {
      visitedPois: new Set(),
      completedRoutes: new Set(),
      completedPaidRoutes: 0,
      completedFreeRoutes: 0,
      activityLog: [],
      updatedAt: nowIso(),
    }
  }
  return {
    visitedPois: new Set(Array.isArray(parsed.visitedPois) ? parsed.visitedPois.map(Number) : []),
    completedRoutes: new Set(
      Array.isArray(parsed.completedRoutes) ? parsed.completedRoutes.map(Number) : [],
    ),
    completedPaidRoutes: Number(parsed.completedPaidRoutes) || 0,
    completedFreeRoutes: Number(parsed.completedFreeRoutes) || 0,
    activityLog: Array.isArray(parsed.activityLog) ? parsed.activityLog.slice(0, 150) : [],
    updatedAt: parsed.updatedAt || nowIso(),
  }
}

function loadAchievements() {
  const parsed = safeJsonParse(localStorage.getItem(ACHIEVEMENTS_KEY), null)
  if (!parsed) return { unlocked: [], progress: {}, updatedAt: nowIso() }
  return {
    unlocked: Array.isArray(parsed.unlocked) ? parsed.unlocked : [],
    progress: parsed.progress && typeof parsed.progress === 'object' ? parsed.progress : {},
    updatedAt: parsed.updatedAt || nowIso(),
  }
}

function loadRewards() {
  const parsed = safeJsonParse(localStorage.getItem(REWARDS_KEY), null)
  if (!parsed) return { available: [], history: [], updatedAt: nowIso() }
  return {
    available: Array.isArray(parsed.available) ? parsed.available : [],
    history: Array.isArray(parsed.history) ? parsed.history : [],
    updatedAt: parsed.updatedAt || nowIso(),
  }
}

function saveProgressState(state) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      visitedPois: [...state.visitedPois],
      completedRoutes: [...state.completedRoutes],
      completedPaidRoutes: state.completedPaidRoutes,
      completedFreeRoutes: state.completedFreeRoutes,
      activityLog: state.activityLog.slice(0, 150),
      updatedAt: state.updatedAt,
    }),
  )
}

function saveAchievementsState(state) {
  localStorage.setItem(ACHIEVEMENTS_KEY, JSON.stringify(state))
}

function saveRewardsState(state) {
  localStorage.setItem(REWARDS_KEY, JSON.stringify(state))
}

const progressState = ref(loadState())
const achievementsState = ref(loadAchievements())
const rewardsState = ref(loadRewards())
let persistWatchStarted = false

const ACHIEVEMENTS = [
  { id: 'first_steps', title: 'Первые шаги', type: 'pois', threshold: 3 },
  { id: 'explorer', title: 'Исследователь', type: 'pois', threshold: 10 },
  { id: 'route_starter', title: 'Маршрутизатор', type: 'free_routes', threshold: 3 },
  { id: 'collector', title: 'Коллекционер', type: 'favorites_poi', threshold: 5 },
  { id: 'traveler_plus', title: 'Путешественник+', type: 'paid_routes', threshold: 2 },
]

function pushActivity(type, payload = {}) {
  const entry = { type, at: nowIso(), ...payload }
  progressState.value = {
    ...progressState.value,
    activityLog: [entry, ...progressState.value.activityLog].slice(0, 150),
    updatedAt: nowIso(),
  }
}

function syncAchievementProgress({ visitedPois, freeRoutes, paidRoutes, favoritePois = 0 } = {}) {
  const prev = achievementsState.value
  const progress = { ...prev.progress }
  progress.first_steps = Number(visitedPois ?? progress.first_steps ?? 0)
  progress.explorer = Number(visitedPois ?? progress.explorer ?? 0)
  progress.route_starter = Number(freeRoutes ?? progress.route_starter ?? 0)
  progress.collector = Number(favoritePois ?? progress.collector ?? 0)
  progress.traveler_plus = Number(paidRoutes ?? progress.traveler_plus ?? 0)

  const unlocked = new Set(prev.unlocked)
  for (const ach of ACHIEVEMENTS) {
    const value = Number(progress[ach.id]) || 0
    if (value >= ach.threshold && !unlocked.has(ach.id)) {
      unlocked.add(ach.id)
      pushActivity('achievement_unlocked', { achievementId: ach.id, title: ach.title })
    }
  }

  achievementsState.value = {
    unlocked: [...unlocked],
    progress,
    updatedAt: nowIso(),
  }
}

function grantActivityRewardIfEligible() {
  const p = progressState.value
  const eligiblePacks = Math.min(Math.floor(p.completedPaidRoutes / 2), Math.floor(p.completedFreeRoutes / 3))
  const alreadyGranted = rewardsState.value.available.length + rewardsState.value.history.length
  const toGrant = Math.max(eligiblePacks - alreadyGranted, 0)
  if (toGrant <= 0) return

  const nextAvailable = [...rewardsState.value.available]
  for (let i = 0; i < toGrant; i += 1) {
    const token = `rw_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    nextAvailable.push({
      id: token,
      kind: 'free_paid_route',
      createdAt: nowIso(),
      usedAt: null,
      usedForRouteId: null,
    })
    pushActivity('reward_granted', { rewardId: token, kind: 'free_paid_route' })
  }
  rewardsState.value = { ...rewardsState.value, available: nextAvailable, updatedAt: nowIso() }
}

export function useGuestProgress() {
  if (!persistWatchStarted) {
    persistWatchStarted = true
    watch(progressState, () => saveProgressState(progressState.value), { deep: true })
    watch(achievementsState, () => saveAchievementsState(achievementsState.value), { deep: true })
    watch(rewardsState, () => saveRewardsState(rewardsState.value), { deep: true })
  }

  const visitedPoisCount = computed(() => progressState.value.visitedPois.size)
  const completedRoutesCount = computed(() => progressState.value.completedRoutes.size)
  const completedFreeRoutesCount = computed(() => progressState.value.completedFreeRoutes)
  const completedPaidRoutesCount = computed(() => progressState.value.completedPaidRoutes)

  const routeTier = computed(() => {
    const n = completedRoutesCount.value
    if (n >= 10) return 'gold'
    if (n >= 5) return 'silver'
    if (n >= 1) return 'bronze'
    return null
  })

  const rewardProgress = computed(() => {
    const paidRatio = Math.min(progressState.value.completedPaidRoutes / 2, 1)
    const freeRatio = Math.min(progressState.value.completedFreeRoutes / 3, 1)
    const total = Math.round(((paidRatio + freeRatio) / 2) * 100)
    return {
      paidCompleted: progressState.value.completedPaidRoutes,
      freeCompleted: progressState.value.completedFreeRoutes,
      progressPercent: total,
      eligible: progressState.value.completedPaidRoutes >= 2 && progressState.value.completedFreeRoutes >= 3,
    }
  })

  function markPoiVisited(id) {
    const n = Number(id)
    if (!Number.isFinite(n)) return
    const next = new Set(progressState.value.visitedPois)
    const before = next.size
    next.add(n)
    if (next.size === before) return
    progressState.value = { ...progressState.value, visitedPois: next, updatedAt: nowIso() }
    pushActivity('poi_visited', { poiId: n })
    syncAchievementProgress({ visitedPois: next.size })
  }

  function markRouteCompleted(id, options = {}) {
    const n = Number(id)
    if (!Number.isFinite(n)) return
    const next = new Set(progressState.value.completedRoutes)
    const isNew = !next.has(n)
    next.add(n)
    if (!isNew) return

    const paid = Boolean(options.paid)
    progressState.value = {
      ...progressState.value,
      completedRoutes: next,
      completedPaidRoutes: progressState.value.completedPaidRoutes + (paid ? 1 : 0),
      completedFreeRoutes: progressState.value.completedFreeRoutes + (paid ? 0 : 1),
      updatedAt: nowIso(),
    }
    pushActivity('route_completed', { routeId: n, paid })
    syncAchievementProgress({
      freeRoutes: progressState.value.completedFreeRoutes,
      paidRoutes: progressState.value.completedPaidRoutes,
    })
    grantActivityRewardIfEligible()
  }

  function syncFromCounters({ visitedPois, freeRoutes, paidRoutes, favoritePois } = {}) {
    const v = Number(visitedPois)
    const f = Number(freeRoutes)
    const p = Number(paidRoutes)
    progressState.value = {
      ...progressState.value,
      completedFreeRoutes: Number.isFinite(f) && f >= 0 ? Math.max(progressState.value.completedFreeRoutes, f) : progressState.value.completedFreeRoutes,
      completedPaidRoutes: Number.isFinite(p) && p >= 0 ? Math.max(progressState.value.completedPaidRoutes, p) : progressState.value.completedPaidRoutes,
      updatedAt: nowIso(),
    }
    syncAchievementProgress({
      visitedPois: Number.isFinite(v) && v >= 0 ? Math.max(visitedPoisCount.value, v) : visitedPoisCount.value,
      freeRoutes: progressState.value.completedFreeRoutes,
      paidRoutes: progressState.value.completedPaidRoutes,
      favoritePois: Number(favoritePois) || 0,
    })
    grantActivityRewardIfEligible()
  }

  function consumeReward(routeId = null) {
    const available = [...rewardsState.value.available]
    if (!available.length) return false
    const reward = available.shift()
    const history = [
      {
        ...reward,
        usedAt: nowIso(),
        usedForRouteId: routeId != null ? Number(routeId) : null,
      },
      ...rewardsState.value.history,
    ]
    rewardsState.value = {
      ...rewardsState.value,
      available,
      history: history.slice(0, 50),
      updatedAt: nowIso(),
    }
    pushActivity('reward_used', { rewardId: reward.id, routeId: routeId != null ? Number(routeId) : null })
    return true
  }

  function getSnapshot() {
    return {
      progress: {
        visitedPois: [...progressState.value.visitedPois],
        completedRoutes: [...progressState.value.completedRoutes],
        completedPaidRoutes: progressState.value.completedPaidRoutes,
        completedFreeRoutes: progressState.value.completedFreeRoutes,
        activityLog: [...progressState.value.activityLog],
        updatedAt: progressState.value.updatedAt,
      },
      achievements: { ...achievementsState.value },
      rewards: { ...rewardsState.value },
    }
  }

  function mergeSnapshot(snapshot = {}) {
    const remoteProgress = snapshot.progress || {}
    const mergedVisited = new Set([
      ...progressState.value.visitedPois,
      ...((Array.isArray(remoteProgress.visitedPois) ? remoteProgress.visitedPois : []).map(Number)),
    ])
    const mergedCompleted = new Set([
      ...progressState.value.completedRoutes,
      ...((Array.isArray(remoteProgress.completedRoutes) ? remoteProgress.completedRoutes : []).map(Number)),
    ])
    progressState.value = {
      ...progressState.value,
      visitedPois: mergedVisited,
      completedRoutes: mergedCompleted,
      completedPaidRoutes: Math.max(progressState.value.completedPaidRoutes, Number(remoteProgress.completedPaidRoutes) || 0),
      completedFreeRoutes: Math.max(progressState.value.completedFreeRoutes, Number(remoteProgress.completedFreeRoutes) || 0),
      activityLog: [
        ...(Array.isArray(remoteProgress.activityLog) ? remoteProgress.activityLog : []),
        ...progressState.value.activityLog,
      ].slice(0, 150),
      updatedAt: nowIso(),
    }

    const remoteAch = snapshot.achievements || {}
    const unlocked = new Set([
      ...(Array.isArray(achievementsState.value.unlocked) ? achievementsState.value.unlocked : []),
      ...(Array.isArray(remoteAch.unlocked) ? remoteAch.unlocked : []),
    ])
    achievementsState.value = {
      unlocked: [...unlocked],
      progress: {
        ...(remoteAch.progress && typeof remoteAch.progress === 'object' ? remoteAch.progress : {}),
        ...achievementsState.value.progress,
      },
      updatedAt: nowIso(),
    }

    const remoteRewards = snapshot.rewards || {}
    rewardsState.value = {
      available: [
        ...(Array.isArray(remoteRewards.available) ? remoteRewards.available : []),
        ...rewardsState.value.available,
      ],
      history: [
        ...(Array.isArray(remoteRewards.history) ? remoteRewards.history : []),
        ...rewardsState.value.history,
      ].slice(0, 50),
      updatedAt: nowIso(),
    }
    grantActivityRewardIfEligible()
  }

  return {
    markPoiVisited,
    markRouteCompleted,
    syncFromCounters,
    consumeReward,
    getSnapshot,
    mergeSnapshot,
    visitedPoisCount,
    completedRoutesCount,
    completedFreeRoutesCount,
    completedPaidRoutesCount,
    rewardProgress,
    routeTier,
    achievements: computed(() => ACHIEVEMENTS),
    achievementsState: computed(() => achievementsState.value),
    rewardsState: computed(() => rewardsState.value),
    activityLog: computed(() => progressState.value.activityLog),
  }
}
