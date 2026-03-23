<template>
  <div class="page-wrapper route-share-page">
    <div class="container">
      <section class="share-hero">
        <p class="text-mono" style="color:var(--accent)">⊹ Маршрут</p>
        <h1 v-if="routeData">{{ routeData.title || routeData.name }}</h1>
        <h1 v-else-if="error">Маршрут не найден</h1>
        <h1 v-else>Загрузка…</h1>
        <p v-if="routeData?.description" class="share-desc">{{ routeData.description }}</p>
        <p v-if="error" class="share-desc">{{ error }}</p>
        <div v-if="routeData" class="share-actions">
          <router-link to="/routes" class="btn btn-accent btn-lg">Все маршруты</router-link>
          <router-link to="/" class="btn btn-ghost">На карту</router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { javaApi } from '@/api/backend.js'

const vueRoute = useRoute()
const routeData = ref(null)
const error = ref('')

async function load(id) {
  error.value = ''
  routeData.value = null
  const num = Number(id)
  if (!Number.isFinite(num)) {
    error.value = 'Некорректная ссылка'
    return
  }
  try {
    const data = await javaApi.routes.getById(num)
    if (data && (data.id != null || data.title)) {
      routeData.value = data
    } else {
      error.value = 'Маршрут не найден или недоступен'
    }
  } catch {
    error.value = 'Не удалось загрузить маршрут'
  }
}

onMounted(() => load(vueRoute.params.id))
watch(() => vueRoute.params.id, (id) => load(id))
</script>

<style scoped>
.route-share-page { padding-bottom: var(--spacing-2xl); min-height: 50vh; }
.share-hero { padding: var(--spacing-2xl) 0; max-width: 640px; }
.share-desc { color: var(--gray-400); line-height: 1.7; margin-top: var(--spacing-md); }
.share-actions { display: flex; flex-wrap: wrap; gap: var(--spacing-md); margin-top: var(--spacing-xl); }
</style>
