// src/composables/usePoiAiTts.js
import { ref, watch } from 'vue'
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

  // Озвучки для Yandex TTS (голос + эмоция). По умолчанию — Джейн.
  const ttsVoices = [
    { voice: 'jane', emotion: 'good', label: 'Джейн (доброжелательно)' },
    { voice: 'oksana', emotion: 'good', label: 'Оксана (доброжелательно)' },
    { voice: 'oksana', emotion: 'neutral', label: 'Оксана (нейтрально)' },
    { voice: 'omazh', emotion: 'neutral', label: 'Омаж (нейтрально)' },
    { voice: 'zahar', emotion: 'good', label: 'Захар (доброжелательно)' },
    { voice: 'ermil', emotion: 'neutral', label: 'Ермил (нейтрально)' },
  ]
  const selectedTtsVoice = ref(0)

  function resetForPoi() {
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

    if (isPlaying.value) {
      stopAllAudio()
      isPlaying.value = false
      return
    }

    ttsLoading.value = true
    let played = false
    try {
      isPlaying.value = true
      const preset = ttsVoices[selectedTtsVoice.value] || ttsVoices[0]
      try {
        const blob = await nodeApi.ai.synthesizeSpeech(text, {
          voice: preset.voice,
          emotion: preset.emotion,
        })
        if (blob && blob.size > 0) {
          const url = URL.createObjectURL(blob)
          const audio = new Audio(url)
          try {
            await new Promise((resolve, reject) => {
              audio.onended = () => { URL.revokeObjectURL(url); resolve() }
              audio.onerror = () => { URL.revokeObjectURL(url); reject(new Error('Playback failed')) }
              audio.play().catch(reject)
            })
            played = true
            return
          } catch (playErr) {
            URL.revokeObjectURL(url)
            console.warn('Playback failed:', playErr.message)
            toastStore.push('Не удалось воспроизвести аудио', 'error')
            return
          }
        }
      } catch (e) {
        console.warn('Node TTS failed:', e.message)
      }
      if (!played) {
        await speakWithWebSpeech(text)
      }
    } catch (e) {
      console.error('TTS error:', e)
      toastStore.push('Ошибка озвучки: ' + e.message, 'error')
    } finally {
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
  }
}