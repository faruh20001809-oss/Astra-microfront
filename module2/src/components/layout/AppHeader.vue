<!-- src/components/AppHeader.vue -->
<template>
  <header class="app-header" :class="{ visible: headerVisible, hovered: headerHovered }">
    <div class="header-inner">
      <!-- Logo -->
      <router-link to="/" class="logo">
        <span class="logo-icon">⬡</span>
        <span class="logo-text">
          <span class="logo-title">Астрахань</span>
          <span class="logo-sub">Живая история</span>
        </span>
      </router-link>

      <!-- Nav -->
      <nav class="main-nav" :class="{ open: menuOpen }">
        <router-link
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            @click="menuOpen = false"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          {{ item.label }}
        </router-link>
      </nav>

      <!-- Right actions -->
      <div class="header-actions">
        <button class="cart-btn btn btn-ghost btn-sm" @click="cartStore.toggleCart()">
          <span>Корзина</span>
          <span v-if="cartStore.totalCount" class="cart-badge">{{ cartStore.totalCount }}</span>
        </button>
        <button class="burger" @click="menuOpen = !menuOpen" :aria-label="menuOpen ? 'Закрыть' : 'Меню'">
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>
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
  { to: '/suggest-poi', label: 'Предложить точку', icon: '💡' },
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
  z-index: 500;
  background: rgba(73, 62, 62, 0.92);
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

.app-header.hovered {
  transform: translateY(0);
  background: rgba(73, 62, 62, 0.98);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
  height: 100%;
  padding: 0 var(--spacing-xl);
  max-width: 1440px;
  margin: 0 auto;
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

.cart-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  background: var(--accent);
  color: var(--ink);
  font-size: 0.65rem;
  font-weight: 700;
  border-radius: 9px;
  padding: 0 4px;
}

/* Burger */
.burger {
  display: none;
  flex-direction: column;
  gap: 5px;
  padding: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
}

.burger span {
  display: block;
  width: 22px;
  height: 2px;
  background: var(--paper);
  transition: all var(--transition);
}

@media (max-width: 768px) {
  .header-inner { padding: 0 var(--spacing-md); gap: var(--spacing-md); }
  .burger { display: flex; }

  .main-nav {
    position: fixed;
    top: var(--nav-h);
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(10, 10, 10, 0.98);
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: var(--spacing-lg);
    transform: translateX(100%);
    transition: transform var(--transition-slow);
  }

  .main-nav.open { transform: translateX(0); }

  .nav-link {
    font-size: 1rem;
    padding: 0.75rem 1.5rem;
  }
}
</style>