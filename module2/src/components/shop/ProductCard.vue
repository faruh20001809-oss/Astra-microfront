<template>
  <div class="product-card card" @click="$emit('open', product)">
    <div class="product-image">
      <img v-if="product.image" :src="product.image" :alt="product.name" />
      <div v-else class="product-image-placeholder">{{ product.emoji || '◻' }}</div>
    </div>
    <div class="product-info">
      <span class="product-category">{{ product.category }}</span>
      <h4 class="product-name">{{ product.name }}</h4>
      <div class="product-footer">
        <span class="product-price">{{ product.price }} ₽</span>
        <button
          class="btn btn-sm btn-ghost cart-add"
          @click.stop="$emit('add-to-cart', product)"
          title="Добавить в корзину"
        >+ В корзину</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ product: Object })
defineEmits(['add-to-cart', 'open'])
</script>

<style scoped>
.product-card { cursor: pointer; }

.product-image {
  height: 200px;
  background: var(--ink);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-bottom: 1px solid var(--gray-800);
}

.product-image img { width: 100%; height: 100%; object-fit: cover; }

.product-image-placeholder {
  font-size: 3.5rem;
  color: var(--gray-600);
  transition: transform var(--transition-slow);
}

.product-card:hover .product-image-placeholder { transform: scale(1.08); }

.product-info {
  padding: var(--spacing-md);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.product-category {
  font-size: 0.65rem;
  font-family: var(--font-mono);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gray-400);
}

.product-name {
  font-size: 0.95rem;
  font-family: var(--font-display);
  line-height: 1.3;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-xs);
}

.product-price {
  font-family: var(--font-mono);
  font-size: 1rem;
  color: var(--accent);
}

.cart-add { font-size: 0.65rem; }
</style>
