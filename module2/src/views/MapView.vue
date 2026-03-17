<template>
  <div class="map-page">
    <!-- Sidebar: на десктопе — слева в потоке; на мобильных скрыт, контент в Drawer -->
    <aside class="map-sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">Исторические объекты</h2>
        <button type="button" class="sidebar-suggest-btn" @click="suggestPoiOpen = true" aria-label="Предложить точку">
          💡 Предложить точку
        </button>
      </div>
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
        <p v-if="gpsActive && mapStore.nearestPoiWithDistance && !mapStore.nearbyPoi" class="gps-nearest">
          Ближайшая точка: {{ mapStore.nearestPoiWithDistance.poi.name }} (~{{ mapStore.nearestPoiWithDistance.distanceMetres }} м)
        </p>
      </div>
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

      <!-- Кнопка «Точки» на мобильных — открывает список слева -->
      <button
          type="button"
          class="map-poins-btn"
          aria-label="Список точек"
          @click="mobileSidebarOpen = true"
      >
        ◎ Точки
      </button>

      <!-- Map controls -->
      <div class="map-controls">
        <button class="map-ctrl-btn" @click="flyToAstrakhan" title="Вернуться к Астрахани">
          ⌂
        </button>
        <button class="map-ctrl-btn" @click="zoomIn" title="Приблизить">+</button>
        <button class="map-ctrl-btn" @click="zoomOut" title="Отдалить">−</button>
      </div>

      <!-- Всплывающее окно: предложить точку на карту -->
      <Dialog
        v-model:visible="suggestPoiOpen"
        modal
        header="Предложить точку на карту"
        :style="{ width: 'min(420px, 92vw)' }"
        class="suggest-poi-dialog"
        :dismissableMask="true"
        @hide="suggestFormError = ''"
      >
        <form class="suggest-poi-form" @submit.prevent="submitSuggestPoi">
          <div class="form-group">
            <label class="form-label">Название *</label>
            <input v-model="suggestForm.name" type="text" class="form-input" placeholder="Например: Дом-музей Кустодиева" required />
          </div>
          <div class="form-group">
            <label class="form-label">Место *</label>
            <input v-model="suggestForm.place" type="text" class="form-input" placeholder="Адрес или описание места" required />
          </div>
          <div class="form-group">
            <label class="form-label">Описание</label>
            <textarea v-model="suggestForm.description" class="form-textarea" placeholder="Краткое описание объекта…" rows="3" />
          </div>
          <div class="form-group">
            <label class="form-label">Почему добавить *</label>
            <textarea v-model="suggestForm.whyAdd" class="form-textarea" placeholder="Почему это место важно?" required rows="3" />
          </div>
          <p v-if="suggestFormError" class="form-error">{{ suggestFormError }}</p>
          <p v-if="suggestFormSuccess" class="form-success">Предложение отправлено. Мы рассмотрим его и при одобрении добавим точку на карту.</p>
          <div class="form-actions">
            <Button type="submit" label="Отправить" :loading="suggestSubmitting" :disabled="suggestSubmitting" />
            <Button type="button" label="Отмена" class="p-button-text p-button-secondary" @click="suggestPoiOpen = false" />
          </div>
        </form>
      </Dialog>

      <!-- Мобильный сайдбар: выезжает слева, при выборе точки закрывается -->
      <Drawer
          v-model:visible="mobileSidebarOpen"
          position="left"
          :modal="true"
          :dismissable="true"
          :showCloseIcon="true"
          class="map-sidebar-drawer"
      >
        <template #header>
          <span class="sidebar-title">Исторические объекты</span>
        </template>
        <div class="map-sidebar-drawer-content">
          <button type="button" class="drawer-suggest-btn" @click="suggestPoiOpen = true; mobileSidebarOpen = false" aria-label="Предложить точку">
            💡 Предложить точку
          </button>
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
            <p v-if="gpsActive && mapStore.nearestPoiWithDistance && !mapStore.nearbyPoi" class="gps-nearest">
              Ближайшая: {{ mapStore.nearestPoiWithDistance.poi.name }} (~{{ mapStore.nearestPoiWithDistance.distanceMetres }} м)
            </p>
          </div>
          <div v-if="mapStore.nearbyPoi" class="geo-trigger-card">
            <p class="text-mono" style="color:var(--accent);margin-bottom:0.375rem">⚡ Рядом с вами</p>
            <p class="geo-trigger-name">{{ mapStore.nearbyPoi.name }}</p>
            <button class="btn btn-accent btn-sm" @click="openPoi(mapStore.nearbyPoi); mobileSidebarOpen = false">
              Подробнее
            </button>
          </div>
          <div class="divider" />
          <div class="poi-list">
            <div
                v-for="poi in mapStore.filteredPois"
                :key="poi.id"
                class="poi-list-item"
                :class="{ active: mapStore.selectedPoi?.id === poi.id }"
                @click="onSelectPoiMobile(poi)"
                role="button"
                tabindex="0"
            >
              <span class="poi-icon">{{ categoryIcon(poi.category) }}</span>
              <div>
                <p class="poi-list-name">{{ poi.name }}</p>
                <p class="poi-list-meta">{{ poi.year || '—' }} · {{ poi.category }}</p>
              </div>
            </div>
          </div>
        </div>
      </Drawer>
      <!-- Connector line from marker to card -->
      <div
        v-if="mapStore.selectedPoi"
        class="poi-connector"
        :style="connectorStyle"
      />

      <!-- Floating POI detail card (UI/UX 2026: чёткая структура, CTA озвучки) -->
      <transition name="fade">
        <div
          v-if="mapStore.selectedPoi"
          class="poi-modal floating"
          :style="modalStyle"
          role="dialog"
          aria-modal="true"
          aria-labelledby="poi-card-title"
        >
          <button
            class="modal-close"
            @click="mapStore.clearSelected()"
            aria-label="Закрыть карточку"
          >
            ✕
          </button>

          <div class="poi-sheet-handle" aria-hidden="true" />

          <div class="poi-modal-body">
            <!-- Слева: прямоугольник с фото + разделительная линия -->
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
            <div class="poi-card-divider" aria-hidden="true" />

            <!-- Справа: название, тип в углу, описание, две кнопки -->
            <div class="poi-content-col">
              <header class="poi-modal-header">
                <h2 id="poi-card-title" class="poi-modal-title">{{ mapStore.selectedPoi.name }}</h2>
                <span class="poi-card-badge poi-type-badge">{{ mapStore.selectedPoi.category }}</span>
                <dl v-if="mapStore.selectedPoi.year || mapStore.selectedPoi.architect" class="poi-meta-list">
                  <template v-if="mapStore.selectedPoi.year">
                    <dt>Год</dt>
                    <dd>{{ mapStore.selectedPoi.year }}</dd>
                  </template>
                  <template v-if="mapStore.selectedPoi.architect">
                    <dt>Архитектор</dt>
                    <dd>{{ mapStore.selectedPoi.architect }}</dd>
                  </template>
                </dl>
              </header>

              <!-- Вкладки: Описание | Фото | Панорама -->
              <div class="poi-tabs" role="tablist" aria-label="Разделы карточки">
                <button
                  v-for="tab in tabs"
                  :key="tab.key"
                  :class="['poi-tab-btn', { active: activeTab === tab.key }]"
                  @click="activeTab = tab.key"
                  role="tab"
                  :aria-selected="activeTab === tab.key"
                  :id="`tab-${tab.key}`"
                >
                  {{ tab.label }}
                </button>
              </div>

              <!-- Вкладка: Описание -->
              <div v-if="activeTab === 'desc'" class="poi-tab-content" role="tabpanel" aria-labelledby="tab-desc">
                <!-- Стиль текста (аудитория) -->
                <section class="poi-section" aria-label="Стиль рассказа">
                  <span class="poi-section-label">Стиль рассказа</span>
                  <div class="audience-btns">
                    <button
                      v-for="a in audiences"
                      :key="a.key"
                      :class="['btn btn-sm', selectedAudience === a.key ? 'btn-primary' : 'btn-ghost']"
                      @click="selectedAudience = a.key"
                      type="button"
                    >
                      {{ a.label }}
                    </button>
                  </div>
                </section>

                <!-- Описание (AI или краткое) -->
                <div v-if="aiContent || aiLoading" class="ai-content-box">
                  <div v-if="aiLoading" class="ai-content-header">
                    <span class="text-mono ai-label">✦ AI-описание</span>
                    <div class="spinner ai-spinner" aria-hidden="true" />
                  </div>
                  <p v-if="aiContent && !aiLoading" class="ai-content-text poi-desc-text">
                    {{ aiContent }}
                  </p>
                </div>
                <p v-else-if="mapStore.selectedPoi.description" class="poi-desc-text poi-desc-fallback">
                  {{ mapStore.selectedPoi.description }}
                </p>
                <p v-else class="poi-desc-placeholder">Нажмите «Сгенерировать», чтобы получить AI-описание.</p>

                <!-- Две кнопки: Озвучить и Описание -->
                <div class="poi-card-actions">
                  <button
                    type="button"
                    :class="['btn', 'btn-tts', 'btn-action-ozvuchit', isPlaying ? 'btn-danger' : 'btn-accent']"
                    :disabled="ttsLoading || !aiContent"
                    @click="toggleTTS"
                    :aria-label="isPlaying ? 'Остановить озвучку' : 'Озвучить текст'"
                  >
                    <span v-if="ttsLoading" class="tts-loading">
                      <span class="spinner" aria-hidden="true" />
                      Подготовка…
                    </span>
                    <span v-else-if="isPlaying">⏹ Остановить</span>
                    <span v-else>▶ Озвучить</span>
                  </button>
                  <button
                    type="button"
                    class="btn btn-ghost btn-action-opisanie"
                    @click="activeTab = 'desc'; isDescExpanded = true"
                    aria-label="Подробное описание"
                  >
                    Описание
                  </button>
                </div>

                <!-- Свернуть/развернуть текстовое описание и генерация AI -->
                <section v-if="mapStore.selectedPoi.description" class="poi-section poi-section-more">
                  <button
                    class="btn btn-ghost btn-xs desc-toggle"
                    type="button"
                    @click="isDescExpanded = !isDescExpanded"
                    :aria-expanded="isDescExpanded"
                  >
                    {{ isDescExpanded ? 'Скрыть текстовое описание' : 'Показать текстовое описание' }}
                  </button>
                  <transition name="fade">
                    <p v-if="isDescExpanded" class="poi-description">
                      {{ mapStore.selectedPoi.description }}
                    </p>
                  </transition>
                </section>
                <button
                  type="button"
                  class="btn btn-ghost btn-sm ai-gen-btn"
                  :disabled="aiLoading"
                  @click="generateAiContent"
                >
                  {{ aiContent ? '↻ Обновить описание' : '✦ Сгенерировать AI-описание' }}
                </button>
              </div>

              <!-- Вкладка: Фото -->
              <div v-if="activeTab === 'photos'" class="poi-tab-content" role="tabpanel" aria-labelledby="tab-photos">
                <div v-if="!mapStore.selectedPoi.photos?.length" class="empty-state">
                  <p>Фотографии не добавлены</p>
                </div>
                <div v-else class="photo-grid">
                  <div
                    v-for="(ph, i) in mapStore.selectedPoi.photos"
                    :key="i"
                    class="photo-card"
                  >
                    <img :src="ph.url" :alt="ph.caption || 'Фото объекта'" loading="lazy" />
                    <p v-if="ph.caption" class="photo-caption">
                      {{ ph.caption }} <span v-if="ph.year">({{ ph.year }})</span>
                    </p>
                  </div>
                </div>
              </div>

              <!-- Вкладка: Панорама -->
              <div v-if="activeTab === 'street'" class="poi-tab-content" role="tabpanel" aria-labelledby="tab-street">
                <div class="street-view-placeholder">
                  <p class="text-mono street-hint">Яндекс Панорамы / 2GIS Street View</p>
                  <p>Откройте объект в картах для просмотра панорамы.</p>
                  <a
                    :href="`https://yandex.ru/maps/?ll=${mapStore.selectedPoi.lng},${mapStore.selectedPoi.lat}&z=17&l=stv,sta`"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="btn btn-ghost btn-sm"
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
import { javaApi } from '@/api/backend.js'

const mapStore = useMapStore()
const toastStore = useToastStore()

// Предложить точку: всплывающее окно на карте
const suggestPoiOpen = ref(false)
const suggestForm = ref({ name: '', place: '', description: '', whyAdd: '' })
const suggestFormError = ref('')
const suggestFormSuccess = ref(false)
const suggestSubmitting = ref(false)
async function submitSuggestPoi() {
  suggestFormError.value = ''
  suggestFormSuccess.value = false
  suggestSubmitting.value = true
  try {
    await javaApi.poiSuggestions.submit({
      name: suggestForm.value.name.trim(),
      place: suggestForm.value.place.trim(),
      description: suggestForm.value.description?.trim() || undefined,
      whyAdd: suggestForm.value.whyAdd.trim()
    })
    suggestFormSuccess.value = true
    suggestForm.value = { name: '', place: '', description: '', whyAdd: '' }
    toastStore.push('Предложение отправлено', 'info')
    setTimeout(() => { suggestPoiOpen.value = false; suggestFormSuccess.value = false }, 2000)
  } catch (e) {
    suggestFormError.value = e.message || 'Не удалось отправить. Попробуйте позже.'
  } finally {
    suggestSubmitting.value = false
  }
}

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

// Current map zoom for responsive modal sizing
const zoomLevel = ref(14)

const modalScale = computed(() => {
  const baseZoom = 14
  const z = zoomLevel.value
  const diff = z - baseZoom
  // при приближении (zoom > baseZoom) уменьшаем окно, при отдалении — слегка увеличиваем
  const scale = 1 - diff * 0.06
  // не даём окну стать слишком маленьким или огромным
  return Math.min(1.05, Math.max(0.7, scale))
})

const modalStyle = computed(() => ({
  top: modalPosition.value.top + 'px',
  left: modalPosition.value.left + 'px',
  transform: `scale(${modalScale.value})`,
  transformOrigin: 'top left',
}))

// Collapsed/expanded state for text description
const isDescExpanded = ref(false)

// Мобильный сайдбар с точками (выезжает слева)
const mobileSidebarOpen = ref(false)

// Screen position of marker (for connector line)
const markerScreen = ref({ x: 0, y: 0 })

const connectorStyle = computed(() => {
  const cardX = modalPosition.value.left
  const cardY = modalPosition.value.top + 80 // примерно середина карточки

  const dx = cardX - markerScreen.value.x
  const dy = cardY - markerScreen.value.y
  const length = Math.sqrt(dx * dx + dy * dy)
  const angle = (Math.atan2(dy, dx) * 180) / Math.PI

  return {
    width: length + 'px',
    transform: `translate(${markerScreen.value.x}px, ${markerScreen.value.y}px) rotate(${angle}deg)`,
  }
})

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
      key: '2fa2df2d-9b29-4877-ac65-818e02de807d',
      style: '0651ff51-79b6-409c-8b90-37a9be2e97ad',
    })

    zoomLevel.value = map.getZoom()

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
      zoomLevel.value = map.getZoom()
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

  markerScreen.value = { x, y }
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
  isDescExpanded.value = false
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
}

// На мобильных: выбрать точку и закрыть drawer
function onSelectPoiMobile(poi) {
  selectAndFlyTo(poi)
  mobileSidebarOpen.value = false
  resetForPoi()
  isDescExpanded.value = false
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
  margin-top: var(--nav-h, 64px);
  width: 100%;
  max-width: 100vw;
  position: relative;
  overflow: hidden;
}

/* Sidebar: в потоке слева, не перекрывает карту */
.map-sidebar {
  flex: 0 0 auto;
  width: var(--sidebar-w, 320px);
  min-width: 0;
  background: rgba(15, 15, 15, 0.96);
  border-right: 1px solid var(--gray-800);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: var(--spacing-lg);
  gap: var(--spacing-md);
  z-index: 10;
}

.sidebar-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-sm);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--gray-800);
  flex-shrink: 0;
}

.sidebar-title {
  font-size: 1.25rem;
  line-height: 1.2;
  word-break: break-word;
}

.sidebar-suggest-btn {
  flex-shrink: 0;
  padding: 0.35rem 0.6rem;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.04em;
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--gray-400);
  cursor: pointer;
  transition: all var(--transition);
  white-space: nowrap;
}
.sidebar-suggest-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
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
.gps-nearest {
  font-size: 0.75rem;
  color: var(--gray-400);
  margin-top: 0.25rem;
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
  width: 0; /* flex: 1 1 0 + min-width: 0 + width: 0 — карта занимает остаток без наезда */
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Кнопка «Точки» на мобильных */
.map-poins-btn {
  display: none;
  position: absolute;
  left: var(--spacing-md);
  bottom: calc(var(--spacing-xl) + env(safe-area-inset-bottom, 0));
  z-index: 160;
  min-width: 44px;
  min-height: 44px;
  padding: 0 1rem;
  background: var(--header-bg, rgba(73, 62, 62, 0.98));
  border: 1px solid var(--gray-600);
  color: var(--paper);
  font-family: var(--font-mono);
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--shadow-card);
}
.map-poins-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

/* Контент мобильного drawer с точками */
.map-sidebar-drawer .map-sidebar-drawer-content {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}
.drawer-suggest-btn {
  width: 100%;
  padding: 0.75rem 1rem;
  margin-bottom: var(--spacing-md);
  font-family: var(--font-mono);
  font-size: 0.8rem;
  letter-spacing: 0.04em;
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  background: rgba(200, 169, 110, 0.08);
  color: var(--accent);
  cursor: pointer;
  transition: all var(--transition);
  text-align: center;
}
.drawer-suggest-btn:hover {
  border-color: var(--accent);
  background: rgba(200, 169, 110, 0.15);
}
.map-sidebar-drawer .filter-chips { display: flex; flex-wrap: wrap; gap: var(--spacing-xs); }
.map-sidebar-drawer .poi-list-item { min-height: 48px; padding: 0.75rem 1rem; }

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
  background: var(--header-bg, rgba(73, 62, 62, 0.98));
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

.suggest-poi-form .form-group { margin-bottom: 1rem; }
.suggest-poi-form .form-label { display: block; font-size: 0.75rem; color: var(--gray-400); margin-bottom: 0.35rem; }
.suggest-poi-form .form-input, .suggest-poi-form .form-textarea {
  width: 100%; padding: 0.5rem 0.75rem; font-size: 0.95rem;
  background: var(--gray-800); border: 1px solid var(--gray-600); border-radius: 6px; color: var(--paper);
}
.suggest-poi-form .form-textarea { min-height: 72px; resize: vertical; }
.suggest-poi-form .form-error { color: var(--danger, #e57373); font-size: 0.85rem; margin-bottom: 0.5rem; }
.suggest-poi-form .form-success { color: var(--success, #4caf50); font-size: 0.85rem; margin-bottom: 0.5rem; }
.suggest-poi-form .form-actions { display: flex; gap: 0.75rem; margin-top: 1rem; flex-wrap: wrap; }

/* ===== Connector line from marker to card ===== */
.poi-connector {
  position: absolute;
  height: 2px;
  background: rgba(200, 169, 110, 0.7);
  transform-origin: 0 50%;
  pointer-events: none;
  z-index: 140;
}

/* ===== Floating modal: горизонтальная карточка на десктопе ===== */
.poi-modal.floating {
  position: absolute;
  width: 100%;
  max-width: 640px;
  min-width: 520px;
  max-height: 75vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: rgba(18, 16, 14, 0.96);
  border-radius: 8px;
  padding: 0;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(200, 169, 110, 0.12);
  pointer-events: auto;
}

.poi-modal-body {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  gap: 0;
}

/* Слева: прямоугольник с фото (~42% ширины) */
.poi-photo-col {
  flex: 0 0 42%;
  max-width: 280px;
  min-width: 200px;
  overflow: hidden;
  display: flex;
  align-items: stretch;
}

.poi-photo {
  width: 100%;
  height: 100%;
  min-height: 240px;
  object-fit: cover;
  border-radius: 8px 0 0 0;
}

.poi-photo-placeholder {
  width: 100%;
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  color: var(--gray-500);
  background: var(--gray-800);
  border-radius: 8px 0 0 0;
}

/* Разделительная линия между фото и контентом */
.poi-card-divider {
  flex: 0 0 1px;
  width: 1px;
  background: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(200, 169, 110, 0.15) 15%,
    rgba(200, 169, 110, 0.35) 50%,
    rgba(200, 169, 110, 0.15) 85%,
    transparent 100%
  );
  align-self: stretch;
}

.poi-content-col {
  flex: 1 1 auto;
  min-width: 0;
  padding: var(--spacing-lg);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
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
  margin-bottom: var(--spacing-md);
  position: relative;
  padding-right: 0;
}

/* Название с подчёркиванием */
.poi-modal-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0 0 0.4rem;
  line-height: 1.25;
  color: var(--paper);
  padding-bottom: 0.35rem;
  border-bottom: 2px solid rgba(200, 169, 110, 0.4);
  padding-right: 6rem;
}

/* Тип (категория) в правом углу карточки */
.poi-type-badge {
  position: absolute;
  top: 0;
  right: 0;
  font-size: 0.68rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--accent);
  background: rgba(200, 169, 110, 0.12);
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  border: 1px solid rgba(200, 169, 110, 0.25);
}

.poi-card-badge:not(.poi-type-badge) {
  display: inline-block;
  font-size: 0.7rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--accent);
  background: rgba(200, 169, 110, 0.12);
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(200, 169, 110, 0.25);
}

.poi-meta-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0 1rem;
  margin: 0;
  font-size: 0.75rem;
  color: var(--gray-400);
}

.poi-meta-list dt {
  margin: 0;
  font-weight: 600;
  color: var(--gray-500);
}

.poi-meta-list dd {
  margin: 0;
}

.poi-meta-list dt::after {
  content: ': ';
}

/* Секции контента (UI/UX 2026) */
.poi-section {
  margin-top: var(--spacing-lg);
}

.poi-section-label {
  display: block;
  font-size: 0.7rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--gray-400);
  margin-bottom: var(--spacing-sm);
}

.ai-section .ai-label {
  color: var(--accent);
}

.ai-spinner {
  width: 16px;
  height: 16px;
}

.btn-tts {
  min-height: 44px;
  padding: 0.5rem 1rem;
  font-weight: 600;
}

.tts-loading {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.tts-loading .spinner {
  width: 14px;
  height: 14px;
}

.street-hint {
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

.audience-btns {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

/* Текст описания: читаемый на десктопе */
.poi-desc-text {
  color: var(--gray-200);
  line-height: 1.7;
  font-size: 0.9rem;
  max-width: 52ch;
  white-space: pre-wrap;
  margin: 0 0 var(--spacing-md);
}

.poi-desc-fallback {
  color: var(--gray-300);
  line-height: 1.7;
  font-size: 0.875rem;
}

.poi-desc-placeholder {
  color: var(--gray-500);
  font-size: 0.85rem;
  margin: 0 0 var(--spacing-md);
}

/* Две кнопки: Озвучить + Описание */
.poi-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
}

.btn-action-ozvuchit {
  min-height: 44px;
  padding: 0.5rem 1.25rem;
  font-weight: 600;
}

.btn-action-opisanie {
  min-height: 44px;
  padding: 0.5rem 1rem;
}

.poi-section-more {
  margin-top: var(--spacing-sm);
}

.ai-gen-btn {
  margin-top: var(--spacing-sm);
}

/* ===== AI content box ===== */
.ai-content-box {
  background: rgba(200, 169, 110, 0.04);
  border: 1px solid rgba(200, 169, 110, 0.15);
  border-radius: 6px;
  padding: var(--spacing-md);
  margin-top: var(--spacing-sm);
}

.ai-content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-sm);
}

.ai-content-text {
  color: var(--gray-200);
  line-height: 1.7;
  white-space: pre-wrap;
  font-size: 0.9rem;
  margin: 0;
}

/* ===== TTS row (мобильные / внутри блока) ===== */
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
  touch-action: manipulation;
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

/* ===== Responsive: планшеты ===== */
@media (max-width: 1024px) {
  :root {
    --sidebar-w: 280px;
  }
}

/* ===== Responsive: телефоны (оптимизация точек под сенсор) ===== */
@media (max-width: 768px) {
  .map-page {
    flex-direction: row;
    height: calc(100dvh - var(--nav-h, 64px));
    margin-top: var(--nav-h, 64px);
  }

  .map-sidebar {
    display: none;
  }

  .map-wrapper {
    flex: 1 1 0;
    min-width: 0;
    width: 100%;
  }

  .map-poins-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 48px;
    min-height: 48px;
    padding: 0 1rem;
    touch-action: manipulation;
  }

  /* Маркеры: зона нажатия ≥44px для удобного тапа */
  ::global(.custom-marker) {
    width: 44px;
    height: 44px;
    font-size: 18px;
  }
  ::global(.user-marker) {
    width: 22px;
    height: 22px;
    font-size: 12px;
  }

  /* Список точек в Drawer: высота строки под палец */
  .map-sidebar-drawer-content .poi-list-item,
  .poi-list-item {
    min-height: 48px;
    padding: 0.75rem 0.875rem;
    gap: 0.75rem;
  }
  .map-sidebar-drawer-content .poi-list {
    padding-bottom: env(safe-area-inset-bottom, 0);
    -webkit-overflow-scrolling: touch;
  }
  .map-sidebar-drawer-content {
    padding-bottom: env(safe-area-inset-bottom, 0);
    -webkit-overflow-scrolling: touch;
  }
  .drawer-suggest-btn {
    min-height: 44px;
    padding: 0.6rem 1rem;
    touch-action: manipulation;
  }

  /* Карточка POI: bottom sheet с ручкой и плавной прокруткой */
  .poi-modal.floating {
    position: fixed !important;
    left: 0 !important;
    right: 0 !important;
    top: auto !important;
    bottom: 0 !important;
    max-width: none !important;
    width: 100% !important;
    max-height: 88vh;
    border-radius: 12px 12px 0 0;
    box-shadow: 0 -8px 32px rgba(0, 0, 0, 0.4);
    padding-top: 0;
    padding-bottom: env(safe-area-inset-bottom, 0);
    transform: none !important;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  .poi-modal.floating {
    min-width: 0;
    max-width: none;
  }
  .poi-modal.floating::after { display: none; }
  .poi-card-divider {
    width: 100%;
    height: 1px;
    flex: 0 0 1px;
    background: linear-gradient(to right, transparent, rgba(200, 169, 110, 0.3), transparent);
  }
  .poi-modal-body {
    flex-direction: column;
    overflow-y: auto;
    overflow-x: hidden;
    -webkit-overflow-scrolling: touch;
    overscroll-behavior: contain;
    flex: 1 1 auto;
    min-height: 0;
  }
  .poi-photo-col { flex: 0 0 auto; max-width: none; min-width: 0; max-height: 200px; }
  .poi-photo { min-height: 0; max-height: 200px; object-fit: cover; border-radius: 0; }
  .poi-photo-placeholder { min-height: 0; max-height: 200px; border-radius: 0; }
  .poi-modal-title { font-size: 1.35rem; padding-right: 0; }
  .poi-type-badge { position: static; margin-top: 0.25rem; display: inline-block; }
  .poi-tabs .poi-tab-btn {
    min-height: 44px;
    min-width: 44px;
    padding: 0.6rem 1rem;
    touch-action: manipulation;
  }
  .modal-close {
    min-width: 44px;
    min-height: 44px;
    top: var(--spacing-sm);
    right: var(--spacing-sm);
    touch-action: manipulation;
  }

  .photo-grid {
    grid-template-columns: 1fr;
  }

  .map-ctrl-btn {
    width: 44px;
    height: 44px;
    touch-action: manipulation;
  }
  .map-controls {
    right: var(--spacing-sm);
    bottom: calc(var(--spacing-xl) + env(safe-area-inset-bottom, 0));
  }
  .filter-chip {
    min-height: 44px;
    min-width: 44px;
    padding: 0.5rem 0.875rem;
    touch-action: manipulation;
  }
  .poi-connector { display: none; }

  /* Кнопка озвучки в карточке */
  .btn-tts {
    min-height: 48px;
    width: 100%;
    justify-content: center;
  }
}

/* Ручка bottom sheet (только на мобильных) */
.poi-sheet-handle {
  display: none;
}
@media (max-width: 768px) {
  .poi-sheet-handle {
    display: block;
    flex-shrink: 0;
    padding: 0.5rem 0;
    text-align: center;
    cursor: grab;
    touch-action: none;
  }
  .poi-sheet-handle::before {
    content: '';
    display: inline-block;
    width: 36px;
    height: 4px;
    background: var(--gray-600);
    border-radius: 2px;
  }
}

@media (max-width: 480px) {
  .poi-modal.floating {
    max-height: 90vh;
  }
  .poi-modal-body {
    padding-left: var(--spacing-md);
    padding-right: var(--spacing-md);
  }
  .poi-photo-col { max-height: 180px; }
  .poi-photo, .poi-photo-placeholder { max-height: 180px; }
  .photo-grid {
    grid-template-columns: 1fr;
  }
}
</style>