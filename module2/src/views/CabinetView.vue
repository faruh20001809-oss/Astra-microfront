<template>
  <div class="page-wrapper cabinet-page">
    <div class="container cabinet-container">
      <header class="cabinet-hero motion-reveal">
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
            </div>
          </div>
          <div class="cabinet-actions">
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

      <section class="cabinet-progress" aria-labelledby="cabinet-progress-title">
        <article class="cabinet-card cabinet-card--progress">
          <header class="cabinet-progress__head">
            <p class="cabinet-card-label text-mono" id="cabinet-progress-label">Активность</p>
            <h2 id="cabinet-progress-title" class="cabinet-progress__title">Мой прогресс</h2>
            <p class="cabinet-progress__lead">
              Данные хранятся локально; отправка на сервер — по email из
              <router-link to="/orders" class="cabinet-progress__inline-link">«Заказов»</router-link>
              или из профиля выше.
            </p>
          </header>

          <div class="cabinet-progress__body">
            <ul class="cabinet-progress-stats" aria-label="Сводка прогресса">
              <li class="cabinet-stat">
                <span class="cabinet-stat__label">Посещено точек</span>
                <strong class="cabinet-stat__value">{{ visitedPoisCount }}</strong>
              </li>
              <li class="cabinet-stat">
                <span class="cabinet-stat__label">Маршрутов всего</span>
                <strong class="cabinet-stat__value">{{ completedRoutesCount }}</strong>
              </li>
              <li class="cabinet-stat">
                <span class="cabinet-stat__label">Бесплатных</span>
                <strong class="cabinet-stat__value">{{ completedFreeRoutesCount }}</strong>
              </li>
              <li class="cabinet-stat">
                <span class="cabinet-stat__label">Платных</span>
                <strong class="cabinet-stat__value">{{ completedPaidRoutesCount }}</strong>
              </li>
              <li class="cabinet-stat">
                <span class="cabinet-stat__label">Избранные маршруты</span>
                <strong class="cabinet-stat__value">{{ favoriteRoutesList.length }}</strong>
              </li>
              <li class="cabinet-stat">
                <span class="cabinet-stat__label">Избранные точки</span>
                <strong class="cabinet-stat__value">{{ favoritePoisList.length }}</strong>
              </li>
            </ul>

            <div class="cabinet-reward" aria-labelledby="cabinet-reward-label">
              <p id="cabinet-reward-label" class="cabinet-reward__caption text-mono">
                Награда за активность: {{ rewardProgress.progressPercent }}%
              </p>
              <div
                class="cabinet-reward__track"
                role="progressbar"
                :aria-valuenow="rewardProgress.progressPercent"
                aria-valuemin="0"
                aria-valuemax="100"
                :aria-label="`Прогресс награды ${rewardProgress.progressPercent} процентов`"
              >
                <div
                  class="cabinet-reward__fill"
                  :style="{ transform: `scaleX(${Math.min(1, Math.max(0, rewardProgress.progressPercent / 100))})` }"
                />
              </div>
              <p class="cabinet-reward__hint">
                Платные: {{ rewardProgress.paidCompleted }}/2 · Бесплатные: {{ rewardProgress.freeCompleted }}/3
              </p>
            </div>

            <div class="cabinet-tier" role="list" aria-label="Уровни">
              <span
                v-for="tier in tierList"
                :key="tier.key"
                role="listitem"
                :class="['cabinet-tier__badge', { 'cabinet-tier__badge--active': routeTier === tier.key }]"
              >
                {{ tier.label }}
              </span>
            </div>

            <div class="cabinet-subsection cabinet-subsection--achievements">
              <h3 class="cabinet-subsection__title">Достижения</h3>
              <ul class="cabinet-ach-list" role="list">
                <li
                  v-for="(a, index) in achievementsView"
                  :key="a.id"
                  class="cabinet-ach-item"
                  :class="{ 'is-unlocked': a.unlocked, 'is-locked': !a.unlocked }"
                  :style="{ '--stagger': index }"
                  role="listitem"
                >
                  <div class="cabinet-ach-item__row">
                    <span class="cabinet-ach-item__medal" aria-hidden="true">
                      <svg v-if="a.unlocked" class="cabinet-ach-icon cabinet-ach-icon--ok" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                      </svg>
                      <svg v-else class="cabinet-ach-icon cabinet-ach-icon--pending" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                      </svg>
                    </span>
                    <div class="cabinet-ach-item__body">
                      <span class="cabinet-ach-item__title">{{ a.title }}</span>
                      <div
                        v-if="!a.unlocked"
                        class="cabinet-ach-meter"
                        role="progressbar"
                        :aria-valuenow="achProgressPercent(a)"
                        aria-valuemin="0"
                        aria-valuemax="100"
                        :aria-label="`Прогресс: ${a.progress} из ${a.threshold}`"
                      >
                        <div
                          class="cabinet-ach-meter-fill"
                          :style="{ transform: `scaleX(${Math.min(1, achProgressPercent(a) / 100)})` }"
                        />
                      </div>
                    </div>
                    <span class="cabinet-ach-item__meta text-mono">{{ a.progress }}/{{ a.threshold }}</span>
                  </div>
                </li>
              </ul>
            </div>

            <div class="cabinet-subsection">
              <h3 class="cabinet-subsection__title">Награды</h3>
              <p class="cabinet-reward-available text-mono">
                Доступно: {{ rewardsState.available.length }}
              </p>
              <div class="cabinet-progress-actions">
                <button
                  type="button"
                  class="btn btn-primary btn-sm"
                  :disabled="!rewardsState.available.length"
                  @click="consumeSampleReward"
                >
                  Выбрать платный маршрут бесплатно
                </button>
                <button
                  type="button"
                  class="btn btn-ghost btn-sm"
                  :disabled="syncBusy"
                  @click="syncProgressToServer"
                >
                  {{ syncBusy ? 'Синхронизация…' : 'Синхронизировать' }}
                </button>
              </div>
              <p v-if="syncStatus" class="cabinet-sync-status" role="status">{{ syncStatus }}</p>
            </div>

            <div class="cabinet-subsection cabinet-subsection--last">
              <h3 class="cabinet-subsection__title">История активности</h3>
              <ul class="cabinet-activity-list">
                <li v-for="(event, idx) in recentActivity" :key="idx">{{ activityLabel(event) }}</li>
                <li v-if="!recentActivity.length" class="cabinet-activity-empty">Пока нет событий</li>
              </ul>
            </div>
          </div>
        </article>
      </section>

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
import { clearClientSession, isClientLoggedIn, loadClientProfile } from '@/auth/clientAuth.js'
import { useGuestProgress } from '@/composables/useGuestProgress.js'
import { useFavorites } from '@/composables/useFavorites.js'

const tierList = [
  { key: 'bronze', label: 'Bronze' },
  { key: 'silver', label: 'Silver' },
  { key: 'gold', label: 'Gold' },
]

const {
  visitedPoisCount,
  completedRoutesCount,
  completedFreeRoutesCount,
  completedPaidRoutesCount,
  rewardProgress,
  routeTier,
  achievements,
  achievementsState,
  rewardsState,
  activityLog,
  consumeReward,
  getSnapshot,
} = useGuestProgress()
const { favoriteRoutesList, favoritePoisList } = useFavorites()

const syncBusy = ref(false)
const syncStatus = ref('')

const achievementsView = computed(() =>
  achievements.value.map((a) => ({
    ...a,
    progress: Number(achievementsState.value?.progress?.[a.id]) || 0,
    unlocked: (achievementsState.value?.unlocked || []).includes(a.id),
  })),
)

const recentActivity = computed(() => (activityLog.value || []).slice(0, 8))

function achProgressPercent(a) {
  const th = Number(a?.threshold) || 1
  const p = Number(a?.progress) || 0
  return Math.min(100, Math.round((p / th) * 100))
}

function activityLabel(event) {
  if (!event?.type) return 'Событие'
  if (event.type === 'achievement_unlocked') return `Ачивка: ${event.title}`
  if (event.type === 'reward_granted') return 'Получена награда: бесплатный платный маршрут'
  if (event.type === 'reward_used') return 'Награда использована'
  if (event.type === 'route_completed') return `Пройден маршрут (${event.paid ? 'платный' : 'бесплатный'})`
  if (event.type === 'poi_visited') return 'Посещена новая точка'
  return event.type
}

function consumeSampleReward() {
  consumeReward(null)
  void syncProgressToServer()
}

async function syncProgressToServer() {
  if (!isClientLoggedIn()) {
    syncStatus.value = 'Войдите в профиль, чтобы синхронизировать прогресс с сервером.'
    return
  }
  syncBusy.value = true
  syncStatus.value = ''
  try {
    await javaApi.userProgress.sync(getSnapshot())
    syncStatus.value = 'Прогресс синхронизирован с сервером.'
  } catch (e) {
    syncStatus.value = e?.message || 'Не удалось синхронизировать прогресс.'
  } finally {
    syncBusy.value = false
  }
}

const navLinks = [
  { to: '/orders', title: 'Заказы', desc: 'История, коды доступа, статусы' },
  { to: '/routes', title: 'Маршруты', desc: 'Каталог и прохождение' },
  { to: '/shop', title: 'Магазин', desc: 'Товары и оформление' },
]

const clientEmail = ref(localStorage.getItem('astra_guest_email') || '')
const profileState = ref(loadClientProfile())

const isAuthorized = computed(() => isClientLoggedIn() && Boolean(profileState.value?.username))
const profileEmail = computed(() => profileState.value?.email || clientEmail.value || '')
const profileName = computed(() => profileState.value?.name || profileState.value?.username || 'Гость')
const initials = computed(() => String(profileName.value || 'G').trim().slice(0, 1).toUpperCase())

function refreshAll() {
  clientEmail.value = localStorage.getItem('astra_guest_email') || ''
  profileState.value = loadClientProfile()
}

function handleProfileUpdated() {
  refreshAll()
}

function handleGuestEmailUpdated(event) {
  const nextEmail = String(event?.detail?.email || '').trim()
  if (nextEmail && nextEmail !== clientEmail.value) {
    clientEmail.value = nextEmail
  }
}

function logout() {
  clearClientSession()
  profileState.value = {}
}

onMounted(() => {
  window.addEventListener('storage', refreshAll)
  window.addEventListener('focus', refreshAll)
  window.addEventListener('astra:client-profile-updated', handleProfileUpdated)
  window.addEventListener('astra:guest-email-updated', handleGuestEmailUpdated)
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
  color: var(--paper);
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
  color: var(--gray-500);
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
  color: var(--gray-500);
  line-height: 1.5;
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
  color: var(--cream);
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
  opacity: 0.95;
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

/* —— Мой прогресс (перенесено из заказов): контейнерные запросы, a11y —— */
.cabinet-progress {
  margin-top: clamp(1.25rem, 3vw, 1.85rem);
  container-type: inline-size;
  container-name: cabinet-progress;
}

.cabinet-card--progress {
  padding: clamp(1.15rem, 2.8vw, 1.65rem);
}

.cabinet-progress__head {
  margin-bottom: clamp(1rem, 2.5vw, 1.35rem);
}

.cabinet-progress__title {
  font-family: var(--font-display);
  font-size: clamp(1.15rem, 2cqi + 0.85rem, 1.45rem);
  font-weight: 700;
  color: var(--cream);
  margin: 0.35rem 0 0.5rem;
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.cabinet-progress__lead {
  margin: 0;
  max-width: 42rem;
  color: var(--gray-400);
  font-size: clamp(0.82rem, 1.2cqi + 0.72rem, 0.92rem);
  line-height: 1.6;
}

.cabinet-progress__inline-link {
  color: var(--accent);
  text-decoration: underline;
  text-underline-offset: 0.15em;
}

.cabinet-progress__inline-link:hover {
  color: var(--cream);
}

.cabinet-progress-stats {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.65rem;
  grid-template-columns: 1fr;
}

@container cabinet-progress (min-width: 480px) {
  .cabinet-progress-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@container cabinet-progress (min-width: 720px) {
  .cabinet-progress-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.cabinet-stat {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.75rem 0.85rem;
  min-height: 44px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(212, 184, 150, 0.12);
  background: rgba(0, 0, 0, 0.2);
}

.cabinet-stat__label {
  font-size: 0.72rem;
  font-family: var(--font-mono);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--gray-500);
  line-height: 1.3;
}

.cabinet-stat__value {
  font-family: var(--font-display);
  font-size: clamp(1.25rem, 2cqi + 0.75rem, 1.5rem);
  font-weight: 700;
  color: var(--cream);
  font-variant-numeric: tabular-nums;
}

.cabinet-reward {
  margin-top: clamp(1rem, 2.5vw, 1.35rem);
  padding-top: clamp(0.85rem, 2vw, 1.1rem);
  border-top: 1px solid rgba(212, 184, 150, 0.12);
}

.cabinet-reward__caption {
  margin: 0 0 0.5rem;
  font-size: 0.78rem;
  color: var(--accent);
}

.cabinet-reward__track {
  height: 10px;
  border-radius: 999px;
  background: var(--gray-800);
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.35);
}

.cabinet-reward__fill {
  height: 100%;
  width: 100%;
  background: linear-gradient(90deg, var(--accent-dark), var(--accent));
  border-radius: 999px;
  transform-origin: left;
  transition: transform 0.35s ease;
}

@media (prefers-reduced-motion: reduce) {
  .cabinet-reward__fill {
    transition: none;
  }
}

.cabinet-reward__hint {
  margin: 0.5rem 0 0;
  font-size: 0.78rem;
  color: var(--gray-400);
  line-height: 1.45;
}

.cabinet-tier {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1rem;
}

.cabinet-tier__badge {
  font-size: 0.68rem;
  font-family: var(--font-mono);
  padding: 0.4rem 0.65rem;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  border: 1px solid rgba(212, 184, 150, 0.2);
  border-radius: var(--radius-sm);
  color: var(--gray-500);
}

.cabinet-tier__badge--active {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(212, 184, 150, 0.1);
}

.cabinet-subsection {
  margin-top: clamp(1rem, 2.5vw, 1.25rem);
  padding-top: clamp(0.85rem, 2vw, 1.05rem);
  border-top: 1px solid rgba(212, 184, 150, 0.1);
}

.cabinet-subsection--last {
  margin-bottom: 0;
}

.cabinet-subsection__title {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 600;
  color: var(--cream);
  margin: 0 0 0.65rem;
}

.cabinet-subsection--achievements {
  overflow: hidden;
}

.cabinet-ach-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.cabinet-ach-item {
  --stagger: 0;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(212, 184, 150, 0.1);
  background: rgba(0, 0, 0, 0.18);
  overflow: hidden;
  animation: ach-row-in 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(var(--stagger) * 0.08s);
}

@media (prefers-reduced-motion: reduce) {
  .cabinet-ach-item {
    animation: none;
  }
}

@keyframes ach-row-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.cabinet-ach-item.is-unlocked {
  border-color: rgba(212, 184, 150, 0.35);
  background: linear-gradient(
    125deg,
    rgba(212, 184, 150, 0.1) 0%,
    rgba(0, 0, 0, 0.15) 55%
  );
  box-shadow: 0 0 0 1px rgba(212, 184, 150, 0.06);
  animation: ach-row-in 0.55s cubic-bezier(0.22, 1, 0.36, 1) both,
    ach-unlock-flash 0.85s ease-out calc(var(--stagger) * 0.08s + 0.15s) 1;
}

@media (prefers-reduced-motion: reduce) {
  .cabinet-ach-item.is-unlocked {
    animation: none;
  }
}

@keyframes ach-unlock-flash {
  0% {
    box-shadow: 0 0 0 0 rgba(212, 184, 150, 0.45);
  }
  70% {
    box-shadow: 0 0 24px 2px rgba(212, 184, 150, 0.12);
  }
  100% {
    box-shadow: 0 0 0 1px rgba(212, 184, 150, 0.06);
  }
}

.cabinet-ach-item__row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.65rem;
  align-items: center;
  padding: 0.65rem 0.75rem;
  min-height: 52px;
}

.cabinet-ach-item__medal {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  flex-shrink: 0;
}

.cabinet-ach-icon {
  width: 1.5rem;
  height: 1.5rem;
}

.cabinet-ach-icon--ok {
  color: var(--accent);
  filter: drop-shadow(0 0 8px rgba(212, 184, 150, 0.35));
}

.cabinet-ach-item.is-unlocked .cabinet-ach-icon--ok {
  animation: ach-medal-pop 0.65s cubic-bezier(0.34, 1.56, 0.64, 1) calc(var(--stagger) * 0.08s + 0.2s) both;
}

@media (prefers-reduced-motion: reduce) {
  .cabinet-ach-item.is-unlocked .cabinet-ach-icon--ok {
    animation: none;
  }
}

@keyframes ach-medal-pop {
  0% {
    transform: scale(0.6) rotate(-12deg);
    opacity: 0.5;
  }
  100% {
    transform: scale(1) rotate(0);
    opacity: 1;
  }
}

.cabinet-ach-icon--pending {
  color: var(--gray-500);
}

.cabinet-ach-item__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.cabinet-ach-item__title {
  font-size: 0.88rem;
  font-weight: 600;
  line-height: 1.35;
  color: var(--gray-500);
}

.cabinet-ach-item.is-unlocked .cabinet-ach-item__title {
  color: var(--cream);
}

.cabinet-ach-meter {
  height: 5px;
  border-radius: 999px;
  background: var(--gray-800);
  overflow: hidden;
}

.cabinet-ach-meter-fill {
  height: 100%;
  width: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent-dark), var(--accent));
  transform-origin: left;
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

@media (prefers-reduced-motion: reduce) {
  .cabinet-ach-meter-fill {
    transition: none;
  }
}

.cabinet-ach-item.is-locked .cabinet-ach-meter-fill {
  animation: ach-meter-shimmer 2.2s ease-in-out infinite;
}

@media (prefers-reduced-motion: reduce) {
  .cabinet-ach-item.is-locked .cabinet-ach-meter-fill {
    animation: none;
  }
}

@keyframes ach-meter-shimmer {
  0%,
  100% {
    filter: brightness(1);
  }
  50% {
    filter: brightness(1.15);
  }
}

.cabinet-ach-item__meta {
  font-size: 0.72rem;
  color: var(--gray-400);
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.cabinet-reward-available {
  margin: 0 0 0.65rem;
  font-size: 0.78rem;
  color: var(--gray-400);
}

.cabinet-progress-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 0.65rem;
  align-items: center;
}

.cabinet-progress-actions .btn {
  min-height: 44px;
}

.cabinet-sync-status {
  margin: 0.5rem 0 0;
  font-size: 0.78rem;
  color: var(--gray-400);
  line-height: 1.45;
}

.cabinet-activity-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-size: 0.82rem;
  color: var(--gray-300);
  line-height: 1.5;
}

.cabinet-activity-empty {
  color: var(--gray-500);
  font-style: italic;
}
</style>
