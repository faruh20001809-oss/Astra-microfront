<template>
  <div class="map-page">
    <!-- Sidebar: на десктопе — слева в потоке; на мобильных скрыт, контент в Drawer -->
    <aside class="map-sidebar">
      <div class="sidebar-header motion-reveal">
        <h2 class="sidebar-title">Исторические объекты</h2>
        <button type="button" class="sidebar-suggest-btn" @click="suggestPoiOpen = true" aria-label="Предложить точку">
          💡 Предложить точку
        </button>
      </div>
      <div class="filter-section motion-reveal motion-reveal-delay-1">
        <p class="text-mono" style="color:var(--gray-400);margin-bottom:0.5rem">Категории</p>
        <div class="filter-chips motion-stagger-children">
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
        v-if="mapStore.activeFollowRoute"
        class="map-route-banner"
        role="region"
        aria-label="Активный маршрут"
      >
        <div class="map-route-banner-top">
          <span class="map-route-banner-label">Маршрут</span>
          <span class="map-route-banner-title">{{ mapStore.activeFollowRoute.title }}</span>
          <button type="button" class="btn btn-sm btn-ghost map-route-banner-close" @click="clearFollowRouteFromMap">
            Скрыть
          </button>
        </div>
        <p v-if="followRouteStops.length >= 2" class="map-route-banner-hint text-mono">
          Линия по улицам — 2ГИС Routing API. Отметьте пройденные точки (сохраняется в браузере).
        </p>
        <ul v-if="followRouteStops.length" class="map-route-stops" aria-label="Точки маршрута">
          <li v-for="(stop, idx) in followRouteStops" :key="stop.id" class="map-route-stop-item">
            <label class="map-route-stop-label">
              <input
                type="checkbox"
                class="map-route-stop-check"
                :checked="isRouteStopVisited(stop.id)"
                @change="onRouteStopChecked(stop.id, $event.target.checked)"
              />
              <span class="map-route-stop-text">{{ idx + 1 }}. {{ stop.name }}</span>
            </label>
          </li>
        </ul>
      </div>
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

      <Dialog
        v-model:visible="newPoiDialogOpen"
        modal
        header="На карте новая точка"
        :style="{ width: 'min(400px, 92vw)' }"
        :dismissableMask="true"
        @hide="onNewPoiDialogHide"
      >
        <div v-if="newPoiHighlight" class="new-poi-dialog-body">
          <p class="new-poi-dialog-name">{{ newPoiHighlight.name }}</p>
          <p v-if="newPoiHighlight.description" class="new-poi-dialog-desc">{{ newPoiHighlightDescription }}</p>
          <p v-else class="new-poi-dialog-desc text-mono" style="color:var(--gray-500)">Категория: {{ newPoiHighlight.category || '—' }}</p>
          <div class="new-poi-dialog-actions">
            <Button type="button" label="Открыть на карте" class="btn-accent" @click="openNewPoiOnMap" />
            <Button type="button" label="Закрыть" class="p-button-text p-button-secondary" @click="dismissNewPoiDialog" />
          </div>
        </div>
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
                <div class="poi-modal-title-row">
                  <h2 id="poi-card-title" class="poi-modal-title">{{ mapStore.selectedPoi.name }}</h2>
                  <button
                    type="button"
                    class="poi-fav-btn"
                    :class="{ active: isPoiFavorite(mapStore.selectedPoi.id) }"
                    :aria-pressed="isPoiFavorite(mapStore.selectedPoi.id)"
                    :aria-label="isPoiFavorite(mapStore.selectedPoi.id) ? 'Убрать из избранного' : 'В избранное'"
                    @click.stop="togglePoi(mapStore.selectedPoi.id)"
                  >
                    {{ isPoiFavorite(mapStore.selectedPoi.id) ? '♥' : '♡' }}
                  </button>
                </div>
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

              <!-- Вкладки: сегментированный контрол -->
              <div class="poi-tabs-shell">
                <div class="poi-tabs" role="tablist" aria-label="Разделы карточки">
                  <button
                    v-for="tab in tabs"
                    :key="tab.key"
                    type="button"
                    :class="['poi-tab-btn', { active: activeTab === tab.key }]"
                    @click="activeTab = tab.key"
                    role="tab"
                    :aria-selected="activeTab === tab.key"
                    :id="`tab-${tab.key}`"
                  >
                    {{ tab.label }}
                  </button>
                </div>
              </div>

              <!-- Вкладка: Описание -->
              <div v-if="activeTab === 'desc'" class="poi-tab-content" role="tabpanel" aria-labelledby="tab-desc">
                <!-- Описание: сначала краткий текст POI, затем AI (по запросу) -->
                <p v-if="mapStore.selectedPoi.description" class="poi-desc-text poi-desc-fallback">
                  {{ mapStore.selectedPoi.description }}
                </p>

                <div v-if="aiContent || aiLoading" class="ai-content-box">
                  <div v-if="aiLoading" class="ai-content-header">
                    <span class="text-mono ai-label">✦ AI-описание</span>
                    <div class="spinner ai-spinner" aria-hidden="true" />
                  </div>
                  <p v-if="aiContent && !aiLoading" class="ai-content-text poi-desc-text">
                    {{ aiContent }}
                  </p>
                </div>
                <p v-else class="poi-desc-placeholder">Нажмите «Сгенерировать», чтобы получить AI-описание.</p>

                <div class="poi-tts-voice-row">
                  <label for="poi-ai-audience-map" class="poi-tts-voice-label">Стиль AI-текста:</label>
                  <select
                    id="poi-ai-audience-map"
                    v-model="aiAudience"
                    class="poi-tts-voice-select"
                    :disabled="aiLoading"
                  >
                    <option value="default">Обычное</option>
                    <option value="children">Детям</option>
                    <option value="academic">Академично</option>
                  </select>
                </div>

                <!-- Выбор озвучки -->
                <div class="poi-tts-voice-row">
                  <label for="poi-tts-voice" class="poi-tts-voice-label">Озвучка:</label>
                  <select
                    id="poi-tts-voice"
                    v-model.number="selectedTtsVoice"
                    class="poi-tts-voice-select"
                    :disabled="ttsLoading"
                  >
                    <option v-for="(v, i) in ttsVoices" :key="i" :value="i">
                      {{ v.label }}
                    </option>
                  </select>
                </div>

                <!-- CTA: Озвучить (главная) -->
                <div class="poi-card-actions poi-card-actions--tts">
                  <button
                    type="button"
                    class="btn btn-tts btn-action-ozvuchit btn-accent"
                    :disabled="ttsLoading || isPlaying || !(aiContent || mapStore.selectedPoi.description)"
                    @click="toggleTTS"
                    aria-label="Озвучить текст"
                  >
                    <span v-if="ttsLoading" class="tts-loading">
                      <span class="spinner" aria-hidden="true" />
                      Подготовка…
                    </span>
                    <span v-else>▶ Озвучить</span>
                  </button>
                  <button
                    v-show="isPlaying || ttsLoading"
                    type="button"
                    class="btn btn-tts btn-ghost btn-sm poi-tts-stop-btn"
                    :disabled="!(isPlaying || ttsLoading)"
                    aria-label="Остановить озвучку"
                    @click="stopTtsPlayback"
                  >
                    ⏹ Стоп
                  </button>
                </div>

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
import { useRoute, useRouter } from 'vue-router'
import { useMapStore, useToastStore } from '@/store/index.js'
import { usePoiAiTts } from '@/composables/usePoiAiTts.js'
import { useFavorites } from '@/composables/useFavorites.js'
import { useGuestProgress } from '@/composables/useGuestProgress.js'
import { javaApi } from '@/api/backend.js'
import { fetch2gisRouteCoordinates, isDgisRoutingConfigured } from '@/api/dgisRouting.js'

const LS_POI_CATALOG_MAX = 'astra-poi-catalog-max-id'
const LS_POI_DISMISSED = 'astra-poi-dismissed-new-ids'
const NEW_POI_POLL_MS = 75_000

/** Один JSON из `VITE_DGIS_MAP`: `{ "key": "…", "style": "…" }` (ключ как `DGIS_MAP_KEY` / `app.dgis.map-key` в админке). */
const DGIS_MAP_DEFAULT = Object.freeze({
  key: '2fa2df2d-9b29-4877-ac65-818e02de807d',
  style: '0651ff51-79b6-409c-8b90-37a9be2e97ad',
})

function loadDgisMapFromEnv() {
  const raw = import.meta.env.VITE_DGIS_MAP
  if (!raw || typeof raw !== 'string') return { ...DGIS_MAP_DEFAULT }
  try {
    const o = JSON.parse(raw)
    return {
      key: typeof o.key === 'string' && o.key.trim() ? o.key.trim() : DGIS_MAP_DEFAULT.key,
      style:
        typeof o.style === 'string' && o.style.trim() ? o.style.trim() : DGIS_MAP_DEFAULT.style,
    }
  } catch {
    return { ...DGIS_MAP_DEFAULT }
  }
}

const dgisMap = loadDgisMapFromEnv()

const mapStore = useMapStore()
const toastStore = useToastStore()
const vueRoute = useRoute()
const router = useRouter()
const { isPoiFavorite, togglePoi } = useFavorites()
const { markPoiVisited } = useGuestProgress()

watch(
  () => mapStore.selectedPoi,
  (poi) => {
    if (poi?.id != null) markPoiVisited(poi.id)
  },
)

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
/** Только маркеры ТОИ (пересоздаются в renderMarkers). Метка GPS хранится отдельно — иначе renderMarkers её сносит. */
let markers = {}
/** Маркер «вы здесь» (геолокация), не входит в `markers`. */
let userLocationMarker = null
let mapglCheckInterval = null
/** Линия активного маршрута (2GIS MapGL Polyline). */
let routePolyline = null
/** Сбрасывать фильтры категорий только при смене маршрута (не при каждом poll POI). */
let lastFollowRouteAppliedId = null

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

/** ID точек маршрута, отмеченные пользователем как пройденные (localStorage на ключ маршрута). */
const routeProgressVisited = ref([])

function routeProgressStorageKey(routeId) {
  return `astra-route-progress-${Number(routeId)}`
}

function loadRouteProgress(routeId) {
  if (routeId == null || !Number.isFinite(Number(routeId))) {
    routeProgressVisited.value = []
    return
  }
  try {
    const raw = localStorage.getItem(routeProgressStorageKey(routeId))
    const arr = raw ? JSON.parse(raw) : []
    routeProgressVisited.value = Array.isArray(arr)
      ? [...new Set(arr.map(Number).filter(Number.isFinite))]
      : []
  } catch {
    routeProgressVisited.value = []
  }
}

function persistRouteProgress() {
  const r = mapStore.activeFollowRoute
  if (!r?.id) return
  try {
    localStorage.setItem(routeProgressStorageKey(r.id), JSON.stringify(routeProgressVisited.value))
  } catch (_) {
    /* quota / private mode */
  }
}

function isRouteStopVisited(poiId) {
  return routeProgressVisited.value.includes(Number(poiId))
}

function onRouteStopChecked(poiId, checked) {
  const n = Number(poiId)
  const next = new Set(routeProgressVisited.value)
  if (checked) next.add(n)
  else next.delete(n)
  routeProgressVisited.value = [...next]
  persistRouteProgress()
  renderMarkers()
}

const followRouteStops = computed(() => {
  const r = mapStore.activeFollowRoute
  if (!r?.poiIds?.length) return []
  const out = []
  for (const pid of r.poiIds) {
    const p = mapStore.pois.find((x) => Number(x.id) === Number(pid))
    if (p) out.push(p)
  }
  return out
})

watch(
  () => mapStore.activeFollowRoute?.id,
  (id) => {
    if (id != null && Number.isFinite(Number(id))) loadRouteProgress(Number(id))
    else routeProgressVisited.value = []
  },
)

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
} = usePoiAiTts()

const newPoiDialogOpen = ref(false)
const newPoiHighlight = ref(null)
const newPoiShownSession = new Set()
let newPoiPollTimer = null

const newPoiHighlightDescription = computed(() => {
  const t = newPoiHighlight.value?.description
  if (!t) return ''
  return t.length > 220 ? `${t.slice(0, 217)}…` : t
})

function catalogMaxFromStore() {
  const ids = mapStore.pois.map((p) => Number(p.id)).filter((n) => Number.isFinite(n))
  return ids.length ? Math.max(...ids) : 0
}

function syncCatalogPointerFromPois() {
  try {
    const m = catalogMaxFromStore()
    const prev = parseInt(localStorage.getItem(LS_POI_CATALOG_MAX) || '0', 10)
    if (m > prev) localStorage.setItem(LS_POI_CATALOG_MAX, String(m))
  } catch (_) { /* ignore */ }
}

function loadDismissedSet() {
  try {
    const raw = localStorage.getItem(LS_POI_DISMISSED)
    const arr = raw ? JSON.parse(raw) : []
    return new Set(Array.isArray(arr) ? arr.map(Number).filter((n) => Number.isFinite(n)) : [])
  } catch {
    return new Set()
  }
}

function saveDismissedSet(set) {
  try {
    localStorage.setItem(LS_POI_DISMISSED, JSON.stringify([...set]))
  } catch (_) { /* ignore */ }
}

function normalizeNewPoiFromApi(p) {
  if (!p || p.id == null) return null
  return {
    id: p.id,
    name: p.name || '',
    description: p.description || '',
    category: p.category || '',
    lat: p.latitude ?? p.lat ?? 0,
    lng: p.longitude ?? p.lng ?? 0,
  }
}

function bumpCatalogMaxAfterPoi(poiId) {
  try {
    const stored = parseInt(localStorage.getItem(LS_POI_CATALOG_MAX) || '0', 10)
    localStorage.setItem(LS_POI_CATALOG_MAX, String(Math.max(stored, Number(poiId))))
  } catch (_) { /* ignore */ }
}

async function pollNewCatalogPois() {
  if (newPoiDialogOpen.value) return
  try {
    const rev = await javaApi.pois.getCatalogRevision()
    const stored = parseInt(localStorage.getItem(LS_POI_CATALOG_MAX) || '0', 10)
    if (!Number.isFinite(rev) || rev <= stored) return
    const raw = await javaApi.pois.listSince(stored, 10)
    const dismissed = loadDismissedSet()
    const candidates = raw
      .map(normalizeNewPoiFromApi)
      .filter((p) => p && !dismissed.has(p.id) && !newPoiShownSession.has(p.id))
    if (!candidates.length) {
      localStorage.setItem(LS_POI_CATALOG_MAX, String(rev))
      return
    }
    candidates.sort((a, b) => a.id - b.id)
    const next = candidates[0]
    newPoiShownSession.add(next.id)
    newPoiHighlight.value = next
    newPoiDialogOpen.value = true
  } catch (e) {
    console.warn('POI catalog poll:', e)
  }
}

function dismissNewPoiDialog() {
  newPoiDialogOpen.value = false
}

function onNewPoiDialogHide() {
  const h = newPoiHighlight.value
  if (h?.id == null) return
  const dismissed = loadDismissedSet()
  dismissed.add(h.id)
  saveDismissedSet(dismissed)
  bumpCatalogMaxAfterPoi(h.id)
  newPoiHighlight.value = null
}

async function openNewPoiOnMap() {
  const highlight = newPoiHighlight.value
  const id = highlight?.id
  if (id == null) return
  bumpCatalogMaxAfterPoi(id)
  newPoiHighlight.value = null
  newPoiDialogOpen.value = false
  await mapStore.fetchPois()
  syncCatalogPointerFromPois()
  const poi = mapStore.pois.find((p) => p.id === id)
  if (poi) {
    selectAndFlyTo(poi)
    openPoi(poi)
    resetForPoi()
    isDescExpanded.value = false
  } else {
    toastStore.push('Обновите карту: точка скоро появится в списке', 'info')
  }
}

// Constants
const ASTRAKHAN_CENTER = [48.0408, 46.3497] // [lng, lat]

// ── Lifecycle ──
onMounted(async () => {
  if (!vueRoute.query.route) {
    mapStore.clearActiveFollowRoute()
  }
  await mapStore.fetchPois()
  syncCatalogPointerFromPois()
  newPoiPollTimer = setInterval(pollNewCatalogPois, NEW_POI_POLL_MS)
  setTimeout(pollNewCatalogPois, 12_000)

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
  if (newPoiPollTimer) {
    clearInterval(newPoiPollTimer)
    newPoiPollTimer = null
  }
  if (mapglCheckInterval) {
    clearInterval(mapglCheckInterval)
    mapglCheckInterval = null
  }
  stopGPS()
  destroyRoutePolyline()
  removeUserLocationMarker()
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
      key: dgisMap.key,
      style: dgisMap.style,
    })

    zoomLevel.value = map.getZoom()

    map.on('load', () => {
      renderMarkers()
      void applyFollowRouteFromQuery()
      syncUserLocationMarkerFromStore()
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

  const followIds = mapStore.activeFollowRoute?.poiIds
  const visitedSet = new Set(routeProgressVisited.value)

  for (const poi of mapStore.filteredPois) {
    const el = document.createElement('div')
    el.className = 'custom-marker'
    if (
      followIds?.length &&
      followIds.some((id) => Number(id) === Number(poi.id)) &&
      visitedSet.has(Number(poi.id))
    ) {
      el.classList.add('custom-marker--route-done')
    }
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

function destroyRoutePolyline() {
  if (routePolyline && typeof routePolyline.destroy === 'function') {
    try {
      routePolyline.destroy()
    } catch (_) { /* ignore */ }
  }
  routePolyline = null
}

async function applyFollowRouteFromQuery() {
  if (!map) return
  const rawQ = vueRoute.query.route
  if (rawQ == null || rawQ === '') {
    destroyRoutePolyline()
    mapStore.clearActiveFollowRoute()
    lastFollowRouteAppliedId = null
    return
  }
  const routeId = Number(rawQ)
  if (!Number.isFinite(routeId)) return

  let poiIds = []
  const cached = mapStore.activeFollowRoute
  if (cached && cached.id === routeId && Array.isArray(cached.poiIds) && cached.poiIds.length) {
    poiIds = [...cached.poiIds]
  } else {
    try {
      const data = await javaApi.routes.getById(routeId)
      if (data && Array.isArray(data.pois)) {
        poiIds = data.pois.map(Number).filter(Number.isFinite)
      }
      const title = data?.title || data?.name || 'Маршрут'
      mapStore.setActiveFollowRoute({ id: routeId, title, poiIds })
    } catch (e) {
      console.warn('Route fetch for map:', e)
      toastStore.push('Не удалось загрузить маршрут', 'error')
      return
    }
  }

  if (!poiIds.length) {
    toastStore.push('У маршрута нет привязанных точек на карте', 'info')
    destroyRoutePolyline()
    return
  }

  const ordered = []
  for (const pid of poiIds) {
    const p = mapStore.pois.find((x) => Number(x.id) === Number(pid))
    if (p && Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng))) ordered.push(p)
  }

  if (!ordered.length) {
    toastStore.push('Точки маршрута не найдены среди объектов на карте', 'info')
    destroyRoutePolyline()
    return
  }

  if (lastFollowRouteAppliedId !== routeId) {
    mapStore.activeFilters = []
    lastFollowRouteAppliedId = routeId
  }
  renderMarkers()
  destroyRoutePolyline()

  let lineCoords = ordered.map((p) => [Number(p.lng), Number(p.lat)])
  if (ordered.length >= 2 && isDgisRoutingConfigured()) {
    try {
      const routed = await fetch2gisRouteCoordinates(
        ordered.map((p) => ({ lng: Number(p.lng), lat: Number(p.lat) })),
      )
      if (routed && routed.length >= 2) {
        lineCoords = routed
      }
    } catch (e) {
      console.warn('2GIS Routing API:', e)
      toastStore.push('Не удалось построить маршрут по дорогам — показана прямая линия', 'info')
    }
  }

  if (ordered.length >= 2 && window.mapgl?.Polyline) {
    try {
      routePolyline = new window.mapgl.Polyline(map, {
        coordinates: lineCoords,
        width: 6,
        color: '#c8a96e',
        zIndex: 2,
      })
    } catch (e) {
      console.warn('Polyline:', e)
    }
  }

  selectAndFlyTo(ordered[0])
  openPoi(ordered[0])
}

function clearFollowRouteFromMap() {
  destroyRoutePolyline()
  lastFollowRouteAppliedId = null
  mapStore.clearActiveFollowRoute()
  router.replace({ path: '/', query: {} })
}

// ── Watchers ──
watch(
  () => mapStore.activeFilters,
  () => {
    if (map) renderMarkers()
  },
  { deep: true },
)

watch(
  () => [String(vueRoute.query.route ?? ''), mapStore.pois.length],
  () => {
    if (map) void applyFollowRouteFromQuery()
  },
)

// ── GPS ──
/** Первое успешное положение — подвинуть карту к пользователю. */
let gpsPendingFlyToUser = false

function removeUserLocationMarker() {
  if (userLocationMarker && typeof userLocationMarker.destroy === 'function') {
    try {
      userLocationMarker.destroy()
    } catch (_) { /* ignore */ }
  }
  userLocationMarker = null
}

function updateUserLocationMarker(lat, lng) {
  if (!map || !window.mapgl?.HtmlMarker) return
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return

  if (!userLocationMarker) {
    const el = document.createElement('div')
    el.className = 'user-marker'
    el.innerHTML = '◉'
    el.setAttribute('aria-label', 'Вы здесь')
    el.title = 'Ваше местоположение'
    userLocationMarker = new window.mapgl.HtmlMarker(map, {
      coordinates: [lng, lat],
      html: el,
      anchor: 'center',
    })
  } else {
    userLocationMarker.setCoordinates([lng, lat])
  }

  if (gpsPendingFlyToUser) {
    gpsPendingFlyToUser = false
    try {
      map.setCenter([lng, lat], { animate: true, duration: 500 })
      map.setZoom(Math.max(map.getZoom(), 15), { animate: true, duration: 500 })
    } catch (_) { /* ignore */ }
  }
}

function syncUserLocationMarkerFromStore() {
  const loc = mapStore.userLocation
  if (!gpsActive.value || !loc) return
  updateUserLocationMarker(loc.lat, loc.lng)
}

function toggleGPS() {
  if (gpsActive.value) stopGPS()
  else startGPS()
}

const GPS_INSECURE_HINT =
  'Геолокация в браузере разрешена только по HTTPS или на http://localhost. ' +
  'Для телефона в локальной сети запустите фронт командой npm run dev:https и откройте https://… (примите предупреждение о сертификате).'

function startGPS() {
  if (!navigator.geolocation) {
    toastStore.push('Геолокация не поддерживается вашим браузером', 'error')
    return
  }

  if (typeof window !== 'undefined' && !window.isSecureContext) {
    gpsStatus.value = 'Нужен HTTPS или localhost'
    toastStore.push(GPS_INSECURE_HINT, 'error')
    return
  }

  gpsStatus.value = 'Определение местоположения…'
  gpsPendingFlyToUser = true

  watchId = navigator.geolocation.watchPosition(
    (pos) => {
      const { latitude: lat, longitude: lng, accuracy } = pos.coords
      gpsActive.value = true
      gpsStatus.value = `Точность: ±${Math.round(accuracy)} м`

      mapStore.setUserLocation({ lat, lng })
      updateUserLocationMarker(lat, lng)
    },
    (err) => {
      console.error('GPS error:', err)
      const raw = err?.message || ''
      const insecureOrigin =
        /secure origin/i.test(raw) ||
        /insecure/i.test(raw) ||
        (typeof window !== 'undefined' && !window.isSecureContext)
      if (insecureOrigin) {
        gpsStatus.value = 'Нужен HTTPS или localhost'
        toastStore.push(GPS_INSECURE_HINT, 'error')
      } else {
        gpsStatus.value = 'Ошибка: ' + (raw || 'Неизвестная ошибка')
        toastStore.push('Не удалось определить местоположение', 'error')
      }
      gpsActive.value = false
      gpsPendingFlyToUser = false
    },
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 5000 },
  )
}

function stopGPS() {
  if (watchId !== null) {
    navigator.geolocation.clearWatch(watchId)
    watchId = null
  }
  gpsActive.value = false
  gpsStatus.value = ''
  gpsPendingFlyToUser = false
  mapStore.setUserLocation(null)
  removeUserLocationMarker()
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
.map-page {
  display: flex;
  flex-direction: row;
  height: calc(100dvh - env(safe-area-inset-top, 0px) - var(--nav-h, 64px));
  margin-top: calc(env(safe-area-inset-top, 0px) + var(--nav-h, 64px));
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
  background: rgba(200, 169, 110, 0.15);
  border-right: 1px solid var(--header-border-color);
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
  border-bottom: 1px solid var(--header-border-color);
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

.map-route-banner {
  position: absolute;
  top: var(--spacing-md);
  left: 50%;
  transform: translateX(-50%);
  z-index: 170;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: min(440px, calc(100% - 2rem));
  max-height: min(52vh, 420px);
  overflow-y: auto;
  padding: 0.6rem 0.75rem;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  background: rgba(18, 16, 14, 0.92);
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-card);
}

.map-route-banner-top {
  display: flex;
  align-items: center;
  gap: 0.5rem 0.75rem;
  flex-wrap: wrap;
}

.map-route-banner-hint {
  margin: 0;
  font-size: 0.62rem;
  line-height: 1.45;
  color: var(--gray-400);
  letter-spacing: 0.04em;
}

.map-route-stops {
  list-style: none;
  margin: 0;
  padding: 0.15rem 0 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.map-route-stop-item {
  margin: 0;
}

.map-route-stop-label {
  display: flex;
  align-items: flex-start;
  gap: 0.45rem;
  cursor: pointer;
  font-size: 0.78rem;
  line-height: 1.35;
  color: var(--gray-200);
}

.map-route-stop-check {
  flex-shrink: 0;
  margin-top: 0.12rem;
  width: 1rem;
  height: 1rem;
  accent-color: var(--accent);
}

.map-route-stop-text {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.map-route-banner-label {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent);
}
.map-route-banner-title {
  font-size: 0.85rem;
  color: var(--paper);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.map-route-banner-close {
  flex-shrink: 0;
  padding: 0.25rem 0.5rem !important;
  font-size: 0.75rem !important;
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

.new-poi-dialog-body { padding: 0.25rem 0; }
.new-poi-dialog-name { font-family: var(--font-display); font-size: 1.1rem; margin: 0 0 0.5rem; color: var(--paper); }
.new-poi-dialog-desc { font-size: 0.9rem; color: var(--gray-300); margin: 0 0 1rem; line-height: 1.45; }
.new-poi-dialog-actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }

/* Тёмное «тело» линии — читается на светлом фоне карты; лёгкий тёплый акцент в центре */
.poi-connector {
  position: absolute;
  height: 5px;
  transform-origin: 0 50%;
  pointer-events: none;
  z-index: 165;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    rgba(20, 16, 13, 0.12) 0%,
    rgba(42, 35, 30, 0.88) 18%,
    rgba(62, 52, 44, 0.96) 50%,
    rgba(42, 35, 30, 0.88) 82%,
    rgba(20, 16, 13, 0.12) 100%
  );
  box-shadow:
    0 0 0 1px rgba(20, 16, 13, 0.5),
    0 1px 3px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 252, 248, 0.12) inset,
    0 0 10px rgba(212, 184, 150, 0.22);
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.4));
}

@media (prefers-reduced-motion: reduce) {
  .poi-connector {
    filter: none;
    box-shadow:
      0 0 0 2px rgba(42, 35, 30, 0.9),
      0 1px 2px rgba(0, 0, 0, 0.3);
  }
}

/* ===== Floating modal: горизонтальная карточка на десктопе ===== */
.poi-modal.floating {
  position: absolute;
  z-index: 220;
  width: 100%;
  max-width: min(660px, 96vw);
  min-width: min(540px, 96vw);
  max-height: 78vh;
  overflow: visible;
  display: flex;
  flex-direction: column;
  background: linear-gradient(
    155deg,
    rgba(30, 25, 21, 0.97) 0%,
    rgba(18, 16, 14, 0.98) 45%,
    rgba(14, 12, 10, 0.99) 100%
  );
  border-radius: 12px;
  padding: clamp(2.25rem, 3vw, 2.75rem) clamp(2.5rem, 3vw, 2.75rem) 0 clamp(0.75rem, 1.5vw, 1rem);
  border: 1px solid rgba(212, 184, 150, 0.14);
  box-shadow:
    var(--shadow-modal),
    0 0 0 1px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 252, 248, 0.04);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
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
  border-radius: 12px 0 0 0;
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
  border-radius: 12px 0 0 0;
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
  display: flex;
  flex-direction: column;
  gap: 0;
}

.modal-close {
  position: absolute;
  top: var(--spacing-sm);
  right: var(--spacing-sm);
  background: rgba(20, 16, 13, 0.88);
  border: 1px solid rgba(212, 184, 150, 0.22);
  color: var(--paper);
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 999px;
  font-size: 0.9rem;
  transition: border-color var(--transition), background var(--transition), color var(--transition);
  z-index: 20;
}

.modal-close:hover {
  border-color: var(--accent);
  color: var(--cream);
}

.modal-close:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.poi-modal-header {
  margin-bottom: var(--spacing-md);
  position: relative;
  padding-right: 0;
  flex-shrink: 0;
}

.poi-modal-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.35rem;
}

.poi-fav-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  border: 1px solid var(--gray-600);
  background: rgba(0, 0, 0, 0.35);
  color: var(--gray-300);
  font-size: 1rem;
  cursor: pointer;
  transition: all var(--transition);
}

.poi-fav-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.poi-fav-btn.active {
  color: #e8a0a0;
  border-color: rgba(232, 160, 160, 0.45);
}

/* Название с подчёркиванием */
.poi-modal-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0;
  line-height: 1.25;
  color: var(--paper);
  padding-bottom: 0.35rem;
  border-bottom: 2px solid rgba(200, 169, 110, 0.4);
  flex: 1;
  min-width: 0;
}

/* Тип (категория) */
.poi-type-badge {
  position: static;
  display: inline-block;
  font-size: 0.68rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--accent);
  background: rgba(200, 169, 110, 0.12);
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  border: 1px solid rgba(200, 169, 110, 0.25);
  margin-bottom: 0.35rem;
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

/* ===== Tabs: сегмент в «дорожке» ===== */
.poi-tabs-shell {
  width: 100%;
  flex-shrink: 0;
  margin-bottom: var(--spacing-lg);
  margin-top: 0;
}

.poi-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
  width: 100%;
  padding: 4px;
  border-radius: 10px;
  background: rgba(20, 16, 13, 0.65);
  border: 1px solid rgba(212, 184, 150, 0.12);
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.25);
}

.poi-tab-btn {
  padding: 0.45rem 0.35rem;
  font-family: var(--font-mono);
  font-size: clamp(0.58rem, 0.45vw + 0.52rem, 0.72rem);
  letter-spacing: 0.05em;
  text-transform: uppercase;
  background: transparent;
  border: none;
  border-radius: 7px;
  color: var(--gray-400);
  cursor: pointer;
  transition:
    color var(--transition),
    background var(--transition),
    box-shadow var(--transition);
  min-width: 0;
  text-align: center;
  line-height: 1.25;
}

.poi-tab-btn:hover {
  color: var(--paper);
}

.poi-tab-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}

.poi-tab-btn.active {
  color: var(--ink);
  background: linear-gradient(180deg, rgba(242, 232, 220, 0.98) 0%, rgba(212, 184, 150, 0.55) 100%);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
}

.poi-tab-content {
  animation: fadeIn 0.2s ease;
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
}

@media (prefers-reduced-motion: reduce) {
  .poi-tab-content {
    animation: none;
  }
  .poi-tab-btn {
    transition: none;
  }
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

/* Выбор озвучки */
.poi-tts-voice-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}
.poi-tts-voice-label {
  font-size: 0.8rem;
  color: var(--gray-400);
  white-space: nowrap;
}
.poi-tts-voice-select {
  flex: 1;
  min-width: 0;
  max-width: 280px;
  padding: 0.4rem 0.6rem;
  font-size: 0.85rem;
  font-family: var(--font-mono);
  background: var(--gray-800);
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  color: var(--paper);
  cursor: pointer;
}
.poi-tts-voice-select:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Кнопка Озвучить */
.poi-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.poi-card-actions--tts {
  align-items: center;
}

.poi-tts-stop-btn {
  min-height: 44px;
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

/* Точка маршрута отмечена как пройденная (чекбокс в баннере) */
::global(.custom-marker--route-done) {
  border-color: rgba(80, 160, 110, 0.95);
  box-shadow: 0 0 0 2px rgba(80, 160, 110, 0.35);
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
    height: calc(100dvh - env(safe-area-inset-top, 0px) - var(--nav-h, 64px));
    margin-top: calc(env(safe-area-inset-top, 0px) + var(--nav-h, 64px));
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
    z-index: 320 !important;
    left: 0 !important;
    right: 0 !important;
    top: auto !important;
    bottom: 0 !important;
    max-width: none !important;
    min-width: 0 !important;
    width: 100% !important;
    /* Карточка занимает нижнюю часть экрана, сверху остаётся карта */
    max-height: 70vh;
    min-height: 40vh;
    border-radius: 16px 16px 0 0;
    border: 1px solid rgba(212, 184, 150, 0.14);
    border-bottom: none;
    box-shadow:
      0 -12px 40px rgba(0, 0, 0, 0.45),
      inset 0 1px 0 rgba(255, 252, 248, 0.05);
    padding-top: 0;
    padding-left: 0;
    padding-right: 0;
    padding-bottom: env(safe-area-inset-bottom, 0);
    transform: none !important;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  .poi-tabs .poi-tab-btn.active {
    color: var(--ink);
  }
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
  .poi-modal-title { font-size: 1.35rem; }
  .poi-type-badge { position: static; margin-top: 0.25rem; display: inline-block; }
  .poi-tabs .poi-tab-btn {
    min-height: 44px;
    min-width: 0;
    padding: 0.5rem 0.25rem;
    touch-action: manipulation;
  }
  .modal-close {
    min-width: 36px;
    min-height: 36px;
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

<style>
/*
 * Светлая тема: плавающая карточка POI.
 * Отдельный блок без scoped: в scoped-стилях цепочка :global(#app-root.app-light) .poi-modal.floating
 * после сборки превращается в «#app-root.app-light, body.app-light-theme { … }» без .poi-modal —
 * из‑за этого красился весь #app-root (в т.ч. фон у .map-sidebar через полупрозрачность).
 */
#app-root.app-light .poi-modal.floating,
body.app-light-theme .poi-modal.floating {
  background: #bb9970 !important;
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow:
    var(--shadow-modal),
    0 0 0 1px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

#app-root.app-light .poi-modal.floating .poi-modal-title,
body.app-light-theme .poi-modal.floating .poi-modal-title {
  color: #fff;
  border-bottom-color: rgba(255, 255, 255, 0.35);
}

#app-root.app-light .poi-modal.floating .modal-close,
body.app-light-theme .poi-modal.floating .modal-close {
  background: #bb9970;
  border: 1px solid rgba(255, 255, 255, 0.35);
  color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}

#app-root.app-light .poi-modal.floating .modal-close:hover,
body.app-light-theme .poi-modal.floating .modal-close:hover {
  background: #a88962;
  border-color: rgba(255, 255, 255, 0.5);
  color: #fff;
}

#app-root.app-light .poi-modal.floating .poi-fav-btn,
body.app-light-theme .poi-modal.floating .poi-fav-btn {
  background: rgba(0, 0, 0, 0.12);
  border-color: rgba(255, 255, 255, 0.35);
  color: #fff;
}

#app-root.app-light .poi-modal.floating .poi-fav-btn:hover,
body.app-light-theme .poi-modal.floating .poi-fav-btn:hover {
  border-color: rgba(255, 255, 255, 0.55);
  color: #fff;
}

#app-root.app-light .poi-modal.floating .poi-fav-btn.active,
body.app-light-theme .poi-modal.floating .poi-fav-btn.active {
  color: #ffe4e4;
  border-color: rgba(255, 200, 200, 0.55);
}

#app-root.app-light .poi-modal.floating .btn.btn-ghost,
body.app-light-theme .poi-modal.floating .btn.btn-ghost {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.35);
}

#app-root.app-light .poi-modal.floating .btn.btn-ghost:hover,
body.app-light-theme .poi-modal.floating .btn.btn-ghost:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.55);
  background: rgba(255, 255, 255, 0.12);
}

#app-root.app-light .poi-modal.floating .btn.btn-accent,
body.app-light-theme .poi-modal.floating .btn.btn-accent {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.35);
}

#app-root.app-light .poi-modal.floating .btn.btn-accent:hover,
body.app-light-theme .poi-modal.floating .btn.btn-accent:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.5);
}

#app-root.app-light .poi-modal.floating .btn.btn-danger,
body.app-light-theme .poi-modal.floating .btn.btn-danger {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.35);
}

#app-root.app-light .poi-modal.floating .ai-gen-btn,
body.app-light-theme .poi-modal.floating .ai-gen-btn {
  background: rgba(255, 255, 255, 0.28);
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  color: #fff !important;
}

#app-root.app-light .poi-modal.floating .ai-gen-btn:hover:not(:disabled),
body.app-light-theme .poi-modal.floating .ai-gen-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.4);
  border-color: rgba(255, 255, 255, 0.55) !important;
  color: #fff !important;
}

#app-root.app-light .poi-modal.floating .poi-tabs,
body.app-light-theme .poi-modal.floating .poi-tabs {
  background: rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.08);
}

#app-root.app-light .poi-modal.floating .poi-tab-btn,
body.app-light-theme .poi-modal.floating .poi-tab-btn {
  color: rgba(255, 255, 255, 0.88);
}

#app-root.app-light .poi-modal.floating .poi-tab-btn:hover,
body.app-light-theme .poi-modal.floating .poi-tab-btn:hover {
  color: #fff;
}

#app-root.app-light .poi-modal.floating .poi-tabs .poi-tab-btn.active,
body.app-light-theme .poi-modal.floating .poi-tabs .poi-tab-btn.active {
  color: var(--ink);
}

#app-root.app-light .poi-modal.floating .poi-desc-text,
body.app-light-theme .poi-modal.floating .poi-desc-text,
#app-root.app-light .poi-modal.floating .poi-description,
body.app-light-theme .poi-modal.floating .poi-description,
#app-root.app-light .poi-modal.floating .ai-content-text,
body.app-light-theme .poi-modal.floating .ai-content-text {
  color: rgba(255, 255, 255, 0.95);
}

#app-root.app-light .poi-modal.floating .poi-desc-fallback,
body.app-light-theme .poi-modal.floating .poi-desc-fallback {
  color: rgba(255, 255, 255, 0.88);
}

#app-root.app-light .poi-modal.floating .poi-desc-placeholder,
body.app-light-theme .poi-modal.floating .poi-desc-placeholder,
#app-root.app-light .poi-modal.floating .empty-state,
body.app-light-theme .poi-modal.floating .empty-state {
  color: rgba(255, 255, 255, 0.85);
}

#app-root.app-light .poi-modal.floating .poi-tts-voice-label,
body.app-light-theme .poi-modal.floating .poi-tts-voice-label,
#app-root.app-light .poi-modal.floating .poi-meta-list,
body.app-light-theme .poi-modal.floating .poi-meta-list,
#app-root.app-light .poi-modal.floating .poi-meta-list dt,
body.app-light-theme .poi-modal.floating .poi-meta-list dt {
  color: rgba(255, 255, 255, 0.88);
}

#app-root.app-light .poi-modal.floating .poi-type-badge,
body.app-light-theme .poi-modal.floating .poi-type-badge {
  color: #fff;
  background: rgba(0, 0, 0, 0.15);
  border-color: rgba(255, 255, 255, 0.35);
}

#app-root.app-light .poi-modal.floating .street-view-placeholder,
body.app-light-theme .poi-modal.floating .street-view-placeholder {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.35);
  color: rgba(255, 255, 255, 0.92);
}

#app-root.app-light .poi-modal.floating .poi-tts-voice-select,
body.app-light-theme .poi-modal.floating .poi-tts-voice-select {
  background: rgba(255, 255, 255, 0.22);
  border-color: rgba(255, 255, 255, 0.35);
  color: #fff;
}

#app-root.app-light .poi-modal.floating .ai-content-box,
body.app-light-theme .poi-modal.floating .ai-content-box {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.28);
}

@media (max-width: 768px) {
  #app-root.app-light .poi-modal.floating,
  body.app-light-theme .poi-modal.floating {
    background: #bb9970 !important;
    border: 1px solid rgba(255, 255, 255, 0.22);
    border-bottom: none;
    box-shadow:
      0 -12px 28px rgba(0, 0, 0, 0.1),
      inset 0 1px 0 rgba(255, 255, 255, 0.95);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
  }
}
</style>