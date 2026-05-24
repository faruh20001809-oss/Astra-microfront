/**
 * Кэш обложек маршрутов с /java-api/api/v1/routes/{id}/image (blob URL).
 */
import { ref, watchEffect } from 'vue'
import { pickRouteCoverSource, resolveApiMediaUrl } from '@/utils/routeMedia.js'

const cache = new Map()

function isRouteImageEndpoint(url) {
  return typeof url === 'string' && url.includes('/routes/') && url.includes('/image')
}

/**
 * @param {import('vue').Ref<Object>|Object} routeRef
 * @returns {import('vue').Ref<string|null>}
 */
export function useRouteImage(routeRef) {
  const imageUrl = ref(null)

  watchEffect(() => {
    const route = routeRef?.value ?? routeRef
    if (!route) {
      imageUrl.value = null
      return
    }
    const raw = pickRouteCoverSource(route)
    const url = resolveApiMediaUrl(raw)
    const id = route.id
    if (!url) {
      imageUrl.value = null
      return
    }
    if (!isRouteImageEndpoint(url)) {
      imageUrl.value = url
      return
    }
    if (id != null && cache.has(id)) {
      imageUrl.value = cache.get(id)
      return
    }
    imageUrl.value = url
    fetch(url)
      .then((r) => {
        if (!r.ok) throw new Error()
        return r.blob()
      })
      .then((blob) => {
        const blobUrl = URL.createObjectURL(blob)
        if (id != null) cache.set(id, blobUrl)
        imageUrl.value = blobUrl
      })
      .catch(() => {})
  })

  return imageUrl
}

export { cache as routeImageCache }
