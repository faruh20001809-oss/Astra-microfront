<template>
  <div
    ref="mapEl"
    class="poi-mini-map"
    role="img"
    :aria-label="ariaLabel"
  />
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  lat: { type: Number, required: true },
  lng: { type: Number, required: true },
  ariaLabel: { type: String, default: 'Мини-карта расположения объекта' },
})

import { getDgisMapKey, getDgisMapStyle } from '@/api/dgisConfig.js'

const dgisMap = { key: getDgisMapKey(), style: getDgisMapStyle() }
const mapEl = ref(null)
let map = null
let marker = null
let checkTimer = null

function destroyMap() {
  if (marker?.destroy) {
    try { marker.destroy() } catch (_) { /* ignore */ }
  }
  marker = null
  if (map?.destroy) {
    try { map.destroy() } catch (_) { /* ignore */ }
  }
  map = null
}

function renderMarker() {
  if (!map || !window.mapgl?.HtmlMarker) return
  if (marker?.destroy) {
    try { marker.destroy() } catch (_) { /* ignore */ }
  }
  const lat = Number(props.lat)
  const lng = Number(props.lng)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return
  marker = new window.mapgl.HtmlMarker(map, {
    coordinates: [lng, lat],
    html: '<div class="poi-mini-map-marker" aria-hidden="true"></div>',
    anchor: 'center',
  })
}

function tryInit() {
  if (!mapEl.value || !window.mapgl) return false
  if (mapEl.value.clientWidth === 0) return false
  const lat = Number(props.lat)
  const lng = Number(props.lng)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return false

  destroyMap()
  map = new window.mapgl.Map(mapEl.value, {
    key: dgisMap.key,
    style: dgisMap.style,
    center: [lng, lat],
    zoom: 16,
    zoomControl: false,
    fullscreenControl: false,
  })
  renderMarker()
  return true
}

onMounted(() => {
  if (tryInit()) return
  checkTimer = setInterval(() => {
    if (tryInit()) {
      clearInterval(checkTimer)
      checkTimer = null
    }
  }, 200)
})

watch(
  () => [props.lat, props.lng],
  () => {
    if (!map) {
      tryInit()
      return
    }
    const lat = Number(props.lat)
    const lng = Number(props.lng)
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return
    map.setCenter([lng, lat], { animate: false })
    map.setZoom(16, { animate: false })
    renderMarker()
  },
)

onUnmounted(() => {
  if (checkTimer) clearInterval(checkTimer)
  destroyMap()
})
</script>

<style scoped>
.poi-mini-map {
  width: 100%;
  aspect-ratio: 1 / 1;
  max-width: 200px;
  min-height: 160px;
  border-radius: 4px;
  overflow: hidden;
  background: #e8e8e8;
}

:global(.poi-mini-map-marker) {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #1d1d1b;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.35);
}
</style>
