/**
 * Общая конфигурация 2GIS (MapGL, Static API, Routing).
 * Документация: https://docs.2gis.com/
 */

export const DGIS_MAP_DEFAULT = Object.freeze({
  key: 'e9f7375c-7ad1-4258-854a-399d99eb65cb',
  style: '0651ff51-79b6-409c-8b90-37a9be2e97ad',
})

export function loadDgisMapFromEnv() {
  const raw = import.meta.env.VITE_DGIS_MAP
  if (!raw || typeof raw !== 'string') return { ...DGIS_MAP_DEFAULT }
  try {
    const o = JSON.parse(raw)
    return {
      key: typeof o.key === 'string' && o.key.trim() ? o.key.trim() : DGIS_MAP_DEFAULT.key,
      style:
        typeof o.style === 'string' && o.style.trim() ? o.style.trim() : DGIS_MAP_DEFAULT.style,
      staticKey:
        typeof o.staticKey === 'string' && o.staticKey.trim() ? o.staticKey.trim() : null,
    }
  } catch {
    return { ...DGIS_MAP_DEFAULT }
  }
}

/** Ключ MapGL / общий fallback (не подставлять в Static API без проверки). */
export function getDgisMapKey() {
  const routing = import.meta.env.VITE_DGIS_ROUTING_KEY
  if (routing != null && String(routing).trim() !== '') return String(routing).trim()
  return loadDgisMapFromEnv().key
}

/**
 * Ключ только для Static API (превью в виртуальном музее).
 * Ключ MapGL (`key` в VITE_DGIS_MAP) часто не даёт static.maps.2gis.com → 403.
 * @see https://docs.2gis.com/en/maps/others/static/overview
 */
export function getDgisStaticKey() {
  const explicit = import.meta.env.VITE_DGIS_STATIC_KEY
  if (explicit != null && String(explicit).trim() !== '') return String(explicit).trim()
  const { staticKey, key } = loadDgisMapFromEnv()
  if (staticKey) return staticKey
  return key
}

export function getDgisMapStyle() {
  return loadDgisMapFromEnv().style
}
