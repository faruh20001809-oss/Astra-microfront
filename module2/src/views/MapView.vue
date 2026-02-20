<template>
  <div class="map-page">

    <!-- 👇 Backdrop для мобильного (должен быть sibling, не child sidebar) -->
    <Transition name="fade">
      <div
          v-if="sidebarOpen && isMobile"
          class="sidebar-backdrop"
          @click="sidebarOpen = false"
          aria-hidden="true"
      ></div>
    </Transition>

    <!-- Sidebar -->
    <aside class="map-sidebar" :class="{ collapsed: !sidebarOpen }">
      <div class="sidebar-header">
        <h2 class="sidebar-title">Исторические<br />объекты</h2>
        <button
            class="btn btn-ghost btn-sm"
            @click="sidebarOpen = !sidebarOpen"
            :aria-label="sidebarOpen ? 'Свернуть панель' : 'Развернуть панель'"
        >
          {{ sidebarOpen ? '◁' : '▷' }}
        </button>
      </div>

      <template v-if="sidebarOpen">
        <!-- Filters -->
        <div class="filter-section">
          <p class="text-mono" style="color:var(--gray-400);margin-bottom:0.5rem">Категории</p>
          <div class="filter-chips">
            <button
                v-for="cat in mapStore.categories"
                :key="cat"
                :class="['filter-chip', { active: mapStore.activeFilters.includes(cat) }]"
                @click="mapStore.toggleFilter(cat)"
            >
              {{ categoryIcon(cat) }} {{ cat }}
            </button>
          </div>
        </div>

        <div class="divider" />

        <!-- GPS toggle -->
        <div class="gps-section">
          <div class="gps-header">
            <span class="text-mono">GPS-трекинг</span>
            <div v-if="gpsActive" class="pulse-dot" />
          </div>
          <button
              :class="['btn btn-sm', gpsActive ? 'btn-danger' : 'btn-ghost']"
              @click="toggleGPS"
          >
            {{ gpsActive ? '⬡ Остановить' : '◉ Включить GPS' }}
          </button>
          <p v-if="gpsStatus" class="gps-status">{{ gpsStatus }}</p>
        </div>

        <!-- Geo-trigger notification -->
        <transition name="slide-up">
          <div v-if="mapStore.nearbyPoi" class="geo-trigger-card">
            <p class="text-mono" style="color:var(--accent);margin-bottom:0.375rem">⚡ Рядом с вами</p>
            <p class="geo-trigger-name">{{ mapStore.nearbyPoi.name }}</p>
            <button class="btn btn-accent btn-sm" @click="openPoi(mapStore.nearbyPoi)">
              Подробнее
            </button>
          </div>
        </transition>

        <div class="divider" />

        <!-- POI list -->
        <div class="poi-list">
          <div
              v-for="poi in mapStore.filteredPois"
              :key="poi.id"
              class="poi-list-item"
              :class="{ active: mapStore.selectedPoi?.id === poi.id }"
              @click="selectAndFlyTo(poi)"
              role="button"
              tabindex="0"
              @keydown.enter="selectAndFlyTo(poi)"
          >
            <span class="poi-icon">{{ categoryIcon(poi.category) }}</span>
            <div>
              <p class="poi-list-name">{{ poi.name }}</p>
              <p class="poi-list-meta">{{ poi.year || '—' }} · {{ poi.category }}</p>
            </div>
          </div>
        </div>
      </template>
    </aside>

    <!-- Map container -->
    <div class="map-wrapper">
      <div ref="mapEl" class="map-container" role="application" aria-label="Интерактивная карта Астрахани" />

      <!-- Loading overlay -->
      <transition name="fade">
        <div v-if="mapStore.isLoadingPois" class="map-loading">
          <div class="spinner" />
          <p>Загрузка объектов…</p>
        </div>
      </transition>

      <!-- Map controls -->
      <div class="map-controls">
        <button class="map-ctrl-btn" @click="flyToAstrakhan" title="Вернуться к Астрахани">⌂</button>
        <button class="map-ctrl-btn" @click="zoomIn" title="Приблизить">+</button>
        <button class="map-ctrl-btn" @click="zoomOut" title="Отдалить">−</button>
      </div>
    </div>

    <!-- POI Detail Modal -->
    <transition name="fade">
      <div v-if="mapStore.selectedPoi" class="modal-backdrop" @click.self="mapStore.clearSelected()">
        <div class="modal-box poi-modal" role="dialog" aria-modal="true">
          <button class="modal-close" @click="mapStore.clearSelected()" aria-label="Закрыть">✕</button>

          <!-- Header -->
          <div class="poi-modal-header">
            <span :class="['tag', 'tag-accent']">{{ mapStore.selectedPoi.category }}</span>
            <h3 class="poi-modal-title">{{ mapStore.selectedPoi.name }}</h3>
            <p class="poi-modal-meta">
              <span v-if="mapStore.selectedPoi.year">Год: {{ mapStore.selectedPoi.year }}</span>
              <span v-if="mapStore.selectedPoi.architect"> · Архитектор: {{ mapStore.selectedPoi.architect }}</span>
            </p>
          </div>

          <!-- Content tabs -->
          <div class="poi-tabs" role="tablist">
            <button
                v-for="tab in tabs"
                :key="tab.key"
                :class="['poi-tab-btn', { active: activeTab === tab.key }]"
                @click="activeTab = tab.key"
                role="tab"
                :aria-selected="activeTab === tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <!-- Tab: Description -->
          <div v-if="activeTab === 'desc'" class="poi-tab-content" role="tabpanel">
            <p class="poi-description">{{ mapStore.selectedPoi.description }}</p>

            <!-- Audience selector -->
            <div class="audience-row">
              <span class="text-mono">Стиль текста:</span>
              <div class="audience-btns">
                <button
                    v-for="a in audiences"
                    :key="a.key"
                    :class="['btn btn-sm', selectedAudience === a.key ? 'btn-primary' : 'btn-ghost']"
                    @click="selectedAudience = a.key"
                >{{ a.label }}</button>
              </div>
            </div>

            <!-- AI content -->
            <div v-if="aiContent || aiLoading" class="ai-content-box">
              <div class="ai-content-header">
                <span class="text-mono" style="color:var(--accent)">✦ AI-описание</span>
                <div v-if="aiLoading" class="spinner" style="width:16px;height:16px" />
              </div>
              <p v-if="aiContent && !aiLoading" class="ai-content-text">{{ aiContent }}</p>

              <!-- TTS Player -->
              <div v-if="aiContent && !aiLoading" class="tts-row">
                <button
                    :class="['btn btn-sm', isPlaying ? 'btn-danger' : 'btn-ghost']"
                    :disabled="ttsLoading"
                    @click="toggleTTS"
                    :aria-label="isPlaying ? 'Остановить озвучку' : 'Озвучить текст'"
                >
                  <span v-if="ttsLoading">
                    <span class="spinner" style="width:12px;height:12px;display:inline-block" />
                    Генерация…
                  </span>
                  <span v-else-if="isPlaying">⏹ Стоп</span>
                  <span v-else>▶ Озвучить</span>
                </button>
                <span v-if="isPlaying" class="pulse-dot" />
              </div>
            </div>

            <button
                class="btn btn-ghost btn-sm ai-gen-btn"
                :disabled="aiLoading"
                @click="generateAiContent"
            >
              {{ aiContent ? '↻ Обновить' : '✦ Сгенерировать AI-описание' }}
            </button>
          </div>

          <!-- Tab: Photos -->
          <div v-if="activeTab === 'photos'" class="poi-tab-content" role="tabpanel">
            <div v-if="!mapStore.selectedPoi.photos?.length" class="empty-state">
              <p>Фотографии не добавлены</p>
            </div>
            <div v-else class="photo-grid">
              <div
                  v-for="(ph, i) in mapStore.selectedPoi.photos"
                  :key="i"
                  class="photo-card"
              >
                <img :src="ph.url" :alt="ph.caption" loading="lazy" />
                <p class="photo-caption">{{ ph.caption }} <span v-if="ph.year">({{ ph.year }})</span></p>
              </div>
            </div>
          </div>

          <!-- Tab: StreetView -->
          <div v-if="activeTab === 'street'" class="poi-tab-content" role="tabpanel">
            <div class="street-view-placeholder">
              <p class="text-mono" style="color:var(--gray-400)">Яндекс Панорамы / 2GIS Street View</p>
              <p>Для интеграции укажите API-ключ Яндекс.Карт</p>
              <a
                  :href="`https://yandex.ru/maps/?ll=${mapStore.selectedPoi.lng},${mapStore.selectedPoi.lat}&z=17&l=stv,sta`"
                  target="_blank"
                  rel="noopener"
                  class="btn btn-ghost btn-sm"
                  style="margin-top:0.75rem"
              >
                ↗ Открыть в Яндекс.Картах
              </a>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useMapStore, useToastStore } from '@/store/index.js'
import { generatePoiContent, synthesizeSpeech, playAudioBlob } from '@/api/groq.js'

const mapStore = useMapStore()
const toastStore = useToastStore()

// DOM refs
const mapEl = ref(null)

// UI state
const sidebarOpen = ref(true)
const isMobile = ref(window.innerWidth < 768)
const activeTab = ref('desc')

// GPS state
const gpsActive = ref(false)
const gpsStatus = ref('')
let watchId = null

// AI/TTS state
const selectedAudience = ref('default')
const aiContent = ref('')
const aiLoading = ref(false)
const ttsLoading = ref(false)
const isPlaying = ref(false)
let currentAudio = null

// 2GIS state
let map = null
let markers = {}
let mapglCheckInterval = null

// Constants
const ASTRAKHAN_CENTER = [48.0408, 46.3497] // [lng, lat]
const tabs = [
  { key: 'desc', label: 'Описание' },
  { key: 'photos', label: 'Фото' },
  { key: 'street', label: 'Street View' }
]
const audiences = [
  { key: 'default', label: 'Обычный' },
  { key: 'children', label: 'Детский' },
  { key: 'academic', label: 'Научный' }
]

/* ── Lifecycle ── */
onMounted(async () => {
  // 1. Resize listener for isMobile
  const handleResize = () => {
    isMobile.value = window.innerWidth < 768
  }
  window.addEventListener('resize', handleResize)

  // 2. Load POIs from store
  await mapStore.fetchPois()

  // 3. Initialize map with safety checks
  const tryInitMap = () => {
    if (!window.mapgl) return false
    if (!mapEl.value) return false
    if (mapEl.value.clientWidth === 0 || mapEl.value.clientHeight === 0) return false
    initMap()
    return true
  }

  if (!tryInitMap()) {
    mapglCheckInterval = setInterval(() => {
      if (tryInitMap()) {
        clearInterval(mapglCheckInterval)
        mapglCheckInterval = null
      }
    }, 200)
  }

  // Cleanup function
  return () => {
    window.removeEventListener('resize', handleResize)
    if (mapglCheckInterval) {
      clearInterval(mapglCheckInterval)
      mapglCheckInterval = null
    }
    stopGPS()
    if (map) {
      map.destroy()
      map = null
    }
    // Clear markers
    Object.values(markers).forEach(m => m.destroy())
    markers = {}
    // Stop audio if playing
    if (currentAudio) {
      currentAudio.pause()
      currentAudio = null
    }
  }
})

/* ── Map functions ── */
function initMap() {
  if (!mapEl.value || !window.mapgl) return

  // Double-check dimensions
  if (mapEl.value.clientWidth === 0 || mapEl.value.clientHeight === 0) {
    setTimeout(initMap, 100)
    return
  }

  try {
    map = new window.mapgl.Map(mapEl.value, {
      center: ASTRAKHAN_CENTER,
      zoom: 14,
      key: 'acc639af-54cb-4b8e-bd72-23d0af937d50',
      style: 'c080bb6a-8134-4993-93d1-1b8ee55d40be'
    })

    map.on('load', () => {
      renderMarkers()
    })
  } catch (e) {
    console.error('Failed to init 2GIS map:', e)
    toastStore.push('Ошибка загрузки карты', 'error')
  }
}

function renderMarkers() {
  if (!map) return

  // Remove old markers
  Object.values(markers).forEach(m => {
    if (m && typeof m.destroy === 'function') {
      m.destroy()
    }
  })
  markers = {}

  // Create new markers from filtered POIs
  for (const poi of mapStore.filteredPois) {
    const el = document.createElement('div')
    el.className = 'custom-marker'
    el.innerHTML = `<span class="marker-icon" aria-hidden="true">${categoryIcon(poi.category)}</span>`
    el.title = poi.name
    el.setAttribute('role', 'button')
    el.setAttribute('tabindex', '0')

    // 👇 Attach click handler to the HTML ELEMENT, not the marker
    const clickHandler = () => openPoi(poi)
    el.addEventListener('click', clickHandler)
    el.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault()
        clickHandler()
      }
    })

    const marker = new window.mapgl.HtmlMarker(map, {
      coordinates: [poi.lng, poi.lat],
      html: el,  // 👈 DOM element
      anchor: 'center'
    })

    markers[poi.id] = marker
  }
}

/* ── POI interaction ── */
function openPoi(poi) {
  mapStore.setSelected(poi)
  aiContent.value = ''
  activeTab.value = 'desc'
  // On mobile, close sidebar when POI is selected
  if (isMobile.value) sidebarOpen.value = false
}

function selectAndFlyTo(poi) {
  mapStore.setSelected(poi)
  if (map) {
    map.setCenter([poi.lng, poi.lat], { animate: true, duration: 600 })
    map.setZoom(16, { animate: true })
  }
  aiContent.value = ''
  activeTab.value = 'desc'
  if (isMobile.value) sidebarOpen.value = false
}

function flyToAstrakhan() {
  if (map) {
    map.setCenter(ASTRAKHAN_CENTER, { animate: true })
    map.setZoom(14, { animate: true })
  }
}

function zoomIn() {
  if (map) map.setZoom(map.getZoom() + 1, { animate: true })
}

function zoomOut() {
  if (map) map.setZoom(map.getZoom() - 1, { animate: true })
}

/* ── Watchers ── */
// Re-render markers when filters change
watch(() => mapStore.activeFilters, () => {
  if (map) renderMarkers()
}, { deep: true })

// Regenerate AI content when audience changes
watch(selectedAudience, () => {
  if (aiContent.value) generateAiContent()
})

/* ── GPS ── */
function toggleGPS() {
  if (gpsActive.value) stopGPS()
  else startGPS()
}

function startGPS() {
  if (!navigator.geolocation) {
    toastStore.push('Геолокация не поддерживается вашим браузером', 'error')
    return
  }

  gpsStatus.value = 'Определение местоположения…'

  watchId = navigator.geolocation.watchPosition(
      (pos) => {
        const { latitude: lat, longitude: lng, accuracy } = pos.coords
        gpsActive.value = true
        gpsStatus.value = `Точность: ±${Math.round(accuracy)} м`

        mapStore.setUserLocation({ lat, lng })

        if (map) {
          // Update or create user marker
          if (!markers['user']) {
            const el = document.createElement('div')
            el.className = 'user-marker'
            el.innerHTML = '◉'
            markers['user'] = new window.mapgl.HtmlMarker(map, {
              coordinates: [lng, lat],
              html: el,  // 👈 Тоже DOM element
              anchor: 'center'
            })
          } else {
            markers['user'].setCoordinates([lng, lat])
          }
        }
      },
      (err) => {
        console.error('GPS error:', err)
        gpsStatus.value = 'Ошибка: ' + (err.message || 'Неизвестная ошибка')
        gpsActive.value = false
        toastStore.push('Не удалось определить местоположение', 'error')
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
  )
}

function stopGPS() {
  if (watchId !== null) {
    navigator.geolocation.clearWatch(watchId)
    watchId = null
  }
  gpsActive.value = false
  gpsStatus.value = ''
  mapStore.setUserLocation(null)

  if (markers['user']) {
    markers['user'].destroy()
    delete markers['user']
  }
}

/* ── AI Content ── */
async function generateAiContent() {
  if (!mapStore.selectedPoi) return

  aiLoading.value = true
  aiContent.value = ''

  try {
    aiContent.value = await generatePoiContent(
        mapStore.selectedPoi,
        selectedAudience.value
    )
  } catch (e) {
    console.error('AI generation error:', e)
    toastStore.push('Ошибка генерации AI-контента: ' + e.message, 'error')
    // Fallback to original description
    aiContent.value = mapStore.selectedPoi.description
  } finally {
    aiLoading.value = false
  }
}

/* ── TTS ── */
async function toggleTTS() {
  if (isPlaying.value) {
    if (currentAudio) {
      currentAudio.pause()
      currentAudio = null
    }
    isPlaying.value = false
    return
  }

  if (!aiContent.value) return

  ttsLoading.value = true

  try {
    const blob = await synthesizeSpeech(aiContent.value, 'autumn')

    // 🔹 Создаём новый audio элемент
    const url = URL.createObjectURL(blob)
    currentAudio = new Audio(url)

    isPlaying.value = true

    // 🔹 Используем { once: true } — слушатель удалится автоматически
    currentAudio.addEventListener('ended', () => {
      isPlaying.value = false
      currentAudio = null
      URL.revokeObjectURL(url)  // 🔹 Очистка памяти
    }, { once: true })

    currentAudio.addEventListener('error', () => {
      isPlaying.value = false
      ttsLoading.value = false
      URL.revokeObjectURL(url)
      toastStore.push('Ошибка воспроизведения аудио', 'error')
    }, { once: true })

    currentAudio.play()

  } catch (e) {
    console.error('TTS error:', e)
    toastStore.push('Ошибка озвучки: ' + e.message, 'error')
  } finally {
    ttsLoading.value = false
  }
}

/* ── Utils ── */
function categoryIcon(cat) {
  const icons = {
    'архитектура': '⬡',
    'музеи': '◫',
    'парки': '⊹',
    'памятники': '◈',
    'религия': '✦',
    'культура': '◇'
  }
  return icons[cat] || '◎'
}
</script>

<style scoped>
/* ===== Page layout ===== */
.map-page {
  display: flex;
  height: calc(100vh - var(--nav-h));
  position: relative;
  overflow: hidden;
}

/* ===== Sidebar backdrop (mobile) ===== */
.sidebar-backdrop {
  position: fixed;
  top: var(--nav-h, 64px);
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 400;
  cursor: pointer;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== Sidebar ===== */
.map-sidebar {
  width: var(--sidebar-w);
  flex-shrink: 0;
  background: rgba(15, 15, 15, 0.96);
  border-right: 1px solid var(--gray-800);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  transition: width var(--transition-slow);
  padding: var(--spacing-lg);
  gap: var(--spacing-md);
  z-index: 500;
}

.map-sidebar.collapsed {
  width: 52px;
  padding: var(--spacing-md) var(--spacing-sm);
}

.sidebar-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-sm);
}

.sidebar-title {
  font-size: 1.25rem;
  line-height: 1.2;
}

/* ===== Filters ===== */
.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.filter-chip {
  padding: 0.3rem 0.65rem;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.06em;
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--gray-400);
  cursor: pointer;
  transition: all var(--transition);
}

.filter-chip:hover { border-color: var(--gray-400); color: var(--paper); }
.filter-chip.active {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(200,169,110,0.08);
}

/* ===== GPS section ===== */
.gps-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.gps-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.gps-status {
  font-size: 0.7rem;
  color: var(--gray-400);
}

/* ===== Geo-trigger card ===== */
.geo-trigger-card {
  background: rgba(200, 169, 110, 0.08);
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  padding: var(--spacing-md);
}

.geo-trigger-name {
  font-family: var(--font-display);
  font-size: 1rem;
  margin-bottom: var(--spacing-sm);
}

/* ===== POI list ===== */
.poi-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.poi-list-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  padding: 0.6rem 0.5rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--transition);
}

.poi-list-item:hover { background: var(--gray-800); }
.poi-list-item.active { background: rgba(200,169,110,0.12); }

.poi-icon {
  font-size: 0.875rem;
  color: var(--accent);
  flex-shrink: 0;
  margin-top: 2px;
}

.poi-list-name {
  font-size: 0.8rem;
  color: var(--paper);
  line-height: 1.3;
}

.poi-list-meta {
  font-size: 0.65rem;
  color: var(--gray-400);
  margin-top: 2px;
}

/* ===== Map wrapper ===== */
.map-wrapper {
  flex: 1;
  position: relative;
}

.map-container {
  width: 100%;
  height: 100%;
}

/* ===== Loading overlay ===== */
.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  background: rgba(0,0,0,0.6);
  font-size: 0.875rem;
  color: var(--gray-400);
  z-index: 200;
}

/* ===== Map controls ===== */
.map-controls {
  position: absolute;
  right: var(--spacing-md);
  bottom: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: 2px;
  z-index: 150;
}

.map-ctrl-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ink);
  border: 1px solid var(--gray-600);
  color: var(--paper);
  font-size: 1rem;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition);
}

.map-ctrl-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

/* ===== Modal ===== */
.poi-modal {
  max-width: 680px;
  width: 100%;
  position: relative;
}

.modal-close {
  position: absolute;
  top: var(--spacing-md);
  right: var(--spacing-md);
  background: transparent;
  border: 1px solid var(--gray-600);
  color: var(--paper);
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  transition: all var(--transition);
  z-index: 10;
}

.modal-close:hover { border-color: var(--paper); }

.poi-modal-header { margin-bottom: var(--spacing-lg); }

.poi-modal-title {
  font-family: var(--font-display);
  font-size: 1.75rem;
  margin: 0.5rem 0 0.25rem;
}

.poi-modal-meta {
  font-size: 0.75rem;
  color: var(--gray-400);
}

/* ===== Tabs ===== */
.poi-tabs {
  display: flex;
  gap: 2px;
  border-bottom: 1px solid var(--gray-800);
  margin-bottom: var(--spacing-lg);
}

.poi-tab-btn {
  padding: 0.5rem 1rem;
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: transparent;
  border: none;
  color: var(--gray-400);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all var(--transition);
  margin-bottom: -1px;
}

.poi-tab-btn:hover { color: var(--paper); }
.poi-tab-btn.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
}

.poi-tab-content {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.poi-description {
  color: var(--gray-200);
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ===== Audience selector ===== */
.audience-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
  margin-top: var(--spacing-lg);
}

.audience-btns {
  display: flex;
  gap: var(--spacing-xs);
}

.ai-gen-btn { margin-top: var(--spacing-md); }

/* ===== AI content box ===== */
.ai-content-box {
  background: rgba(200,169,110,0.05);
  border: 1px solid rgba(200,169,110,0.2);
  border-radius: var(--radius-sm);
  padding: var(--spacing-md);
  margin-top: var(--spacing-md);
}

.ai-content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-sm);
}

.ai-content-text {
  color: var(--gray-200);
  line-height: 1.8;
  white-space: pre-wrap;
  font-size: 0.85rem;
}

/* ===== TTS row ===== */
.tts-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}

/* ===== Photos grid ===== */
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: var(--spacing-md);
}

.photo-card img {
  width: 100%;
  aspect-ratio: 4/3;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--gray-600);
}

.photo-caption {
  font-size: 0.7rem;
  color: var(--gray-400);
  margin-top: var(--spacing-xs);
}

/* ===== Street view placeholder ===== */
.street-view-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-height: 200px;
  background: var(--gray-800);
  border: 1px dashed var(--gray-600);
  border-radius: var(--radius-sm);
  padding: var(--spacing-xl);
  gap: var(--spacing-sm);
  color: var(--gray-400);
  font-size: 0.85rem;
}

/* ===== Empty state ===== */
.empty-state {
  text-align: center;
  padding: var(--spacing-xl);
  color: var(--gray-400);
  font-size: 0.85rem;
}

/* ===== Custom markers (2GIS) ===== */
:global(.custom-marker) {
  cursor: pointer;
  width: 32px;
  height: 32px;
  background: var(--ink, #0a0a0a);
  border: 2px solid #c8a96e;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: transform 0.15s;
  outline: none;
}

:global(.custom-marker:hover),
:global(.custom-marker:focus) {
  transform: scale(1.2);
  border-color: var(--accent);
}

:global(.user-marker) {
  width: 16px;
  height: 16px;
  background: #c8a96e;
  border: 2px solid white;
  border-radius: 50%;
  font-size: 10px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(200, 169, 110, 0.7); }
  70% { box-shadow: 0 0 0 10px rgba(200, 169, 110, 0); }
  100% { box-shadow: 0 0 0 0 rgba(200, 169, 110, 0); }
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .map-page {
    flex-direction: column;
    height: 100vh;
    padding-top: 0;
  }

  .map-sidebar {
    width: 100%;
    height: 220px;
    border-right: none;
    border-bottom: 1px solid var(--gray-800);
    overflow-y: auto;
  }

  .map-sidebar.collapsed {
    height: 52px;
  }

  .map-wrapper {
    flex: 1;
  }

  /* On mobile, sidebar can overlay map */
  .map-sidebar {
    position: fixed;
    top: var(--nav-h, 64px);
    left: 0;
    z-index: 600;
    transform: translateY(0);
    transition: transform 0.3s ease;
  }

  .map-sidebar.collapsed {
    transform: translateY(calc(-100% + 52px));
  }
}

@media (max-width: 480px) {
  .poi-modal {
    margin: var(--spacing-md);
    max-height: calc(100vh - 100px);
  }

  .photo-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== Slide-up transition for geo-trigger ===== */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}
.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>