<!-- Обёртка: используйте PoiStaticMap2gis напрямую в новом коде -->
<template>
  <PoiStaticMap2gis
    :lat="lat"
    :lng="lng"
    :width="width"
    :height="height"
    :zoom="zoom"
    :alt="alt"
  />
</template>

<script setup>
import PoiStaticMap2gis from '@/components/poi/PoiStaticMap2gis.vue'

defineProps({
  lat: { type: Number, default: 0 },
  lng: { type: Number, default: 0 },
  width: { type: Number, default: 640 },
  height: { type: Number, default: 280 },
  zoom: { type: Number, default: 16 },
  alt: { type: String, default: 'Расположение на карте' },
})
</script>
