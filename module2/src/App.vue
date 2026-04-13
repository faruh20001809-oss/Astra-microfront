<template>
  <div id="app-root" class="app-dark">
    <!-- Navigation -->
    <AppHeader />

    <!-- Main content -->
    <main id="main-content" tabindex="-1">
      <router-view v-slot="{ Component, route }">
        <transition :name="route.meta.transition || 'page'" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>

    <!-- Cart Drawer -->
    <CartDrawer />

    <!-- Toasts -->
    <div class="toast-container">
      <transition-group name="toast-pop">
        <div
          v-for="t in toastStore.toasts"
          :key="t.id"
          :class="['toast', t.type === 'achievement' ? 'toast--achievement' : t.type]"
          role="status"
          :aria-live="t.type === 'achievement' ? 'polite' : 'off'"
        >
          <template v-if="t.type === 'achievement'">
            <span class="toast-achievement__glow" aria-hidden="true" />
            <span class="toast-achievement__icon" aria-hidden="true">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="26" height="26">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09ZM18.259 8.715 18 9.75l-.259-1.035a3.375 3.375 0 0 0-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 0 0 2.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 0 0 2.455 2.456L21.75 6l-1.035.259a3.375 3.375 0 0 0-2.455 2.456ZM16.894 20.567 16.5 21.75l-.394-1.183a2.25 2.25 0 0 0-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 0 0 1.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 0 0 1.423 1.423l1.183.394-1.183.394a2.25 2.25 0 0 0-1.423 1.423Z" />
              </svg>
            </span>
            <div class="toast-achievement__body">
              <span class="toast-achievement__eyebrow text-mono">Достижение</span>
              <strong class="toast-achievement__title">{{ t.achievementTitle }}</strong>
            </div>
          </template>
          <template v-else>
            {{ t.msg }}
          </template>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useToastStore } from '@/store/index.js'
import AppHeader from '@/components/layout/AppHeader.vue'
import CartDrawer from '@/components/shop/CartDrawer.vue'

const toastStore = useToastStore()

function onAchievementUnlocked(e) {
  const title = e?.detail?.title
  if (title) toastStore.pushAchievement(title)
}

onMounted(() => {
  window.addEventListener('astra:achievement-unlocked', onAchievementUnlocked)
})
onUnmounted(() => {
  window.removeEventListener('astra:achievement-unlocked', onAchievementUnlocked)
})
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
