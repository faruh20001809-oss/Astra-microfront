<!-- src/components/AppHeader.vue -->
<template>
  <header class="app-header" :class="{ visible: headerVisible, hovered: headerHovered }">
    <div class="header-inner">
      <router-link to="/" class="logo">
        <span class="logo-icon">⬡</span>
        <span class="logo-text">
          <span class="logo-title">Астрахань</span>
          <span class="logo-sub">Живая история</span>
        </span>
      </router-link>

      <nav class="main-nav">
        <template v-for="item in navItems" :key="item.to">
          <a
            v-if="item.external"
            :href="item.to"
            class="nav-link"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            {{ item.label }}
          </a>
          <router-link
            v-else
            :to="item.to"
            class="nav-link"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            {{ item.label }}
          </router-link>
        </template>
      </nav>

      <div class="header-actions">
        <!-- Кнопка корзины показывается только когда в корзине есть товары (на мобилках иначе непонятный пустой квадрат) -->
        <Button
            v-if="cartStore.totalCount > 0"
            class="p-button-text p-button-secondary cart-btn"
            @click="cartStore.toggleCart()"
            aria-label="Корзина"
        >
          <span class="cart-btn-text">Корзина</span>
          <Badge :value="cartStore.totalCount" class="cart-badge-pv" />
        </Button>
        <Button
            icon="pi pi-bars"
            class="p-button-text p-button-secondary burger-btn"
            @click="menuOpen = true"
            aria-label="Меню"
        />
      </div>
    </div>

    <!-- Мобильное меню: PrimeVue Drawer -->
    <Drawer
        v-model:visible="menuOpen"
        position="right"
        :modal="true"
        :dismissable="true"
        :showCloseIcon="true"
        class="nav-drawer"
        @hide="menuOpen = false"
    >
      <template #header>
        <span class="nav-drawer-title">Меню</span>
      </template>
      <nav class="nav-drawer-list">
        <template v-for="item in navItems" :key="item.to">
          <a
            v-if="item.external"
            :href="item.to"
            class="nav-drawer-link"
            @click="menuOpen = false"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            {{ item.label }}
          </a>
          <router-link
            v-else
            :to="item.to"
            class="nav-drawer-link"
            :class="{ 'router-link-active': $route.path === item.to }"
            @click="menuOpen = false"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            {{ item.label }}
          </router-link>
        </template>
      </nav>
    </Drawer>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useCartStore } from '@/store/index.js'

const cartStore = useCartStore()
const menuOpen = ref(false)
const headerVisible = ref(true)
const headerHovered = ref(false)
let lastScrollY = 0
let hoverTimeout = null

const navItems = [
  { to: '/', label: 'Карта', icon: '◎' },
  { to: '/routes', label: 'Маршруты', icon: '⊹' },
  { to: '/shop', label: 'Магазин', icon: '◻' },
  { to: '/contact', label: 'Контакты', icon: '◇' }
]

const handleScroll = () => {
  const currentScrollY = window.scrollY
  const topZone = currentScrollY < 100

  if (topZone) {
    headerVisible.value = true
  } else {
    if (currentScrollY > lastScrollY) {
      headerVisible.value = false
    } else {
      headerVisible.value = true
    }
  }
  lastScrollY = currentScrollY
}

const handleMouseMove = (e) => {
  if (e.clientY < 80) {
    headerHovered.value = true
    if (hoverTimeout) clearTimeout(hoverTimeout)
  } else {
    hoverTimeout = setTimeout(() => {
      headerHovered.value = false
    }, 500)
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('mousemove', handleMouseMove)
  if (hoverTimeout) clearTimeout(hoverTimeout)
})
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--nav-h);
  padding-top: env(safe-area-inset-top, 0);
  padding-left: env(safe-area-inset-left, 0);
  padding-right: env(safe-area-inset-right, 0);
  z-index: 1000;
  background: var(--header-bg, rgba(73, 62, 62, 0.98));
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--gray-800);
  transform: translateY(0);
  transition: transform 0.3s ease, background 0.3s ease;
}

.app-header.visible {
  transform: translateY(0);
}

.app-header:not(.visible):not(.hovered) {
  transform: translateY(-100%);
}

/* На мобильных хедер всегда виден — удобнее навигация */
@media (max-width: 768px) {
  .app-header:not(.visible):not(.hovered) {
    transform: translateY(0);
  }
}

.app-header.hovered {
  transform: translateY(0);
  background: var(--header-bg, rgba(73, 62, 62, 0.98));
}

.header-inner {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  height: 100%;
  padding: 0 var(--spacing-xl);
  max-width: 1440px;
  margin: 0 auto;
  min-width: 0;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-icon {
  font-size: 1.5rem;
  color: var(--accent);
  line-height: 1;
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.logo-title {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: 700;
  line-height: 1.1;
  color: var(--paper);
}

.logo-sub {
  font-family: var(--font-mono);
  font-size: 0.6rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--gray-400);
}

/* Nav */
.main-nav {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-left: auto;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4rem 0.75rem;
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gray-400);
  border-radius: var(--radius-sm);
  transition: all var(--transition);
  text-decoration: none;
}

.nav-link:hover { color: var(--paper); background: var(--gray-800); }
.nav-link.router-link-active { color: var(--paper); }
.nav-link.router-link-exact-active {
  color: var(--accent);
  background: rgba(200, 169, 110, 0.08);
}

.nav-icon { font-size: 0.85rem; }

/* Actions */
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.cart-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  position: relative;
}

.cart-btn-text { margin-right: 0.25rem; }
.cart-badge-pv { margin-left: 0.25rem; }

.burger-btn {
  display: none;
}

/* Drawer меню */
.nav-drawer-title { font-family: var(--font-display); font-size: 1.125rem; }
.nav-drawer-list { display: flex; flex-direction: column; gap: 0; }
.nav-drawer-link {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.875rem 1.25rem;
  font-family: var(--font-mono); font-size: 1rem;
  color: var(--gray-400); text-decoration: none;
  border-bottom: 1px solid var(--gray-800);
  transition: background var(--transition), color var(--transition);
}
.nav-drawer-link:hover { background: var(--gray-800); color: var(--paper); }
.nav-drawer-link.router-link-active { color: var(--accent); background: rgba(200,169,110,0.08); }
.nav-drawer-link .nav-icon { font-size: 1.1rem; }

@media (max-width: 768px) {
  .header-inner {
    padding: 0 var(--spacing-sm);
    gap: var(--spacing-sm);
    justify-content: space-between;
  }
  .logo { min-width: 0; }
  .logo-sub { display: none; }
  .logo-title { font-size: 1rem; }
  .main-nav { display: none; }
  .burger-btn {
    display: inline-flex;
    min-width: 44px;
    min-height: 44px;
    flex-shrink: 0;
  }
  .header-actions { margin-left: auto; }
  .cart-btn {
    min-height: 44px;
    min-width: 44px;
    padding: 0.5rem;
    justify-content: center;
    flex-shrink: 0;
  }
  .cart-btn-text { display: none; }
  .cart-badge-pv { margin-left: 0; }
}
</style>