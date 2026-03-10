/**
 * Кэш картинок товаров: при первой загрузке по URL API сохраняем blob URL,
 * при повторном отображении берём из кэша (картинки с Java не перезапрашиваются).
 */
import { ref, watchEffect } from 'vue'

const cache = new Map()

function isProductImageEndpoint(url) {
  return typeof url === 'string' && url.includes('/products/') && url.includes('/image')
}

/**
 * @param {import('vue').Ref<Object>|Object} productRef - продукт (ref или объект) с полями id, image
 * @returns {import('vue').Ref<string|null>} - URL картинки для отображения (из кэша или исходный)
 */
export function useProductImage(productRef) {
  const imageUrl = ref(null)

  watchEffect(() => {
    const product = productRef?.value ?? productRef
    if (!product) {
      imageUrl.value = null
      return
    }
    const url = product.image
    const id = product.id
    if (!url) {
      imageUrl.value = null
      return
    }
    if (!isProductImageEndpoint(url)) {
      imageUrl.value = url
      return
    }
    if (cache.has(id)) {
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
        cache.set(id, blobUrl)
        imageUrl.value = blobUrl
      })
      .catch(() => {})
  })

  return imageUrl
}

export { cache as productImageCache }
