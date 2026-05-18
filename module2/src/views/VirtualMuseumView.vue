<template>
  <!--
    VirtualMuseumView — отдельный маршрут /virtual-museum (ТЗ, раздел 4.1).
    Сейчас оборачивает существующий MapView, который содержит hero,
    рассказ о деревянной архитектуре и карту. Это даёт отдельные:
      - URL (/virtual-museum),
      - title из роутера,
      - SEO-точку входа,
    при этом не дублируя сложный код карты.
  -->
  <MapView />
</template>

<script setup>
import MapView from '@/views/MapView.vue'
</script>
