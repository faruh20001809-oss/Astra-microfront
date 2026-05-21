/** Ссылки на веб-карту 2ГИС (https://dev.2gis.ru/api). */

export function poiHasValidCoords(lat, lng) {
  const la = Number(lat)
  const ln = Number(lng)
  return Number.isFinite(la) && Number.isFinite(ln) && (la !== 0 || ln !== 0)
}

/** Открыть точку в 2ГИС в браузере. */
export function get2gisWebUrl(lat, lng) {
  const la = Number(lat)
  const ln = Number(lng)
  if (!poiHasValidCoords(la, ln)) return 'https://2gis.ru/astrakhan'
  return `https://2gis.ru/astrakhan/geo/${la},${ln}`
}
