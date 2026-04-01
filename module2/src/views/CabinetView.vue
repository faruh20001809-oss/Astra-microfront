<template>
  <div class="page-wrapper cabinet-page">
    <div class="container cabinet-container">
      <header class="cabinet-hero">
        <div class="cabinet-hero__text">
          <p class="cabinet-eyebrow text-mono">Личный кабинет</p>
          <h1 class="cabinet-title">Профиль клиента</h1>
          <p class="cabinet-lead">
            Заказы, маршруты и статус аккаунта — единая точка входа в сервис «Астрахань. Живая история».
          </p>
        </div>
        <div v-if="!isAuthorized" class="cabinet-hero__cta">
          <router-link to="/profile/auth" class="btn btn-primary btn-lg cabinet-cta-primary">
            Войти или зарегистрироваться
          </router-link>
        </div>
      </header>

      <div class="cabinet-bento">
        <article
          class="cabinet-card cabinet-card--identity"
          aria-labelledby="cabinet-identity-title"
        >
          <div class="cabinet-identity__row">
            <div class="cabinet-avatar" aria-hidden="true">{{ initials }}</div>
            <div class="cabinet-identity__body">
              <p class="cabinet-card-label text-mono" id="cabinet-identity-label">Аккаунт</p>
              <h2 id="cabinet-identity-title" class="cabinet-name">{{ profileName }}</h2>
              <p class="cabinet-line">{{ profileEmail || 'Email не указан' }}</p>
              <p class="cabinet-line cabinet-line--status" :class="telegramLinked ? 'is-ok' : 'is-warn'">
                <span class="cabinet-status-dot" aria-hidden="true" />
                Telegram: {{ telegramLinked ? 'подтверждён' : 'не подтверждён' }}
              </p>
            </div>
          </div>
          <div class="cabinet-actions">
            <button
              type="button"
              class="btn btn-ghost btn-sm"
              :disabled="!profileEmail || statusLoading"
              @click="reloadClientStatus"
            >
              {{ statusLoading ? 'Проверка…' : 'Обновить статус' }}
            </button>
            <button v-if="isAuthorized" type="button" class="btn btn-ghost btn-sm" @click="logout">
              Выйти
            </button>
            <router-link v-else to="/profile/auth" class="btn btn-primary btn-sm">Войти в профиль</router-link>
          </div>
        </article>

        <article class="cabinet-card cabinet-card--nav" aria-labelledby="cabinet-nav-title">
          <p class="cabinet-card-label text-mono" id="cabinet-nav-title">Разделы</p>
          <nav class="cabinet-nav" aria-label="Сервис">
            <router-link
              v-for="item in navLinks"
              :key="item.to"
              :to="item.to"
              class="cabinet-nav-link"
            >
              <span class="cabinet-nav-link__title">{{ item.title }}</span>
              <span class="cabinet-nav-link__desc">{{ item.desc }}</span>
            </router-link>
          </nav>
        </article>
      </div>

      <section v-if="!isAuthorized" class="cabinet-empty" aria-live="polite">
        <h3 class="cabinet-empty__title">Профиль не активирован</h3>
        <p class="cabinet-empty__text">
          Создайте учётную запись по логину, email и паролю — так персонализируются заказы и уведомления.
        </p>
        <router-link to="/profile/auth" class="btn btn-accent btn-lg">Создать профиль</router-link>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { javaApi } from '@/api/backend.js'

const navLinks = [
  { to: '/orders', title: 'Заказы', desc: 'История, коды доступа, статусы' },
  { to: '/routes', title: 'Маршруты', desc: 'Каталог и прохождение' },
  { to: '/shop', title: 'Магазин', desc: 'Товары и оформление' },
]

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
.cabinet-page {
  --page-pad-top-extra: clamp(0.65rem, 2vw, 1.1rem);
  min-height: auto;
  flex: 0 1 auto;
  width: 100%;
  padding-bottom: clamp(1.5rem, 4vw, 2.5rem);
}

.cabinet-container {
  max-width: 1040px;
}

.cabinet-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: clamp(0.75rem, 2vw, 1.25rem);
  margin-bottom: clamp(1rem, 2.5vw, 1.5rem);
  padding-bottom: clamp(0.85rem, 2vw, 1.15rem);
  border-bottom: 1px solid rgba(212, 184, 150, 0.14);
}

.cabinet-hero__text {
  flex: 1 1 min(100%, 36rem);
  min-width: 0;
}

.cabinet-eyebrow {
  color: var(--accent);
  margin: 0 0 0.35rem;
  letter-spacing: 0.1em;
}

.cabinet-title {
  font-family: var(--font-display);
  font-size: clamp(1.35rem, 2.8vw + 0.6rem, 1.85rem);
  font-weight: 700;
  line-height: 1.2;
  color: var(--cream);
  margin: 0 0 0.45rem;
  letter-spacing: -0.02em;
}

.cabinet-lead {
  margin: 0;
  max-width: 38rem;
  color: var(--gray-400);
  font-size: clamp(0.875rem, 1.1vw + 0.75rem, 0.98rem);
  line-height: 1.55;
}

.cabinet-hero__cta {
  flex: 0 0 auto;
  align-self: center;
}

.cabinet-cta-primary {
  min-height: 48px;
}

.cabinet-bento {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: clamp(1rem, 2.5vw, 1.35rem);
  align-items: stretch;
}

.cabinet-card {
  position: relative;
  border-radius: var(--radius-md);
  padding: clamp(1.1rem, 2.5vw, 1.5rem);
  border: 1px solid rgba(212, 184, 150, 0.2);
  background:
    linear-gradient(155deg, rgba(46, 38, 32, 0.92) 0%, rgba(26, 21, 18, 0.88) 100%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.04) inset,
    0 18px 48px rgba(0, 0, 0, 0.35);
  overflow: hidden;
}

.cabinet-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 80% at 0% 0%, rgba(212, 184, 150, 0.09), transparent 55%);
  pointer-events: none;
}

.cabinet-card > * {
  position: relative;
  z-index: 1;
}

.cabinet-card-label {
  margin: 0 0 0.75rem;
  color: var(--gray-500);
  letter-spacing: 0.1em;
}

.cabinet-card--identity {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.cabinet-identity__row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: clamp(0.85rem, 2vw, 1.15rem);
  align-items: start;
}

.cabinet-avatar {
  width: clamp(3.5rem, 8vw, 4.25rem);
  height: clamp(3.5rem, 8vw, 4.25rem);
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-size: clamp(1.2rem, 3vw, 1.5rem);
  font-weight: 700;
  color: var(--ink);
  background: linear-gradient(145deg, var(--accent) 0%, #c9a66b 100%);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}

.cabinet-name {
  font-family: var(--font-display);
  font-size: clamp(1.2rem, 2.5vw, 1.45rem);
  font-weight: 600;
  color: var(--cream);
  margin: 0 0 0.35rem;
  line-height: 1.25;
}

.cabinet-line {
  margin: 0.25rem 0 0;
  font-size: 0.92rem;
  color: var(--gray-300);
  line-height: 1.5;
}

.cabinet-line--status {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.cabinet-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--gray-600);
}

.cabinet-line.is-ok .cabinet-status-dot {
  background: var(--success);
  box-shadow: 0 0 0 2px rgba(45, 107, 69, 0.35);
}

.cabinet-line.is-warn .cabinet-status-dot {
  background: var(--warning);
  box-shadow: 0 0 0 2px rgba(201, 162, 39, 0.3);
}

.cabinet-line.is-ok {
  color: #8fd4a8;
}

.cabinet-line.is-warn {
  color: #e8c06d;
}

.cabinet-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 0.65rem;
  padding-top: 0.25rem;
}

.cabinet-actions .btn {
  min-height: 44px;
}

.cabinet-card--nav .cabinet-nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.cabinet-nav-link {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.2rem;
  padding: 0.85rem 1rem;
  min-height: 72px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(212, 184, 150, 0.14);
  background: rgba(0, 0, 0, 0.22);
  color: var(--paper);
  text-decoration: none;
  transition:
    border-color var(--transition),
    background var(--transition),
    transform 0.22s ease;
}

.cabinet-nav-link:hover {
  border-color: rgba(212, 184, 150, 0.45);
  background: rgba(212, 184, 110, 0.06);
}

@media (prefers-reduced-motion: no-preference) {
  .cabinet-nav-link:hover {
    transform: translateY(-2px);
  }
}

.cabinet-nav-link__title {
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--cream);
}

.cabinet-nav-link__desc {
  font-size: 0.78rem;
  color: var(--gray-400);
  line-height: 1.45;
}

.cabinet-empty {
  margin-top: clamp(1.25rem, 3vw, 1.75rem);
  padding: clamp(1.25rem, 3vw, 1.75rem);
  border-radius: var(--radius-md);
  border: 1px dashed rgba(212, 184, 150, 0.28);
  background: rgba(0, 0, 0, 0.2);
  text-align: center;
}

.cabinet-empty__title {
  font-family: var(--font-display);
  font-size: 1.2rem;
  color: var(--cream);
  margin: 0 0 0.5rem;
}

.cabinet-empty__text {
  margin: 0 auto 1.25rem;
  max-width: 32rem;
  color: var(--gray-400);
  line-height: 1.6;
  font-size: 0.95rem;
}

@media (max-width: 900px) {
  .cabinet-bento {
    grid-template-columns: 1fr;
  }

  .cabinet-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .cabinet-hero__cta {
    width: 100%;
    align-self: stretch;
  }

  .cabinet-cta-primary {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 520px) {
  .cabinet-identity__row {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .cabinet-avatar {
    margin: 0 auto;
  }

  .cabinet-actions {
    justify-content: center;
  }
}
</style>
