// src/api/groq.js

// ✅ Модели Gemini (OpenAI-совместимый формат)
export const CHAT_MODELS = {
  FAST: 'openrouter/free',
  SMART: 'openrouter/free',
  CODE: 'openrouter/free',
}

/**
 * Генерация текста через Gemini API (через Vite proxy)
 */
export async function generatePoiContent(poi, audience = 'default', model = CHAT_MODELS.SMART) {
  const prompts = {
    default: `Ты опытный экскурсовод Астрахани. Напиши маленькое увлекательное описание для объекта "${poi.name}". 
Факты: год постройки — ${poi.year || 'неизвестен'}, категория — ${poi.category}.
Базовое описание: ${poi.description || 'нет данных'}
Расскажи об архитектурных особенностях, исторических событиях и интересных фактах. Пиши живо и эмоционально.`,
    children: `Ты добрый гид для детей. Объясни простыми словами, что такое "${poi.name}" в Астрахани.
Год: ${poi.year || 'давным-давно'}. Категория: ${poi.category}.
Используй простые слова, интересные сравнения`,
    academic: `Напиши академическое описание объекта культурного наследия "${poi.name}" (г. Астрахань).
Год: ${poi.year}. Архитектор: ${poi.architect || 'неизвестен'}. Категория: ${poi.category}.
Опиши историко-архитектурную ценность, стиль, культурное значение.`
  }

  const response = await fetch('/api/ai/chat/completions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model,
      messages: [
        { role: 'system', content: 'Ты помощник-историк, специалист по архитектуре и истории Астрахани. Отвечай на русском языке.' },
        { role: 'user', content: prompts[audience] }
      ],
      temperature: 0.3,
      max_tokens: 600
    })
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    console.error('AI chat error response:', errorData)
    throw new Error(errorData.error?.message || `HTTP ${response.status}`)
  }

  const data = await response.json()
  return data.choices?.[0]?.message?.content?.trim() || 'Не удалось сгенерировать описание'
}

/**
 * 🛑 Остановить всё воспроизведение
 */
export function stopAllAudio() {
  document.querySelectorAll('audio').forEach(audio => {
    audio.pause()
    audio.src = ''
  })
  if (window.speechSynthesis) {
    window.speechSynthesis.cancel()
  }
}

/**
 * 🔄 Проверка поддержки Web Speech API
 */
export function isSpeechSupported() {
  return 'speechSynthesis' in window
}

/**
 * 🗣️ TTS через Web Speech API
 */
export function speakWithWebSpeech(text, lang = 'ru-RU') {
  return new Promise((resolve, reject) => {
    if (!isSpeechSupported()) {
      reject(new Error('Web Speech API not supported'))
      return
    }
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = lang
    utterance.rate = 0.95
    const voices = window.speechSynthesis.getVoices()
    const ruVoice = voices.find(v => v.lang.startsWith('ru'))
    if (ruVoice) utterance.voice = ruVoice
    utterance.onend = () => resolve()
    utterance.onerror = (e) => reject(new Error(e.error))
    if (voices.length === 0) {
      window.speechSynthesis.onvoiceschanged = () => window.speechSynthesis.speak(utterance)
    } else {
      window.speechSynthesis.speak(utterance)
    }
  })
}