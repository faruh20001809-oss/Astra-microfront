<template>
  <section
    class="poi-interactive-2gis"
    :class="{
      'poi-interactive-2gis--large': large,
      'poi-interactive-2gis--readonly': readonly,
      'poi-interactive-2gis--compact': compact,
    }"
    :aria-label="ariaLabel"
  >
    <div v-if="loadError" class="poi-interactive-2gis__error" role="alert">
      <p>{{ loadError }}</p>
      <a :href="webUrl" target="_blank" rel="noopener noreferrer" class="poi-interactive-2gis__link">
        Открыть в 2ГИС →
      </a>
    </div>
    <template v-else>
      <div
        ref="mapEl"
        class="poi-interactive-2gis__canvas"
        :role="readonly ? 'img' : 'application'"
      />
      <div v-if="ready && readonly" class="poi-interactive-2gis__preview-label" aria-hidden="true">
        2ГИС
      </div>
      <div v-if="ready && !readonly" class="poi-interactive-2gis__toolbar">
        <button type="button" class="poi-interactive-2gis__btn" title="Приблизить" @click="zoomIn">+</button>
        <button type="button" class="poi-interactive-2gis__btn" title="Отдалить" @click="zoomOut">−</button>
        <button type="button" class="poi-interactive-2gis__btn" title="К объекту" @click="recenter">⌂</button>
        <a
          :href="webUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="poi-interactive-2gis__btn poi-interactive-2gis__btn--link"
        >
          2ГИС
        </a>
      </div>
      <p v-if="!coordsValid" class="poi-interactive-2gis__note">Координаты объекта не заданы</p>
    </template>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import {
  waitForMapgl,
  getDgisMapglConfig,
  flyToPoi,
  destroyPoiMap,
} from '@/composables/useDgisMapgl.js'
import { get2gisWebUrl, poiHasValidCoords } from '@/utils/dgisLinks.js'

const props = defineProps({
  lat: { type: Number, required: true },
  lng: { type: Number, required: true },
  zoom: { type: Number, default: 16 },
  /** Полноразмерный блок на странице точки */
  large: { type: Boolean, default: false },
  /** Компактное превью в карточке каталога */
  compact: { type: Boolean, default: false },
  /**
   * Только просмотр: MapGL рисуется, но pointer-events отключены —
   * клики проходят к родителю (выбор карточки в музее).
   */
  readonly: { type: Boolean, default: false },
  ariaLabel: { type: String, default: 'Интерактивная карта 2ГИС' },
})

const mapEl = ref(null)
const ready = ref(false)
const loadError = ref('')

let map = null
let marker = null

const coordsValid = computed(() => poiHasValidCoords(props.lat, props.lng))
const webUrl = computed(() => get2gisWebUrl(props.lat, props.lng))

function zoomIn() {
  if (map) map.setZoom(map.getZoom() + 1, { animate: true })
}

function zoomOut() {
  if (map) map.setZoom(Math.max(10, map.getZoom() - 1), { animate: true })
}

function recenter() {
  if (!map || !coordsValid.value) return
  flyToPoi(map, props.lat, props.lng, props.zoom)
  updateMarker()
}

function updateMarker() {
  if (!map || !window.mapgl?.HtmlMarker || !coordsValid.value) return
  if (marker?.destroy) {
    try {
      marker.destroy()
    } catch (_) {
      /* ignore */
    }
  }
  const lat = Number(props.lat)
  const lng = Number(props.lng)
  const markerClass = props.large && !props.readonly
    ? 'dgis-poi-marker dgis-poi-marker--large'
    : 'dgis-poi-marker'
  marker = new window.mapgl.HtmlMarker(map, {
    coordinates: [lng, lat],
    html: `<div class="${markerClass}" aria-hidden="true"></div>`,
    anchor: [0.5, 1],
  })
}

async function initMap() {
  loadError.value = ''
  ready.value = false
  if (!mapEl.value || !coordsValid.value) return

  destroyPoiMap(map, marker)
  map = null
  marker = null

  try {
    const mapgl = await waitForMapgl()
    const { key, style } = getDgisMapglConfig()
    const lat = Number(props.lat)
    const lng = Number(props.lng)

    map = new mapgl.Map(mapEl.value, {
      key,
      style,
      center: [lng, lat],
      zoom: props.zoom,
      zoomControl: false,
      fullscreenControl: false,
    })

    updateMarker()
    ready.value = true

    requestAnimationFrame(() => {
      map?.resize?.()
    })
  } catch (e) {
    loadError.value = e?.message || 'Не удалось загрузить карту 2ГИС'
  }
}

onMounted(() => {
  initMap()
})

watch(
  () => [props.lat, props.lng, props.zoom],
  () => {
    if (!map) {
      initMap()
      return
    }
    if (!coordsValid.value) return
    flyToPoi(map, props.lat, props.lng, props.zoom)
    updateMarker()
  },
)

onUnmounted(() => {
  destroyPoiMap(map, marker)
  map = null
  marker = null
})
</script>

<style scoped>
.poi-interactive-2gis {
  position: relative;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
  background: #e8e8e8;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.06);
}

.poi-interactive-2gis--large .poi-interactive-2gis__canvas {
  min-height: min(52vw, 420px);
}

.poi-interactive-2gis__canvas {
  width: 100%;
  min-height: 200px;
  aspect-ratio: 4 / 3;
}

.poi-interactive-2gis--compact .poi-interactive-2gis__canvas {
  min-height: 120px;
  aspect-ratio: 2.3 / 1;
}

.poi-interactive-2gis--readonly {
  pointer-events: none;
  user-select: none;
}

.poi-interactive-2gis--readonly .poi-interactive-2gis__canvas,
.poi-interactive-2gis--readonly :deep(.mapgl-canvas-container),
.poi-interactive-2gis--readonly :deep(canvas) {
  pointer-events: none !important;
  touch-action: none !important;
}

.poi-interactive-2gis__preview-label {
  position: absolute;
  left: 0.5rem;
  bottom: 0.5rem;
  z-index: 2;
  padding: 0.15rem 0.45rem;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.88);
  color: #1d1d1b;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  pointer-events: none;
}

.poi-interactive-2gis__toolbar {
  position: absolute;
  top: 0.65rem;
  right: 0.65rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  z-index: 2;
}

.poi-interactive-2gis__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 2.1rem;
  min-height: 2.1rem;
  padding: 0 0.5rem;
  border: none;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.94);
  color: #1d1d1b;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  text-decoration: none;
}

.poi-interactive-2gis__btn:hover {
  background: #fff;
}

.poi-interactive-2gis__btn--link {
  font-size: 0.72rem;
  letter-spacing: 0.02em;
}

.poi-interactive-2gis__error {
  padding: 1.5rem;
  text-align: center;
  font-size: 0.9rem;
  color: #444;
}

.poi-interactive-2gis__link {
  display: inline-block;
  margin-top: 0.5rem;
  font-weight: 700;
  color: #1d1d1b;
}

.poi-interactive-2gis__note {
  position: absolute;
  bottom: 0.5rem;
  left: 0.75rem;
  margin: 0;
  font-size: 0.78rem;
  color: #5f5f5f;
  background: rgba(255, 255, 255, 0.85);
  padding: 0.2rem 0.5rem;
  border-radius: 2px;
}

:global(.dgis-poi-marker) {
  width: 14px;
  height: 14px;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  background: #e74c3c;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

:global(.dgis-poi-marker--large) {
  width: 18px;
  height: 18px;
}
</style>
