// src/composables/usePoiAiTts.js
import { ref } from 'vue'
import { useMapStore, useToastStore } from '@/store/index.js'
import {
  generatePoiContent,
  speakWithWebSpeech,
  stopAllAudio,
} from '@/api/groq.js'
import { nodeApi } from '@/api/backend.js'

export function usePoiAiTts() {
  const mapStore = useMapStore()
  const toastStore = useToastStore()

  const activeTab = ref('desc')

  const aiContent = ref('')
  const aiLoading = ref(false)
  /** @type {import('vue').Ref<'default'|'children'|'academic'>} */
  const aiAudience = ref('default')

  const ttsLoading = ref(false)
  const isPlaying = ref(false)

  const tabs = [
    { key: 'desc', label: 'Описание' },
    { key: 'photos', label: 'Фото' },
    { key: 'street', label: 'Панорама' },
  ]

  /**
   * Имена голосов — ключи из yandex_tts_free.YandexFreeTTS.voices (не «ermil», а «ermilov»).
   * Эмоции: neutral | good | evil
   */
  const ttsVoices = [
    { voice: 'jane', emotion: 'good', label: 'Джейн (доброжелательно)' },
    { voice: 'oksana', emotion: 'good', label: 'Оксана (доброжелательно)' },
    { voice: 'oksana', emotion: 'neutral', label: 'Оксана (нейтрально)' },
    { voice: 'omazh', emotion: 'neutral', label: 'Омаж (нейтрально)' },
    { voice: 'zahar', emotion: 'good', label: 'Захар (доброжелательно)' },
    { voice: 'ermilov', emotion: 'neutral', label: 'Ермил (нейтрально)' },
  ]
  const selectedTtsVoice = ref(0)

  /** @type {HTMLAudioElement | null} */
  let currentTtsAudio = null
  /** @type {AbortController | null} */
  let ttsAbortController = null
  /** Разблокировать await после ручного стопа во время play() */
  let pendingAudioDone = null

  function ttsPresetAt(index) {
    const i = Number(index)
    const idx =
      Number.isFinite(i) && i >= 0 && i < ttsVoices.length ? i : 0
    return ttsVoices[idx]
  }

  /** Остановить озвучку (Node Audio, fetch, Web Speech). */
  function stopTtsPlayback() {
    if (ttsAbortController) {
      try {
        ttsAbortController.abort()
      } catch (_) {}
      ttsAbortController = null
    }
    if (currentTtsAudio) {
      try {
        currentTtsAudio.pause()
        currentTtsAudio.removeAttribute('src')
        currentTtsAudio.load()
      } catch (_) {}
      currentTtsAudio = null
    }
    if (pendingAudioDone) {
      pendingAudioDone()
      pendingAudioDone = null
    }
    stopAllAudio()
    isPlaying.value = false
    ttsLoading.value = false
  }

  function resetForPoi() {
    stopTtsPlayback()
    aiContent.value = ''
    activeTab.value = 'desc'
  }

  async function generateAiContent() {
    if (!mapStore.selectedPoi) return
    aiLoading.value = true
    aiContent.value = ''
    const audience = aiAudience.value || 'default'
    try {
      aiContent.value = await generatePoiContent(
        mapStore.selectedPoi,
        audience,
      )
    } catch (e) {
      console.warn('OpenRouter AI failed, trying Node fallback:', e.message)
      try {
        const data = await nodeApi.ai.generatePoiContent(
          mapStore.selectedPoi,
          audience,
        )
        aiContent.value = (typeof data === 'string' ? data : data?.content) || mapStore.selectedPoi.description || ''
      } catch (e2) {
        console.error('AI generation error:', e2)
        toastStore.push('Ошибка генерации AI-контента: ' + (e2.message || e.message), 'error')
        aiContent.value = mapStore.selectedPoi.description || ''
      }
    } finally {
      aiLoading.value = false
    }
  }

  async function toggleTTS() {
    const text = aiContent.value || mapStore.selectedPoi?.description || ''
    if (!text) return

    if (isPlaying.value || ttsLoading.value) {
      stopTtsPlayback()
      return
    }

    ttsLoading.value = true
    isPlaying.value = true
    ttsAbortController = new AbortController()
    const signal = ttsAbortController.signal

    const preset = ttsPresetAt(selectedTtsVoice.value)
    let played = false

    try {
      try {
        const blob = await nodeApi.ai.synthesizeSpeech(text, {
          voice: preset.voice,
          emotion: preset.emotion,
          signal,
        })
        if (signal.aborted) return

        if (blob && blob.size > 0) {
          const url = URL.createObjectURL(blob)
          const audio = new Audio(url)
          currentTtsAudio = audio
          try {
            await new Promise((resolve, reject) => {
              const finish = () => {
                pendingAudioDone = null
                URL.revokeObjectURL(url)
                if (currentTtsAudio === audio) currentTtsAudio = null
                resolve()
              }
              pendingAudioDone = finish
              audio.onended = finish
              audio.onerror = () => {
                pendingAudioDone = null
                URL.revokeObjectURL(url)
                if (currentTtsAudio === audio) currentTtsAudio = null
                reject(new Error('Playback failed'))
              }
              audio.play().catch(reject)
            })
            played = true
            return
          } catch (playErr) {
            pendingAudioDone = null
            if (currentTtsAudio === audio) currentTtsAudio = null
            try {
              URL.revokeObjectURL(url)
            } catch (_) {}
            console.warn('Playback failed:', playErr.message)
            toastStore.push('Не удалось воспроизвести аудио', 'error')
            return
          }
        }
      } catch (e) {
        if (e.name === 'AbortError' || signal.aborted) return
        console.warn('Node TTS failed:', e.message)
      }

      if (!played && !signal.aborted) {
        await speakWithWebSpeech(text)
      }
    } catch (e) {
      if (e.name === 'AbortError' || signal.aborted) return
      console.error('TTS error:', e)
      toastStore.push('Ошибка озвучки: ' + e.message, 'error')
    } finally {
      ttsAbortController = null
      isPlaying.value = false
      ttsLoading.value = false
    }
  }

  return {
    activeTab,
    tabs,
    ttsVoices,
    selectedTtsVoice,
    aiAudience,
    aiContent,
    aiLoading,
    ttsLoading,
    isPlaying,
    resetForPoi,
    generateAiContent,
    toggleTTS,
    stopTtsPlayback,
  }
}
