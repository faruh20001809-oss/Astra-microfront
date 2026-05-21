/**
 * Ожидание и инициализация 2GIS MapGL (https://docs.2gis.com/ru/mapgl/overview).
 * Скрипт подключается в index.html: https://mapgl.2gis.com/api/js/v1
 */

import { getDgisMapKey, getDgisMapStyle } from '@/api/dgisConfig.js'

const MAPGL_WAIT_MS = 12_000
const MAPGL_POLL_MS = 200

/**
 * @returns {Promise<typeof window.mapgl>}
 */
export function waitForMapgl(timeoutMs = MAPGL_WAIT_MS) {
  return new Promise((resolve, reject) => {
    if (typeof window !== 'undefined' && window.mapgl?.Map) {
      resolve(window.mapgl)
      return
    }
    const started = Date.now()
    const timer = setInterval(() => {
      if (window.mapgl?.Map) {
        clearInterval(timer)
        resolve(window.mapgl)
        return
      }
      if (Date.now() - started >= timeoutMs) {
        clearInterval(timer)
        reject(new Error('2GIS MapGL не загрузился. Проверьте доступ к mapgl.2gis.com'))
      }
    }, MAPGL_POLL_MS)
  })
}

export function getDgisMapglConfig() {
  return {
    key: getDgisMapKey(),
    style: getDgisMapStyle(),
  }
}

/**
 * @param {HTMLElement} container
 * @param {{ lat: number, lng: number, zoom?: number, interactive?: boolean }} opts
 */
export async function createPoiMap(container, opts) {
  const mapgl = await waitForMapgl()
  const { key, style } = getDgisMapglConfig()
  const lat = Number(opts.lat)
  const lng = Number(opts.lng)
  const zoom = opts.zoom ?? 16

  const map = new mapgl.Map(container, {
    key,
    style,
    center: [lng, lat],
    zoom,
    zoomControl: false,
    fullscreenControl: false,
  })

  let marker = null
  if (mapgl.HtmlMarker) {
    marker = new mapgl.HtmlMarker(map, {
      coordinates: [lng, lat],
      html: '<div class="dgis-poi-marker" aria-hidden="true"></div>',
      anchor: [0.5, 1],
    })
  }

  return { map, marker, mapgl }
}

export function flyToPoi(map, lat, lng, zoom = 16) {
  if (!map) return
  map.setCenter([lng, lat], { animate: true, duration: 400 })
  map.setZoom(zoom, { animate: true, duration: 400 })
}

export function destroyPoiMap(map, marker) {
  if (marker?.destroy) {
    try {
      marker.destroy()
    } catch (_) {
      /* ignore */
    }
  }
  if (map?.destroy) {
    try {
      map.destroy()
    } catch (_) {
      /* ignore */
    }
  }
}
