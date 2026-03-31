import { beforeEach, describe, expect, it, vi } from 'vitest'

function createLocalStorageMock() {
  const storage = new Map()
  return {
    getItem: (key) => (storage.has(key) ? storage.get(key) : null),
    setItem: (key, value) => storage.set(key, String(value)),
    removeItem: (key) => storage.delete(key),
    clear: () => storage.clear(),
  }
}

async function loadComposable() {
  vi.resetModules()
  return import('./useGuestProgress.js')
}

describe('useGuestProgress', () => {
  beforeEach(() => {
    globalThis.localStorage = createLocalStorageMock()
  })

  it('increments paid/free counters once per unique route', async () => {
    const { useGuestProgress } = await loadComposable()
    const progress = useGuestProgress()

    progress.markRouteCompleted(101, { paid: true })
    progress.markRouteCompleted(101, { paid: true })
    progress.markRouteCompleted(202, { paid: false })

    expect(progress.completedPaidRoutesCount.value).toBe(1)
    expect(progress.completedFreeRoutesCount.value).toBe(1)
    expect(progress.completedRoutesCount.value).toBe(2)
  })

  it('grants one reward after 2 paid and 3 free routes', async () => {
    const { useGuestProgress } = await loadComposable()
    const progress = useGuestProgress()

    progress.markRouteCompleted(1, { paid: true })
    progress.markRouteCompleted(2, { paid: true })
    progress.markRouteCompleted(3, { paid: false })
    progress.markRouteCompleted(4, { paid: false })
    progress.markRouteCompleted(5, { paid: false })

    expect(progress.rewardsState.value.available).toHaveLength(1)
    expect(progress.rewardProgress.value.eligible).toBe(true)
  })

  it('consumes reward and writes usage history', async () => {
    const { useGuestProgress } = await loadComposable()
    const progress = useGuestProgress()

    progress.syncFromCounters({ paidRoutes: 2, freeRoutes: 3 })
    const consumed = progress.consumeReward(777)

    expect(consumed).toBe(true)
    expect(progress.rewardsState.value.available).toHaveLength(0)
    expect(progress.rewardsState.value.history).toHaveLength(1)
    expect(progress.rewardsState.value.history[0].usedForRouteId).toBe(777)
  })
})
