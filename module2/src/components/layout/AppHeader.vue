<!-- src/components/layout/AppHeader.vue -->
<template>
  <header class="app-header" :class="{ visible: headerVisible, hovered: headerHovered }">
    <div class="header-inner">
      <router-link
        to="/"
        class="logo"
        aria-label="Астрахань. Живая история — на главную"
      >
        <NavIcon name="logo" :size="22" class="logo-mark" />
        <span class="logo-text">
          <span class="logo-title">Астрахань</span>
          <span class="logo-sub">Живая история</span>
        </span>
      </router-link>

      <nav class="main-nav" aria-label="Основное меню">
        <template v-for="item in navItems" :key="item.to">
          <a
            v-if="item.external"
            :href="item.to"
            class="nav-link"
            :class="{ 'nav-link-current': isNavCurrent(item.to) }"
          >
            <NavIcon :name="item.icon" :size="18" class="nav-icon-el" />
            {{ item.label }}
          </a>
          <router-link
            v-else
            :to="item.to"
            class="nav-link"
            :class="{ 'nav-link-current': isNavCurrent(item.to) }"
            :aria-current="isNavCurrent(item.to) ? 'page' : undefined"
          >
            <NavIcon :name="item.icon" :size="18" class="nav-icon-el" />
            {{ item.label }}
          </router-link>
        </template>
      </nav>

      <div class="header-actions">
        <router-link
          to="/profile"
          class="profile-nav-btn"
          :class="{ 'router-link-active': route.path.startsWith('/profile') }"
          aria-label="Личный кабинет"
          title="Личный кабинет"
        >
          <NavIcon name="profile" :size="20" />
        </router-link>
        <Button
            :icon="uiStore.isDark ? 'pi pi-sun' : 'pi pi-moon'"
            class="p-button-text p-button-secondary theme-btn"
            @click="uiStore.toggleTheme()"
            :aria-label="uiStore.isDark ? 'Переключить на светлую тему' : 'Переключить на тёмную тему'"
            :title="uiStore.isDark ? 'Светлая тема' : 'Тёмная тема'"
        />
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
            aria-label="Открыть меню"
            aria-haspopup="dialog"
            :aria-expanded="menuOpen"
        />
      </div>
    </div>

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
        <h2 class="nav-drawer-title" id="nav-drawer-heading">Меню</h2>
      </template>
      <div class="nav-drawer-theme">
        <Button
          :label="uiStore.isDark ? 'Светлая тема' : 'Тёмная тема'"
          :icon="uiStore.isDark ? 'pi pi-sun' : 'pi pi-moon'"
          class="p-button-text p-button-secondary"
          @click="uiStore.toggleTheme(); closeDrawer()"
        />
      </div>
      <nav class="nav-drawer-list" aria-labelledby="nav-drawer-heading">
        <template v-for="item in navItems" :key="item.to">
          <a
            v-if="item.external"
            :href="item.to"
            class="nav-drawer-link"
            @click="closeDrawer"
          >
            <NavIcon :name="item.icon" :size="22" class="nav-icon-el" />
            {{ item.label }}
          </a>
          <router-link
            v-else
            :to="item.to"
            class="nav-drawer-link"
            :class="{ 'router-link-active': isNavCurrent(item.to) }"
            :aria-current="isNavCurrent(item.to) ? 'page' : undefined"
            @click="closeDrawer"
          >
            <NavIcon :name="item.icon" :size="22" class="nav-icon-el" />
            {{ item.label }}
          </router-link>
        </template>
        <router-link
          to="/profile"
          class="nav-drawer-link nav-drawer-link--profile"
          :class="{ 'router-link-active': route.path.startsWith('/profile') }"
          :aria-current="route.path.startsWith('/profile') ? 'page' : undefined"
          @click="closeDrawer"
        >
          <NavIcon name="profile" :size="22" class="nav-icon-el" />
          Личный кабинет
        </router-link>
      </nav>
    </Drawer>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useCartStore, useUiStore } from '@/store/index.js'
import NavIcon from '@/components/common/NavIcon.vue'

const route = useRoute()
const cartStore = useCartStore()
const uiStore = useUiStore()
const menuOpen = ref(false)
const headerVisible = ref(true)
const headerHovered = ref(false)
let lastScrollY = 0
let hoverTimeout = null

const navItems = [
  { to: '/', label: 'главная', icon: 'map' },
  { to: '/#museum-map', label: 'виртуальный музей', icon: 'map' },
  { to: '/routes', label: 'онлайн-маршруты', icon: 'routes' },
  { to: '/shop', label: 'магазин мерча', icon: 'shop' },
  { to: '/contact', label: 'помощь проекту', icon: 'contact' },
]

function isNavCurrent(path) {
  const [targetPath, targetHash = ''] = path.split('#')
  if (targetPath === '/') return route.path === '/' && route.hash === (targetHash ? `#${targetHash}` : '')
  return route.path === targetPath || route.path.startsWith(`${targetPath}/`)
}

function closeDrawer() {
  menuOpen.value = false
}

watch(menuOpen, (open) => {
  if (typeof document === 'undefined') return
  document.body.classList.toggle('nav-drawer-open', open)
})

const handleScroll = () => {
  const currentScrollY = window.scrollY
  const topZone = currentScrollY < 100

  if (topZone) {
    headerVisible.value = true
  } else if (currentScrollY > lastScrollY) {
    headerVisible.value = false
  } else {
    headerVisible.value = true
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

const attachMotionListeners = () => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    headerVisible.value = true
    return
  }
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('mousemove', handleMouseMove)
}

onMounted(() => {
  attachMotionListeners()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('mousemove', handleMouseMove)
  if (hoverTimeout) clearTimeout(hoverTimeout)
  document.body.classList.remove('nav-drawer-open')
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
  background: var(--header-bg);
  backdrop-filter: blur(14px) saturate(1.05);
  border-bottom: 1px solid rgba(212, 184, 150, 0.1);
  transform: translateY(0);
  transition: transform 0.3s ease, background 0.3s ease;
}

@media (prefers-reduced-motion: reduce) {
  .app-header {
    transition: none;
  }
}

.app-header.visible {
  transform: translateY(0);
}

.app-header:not(.visible):not(.hovered) {
  transform: translateY(-100%);
}

@media (max-width: 768px) {
  .app-header:not(.visible):not(.hovered) {
    transform: translateY(0);
  }
}

.app-header.hovered {
  transform: translateY(0);
  background: var(--header-bg);
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

.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  flex-shrink: 0;
  cursor: pointer;
  color: inherit;
  transition: transform 0.4s var(--ease-spring, cubic-bezier(0.22, 1, 0.36, 1));
}

.logo:hover {
  transform: translateY(-2px);
}

.logo-mark {
  color: var(--accent);
  transition: transform 0.5s var(--ease-spring, cubic-bezier(0.22, 1, 0.36, 1));
}

.logo:hover .logo-mark {
  transform: rotate(-8deg) scale(1.06);
}

@media (prefers-reduced-motion: reduce) {
  .logo,
  .logo-mark {
    transition: none;
  }
  .logo:hover,
  .logo:hover .logo-mark {
    transform: none;
  }
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
  transition: color var(--transition), background var(--transition);
  text-decoration: none;
  cursor: pointer;
  position: relative;
}

.nav-link::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 3px;
  width: 0;
  height: 2px;
  border-radius: 1px;
  background: linear-gradient(90deg, transparent, var(--accent), transparent);
  transform: translateX(-50%);
  transition: width 0.35s var(--ease-spring, cubic-bezier(0.22, 1, 0.36, 1));
  pointer-events: none;
  opacity: 0.95;
}

.nav-link:hover {
  color: var(--paper);
  background: var(--gray-800);
}

.nav-link:hover::after {
  width: calc(100% - 1.25rem);
}

.nav-link.router-link-active {
  color: var(--paper);
}

.nav-link.router-link-active::after {
  width: calc(100% - 1.25rem);
}

.nav-link.router-link-exact-active {
  color: var(--accent);
  background: var(--accent-soft, rgba(212, 184, 150, 0.18));
}

.nav-link.router-link-exact-active::after {
  width: calc(100% - 1.25rem);
  background: var(--accent);
  opacity: 1;
}

@media (prefers-reduced-motion: reduce) {
  .nav-link::after {
    transition: none;
  }
}

.nav-icon-el {
  color: currentColor;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.profile-nav-btn {
  display: inline-flex;
  width: 44px;
  min-width: 44px;
  min-height: 36px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid var(--gray-600);
  border-radius: 999px;
  color: var(--paper);
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-decoration: none;
  text-transform: uppercase;
  transition: background var(--transition), border-color var(--transition), color var(--transition);
}

.profile-nav-btn:hover,
.profile-nav-btn.router-link-active {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--ink);
}

.cart-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  position: relative;
  cursor: pointer;
}

.theme-btn {
  min-width: 44px;
  min-height: 44px;
  cursor: pointer;
}

.cart-btn-text { margin-right: 0.25rem; }
.cart-badge-pv { margin-left: 0.25rem; }

.burger-btn {
  display: none;
  cursor: pointer;
}

.nav-drawer-theme {
  padding: 0 var(--spacing-md) var(--spacing-sm);
  border-bottom: 1px solid var(--gray-800, rgba(255, 255, 255, 0.08));
  margin-bottom: var(--spacing-sm);
}

.app-light .nav-drawer-theme {
  border-bottom-color: var(--gray-200, #e8e8e8);
}

.nav-drawer-theme :deep(.p-button) {
  width: 100%;
  justify-content: flex-start;
}

.nav-drawer-title {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: 700;
  margin: 0;
  color: var(--cream);
}

.nav-drawer-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.nav-drawer-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1.25rem;
  font-family: var(--font-mono);
  font-size: 1rem;
  color: var(--gray-400);
  text-decoration: none;
  border-bottom: 1px solid var(--gray-800);
  transition:
    background var(--transition),
    color var(--transition),
    transform 0.3s var(--ease-spring, cubic-bezier(0.22, 1, 0.36, 1)),
    padding-left var(--transition);
  cursor: pointer;
}

.nav-drawer-link:hover {
  background: var(--gray-800);
  color: var(--paper);
  transform: translateX(6px);
  padding-left: 1.5rem;
}

@media (prefers-reduced-motion: reduce) {
  .nav-drawer-link {
    transition: background var(--transition), color var(--transition);
  }
  .nav-drawer-link:hover {
    transform: none;
    padding-left: 1.25rem;
  }
}

.nav-drawer-link.router-link-active {
  color: var(--accent);
  background: var(--accent-soft, rgba(212, 184, 150, 0.18));
}

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
  .profile-nav-btn {
    min-width: 42px;
    padding: 0 0.75rem;
  }
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
