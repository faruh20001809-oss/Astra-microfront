<template>
  <!--
    VirtualMuseumView — маршрут /virtual-museum (ТЗ §4.1):
    каталог всех опубликованных точек + интерактивная карта.
  -->
  <MapView show-poi-catalog />
</template>

<script setup>
import MapView from '@/views/MapView.vue'
</script>
