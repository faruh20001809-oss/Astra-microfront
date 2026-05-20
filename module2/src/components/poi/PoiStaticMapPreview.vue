<template>
  <img
    v-if="mapUrl && !failed"
    :src="mapUrl"
    :alt="alt"
    class="poi-static-map-preview"
    loading="lazy"
    decoding="async"
    @error="failed = true"
  />
  <div v-else class="poi-static-map-preview poi-static-map-preview--fallback" aria-hidden="true">
    <span class="poi-static-map-preview__pin" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getPoiStaticMapUrl } from '@/utils/poiStaticMap.js'

const props = defineProps({
  lat: { type: Number, default: 0 },
  lng: { type: Number, default: 0 },
  width: { type: Number, default: 640 },
  height: { type: Number, default: 280 },
  zoom: { type: Number, default: 16 },
  alt: { type: String, default: 'Расположение на карте' },
})

const failed = ref(false)

const mapUrl = computed(() =>
  getPoiStaticMapUrl(props.lat, props.lng, {
    width: props.width,
    height: props.height,
    zoom: props.zoom,
  }),
)

watch(
  () => [props.lat, props.lng, props.width, props.height, props.zoom],
  () => {
    failed.value = false
  },
)
</script>

<style scoped>
.poi-static-map-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  background: #e5e5e5;
}

.poi-static-map-preview--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #ececec 0%, #d4d4d4 100%);
}

.poi-static-map-preview__pin {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #1d1d1b;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}
</style>
