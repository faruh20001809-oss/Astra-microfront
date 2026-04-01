<template>
  <div id="app-root" class="app-dark">
    <!-- Navigation -->
    <AppHeader />

    <!-- Main content -->
    <main id="main-content" tabindex="-1">
      <router-view v-slot="{ Component, route }">
        <transition :name="route.meta.transition || 'fade'" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>

    <!-- Cart Drawer -->
    <CartDrawer />

    <!-- Toasts -->
    <div class="toast-container">
      <transition-group name="fade">
        <div
          v-for="t in toastStore.toasts"
          :key="t.id"
          :class="['toast', t.type]"
        >
          {{ t.msg }}
        </div>
      </transition-group>
    </div>
  </div>
</template>

<script setup>
import { useToastStore } from '@/store/index.js'
import AppHeader from '@/components/layout/AppHeader.vue'
import CartDrawer from '@/components/shop/CartDrawer.vue'

const toastStore = useToastStore()
</script>

<style>
#app-root {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  padding-left: env(safe-area-inset-left, 0);
  padding-right: env(safe-area-inset-right, 0);
}

#main-content {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-width: 0;
  width: 100%;
}
</style>
