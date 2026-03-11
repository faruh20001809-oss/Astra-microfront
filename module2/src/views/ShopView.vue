<template>
  <div class="page-wrapper shop-page">
    <div class="container">

      <!-- Hero -->
      <section class="shop-hero">
        <p class="text-mono" style="color:var(--accent)">◻ Магазин</p>
        <h1>Мерч<br />Астрахани</h1>
        <p class="shop-subtitle">Товары с историческими мотивами города — футболки, кружки, открытки и сувениры.</p>
      </section>

      <!-- Filters -->
      <div class="shop-toolbar">
        <div class="filter-group">
          <button
            v-for="cat in allCategories"
            :key="cat"
            :class="['filter-chip', { active: activeCategory === cat }]"
            @click="activeCategory = activeCategory === cat ? null : cat"
          >{{ cat }}</button>
        </div>
        <div class="sort-group">
          <span class="text-mono" style="color:var(--gray-400)">Сортировка:</span>
          <select v-model="sortBy" class="form-select sort-select">
            <option value="default">По умолчанию</option>
            <option value="price-asc">Цена ↑</option>
            <option value="price-desc">Цена ↓</option>
            <option value="name">По названию</option>
          </select>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="isLoading" class="shop-loading">
        <div class="spinner" />
        <span>Загрузка товаров…</span>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="shop-error">
        <p class="text-mono" style="color:var(--accent)">⚠ Внимание</p>
        <p>{{ error }}</p>
        <button class="btn btn-ghost btn-sm" @click="loadProducts">🔄 Попробовать снова</button>
      </div>

      <!-- Empty state (фильтр не дал результатов) -->
      <div v-else-if="!sortedProducts.length" class="shop-empty">
        <p class="shop-empty-title">В этой категории пока ничего нет</p>
        <p class="shop-empty-hint">Попробуйте другую категорию или сбросьте фильтр.</p>
        <button class="btn btn-ghost btn-sm" @click="activeCategory = null">Сбросить фильтр</button>
      </div>

      <!-- Product grid -->
      <transition-group v-else name="fade" tag="div" class="product-grid">
        <ProductCard
          v-for="p in sortedProducts"
          :key="p.id"
          :product="p"
          @add-to-cart="addToCart(p)"
          @open="openProduct(p)"
        />
      </transition-group>

    </div>

    <!-- Product modal -->
    <transition name="fade">
      <div v-if="selectedProduct" class="modal-backdrop" @click.self="selectedProduct = null">
        <div class="modal-box product-modal">
          <button class="modal-close-btn" @click="selectedProduct = null">✕</button>

          <div class="product-modal-body">
            <div class="product-modal-image">
              <img v-if="modalImageUrl" :src="modalImageUrl" :alt="selectedProduct.name" />
              <div v-else class="product-img-placeholder">{{ selectedProduct.emoji || '◻' }}</div>
            </div>
            <div class="product-modal-info">
              <span class="tag">{{ selectedProduct.category }}</span>
              <h2 class="product-modal-name">{{ selectedProduct.name }}</h2>
              <p class="product-modal-price">{{ selectedProduct.price }} ₽</p>
              <p class="product-modal-desc">{{ selectedProduct.description }}</p>

              <div v-if="selectedProduct.variants?.length" class="variants-section">
                <p class="text-mono" style="color:var(--gray-400);margin-bottom:0.5rem">Вариант</p>
                <div class="variants-row">
                  <button
                    v-for="v in selectedProduct.variants"
                    :key="v"
                    :class="['variant-btn', { active: selectedVariant === v }]"
                    @click="selectedVariant = v"
                  >{{ v }}</button>
                </div>
              </div>

              <div class="qty-row">
                <span class="text-mono" style="color:var(--gray-400)">Кол-во:</span>
                <div class="qty-control">
                  <button class="qty-btn" @click="qty = Math.max(1, qty - 1)">−</button>
                  <span class="qty-val">{{ qty }}</span>
                  <button class="qty-btn" @click="qty++">+</button>
                </div>
              </div>

              <button class="btn btn-primary btn-lg" @click="addToCartModal">
                В корзину — {{ selectedProduct.price * qty }} ₽
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import ProductCard from '@/components/shop/ProductCard.vue'
import { useCartStore, useToastStore } from '@/store/index.js'
import { javaApi, apiService } from '@/api/backend'
import { useProductImage } from '@/composables/useProductImageCache.js'

const cartStore = useCartStore()
const toastStore = useToastStore()

const products = ref([])
const isLoading = ref(false)
const activeCategory = ref(null)
const sortBy = ref('default')
const selectedProduct = ref(null)
const selectedVariant = ref(null)
const qty = ref(1)
const error = ref(null)
const modalImageUrl = useProductImage(selectedProduct)

const allCategories = computed(() => {
  if (!Array.isArray(products.value)) return []
  return [...new Set(products.value.map(p => p.category))]
})

const filteredProducts = computed(() => {
  if (!activeCategory.value) return products.value || []
  return (products.value || []).filter(p => p.category === activeCategory.value)
})

const sortedProducts = computed(() => {
  const list = [...(filteredProducts.value || [])]
  if (sortBy.value === 'price-asc') list.sort((a, b) => a.price - b.price)
  else if (sortBy.value === 'price-desc') list.sort((a, b) => b.price - a.price)
  else if (sortBy.value === 'name') list.sort((a, b) => a.name.localeCompare(b.name))
  return list
})

async function loadProducts() {
  isLoading.value = true
  error.value = null
  try {
    products.value = await javaApi.products.getList()
  } catch (err) {
    console.warn('Java API недоступен, используем mock-данные:', err)
    error.value = 'Не удалось загрузить товары. Показываем демо-данные.'
    products.value = getMockProducts()
  } finally {
    isLoading.value = false
  }
}

onMounted(loadProducts)

function openProduct(p) {
  selectedProduct.value = p
  selectedVariant.value = p.variants?.[0] || null
  qty.value = 1
}

function addToCart(p) {
  cartStore.addItem(p)
  toastStore.push(`«${p.name}» добавлен в корзину`)
}

function addToCartModal() {
  for (let i = 0; i < qty.value; i++) {
    cartStore.addItem({ ...selectedProduct.value, variant: selectedVariant.value })
  }
  toastStore.push(`«${selectedProduct.value.name}» × ${qty.value} добавлено в корзину`)
  cartStore.isOpen = true
  selectedProduct.value = null
}

function getMockProducts() {
  return [
    { id: 1, name: 'Футболка «Кремль»', category: 'Одежда', price: 1490, emoji: '👕', description: 'Хлопковая футболка с силуэтом Астраханского Кремля, печать по технологии DTF. Доступны размеры S–3XL.', variants: ['S', 'M', 'L', 'XL', '2XL'], image: null },
    { id: 2, name: 'Кружка «Лотос дельты»', category: 'Посуда', price: 890, emoji: '☕', description: 'Керамическая кружка 350 мл с авторской иллюстрацией лотосовых полей дельты Волги.', variants: null, image: null },
    { id: 3, name: 'Открытка «Зодчество»', category: 'Открытки', price: 120, emoji: '✉', description: 'Набор из 5 открыток с историческими фасадами астраханского зодчества XIX–XX веков. Мелованная бумага 350 г.', variants: null, image: null },
    { id: 4, name: 'Магнит «Кремль» (3D)', category: 'Сувениры', price: 290, emoji: '🧲', description: 'Объёмный магнит-копия Астраханского Кремля из полирезины. Размер 8×5 см.', variants: null, image: null },
    { id: 5, name: 'Толстовка «Астрахань 1556»', category: 'Одежда', price: 2890, emoji: '🧥', description: 'Оверсайз-толстовка с датой основания города. 100% хлопок, плотность 360 г/м².', variants: ['S', 'M', 'L', 'XL', '2XL'], image: null },
    { id: 6, name: 'Блокнот «История края»', category: 'Канцелярия', price: 450, emoji: '📒', description: 'Блокнот А5 в твёрдой обложке с репродукциями исторических карт Астраханской губернии. 192 стр., в точку.', variants: null, image: null },
    { id: 7, name: 'Постер «Кремль» А2', category: 'Плакаты', price: 690, emoji: '🖼', description: 'Высококачественная печать на матовой бумаге 200 г. Архитектурная иллюстрация в монохромном стиле.', variants: null, image: null },
    { id: 8, name: 'Бейсболка «Волга»', category: 'Одежда', price: 990, emoji: '🧢', description: 'Шестипанельная бейсболка с вышивкой. Регулируемый ремешок. Один размер.', variants: ['Чёрная', 'Кремовая'], image: null }
  ]
}
</script>

<style scoped>
.shop-page { padding-bottom: var(--spacing-2xl); }

.shop-hero {
  padding: var(--spacing-2xl) 0 var(--spacing-xl);
  max-width: 480px;
}

.shop-subtitle {
  margin-top: var(--spacing-md);
  color: var(--gray-400);
  line-height: 1.8;
  font-size: 0.9rem;
}

/* Toolbar */
.shop-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) 0;
  border-top: 1px solid var(--gray-800);
  border-bottom: 1px solid var(--gray-800);
  margin-bottom: var(--spacing-xl);
}

.filter-group { display: flex; flex-wrap: wrap; gap: var(--spacing-xs); }

.filter-chip {
  padding: 0.35rem 0.8rem;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.06em;
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--gray-400);
  cursor: pointer;
  transition: all var(--transition);
}

.filter-chip:hover { border-color: var(--gray-400); color: var(--paper); }
.filter-chip.active { border-color: var(--accent); color: var(--accent); background: rgba(200,169,110,0.08); }

.sort-group {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.sort-select {
  width: auto;
  padding: 0.35rem 0.75rem;
  font-size: 0.75rem;
}

/* Grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: var(--spacing-lg);
}

.shop-loading {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  color: var(--gray-400);
  padding: var(--spacing-xl) 0;
}

.shop-error {
  padding: var(--spacing-xl) 0;
  text-align: center;
  color: var(--gray-400);
}
.shop-error p { margin: var(--spacing-sm) 0; }
.shop-error .btn { margin-top: var(--spacing-md); }

.shop-empty {
  text-align: center;
  padding: var(--spacing-2xl) var(--spacing-lg);
  color: var(--gray-400);
}
.shop-empty-title { font-family: var(--font-display); font-size: 1.1rem; color: var(--paper); margin: 0 0 var(--spacing-sm); }
.shop-empty-hint { font-size: 0.85rem; margin: 0 0 var(--spacing-md); line-height: 1.5; }
.shop-empty .btn { margin-top: var(--spacing-sm); }

/* Product modal */
.product-modal { max-width: 760px; position: relative; }

.modal-close-btn {
  position: absolute;
  top: var(--spacing-md); right: var(--spacing-md);
  background: transparent; border: 1px solid var(--gray-600);
  color: var(--paper); width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; border-radius: var(--radius-sm);
  transition: all var(--transition);
}
.modal-close-btn:hover { border-color: var(--paper); }

.product-modal-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-xl);
}

.product-modal-image {
  aspect-ratio: 1;
  background: var(--ink);
  border: 1px solid var(--gray-800);
  border-radius: var(--radius-sm);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-modal-image img { width: 100%; height: 100%; object-fit: cover; }

.product-img-placeholder {
  font-size: 4rem;
  color: var(--gray-600);
}

.product-modal-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.product-modal-name { font-size: 1.5rem; }
.product-modal-price { font-size: 1.75rem; font-family: var(--font-mono); color: var(--accent); }
.product-modal-desc { color: var(--gray-400); line-height: 1.7; font-size: 0.85rem; }

/* Variants */
.variants-row { display: flex; flex-wrap: wrap; gap: var(--spacing-xs); }

.variant-btn {
  padding: 0.3rem 0.75rem;
  background: transparent;
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  color: var(--gray-400);
  font-family: var(--font-mono);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all var(--transition);
}

.variant-btn:hover { border-color: var(--paper); color: var(--paper); }
.variant-btn.active { border-color: var(--accent); color: var(--accent); }

/* Qty */
.qty-row { display: flex; align-items: center; gap: var(--spacing-md); }

.qty-control { display: flex; align-items: center; gap: var(--spacing-sm); }

.qty-btn {
  width: 30px; height: 30px;
  background: var(--gray-800);
  border: 1px solid var(--gray-600);
  color: var(--paper);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; border-radius: var(--radius-sm);
  font-size: 1rem; transition: all var(--transition);
}

.qty-btn:hover { border-color: var(--accent); color: var(--accent); }
.qty-val { font-family: var(--font-mono); min-width: 24px; text-align: center; }

@media (max-width: 768px) {
  .product-grid { grid-template-columns: repeat(2, 1fr); gap: var(--spacing-md); }
  .product-modal-body { grid-template-columns: 1fr; }
  .shop-toolbar { flex-direction: column; align-items: flex-start; gap: var(--spacing-sm); }
  .product-modal { max-width: 100%; margin: var(--spacing-md); }
  .filter-group .filter-chip { min-height: 44px; padding: 0.5rem 1rem; }
  .product-card { padding: var(--spacing-md); }
}

@media (max-width: 480px) {
  .product-grid { grid-template-columns: 1fr; }
  .shop-hero { padding: var(--spacing-lg) 0 var(--spacing-md); }
  .product-modal { margin: var(--spacing-sm); padding: var(--spacing-md); }
  .product-modal-image { min-height: 200px; }
  .qty-control button { min-width: 44px; min-height: 44px; }
}
</style>
