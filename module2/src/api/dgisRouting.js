/**
 * 2GIS Routing API 7.x (global) — построение полилинии по дорогам / пешеходным путям.
 * Документация: https://docs.2gis.com/api/navigation/routing/overview
 *
 * Ключ: VITE_DGIS_ROUTING_KEY (отдельный Directions API), иначе fallback на key из VITE_DGIS_MAP JSON.
 * Базовый URL: VITE_DGIS_ROUTING_BASE (по умолчанию https://routing.api.2gis.com), для dev при CORS — /api/dgis-routing + proxy в vite.config.js.
 */

const ROUTING_API_VERSION = '7.0.0'

import { getDgisMapKey } from '@/api/dgisConfig.js'

/** Есть ли непустой ключ для запросов к Routing API. */
export function isDgisRoutingConfigured() {
  const k = getRoutingApiKey()
  return Boolean(k && String(k).trim())
}

export function getRoutingApiKey() {
  const explicit = import.meta.env.VITE_DGIS_ROUTING_KEY
  if (explicit != null && String(explicit).trim() !== '') return String(explicit).trim()
  return getDgisMapKey()
}

function getRoutingBaseUrl() {
  const raw = import.meta.env.VITE_DGIS_ROUTING_BASE
  if (raw != null && String(raw).trim() !== '') {
    return String(raw).trim().replace(/\/$/, '')
  }
  return 'https://routing.api.2gis.com'
}

function getTransport() {
  const t = import.meta.env.VITE_DGIS_ROUTING_TRANSPORT
  if (t && String(t).trim()) return String(t).trim()
  return 'walking'
}

/**
 * Извлекает координаты [lng, lat][] из ответа Routing API (как в официальном примере 2GIS).
 * @param {unknown} parsed — JSON ответа POST /routing/7.0.0/global
 * @returns {number[][] | null}
 */
export function extractPolylineFromRoutingResponse(parsed) {
  if (!parsed || typeof parsed !== 'object') return null
  const result = parsed.result
  if (!Array.isArray(result) || result.length === 0) return null

  const maneuvers = result[0]?.maneuvers
  if (!Array.isArray(maneuvers)) return null

  const coordinates = maneuvers.flatMap((maneuver) => {
    const geoms = maneuver?.outcoming_path?.geometry
    if (!Array.isArray(geoms) || geoms.length === 0) return []

    return geoms.flatMap((geometry) => {
      const selection = geometry?.selection
      if (typeof selection !== 'string' || !selection.trim()) return []

      try {
        const inner = selection
          .replace(/^LINESTRING\s*\(/i, '')
          .replace(/\)\s*$/, '')
        return inner.split(',').map((point) =>
          point
            .trim()
            .split(/\s+/)
            .map(Number)
            .filter((n) => Number.isFinite(n)),
        )
      } catch {
        return []
      }
    })
  })

  const flat = coordinates.filter((pair) => Array.isArray(pair) && pair.length >= 2)
  return flat.length > 0 ? flat : null
}

/**
 * Один сегмент A→B через Routing API.
 * @param {{ lon: number, lat: number }} a
 * @param {{ lon: number, lat: number }} b
 */
async function fetchRouteSegment(a, b) {
  const key = getRoutingApiKey()
  if (!key) return null

  const base = getRoutingBaseUrl()
  const url = `${base}/routing/${ROUTING_API_VERSION}/global?key=${encodeURIComponent(key)}`

  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      points: [a, b],
      locale: 'ru',
      transport: getTransport(),
      filters: ['dirt_road', 'toll_road', 'ferry'],
      output: 'detailed',
    }),
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`Routing API ${res.status}: ${text.slice(0, 200)}`)
  }

  const parsed = await res.json()
  return extractPolylineFromRoutingResponse(parsed)
}

/**
 * Полилиния по цепочке точек (POI маршрута). Для N>2 запрашивает сегменты подряд и склеивает.
 * Точки: { lng, lat } в порядке прохождения (как на карте).
 * @param {Array<{ lng: number, lat: number }>} points
 * @returns {Promise<number[][] | null>} координаты [lng, lat] для mapgl.Polyline или null
 */
export async function fetch2gisRouteCoordinates(points) {
  if (!Array.isArray(points) || points.length < 2) return null
  if (!isDgisRoutingConfigured()) return null

  const normalized = points.map((p) => ({
    lon: Number(p.lng),
    lat: Number(p.lat),
  }))
  if (normalized.some((p) => !Number.isFinite(p.lon) || !Number.isFinite(p.lat))) return null

  if (normalized.length === 2) {
    return fetchRouteSegment(normalized[0], normalized[1])
  }

  const merged = []
  for (let i = 0; i < normalized.length - 1; i++) {
    const segment = await fetchRouteSegment(normalized[i], normalized[i + 1])
    if (!segment || segment.length < 2) return null
    if (i === 0) {
      merged.push(...segment)
    } else {
      merged.push(...segment.slice(1))
    }
  }
  return merged.length >= 2 ? merged : null
}
