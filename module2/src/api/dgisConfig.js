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
    }
  } catch {
    return { ...DGIS_MAP_DEFAULT }
  }
}

/** Ключ API 2GIS (Static / MapGL / Routing fallback). */
export function getDgisMapKey() {
  const explicit = import.meta.env.VITE_DGIS_STATIC_KEY
  if (explicit != null && String(explicit).trim() !== '') return String(explicit).trim()
  const routing = import.meta.env.VITE_DGIS_ROUTING_KEY
  if (routing != null && String(routing).trim() !== '') return String(routing).trim()
  return loadDgisMapFromEnv().key
}

export function getDgisMapStyle() {
  return loadDgisMapFromEnv().style
}
