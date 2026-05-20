/**
 * Простой кэш в памяти + sessionStorage для снижения числа запросов к API
 * (в т.ч. платных вызовов 2GIS Static API) и ускорения повторных переходов.
 */

const memory = new Map()

export const CACHE_KEYS = {
  poisPublished: 'astra-cache-pois-published-v1',
  poiDetail: (id) => `astra-cache-poi-${id}`,
  staticMap: (lat, lng, w, h, z) =>
    `astra-cache-static-v2-${lat.toFixed(5)}-${lng.toFixed(5)}-${w}x${h}-z${z}`,
}

const DEFAULT_TTL_MS = 15 * 60 * 1000

function readEntry(key) {
  const mem = memory.get(key)
  if (mem) return mem
  try {
    const raw = sessionStorage.getItem(key)
    if (!raw) return null
    const entry = JSON.parse(raw)
    if (entry && typeof entry.at === 'number') {
      memory.set(key, entry)
      return entry
    }
  } catch (_) {
    /* ignore */
  }
  return null
}

/**
 * @template T
 * @param {string} key
 * @param {{ ttl?: number }} [opts]
 * @returns {T | null}
 */
export function cacheGet(key, opts = {}) {
  const ttl = opts.ttl ?? DEFAULT_TTL_MS
  const entry = readEntry(key)
  if (!entry) return null
  if (Date.now() - entry.at > ttl) {
    memory.delete(key)
    try {
      sessionStorage.removeItem(key)
    } catch (_) {
      /* ignore */
    }
    return null
  }
  return entry.value ?? null
}

export function cacheSet(key, value) {
  const entry = { value, at: Date.now() }
  memory.set(key, entry)
  try {
    sessionStorage.setItem(key, JSON.stringify(entry))
  } catch (_) {
    /* quota */
  }
}

export function cacheInvalidate(key) {
  memory.delete(key)
  try {
    sessionStorage.removeItem(key)
  } catch (_) {
    /* ignore */
  }
}
