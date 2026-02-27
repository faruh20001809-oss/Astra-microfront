<template>
  <div class="map-page">
    <!-- Sidebar — всегда виден, без сворачивания -->
    <aside class="map-sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">Исторические<br />объекты</h2>
      </div>

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
    </aside>

    <!-- Map container -->
    <div class="map-wrapper">
      <div
        ref="mapEl"
        class="map-container"
        role="application"
        aria-label="Интерактивная карта Астрахани"
      />

      <!-- Loading overlay -->
      <transition name="fade">
        <div v-if="mapStore.isLoadingPois" class="map-loading">
          <div class="spinner" />
          <p>Загрузка объектов…</p>
        </div>
      </transition>

      <!-- Map controls -->
      <div class="map-controls">
        <button class="map-ctrl-btn" @click="flyToAstrakhan" title="Вернуться к Астрахани">
          ⌂
        </button>
        <button class="map-ctrl-btn" @click="zoomIn" title="Приблизить">+</button>
        <button class="map-ctrl-btn" @click="zoomOut" title="Отдалить">−</button>
      </div>
      <!-- Floating POI detail card near marker -->
      <transition name="fade">
        <div
          v-if="mapStore.selectedPoi"
          class="poi-modal floating"
          :style="modalStyle"
          role="dialog"
          aria-modal="true"
        >
          <button
            class="modal-close"
            @click="mapStore.clearSelected()"
            aria-label="Закрыть"
          >
            ✕
          </button>

          <div class="poi-modal-body">
            <!-- Left column: photo from Java API -->
            <div class="poi-photo-col">
              <img
                v-if="mapStore.selectedPoi.image"
                :src="mapStore.selectedPoi.image"
                :alt="mapStore.selectedPoi.name"
                class="poi-photo"
              />
              <div v-else class="poi-photo-placeholder">
                Нет фото
              </div>
            </div>

            <!-- Right column: content -->
            <div class="poi-content-col">
              <!-- Header -->
              <div class="poi-modal-header">
                <span :class="['tag', 'tag-accent']">{{ mapStore.selectedPoi.category }}</span>
                <h3 class="poi-modal-title">{{ mapStore.selectedPoi.name }}</h3>
                <p class="poi-modal-meta">
                  <span v-if="mapStore.selectedPoi.year">Год: {{ mapStore.selectedPoi.year }}</span>
                  <span v-if="mapStore.selectedPoi.architect">
                    · Архитектор: {{ mapStore.selectedPoi.architect }}
                  </span>
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
                    >
                      {{ a.label }}
                    </button>
                  </div>
                </div>

                <!-- AI content -->
                <div v-if="aiContent || aiLoading" class="ai-content-box">
                  <div class="ai-content-header">
                    <span class="text-mono" style="color:var(--accent)">✦ AI-описание</span>
                    <div v-if="aiLoading" class="spinner" style="width:16px;height:16px" />
                  </div>
                  <p v-if="aiContent && !aiLoading" class="ai-content-text">
                    {{ aiContent }}
                  </p>

                  <!-- TTS Player -->
                  <div v-if="aiContent && !aiLoading" class="tts-row">
                    <button
                      :class="['btn btn-sm', isPlaying ? 'btn-danger' : 'btn-ghost']"
                      :disabled="ttsLoading"
                      @click="toggleTTS"
                      :aria-label="isPlaying ? 'Остановить озвучку' : 'Озвучить текст'"
                    >
                      <span v-if="ttsLoading">
                        <span
                          class="spinner"
                          style="width:12px;height:12px;display:inline-block"
                        />
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
                    <p class="photo-caption">
                      {{ ph.caption }} <span v-if="ph.year">({{ ph.year }})</span>
                    </p>
                  </div>
                </div>
              </div>

              <!-- Tab: StreetView -->
              <div v-if="activeTab === 'street'" class="poi-tab-content" role="tabpanel">
                <div class="street-view-placeholder">
                  <p class="text-mono" style="color:var(--gray-400)">
                    Яндекс Панорамы / 2GIS Street View
                  </p>
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
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useMapStore, useToastStore } from '@/store/index.js'
import { usePoiAiTts } from '@/composables/usePoiAiTts.js'

const mapStore = useMapStore()
const toastStore = useToastStore()

// DOM refs
const mapEl = ref(null)

// GPS state
const gpsActive = ref(false)
const gpsStatus = ref('')
let watchId = null

// 2GIS state
let map = null
let markers = {}
let mapglCheckInterval = null

// Floating modal position (near selected marker)
const modalPosition = ref({ top: 0, left: 0 })

const modalStyle = computed(() => ({
  top: modalPosition.value.top + 'px',
  left: modalPosition.value.left + 'px',
}))

// AI/TTS (из composable)
const {
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
} = usePoiAiTts()

// Constants
const ASTRAKHAN_CENTER = [48.0408, 46.3497] // [lng, lat]

// ── Lifecycle ──
onMounted(async () => {
  await mapStore.fetchPois()

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
})

onUnmounted(() => {
  if (mapglCheckInterval) {
    clearInterval(mapglCheckInterval)
    mapglCheckInterval = null
  }
  stopGPS()
  if (map) {
    map.destroy()
    map = null
  }
  Object.values(markers).forEach((m) => {
    if (m && typeof m.destroy === 'function') m.destroy()
  })
  markers = {}
})

// ── Map functions ──
function initMap() {
  if (!mapEl.value || !window.mapgl) return

  if (mapEl.value.clientWidth === 0 || mapEl.value.clientHeight === 0) {
    setTimeout(initMap, 100)
    return
  }

  try {
    map = new window.mapgl.Map(mapEl.value, {
      center: ASTRAKHAN_CENTER,
      zoom: 14,
      key: 'acc639af-54cb-4b8e-bd72-23d0af937d50',
      style: 'c080bb6a-8134-4993-93d1-1b8ee55d40be',
    })

    map.on('load', () => {
      renderMarkers()
    })

    // keep modal attached to marker when map moves/zooms
    map.on('move', () => {
      if (mapStore.selectedPoi) {
        updateModalPosition(mapStore.selectedPoi)
      }
    })
    map.on('zoom', () => {
      if (mapStore.selectedPoi) {
        updateModalPosition(mapStore.selectedPoi)
      }
    })
  } catch (e) {
    console.error('Failed to init 2GIS map:', e)
    toastStore.push('Ошибка загрузки карты', 'error')
  }
}

function updateModalPosition(poi) {
  if (!map || !mapEl.value || !poi) return
  // 2GIS MapGL даёт пиксели через project([lng, lat])
  const [x, y] = map.project([poi.lng, poi.lat])

  const offsetX = 24   // немного вправо от маркера
  const offsetY = -140 // и чуть выше, чтобы “висела” над точкой

  modalPosition.value = {
    left: x + offsetX,
    top: y + offsetY,
  }
}
function renderMarkers() {
  if (!map) return

  Object.values(markers).forEach((m) => {
    if (m && typeof m.destroy === 'function') {
      m.destroy()
    }
  })
  markers = {}

  for (const poi of mapStore.filteredPois) {
    const el = document.createElement('div')
    el.className = 'custom-marker'
    el.innerHTML = `<span class="marker-icon" aria-hidden="true">${categoryIcon(
      poi.category,
    )}</span>`
    el.title = poi.name
    el.setAttribute('role', 'button')
    el.setAttribute('tabindex', '0')

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
      html: el,
      anchor: 'center',
    })

    markers[poi.id] = marker
  }
}

// ── POI interaction ──
function openPoi(poi) {
  mapStore.setSelected(poi)
  resetForPoi()
  if (map) updateModalPosition(poi)
}

function selectAndFlyTo(poi) {
  mapStore.setSelected(poi)
  if (map) {
    map.setCenter([poi.lng, poi.lat], { animate: true, duration: 600 })
    map.setZoom(16, { animate: true })
    setTimeout(() => updateModalPosition(poi), 650)
  } else {
    updateModalPosition(poi)
  }
  resetForPoi()
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

// ── Watchers ──
watch(
  () => mapStore.activeFilters,
  () => {
    if (map) renderMarkers()
  },
  { deep: true },
)

// ── GPS ──
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
        if (!markers['user']) {
          const el = document.createElement('div')
          el.className = 'user-marker'
          el.innerHTML = '◉'
          markers['user'] = new window.mapgl.HtmlMarker(map, {
            coordinates: [lng, lat],
            html: el,
            anchor: 'center',
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
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
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

// ── Utils ──
function categoryIcon(cat) {
  const normalized = (cat || '').toLowerCase()
  const icons = {
    'архитектура': '⬡',
    'история': '◈',
    'музеи': '◫',
    'музей': '◫',
    'парки': '⊹',
    'парки и скверы': '⊹',
    'памятники': '◈',
  }
  return icons[normalized] || '◎'
}
</script>

<style scoped>
/* ===== Page layout ===== */
.map-page {
  display: flex;
  flex-direction: row;
  height: calc(100dvh - var(--nav-h, 64px));
  margin-top: var(--nav-h, 64px); /* 👈 отступ под фиксированный хедер */
  width: 100%;
  max-width: 100vw;
  position: relative;
  overflow: hidden;
}

/* ===== Sidebar — всегда виден, без анимаций ===== */
.map-sidebar {
  width: var(--sidebar-w, 320px);
  flex-shrink: 0;
  background: rgba(15, 15, 15, 0.96);
  border-right: 1px solid var(--gray-800);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: var(--spacing-lg);
  gap: var(--spacing-md);
  z-index: 100;
}

.sidebar-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-sm);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--gray-800);
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

.filter-chip:hover {
  border-color: var(--gray-400);
  color: var(--paper);
}

.filter-chip.active {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(200, 169, 110, 0.08);
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

.poi-list-item:hover {
  background: var(--gray-800);
}

.poi-list-item.active {
  background: rgba(200, 169, 110, 0.12);
}

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
  flex: 1 1 0;
  min-width: 0;
  min-height: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.map-container {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 200px;
  overflow: hidden;
}

/* 2GIS Canvas fix */
.map-container :deep(canvas),
.map-container :deep(.mapgl-canvas-container),
.map-container :deep(.mapgl-map) {
  display: block !important;
  width: 100% !important;
  height: 100% !important;
  max-height: 100%;
  margin: 0 !important;
  padding: 0 !important;
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
  background: rgba(0, 0, 0, 0.6);
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

/* ===== Floating modal near marker ===== */
.poi-modal.floating {
  position: absolute;
  max-width: 480px;
  width: 100%;
  max-height: 60vh;
  overflow-y: auto;
  background: rgba(10, 10, 10, 0.92);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.7);
  pointer-events: auto;
}

.poi-modal-body {
  display: flex;
  gap: var(--spacing-md);
}

.poi-photo-col {
  flex: 0 0 160px;
}

.poi-photo {
  width: 100%;
  aspect-ratio: 4/5;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--gray-700);
}

.poi-photo-placeholder {
  width: 100%;
  aspect-ratio: 4/5;
  border-radius: var(--radius-sm);
  border: 1px dashed var(--gray-600);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  color: var(--gray-400);
}

.poi-content-col {
  flex: 1 1 auto;
  min-width: 0;
}

.poi-modal.floating::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 40px;
  border-width: 10px 10px 0 10px;
  border-style: solid;
  border-color: rgba(10, 10, 10, 0.92) transparent transparent transparent;
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

.modal-close:hover {
  border-color: var(--paper);
}

.poi-modal-header {
  margin-bottom: var(--spacing-lg);
}

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

.poi-tab-btn:hover {
  color: var(--paper);
}

.poi-tab-btn.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
}

.poi-tab-content {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
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

.ai-gen-btn {
  margin-top: var(--spacing-md);
}

/* ===== AI content box ===== */
.ai-content-box {
  background: rgba(200, 169, 110, 0.05);
  border: 1px solid rgba(200, 169, 110, 0.2);
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
::global(.custom-marker) {
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

::global(.custom-marker:hover),
::global(.custom-marker:focus) {
  transform: scale(1.2);
  border-color: var(--accent);
}

::global(.user-marker) {
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
  0% {
    box-shadow: 0 0 0 0 rgba(200, 169, 110, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(200, 169, 110, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(200, 169, 110, 0);
  }
}

/* ===== Transitions ===== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

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

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  :root {
    --sidebar-w: 280px;
  }
}

@media (max-width: 768px) {
  .map-page {
    flex-direction: column;
    height: 100dvh;
    margin-top: 0; /* На мобильных хедер может быть скрыт или иначе позиционирован */
  }

  .map-sidebar {
    width: 100%;
    height: auto;
    max-height: 30vh;
    border-right: none;
    border-bottom: 1px solid var(--gray-800);
    overflow-y: auto;
  }

  .map-wrapper {
    flex: 1;
    min-height: 70vh;
  }

  .poi-modal {
    margin: var(--spacing-md);
    max-height: calc(100dvh - 100px);
  }

  .photo-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .poi-modal {
    margin: var(--spacing-md);
    max-height: calc(100dvh - 100px);
  }

  .photo-grid {
    grid-template-columns: 1fr;
  }
}
</style>