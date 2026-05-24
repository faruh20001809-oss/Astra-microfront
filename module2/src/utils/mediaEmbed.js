/**
 * Встраивание видео: YouTube / Rutube / прямой файл.
 * @param {string} url
 * @returns {{ type: 'iframe'|'video', src: string }|null}
 */
export function resolveVideoEmbed(url) {
  if (!url || typeof url !== 'string') return null
  const trimmed = url.trim()
  if (!trimmed) return null

  const yt =
    trimmed.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([\w-]{6,})/i)
    || trimmed.match(/youtube\.com\/shorts\/([\w-]{6,})/i)
  if (yt?.[1]) {
    return {
      type: 'iframe',
      src: `https://www.youtube-nocookie.com/embed/${yt[1]}`,
    }
  }

  const rutube = trimmed.match(/rutube\.ru\/video\/([a-f0-9]+)/i)
  if (rutube?.[1]) {
    return {
      type: 'iframe',
      src: `https://rutube.ru/play/embed/${rutube[1]}`,
    }
  }

  if (/\.(mp4|webm|ogg)(\?|$)/i.test(trimmed) || trimmed.startsWith('blob:')) {
    return { type: 'video', src: trimmed }
  }

  if (/^https?:\/\//i.test(trimmed)) {
    return { type: 'iframe', src: trimmed }
  }

  return null
}

export function normalizeUrlList(value) {
  if (Array.isArray(value)) {
    return value.map((u) => String(u).trim()).filter(Boolean)
  }
  if (typeof value === 'string' && value.trim()) {
    return value.split(/[\r\n,]+/).map((s) => s.trim()).filter(Boolean)
  }
  return []
}
