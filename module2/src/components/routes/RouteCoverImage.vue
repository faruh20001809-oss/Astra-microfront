<template>
  <img
    v-if="displayUrl"
    :src="displayUrl"
    :alt="alt"
    :loading="loading"
    :class="imgClass"
  />
</template>

<script setup>
import { toRef } from 'vue'
import { useRouteImage } from '@/composables/useRouteImageCache.js'

const props = defineProps({
  route: { type: Object, required: true },
  alt: { type: String, default: '' },
  loading: { type: String, default: 'lazy' },
  imgClass: { type: String, default: '' },
})

const displayUrl = useRouteImage(toRef(props, 'route'))
</script>
