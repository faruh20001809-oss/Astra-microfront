// src/composables/usePoiAiTts.js
import { ref, watch } from 'vue'
import { useMapStore, useToastStore } from '@/store/index.js'
import {
  generatePoiContent,
  speakWithWebSpeech,
  stopAllAudio,
} from '@/api/groq.js'

export function usePoiAiTts() {
  const mapStore = useMapStore()
  const toastStore = useToastStore()

  const activeTab = ref('desc')

  const selectedAudience = ref('default')
  const aiContent = ref('')
  const aiLoading = ref(false)

  const ttsLoading = ref(false)
  const isPlaying = ref(false)

  const tabs = [
    { key: 'desc', label: 'Описание' },
    { key: 'photos', label: 'Фото' },
    { key: 'street', label: 'Street View' },
  ]

  const audiences = [
    { key: 'default', label: 'Обычный' },
    { key: 'children', label: 'Детский' },
    { key: 'academic', label: 'Научный' },
  ]

  function resetForPoi() {
    aiContent.value = ''
    activeTab.value = 'desc'
  }

  async function generateAiContent() {
    if (!mapStore.selectedPoi) return
    aiLoading.value = true
    aiContent.value = ''
    try {
      aiContent.value = await generatePoiContent(
        mapStore.selectedPoi,
        selectedAudience.value,
      )
    } catch (e) {
      console.error('AI generation error:', e)
      toastStore.push('Ошибка генерации AI-контента: ' + e.message, 'error')
      aiContent.value = mapStore.selectedPoi.description
    } finally {
      aiLoading.value = false
    }
  }

  async function toggleTTS() {
    if (!aiContent.value) return

    if (isPlaying.value) {
      stopAllAudio()
      isPlaying.value = false
      return
    }

    ttsLoading.value = true
    try {
      isPlaying.value = true
      await speakWithWebSpeech(aiContent.value)
    } catch (e) {
      console.error('TTS error:', e)
      toastStore.push('Ошибка озвучки: ' + e.message, 'error')
    } finally {
      isPlaying.value = false
      ttsLoading.value = false
    }
  }

  watch(selectedAudience, () => {
    if (aiContent.value) generateAiContent()
  })

  return {
    activeTab,
    tabs,
    selectedAudience,
    audiences,
    aiContent,
    aiLoading,
    ttsLoading,
    isPlaying,
    resetForPoi,
    generateAiContent,
    toggleTTS,
  }
}