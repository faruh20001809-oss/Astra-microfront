<template>
  <div class="page-wrapper cabinet-page">
    <div class="container">
      <section class="cabinet-hero">
        <p class="text-mono" style="color:var(--accent)">◫ Личный кабинет</p>
        <h1>Профиль пользователя</h1>
        <p class="cabinet-lead">
          Выберите нужный раздел: история заказов, маршруты или магазин.
        </p>
      </section>

      <section class="cabinet-client card">
        <div>
          <p class="text-mono" style="margin:0;color:var(--accent)">Профиль клиента</p>
          <h2 class="section-heading" style="margin-top:0.35rem">Информация о клиенте</h2>
          <p class="orders-lead" style="margin-bottom:0.4rem">
            Email: <strong>{{ clientEmail || 'Не указан' }}</strong>
          </p>
          <p class="orders-lead" :class="telegramLinked ? 'ok' : 'warn'" style="margin:0">
            Telegram: {{ telegramLinked ? 'подтвержден' : 'не подтвержден' }}
          </p>
          <p v-if="!clientEmail" class="orders-lead" style="margin-top:0.5rem">
            Email пока не известен. Войдите в заказы, чтобы привязать профиль клиента.
          </p>
        </div>
        <div class="cabinet-client-actions">
          <button type="button" class="btn btn-ghost btn-sm" :disabled="!clientEmail || statusLoading" @click="reloadClientStatus">
            {{ statusLoading ? 'Проверка…' : 'Обновить статус' }}
          </button>
          <router-link v-if="clientEmail" to="/orders" class="btn btn-accent btn-sm">Управлять профилем</router-link>
          <router-link v-else to="/orders" class="btn btn-primary btn-sm">Войти в заказы</router-link>
        </div>
      </section>

      <section class="cabinet-grid">
        <article class="card cabinet-card">
          <h2 class="section-heading">Мои заказы</h2>
          <p class="orders-lead">Вход по email и коду, статусы, Telegram-уведомления и прогресс.</p>
          <router-link to="/orders" class="btn btn-primary btn-lg">Перейти в заказы</router-link>
        </article>

        <article class="card cabinet-card">
          <h2 class="section-heading">Маршруты</h2>
          <p class="orders-lead">Просматривайте доступные маршруты и отмечайте пройденные.</p>
          <router-link to="/routes" class="btn btn-ghost btn-lg">Открыть маршруты</router-link>
        </article>

        <article class="card cabinet-card">
          <h2 class="section-heading">Магазин</h2>
          <p class="orders-lead">Покупайте товары и отслеживайте заказы в кабинете.</p>
          <router-link to="/shop" class="btn btn-ghost btn-lg">Открыть магазин</router-link>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { javaApi } from '@/api/backend.js'

const clientEmail = ref(localStorage.getItem('astra_guest_email') || '')
const telegramLinked = ref(false)
const statusLoading = ref(false)

async function reloadClientStatus() {
  const email = String(clientEmail.value || '').trim()
  if (!email) return
  statusLoading.value = true
  try {
    const data = await javaApi.telegram.getLinkStatus({ email })
    telegramLinked.value = !!data?.linked
  } catch {
    telegramLinked.value = false
  } finally {
    statusLoading.value = false
  }
}

function refreshEmailFromStorage() {
  const nextEmail = localStorage.getItem('astra_guest_email') || ''
  if (nextEmail !== clientEmail.value) {
    clientEmail.value = nextEmail
    void reloadClientStatus()
  }
}

function handleGuestEmailUpdated(event) {
  const nextEmail = String(event?.detail?.email || '').trim()
  if (!nextEmail) return
  if (nextEmail !== clientEmail.value) {
    clientEmail.value = nextEmail
    void reloadClientStatus()
  }
}

onMounted(() => {
  window.addEventListener('storage', refreshEmailFromStorage)
  window.addEventListener('focus', refreshEmailFromStorage)
  window.addEventListener('astra:guest-email-updated', handleGuestEmailUpdated)
  void reloadClientStatus()
})

onUnmounted(() => {
  window.removeEventListener('storage', refreshEmailFromStorage)
  window.removeEventListener('focus', refreshEmailFromStorage)
  window.removeEventListener('astra:guest-email-updated', handleGuestEmailUpdated)
})
</script>

<style scoped>
.cabinet-page { padding-top: 1.5rem; }
.cabinet-hero { margin-bottom: 1.25rem; }
.cabinet-lead { color: var(--gray-400); max-width: 760px; }
.cabinet-client {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}
.cabinet-client-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.ok { color: #2f9e44; }
.warn { color: #d08770; }
.cabinet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1rem;
}
.cabinet-card { display: flex; flex-direction: column; gap: 0.9rem; }
@media (max-width: 768px) {
  .cabinet-client { flex-direction: column; align-items: flex-start; }
}
</style>
