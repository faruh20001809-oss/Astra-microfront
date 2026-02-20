// Groq API — все запросы проксируются через Node.js сервер
const GROQ_PROXY = '/api/groq'

/**
 * Генерирует контекстный текст о точке интереса через Groq LLM
 * @param {Object} poi - данные точки
 * @param {'default'|'children'|'academic'} audience - целевая аудитория
 * @returns {Promise<string>} сгенерированный текст
 */
export async function generatePoiContent(poi, audience = 'default') {
  const prompts = {
    default: `Ты опытный экскурсовод Астрахани. Напиши увлекательное описание (2–3 абзаца) для объекта "${poi.name}". 
Факты: год постройки — ${poi.year || 'неизвестен'}, категория — ${poi.category}.
Базовое описание: ${poi.description}
Расскажи об архитектурных особенностях, исторических событиях и интересных фактах. Пиши живо и эмоционально.`,

    children: `Ты добрый гид для детей. Объясни простыми словами, что такое "${poi.name}" в Астрахани.
Год: ${poi.year || 'давным-давно'}. Категория: ${poi.category}.
Используй простые слова, интересные сравнения и 1–2 забавных факта. Длина — 3–4 предложения.`,

    academic: `Напиши академическое описание объекта культурного наследия "${poi.name}" (г. Астрахань).
Год: ${poi.year}. Архитектор: ${poi.architect || 'неизвестен'}. Категория: ${poi.category}.
Опиши историко-архитектурную ценность, стиль, культурное значение. Формат: 3 абзаца, нейтральный научный стиль.`
  }

  const res = await fetch(`${GROQ_PROXY}/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      messages: [
        { role: 'system', content: 'Ты помощник-историк, специалист по архитектуре и истории Астрахани. Отвечай на русском языке.' },
        { role: 'user', content: prompts[audience] }
      ]
    })
  })

  if (!res.ok) throw new Error('Groq API error')
  const data = await res.json()
  return data.content
}

/**
 * Озвучивает текст через Groq TTS
 * @param {string} text
 * @returns {Promise<Blob>} WAV аудио-блоб
 */
export async function synthesizeSpeech(text) {
  const res = await fetch(`${GROQ_PROXY}/tts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text })
  })

  if (!res.ok) throw new Error('TTS error')
  return res.blob()
}

/**
 * Утилита: воспроизводит аудио-блоб
 * @param {Blob} blob
 * @returns {HTMLAudioElement}
 */
export function playAudioBlob(blob) {
  const url = URL.createObjectURL(blob)
  const audio = new Audio(url)
  audio.addEventListener('ended', () => URL.revokeObjectURL(url))
  audio.play()
  return audio
}
