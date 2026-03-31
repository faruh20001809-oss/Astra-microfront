<template>
  <div class="page-wrapper profile-page">
    <div class="container">
      <section class="profile-hero">
        <div class="profile-hero-main">
          <p class="text-mono profile-kicker">◫ User Profile</p>
          <h1>Профиль клиента</h1>
          <p class="profile-lead">Единый центр для заказов, маршрутов и персонального статуса аккаунта.</p>
        </div>
        <router-link v-if="!isAuthorized" to="/profile/auth" class="btn btn-primary btn-lg">Войти / Регистрация</router-link>
      </section>

      <section class="profile-panel">
        <article class="profile-card profile-identity">
          <div class="avatar">{{ initials }}</div>
          <div>
            <p class="text-mono profile-label">Identity</p>
            <h2 class="section-heading profile-name">{{ profileName }}</h2>
            <p class="orders-lead">{{ profileEmail || 'Email не указан' }}</p>
            <p class="orders-lead" :class="telegramLinked ? 'ok' : 'warn'">
              Telegram: {{ telegramLinked ? 'подтвержден' : 'не подтвержден' }}
            </p>
          </div>
          <div class="profile-actions">
            <button type="button" class="btn btn-ghost btn-sm" :disabled="!profileEmail || statusLoading" @click="reloadClientStatus">
              {{ statusLoading ? 'Проверка...' : 'Обновить статус' }}
            </button>
            <button v-if="isAuthorized" type="button" class="btn btn-ghost btn-sm" @click="logout">Выйти</button>
            <router-link v-else to="/profile/auth" class="btn btn-primary btn-sm">Войти в профиль</router-link>
          </div>
        </article>

        <article class="profile-card quick-card">
          <p class="text-mono profile-label">Quick actions</p>
          <div class="quick-grid">
            <router-link to="/orders" class="quick-link">
              <strong>Заказы</strong><span>История, коды доступа, статусы</span>
            </router-link>
            <router-link to="/routes" class="quick-link">
              <strong>Маршруты</strong><span>Каталог и прохождение</span>
            </router-link>
            <router-link to="/shop" class="quick-link">
              <strong>Магазин</strong><span>Товары и оформление</span>
            </router-link>
          </div>
        </article>
      </section>

      <section v-if="!isAuthorized" class="profile-empty card">
        <h3 class="section-heading">Профиль не активирован</h3>
        <p class="orders-lead">Создайте профиль клиента по логину, email и паролю, чтобы персонализировать работу с сервисом.</p>
        <router-link to="/profile/auth" class="btn btn-accent btn-lg">Создать профиль</router-link>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { javaApi } from '@/api/backend.js'

const clientEmail = ref(localStorage.getItem('astra_guest_email') || '')
const profileState = ref(loadProfile())
const telegramLinked = ref(false)
const statusLoading = ref(false)

function loadProfile() {
  try {
    return JSON.parse(localStorage.getItem('astra_client_profile') || '{}') || {}
  } catch {
    return {}
  }
}

const isAuthorized = computed(() => Boolean(profileState.value?.username))
const profileEmail = computed(() => profileState.value?.email || clientEmail.value || '')
const profileName = computed(() => profileState.value?.name || profileState.value?.username || 'Гость')
const initials = computed(() => String(profileName.value || 'G').trim().slice(0, 1).toUpperCase())

async function reloadClientStatus() {
  const email = String(profileEmail.value || '').trim()
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

function refreshAll() {
  clientEmail.value = localStorage.getItem('astra_guest_email') || ''
  profileState.value = loadProfile()
  void reloadClientStatus()
}

function handleProfileUpdated() {
  refreshAll()
}

function handleGuestEmailUpdated(event) {
  const nextEmail = String(event?.detail?.email || '').trim()
  if (nextEmail && nextEmail !== clientEmail.value) {
    clientEmail.value = nextEmail
    void reloadClientStatus()
  }
}

function logout() {
  localStorage.removeItem('astra_client_profile')
  profileState.value = {}
}

onMounted(() => {
  window.addEventListener('storage', refreshAll)
  window.addEventListener('focus', refreshAll)
  window.addEventListener('astra:client-profile-updated', handleProfileUpdated)
  window.addEventListener('astra:guest-email-updated', handleGuestEmailUpdated)
  void reloadClientStatus()
})

onUnmounted(() => {
  window.removeEventListener('storage', refreshAll)
  window.removeEventListener('focus', refreshAll)
  window.removeEventListener('astra:client-profile-updated', handleProfileUpdated)
  window.removeEventListener('astra:guest-email-updated', handleGuestEmailUpdated)
})
</script>

<style scoped>
.profile-page { padding-top: 1.6rem; }
.profile-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
  margin-bottom: 1rem;
}
.profile-kicker { color: var(--accent); margin-bottom: 0.4rem; }
.profile-lead { color: var(--gray-300); max-width: 780px; }
.profile-panel {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 1rem;
}
.profile-card {
  border: 1px solid rgba(200, 169, 110, 0.25);
  background: linear-gradient(145deg, rgba(245, 240, 232, 0.07), rgba(245, 240, 232, 0.02));
  border-radius: 20px;
  padding: 1rem;
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.24);
}
.profile-identity {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1rem;
  align-items: center;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: var(--ink);
  background: linear-gradient(135deg, var(--accent), #f0d8a2);
  font-family: var(--font-display);
  font-size: 1.35rem;
}
.profile-label { color: var(--gray-400); margin-bottom: 0.35rem; }
.profile-name { margin: 0; }
.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  align-items: flex-end;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.65rem;
  margin-top: 0.7rem;
}
.quick-link {
  border: 1px solid var(--gray-700);
  border-radius: 14px;
  padding: 0.8rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  color: var(--paper);
  background: rgba(3, 7, 18, 0.35);
}
.quick-link strong { font-family: var(--font-display); font-size: 1.02rem; }
.quick-link span { color: var(--gray-400); font-size: 0.78rem; }
.quick-link:hover {
  border-color: var(--accent);
  transform: translateY(-2px);
}
.profile-empty {
  margin-top: 1rem;
  border: 1px dashed var(--gray-600);
  background: rgba(3, 7, 18, 0.35);
}
.ok { color: #38c172; }
.warn { color: #f6ad55; }
@media (max-width: 980px) {
  .profile-panel { grid-template-columns: 1fr; }
  .quick-grid { grid-template-columns: 1fr; }
  .profile-identity { grid-template-columns: auto 1fr; }
  .profile-actions { grid-column: 1 / -1; align-items: flex-start; flex-direction: row; flex-wrap: wrap; }
}
</style>
