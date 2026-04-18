<template>
  <div class="cart-item-img">
    <img
      v-if="imageUrl"
      :src="imageUrl"
      :alt="item.name"
      class="cart-item-img__photo"
      loading="lazy"
      decoding="async"
    />
    <span v-else class="cart-item-img__fallback" aria-hidden="true">{{ item.emoji || '◻' }}</span>
  </div>
</template>

<script setup>
import { toRef } from 'vue'
import { useProductImage } from '@/composables/useProductImageCache.js'

const props = defineProps({
  item: { type: Object, required: true }
})

const imageUrl = useProductImage(toRef(props, 'item'))
</script>

<style scoped>
.cart-item-img {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: var(--ink);
  border: 1px solid var(--gray-700);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-item-img__photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cart-item-img__fallback {
  font-size: 1.45rem;
  line-height: 1;
}
</style>
