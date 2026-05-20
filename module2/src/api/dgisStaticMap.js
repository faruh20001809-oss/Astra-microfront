/**
 * 2GIS Static API — статическое изображение карты с меткой.
 * @see https://docs.2gis.com/en/maps/others/static/overview
 * @see https://docs.2gis.com/en/maps/others/static/reference
 */

import { getDgisMapKey } from '@/api/dgisConfig.js'
import { CACHE_KEYS, cacheGet, cacheSet } from '@/utils/appCache.js'

const STATIC_API_VERSION = '2.0'

function getStaticBaseUrl() {
  const raw = import.meta.env.VITE_DGIS_STATIC_BASE
  if (raw != null && String(raw).trim() !== '') {
    return String(raw).trim().replace(/\/$/, '')
  }
  return `https://static.maps.2gis.com/${STATIC_API_VERSION}`
}

/**
 * URL PNG/JPEG карты 2GIS с меткой в точке (lat, lng).
 * Координаты в параметре pt — широта, долгота (как в документации 2GIS).
 */
export function getDgisStaticMapUrl(lat, lng, options = {}) {
  const la = Number(lat)
  const ln = Number(lng)
  if (!Number.isFinite(la) || !Number.isFinite(ln) || (la === 0 && ln === 0)) {
    return ''
  }

  const key = getDgisMapKey()
  if (!key) return ''

  const width = Math.min(Math.max(Math.round(options.width ?? 640), 80), 2048)
  const height = Math.min(Math.max(Math.round(options.height ?? 280), 60), 2048)
  const zoom = Math.min(Math.max(Math.round(options.zoom ?? 16), 10), 18)

  const params = new URLSearchParams({
    s: `${width}x${height}`,
    z: String(zoom),
    /** pin, red, small — как в примерах Static API */
    pt: `${la},${ln}~k:p~c:rd~s:s`,
    key,
  })

  return `${getStaticBaseUrl()}?${params.toString()}`
}

/** URL Static API с кэшем (одинаковые параметры — без повторного биллинга 2GIS). */
export function getCachedDgisStaticMapUrl(lat, lng, options = {}) {
  const la = Number(lat)
  const ln = Number(lng)
  const width = Math.min(Math.max(Math.round(options.width ?? 640), 80), 2048)
  const height = Math.min(Math.max(Math.round(options.height ?? 280), 60), 2048)
  const zoom = Math.min(Math.max(Math.round(options.zoom ?? 16), 10), 18)

  const key = CACHE_KEYS.staticMap(la, ln, width, height, zoom)
  const cached = cacheGet(key, { ttl: 24 * 60 * 60 * 1000 })
  if (cached) return cached

  const url = getDgisStaticMapUrl(la, ln, { width, height, zoom })
  if (url) cacheSet(key, url)
  return url
}

export function isDgisStaticConfigured() {
  return Boolean(getDgisMapKey())
}
