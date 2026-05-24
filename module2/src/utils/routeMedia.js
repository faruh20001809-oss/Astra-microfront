/**
 * Обложка маршрута из ответа Java API или JSON массовой загрузки (сотрудники).
 */
export function pickRouteCoverSource(route) {
  if (!route || typeof route !== 'object') return null
  const raw =
    route.coverImage
    ?? route.image
    ?? route.imageUrl
    ?? route.image_url
    ?? null
  if (typeof raw !== 'string') return null
  const trimmed = raw.trim()
  return trimmed || null
}

/** Относительные пути /java-api/... → полный URL для <img> и fetch. */
export function resolveApiMediaUrl(url) {
  if (!url || typeof url !== 'string') return null
  const trimmed = url.trim()
  if (!trimmed) return null
  if (/^https?:\/\//i.test(trimmed)) return trimmed
  if (trimmed.startsWith('/') && typeof window !== 'undefined' && window.location?.origin) {
    return `${window.location.origin}${trimmed}`
  }
  return trimmed
}

export function normalizeRouteMedia(route) {
  if (!route || typeof route !== 'object') return route
  const coverImage = resolveApiMediaUrl(pickRouteCoverSource(route))
  return {
    ...route,
    coverImage,
    image: coverImage,
  }
}
