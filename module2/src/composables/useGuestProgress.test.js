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

  it('increments completedRoutesCount once per unique route', async () => {
    const { useGuestProgress } = await loadComposable()
    const progress = useGuestProgress()

    progress.markRouteCompleted(101)
    progress.markRouteCompleted(101)
    progress.markRouteCompleted(202)

    expect(progress.completedRoutesCount.value).toBe(2)
  })

  it('updates routeTier based on completed routes', async () => {
    const { useGuestProgress } = await loadComposable()
    const progress = useGuestProgress()

    progress.markRouteCompleted(1)
    expect(progress.routeTier.value).toBe('bronze')

    progress.markRouteCompleted(2)
    progress.markRouteCompleted(3)
    progress.markRouteCompleted(4)
    progress.markRouteCompleted(5)
    expect(progress.routeTier.value).toBe('silver')
  })

  it('ignores non-numeric POI ids', async () => {
    const { useGuestProgress } = await loadComposable()
    const progress = useGuestProgress()

    progress.markPoiVisited('abc')
    progress.markPoiVisited(77)

    expect(progress.visitedPoisCount.value).toBe(1)
  })
})
