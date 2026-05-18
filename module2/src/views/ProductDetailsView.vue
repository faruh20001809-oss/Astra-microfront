<template>
  <div class="page-wrapper product-details-page">
    <div class="container product-details__inner">

      <nav class="product-breadcrumbs" aria-label="Путь">
        <router-link to="/">Главная</router-link>
        <span aria-hidden="true">/</span>
        <router-link to="/shop">Магазин</router-link>
        <span aria-hidden="true">/</span>
        <span class="product-breadcrumbs__current">{{ product?.name || 'Товар' }}</span>
      </nav>

      <div v-if="loading" class="product-state" role="status" aria-live="polite">
        <div class="spinner" aria-hidden="true" />
        <span>Загружаем товар…</span>
      </div>

      <div v-else-if="error" class="product-state product-state--error">
        <p class="product-state__label text-mono">Не удалось загрузить товар</p>
        <p>{{ error }}</p>
        <div class="product-state__actions">
          <button type="button" class="btn btn-ghost btn-sm" @click="load">Попробовать снова</button>
          <router-link class="btn btn-ghost btn-sm" to="/shop">К магазину</router-link>
        </div>
      </div>

      <article v-else-if="product" class="product-card" aria-labelledby="product-title">
        <div class="product-card__media">
          <img v-if="product.image" :src="product.image" :alt="product.name" />
          <div v-else class="product-card__placeholder" aria-hidden="true">◻</div>
        </div>

        <div class="product-card__body">
          <p v-if="product.category" class="product-eyebrow text-mono">{{ product.category }}</p>
          <h1 id="product-title" class="product-title">{{ product.name }}</h1>
          <p v-if="product.description" class="product-desc">{{ product.description }}</p>

          <dl v-if="hasAttrs" class="product-attrs">
            <div v-if="product.material" class="product-attr">
              <dt>Материал</dt><dd>{{ product.material }}</dd>
            </div>
            <div v-if="product.sizes?.length" class="product-attr">
              <dt>Размеры</dt><dd>{{ product.sizes.join(' / ') }}</dd>
            </div>
            <div v-if="product.colors?.length" class="product-attr">
              <dt>Цвет</dt><dd>{{ product.colors.join(' / ') }}</dd>
            </div>
            <div v-if="typeof product.quantity === 'number'" class="product-attr">
              <dt>В наличии</dt><dd>{{ product.quantity }} шт.</dd>
            </div>
          </dl>

          <div v-if="product.sizes?.length || product.colors?.length" class="product-variant-row">
            <div v-if="product.sizes?.length" class="variant-section">
              <p id="size-label" class="variant-label text-mono">Размер</p>
              <div class="variant-buttons" role="radiogroup" aria-labelledby="size-label">
                <button
                  v-for="size in product.sizes"
                  :key="size"
                  type="button"
                  role="radio"
                  :aria-checked="selectedSize === size"
                  :class="['variant-btn', { active: selectedSize === size }]"
                  @click="selectedSize = size"
                >{{ size }}</button>
              </div>
            </div>
            <div v-if="product.colors?.length" class="variant-section">
              <p id="color-label" class="variant-label text-mono">Цвет</p>
              <div class="variant-buttons" role="radiogroup" aria-labelledby="color-label">
                <button
                  v-for="color in product.colors"
                  :key="color"
                  type="button"
                  role="radio"
                  :aria-checked="selectedColor === color"
                  :class="['variant-btn', { active: selectedColor === color }]"
                  @click="selectedColor = color"
                >{{ color }}</button>
              </div>
            </div>
          </div>

          <div class="product-qty-row">
            <span id="qty-label" class="variant-label text-mono">Количество</span>
            <div class="qty-control" role="group" aria-labelledby="qty-label">
              <button type="button" class="qty-btn" aria-label="Уменьшить количество" @click="qty = Math.max(1, qty - 1)">−</button>
              <span class="qty-val" aria-live="polite" aria-atomic="true">{{ qty }}</span>
              <button type="button" class="qty-btn" aria-label="Увеличить количество" @click="qty++">+</button>
            </div>
          </div>

          <div class="product-cta">
            <p class="product-price">{{ totalPrice }} ₽</p>
            <button type="button" class="btn btn-primary btn-lg product-cta__btn" @click="addToPreorder">
              Добавить в предзаказ →
            </button>
          </div>

          <p class="product-hint text-mono">
            Корзина формирует предзаказ. Сотрудник свяжется по Telegram или MAX, чтобы подтвердить заказ и согласовать доставку.
          </p>
        </div>
      </article>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { javaApi } from '@/api/backend'
import { useCartStore, useToastStore } from '@/store/index.js'

/**
 * ProductDetailsView — отдельная страница `/shop/:productId` (ТЗ, раздел 5.1).
 * Полное описание, варианты (sizes/colors), счетчик и CTA
 * «Добавить в предзаказ» — фактическая логика покупки выполняется
 * в корзине как PREORDER (ТЗ, раздел 5.2).
 */
const route = useRoute()
const cartStore = useCartStore()
const toastStore = useToastStore()

const product = ref(null)
const loading = ref(false)
const error = ref('')
const qty = ref(1)
const selectedSize = ref(null)
const selectedColor = ref(null)

const hasAttrs = computed(() => {
  if (!product.value) return false
  return Boolean(
    product.value.material ||
    product.value.sizes?.length ||
    product.value.colors?.length ||
    typeof product.value.quantity === 'number'
  )
})

const totalPrice = computed(() => {
  const price = Number(product.value?.price) || 0
  return (price * qty.value).toLocaleString('ru-RU')
})

async function load() {
  const idRaw = route.params.productId
  const id = Number(idRaw)
  if (!Number.isFinite(id) || id <= 0) {
    error.value = 'Некорректный идентификатор товара.'
    product.value = null
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await javaApi.products.getById(id)
    if (!data || (Array.isArray(data) && !data.length)) {
      throw new Error('Товар не найден')
    }
    product.value = data
    selectedSize.value = data.sizes?.[0] || null
    selectedColor.value = data.colors?.[0] || null
    document.title = `${data.name} — Магазин · Астрахань`
  } catch (e) {
    console.error('ProductDetailsView: load failed', e)
    error.value = e?.message || 'Не удалось загрузить товар. Попробуйте позже.'
    product.value = null
  } finally {
    loading.value = false
  }
}

function addToPreorder() {
  if (!product.value) return
  const variant = [selectedSize.value, selectedColor.value].filter(Boolean).join(' / ') || null
  for (let i = 0; i < qty.value; i++) {
    cartStore.addItem({
      ...product.value,
      variant,
    })
  }
  toastStore.push(
    `«${product.value.name}» × ${qty.value} добавлено в предзаказ`,
    'success',
    3500
  )
  cartStore.isOpen = true
}

onMounted(load)
watch(() => route.params.productId, (next, prev) => {
  if (next && next !== prev) load()
})
</script>

<style scoped>
.product-details-page {
  padding-bottom: var(--spacing-2xl);
  container-type: inline-size;
  container-name: product;
}

.product-details__inner {
  max-width: 1100px;
}

.product-breadcrumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem 0.35rem;
  align-items: center;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  letter-spacing: 0.04em;
  color: var(--gray-400);
  text-transform: uppercase;
  padding: clamp(0.5rem, 2cqi, 0.85rem) 0;
}

.product-breadcrumbs a {
  color: var(--gray-400);
  text-decoration: none;
  padding: 0.5rem 0.6rem;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  transition: color 0.15s ease, background 0.15s ease;
}

.product-breadcrumbs a:hover {
  color: var(--accent);
  background: rgba(212, 184, 150, 0.08);
}

.product-breadcrumbs a:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.product-breadcrumbs__current {
  color: var(--cream, #f5ead0);
  padding: 0.5rem 0.6rem;
}

@media (max-width: 480px) {
  .product-breadcrumbs a,
  .product-breadcrumbs__current {
    min-height: 44px;
  }
}

.product-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  padding: clamp(2rem, 6cqi, 4rem) var(--spacing-md);
  text-align: center;
  color: var(--gray-400);
}

.product-state--error {
  border: 1px solid rgba(184, 74, 60, 0.4);
  background: rgba(184, 74, 60, 0.08);
  border-radius: var(--radius-md);
}

.product-state__label {
  color: var(--danger);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.product-state__actions {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
  justify-content: center;
}

.product-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: clamp(1rem, 3cqi, 2rem);
  margin-top: var(--spacing-md);
}

@container product (min-width: 720px) {
  .product-card {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr);
    align-items: start;
  }
}

.product-card__media {
  border-radius: var(--radius-md);
  border: 1px solid rgba(212, 184, 150, 0.16);
  overflow: hidden;
  background: rgba(20, 16, 13, 0.65);
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.product-card__placeholder {
  font-size: 4rem;
  color: var(--gray-600);
}

.product-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  min-width: 0;
}

.product-eyebrow {
  margin: 0;
  color: var(--accent);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.7rem;
}

.product-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.65rem, 3.5cqi + 0.9rem, 2.4rem);
  line-height: 1.1;
  color: var(--cream, #f5ead0);
}

.product-desc {
  margin: 0;
  color: var(--gray-300, #d2c7b3);
  font-size: clamp(0.9rem, 0.3cqi + 0.85rem, 1.05rem);
  line-height: 1.65;
  max-width: 58ch;
}

.product-attrs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(45%, 11rem), 1fr));
  gap: var(--spacing-sm) var(--spacing-md);
  margin: 0;
  padding: var(--spacing-md);
  border: 1px solid rgba(212, 184, 150, 0.16);
  border-radius: var(--radius-sm);
  background: rgba(20, 16, 13, 0.45);
}

.product-attr {
  display: flex;
  flex-direction: column;
}

.product-attr dt {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gray-400);
  margin-bottom: 2px;
}

.product-attr dd {
  margin: 0;
  color: var(--paper, #f0e5cd);
  font-size: 0.95rem;
}

.product-variant-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
}

.variant-section {
  min-width: min(220px, 100%);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.variant-label {
  color: var(--gray-400);
  font-size: 0.65rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.variant-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.variant-btn {
  min-height: 44px;
  padding: 0.45rem 0.85rem;
  background: transparent;
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  color: var(--gray-400);
  font-family: var(--font-mono);
  font-size: 0.78rem;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.variant-btn:hover {
  border-color: var(--paper);
  color: var(--paper);
}

.variant-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.variant-btn.active {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(200, 169, 110, 0.1);
}

.product-qty-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.qty-control {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.qty-btn {
  width: 44px;
  height: 44px;
  background: var(--gray-800, #2a231e);
  border: 1px solid var(--gray-600);
  color: var(--paper);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: var(--radius-sm);
  font-size: 1.1rem;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.qty-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.qty-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.qty-val {
  font-family: var(--font-mono);
  min-width: 32px;
  text-align: center;
  font-size: 1.05rem;
}

.product-cta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid rgba(212, 184, 150, 0.14);
}

.product-price {
  margin: 0;
  font-family: var(--font-mono);
  font-size: clamp(1.4rem, 2cqi + 1rem, 1.85rem);
  color: var(--accent);
  letter-spacing: -0.02em;
}

.product-cta__btn {
  min-height: 52px;
  padding-inline: var(--spacing-xl);
}

.product-hint {
  margin: 0;
  font-size: 0.78rem;
  color: var(--gray-400);
  line-height: 1.55;
  max-width: 56ch;
}

@container product (max-width: 540px) {
  .product-cta {
    flex-direction: column;
    align-items: stretch;
  }
  .product-cta__btn {
    width: 100%;
  }
}

/* Light-тема: светлые фоны, чтобы текст оставался читаем. */
:root.app-light .product-card__media {
  background: #faf7f3;
  border-color: rgba(0, 0, 0, 0.1);
}

:root.app-light .product-attrs {
  background: #faf7f3;
  border-color: rgba(0, 0, 0, 0.08);
}

:root.app-light .qty-btn {
  background: #f5f5f5;
}

:root.app-light .product-state--error {
  background: rgba(192, 57, 43, 0.06);
  border-color: rgba(192, 57, 43, 0.35);
}

@media (prefers-reduced-motion: reduce) {
  .variant-btn,
  .qty-btn,
  .product-breadcrumbs a {
    transition: none;
  }
}
</style>
