/**
 * Статические превью точек на карте (для карточек виртуального музея).
 * Используется 2GIS Static API: https://docs.2gis.com/en/maps/others/static/overview
 */

import { getCachedDgisStaticMapUrl, isDgisStaticConfigured } from '@/api/dgisStaticMap.js'

export { isDgisStaticConfigured }

export function getPoiStaticMapUrl(lat, lng, options = {}) {
  return getCachedDgisStaticMapUrl(lat, lng, options)
}

export function poiHasMapCoords(poi) {
  if (!poi) return false
  const la = Number(poi.lat)
  const ln = Number(poi.lng)
  return Number.isFinite(la) && Number.isFinite(ln) && (la !== 0 || ln !== 0)
}
