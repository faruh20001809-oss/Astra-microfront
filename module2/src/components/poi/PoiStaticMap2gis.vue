<template>
  <figure class="poi-static-2gis" :class="{ 'poi-static-2gis--loading': loading && !failed }">
    <img
      v-if="mapUrl && !failed"
      :src="mapUrl"
      :alt="alt"
      class="poi-static-2gis__img"
      loading="lazy"
      decoding="async"
      @load="onLoad"
      @error="onError"
    />
    <div v-else-if="!coordsValid" class="poi-static-2gis__fallback" role="status">
      <span>Координаты не указаны</span>
    </div>
    <div v-else-if="failed || !mapUrl" class="poi-static-2gis__fallback" role="status">
      <span class="poi-static-2gis__pin" aria-hidden="true" />
      <span class="poi-static-2gis__hint">{{ errorHint }}</span>
      <a
        v-if="coordsValid"
        :href="webUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="poi-static-2gis__link"
      >
        Открыть в 2ГИС →
      </a>
    </div>
    <div v-else class="poi-static-2gis__skeleton" aria-hidden="true" />
  </figure>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getCachedDgisStaticMapUrl, isDgisStaticConfigured } from '@/api/dgisStaticMap.js'
import { get2gisWebUrl, poiHasValidCoords } from '@/utils/dgisLinks.js'

const props = defineProps({
  lat: { type: Number, default: 0 },
  lng: { type: Number, default: 0 },
  width: { type: Number, default: 640 },
  height: { type: Number, default: 280 },
  zoom: { type: Number, default: 16 },
  alt: { type: String, default: 'Расположение на карте 2ГИС' },
})

const loading = ref(true)
const failed = ref(false)

const coordsValid = computed(() => poiHasValidCoords(props.lat, props.lng))

const mapUrl = computed(() => {
  if (!coordsValid.value || !isDgisStaticConfigured()) return ''
  return getCachedDgisStaticMapUrl(props.lat, props.lng, {
    width: props.width,
    height: props.height,
    zoom: props.zoom,
  })
})

const webUrl = computed(() => get2gisWebUrl(props.lat, props.lng))

const errorHint = computed(() => {
  if (!isDgisStaticConfigured()) {
    return 'Ключ Static API не настроен (VITE_DGIS_STATIC_KEY)'
  }
  return 'Превью карты недоступно'
})

function onLoad() {
  loading.value = false
  failed.value = false
}

function onError() {
  loading.value = false
  failed.value = true
}

watch(
  () => [props.lat, props.lng, props.width, props.height, props.zoom, mapUrl.value],
  () => {
    loading.value = Boolean(mapUrl.value)
    failed.value = false
  },
  { immediate: true },
)
</script>

<style scoped>
.poi-static-2gis {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: inherit;
  margin: 0;
  overflow: hidden;
  background: #e8e8e8;
}

.poi-static-2gis__img {
  width: 100%;
  height: 100%;
  min-height: inherit;
  object-fit: cover;
  display: block;
}

.poi-static-2gis__skeleton {
  width: 100%;
  height: 100%;
  min-height: 120px;
  background: linear-gradient(90deg, #ececec 0%, #f5f5f5 50%, #ececec 100%);
  background-size: 200% 100%;
  animation: poi-static-shimmer 1.2s ease-in-out infinite;
}

@keyframes poi-static-shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

.poi-static-2gis__fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  height: 100%;
  min-height: 120px;
  padding: 1rem;
  text-align: center;
  background: linear-gradient(160deg, #ececec 0%, #d4d4d4 100%);
  color: #444;
  font-size: 0.82rem;
}

.poi-static-2gis__pin {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #1d1d1b;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.poi-static-2gis__link {
  color: #1d1d1b;
  font-weight: 700;
  text-decoration: underline;
}

.poi-static-2gis__link:hover {
  color: #000;
}
</style>
