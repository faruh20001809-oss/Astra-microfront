<template>
  <div id="app-root">
    <!-- Navigation -->
    <AppHeader />

    <!-- Main content -->
    <router-view v-slot="{ Component, route }">
      <transition :name="route.meta.transition || 'fade'" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>

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
  display: flex;
  flex-direction: column;
}
</style>
