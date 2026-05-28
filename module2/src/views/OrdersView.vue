<template>
  <div class="page-wrapper orders-page">
    <div class="container">
      <section class="orders-hero motion-reveal">
        <p class="text-mono" style="color:var(--accent)">◫ Кабинет гостя</p>
        <h1>Мои заказы</h1>
      </section>
      <div class="orders-layout">
        <div class="orders-form-card card orders-form-card--full">
          <h2 class="section-heading">Вход по email</h2>
          <form class="contact-form" @submit.prevent="sendCode">
            <div class="form-group"><label class="form-label">Email *</label><input v-model.trim="emailInput" type="email" class="form-input" required /></div>
            <button type="submit" class="btn btn-primary btn-lg" :disabled="requestLoading || verifyLoading">{{ requestLoading ? 'Отправка…' : 'Получить код' }}</button>
          </form>
          <p v-if="codeHint" class="form-success" style="margin-top:1rem">{{ codeHint }}</p>
          <p v-if="formError" class="form-error" style="margin-top:0.75rem">{{ formError }}</p>
          <form v-if="codeRequested" class="contact-form" style="margin-top:1rem" @submit.prevent="verifyAndLoad">
            <div class="form-group"><label class="form-label">Код *</label><input v-model.trim="codeInput" type="text" class="form-input" /></div>
            <button type="submit" class="btn btn-accent btn-lg" :disabled="verifyLoading || requestLoading || !telegramConfirmed">{{ verifyLoading ? 'Загрузка…' : 'Показать заказы' }}</button>
          </form>
          <div class="divider" style="margin:1.25rem 0" />
          <div class="telegram-bind-block">
            <h3 class="section-heading" style="font-size:1rem">Привязка Telegram</h3>
            <div class="telegram-bind-actions">
              <button type="button" class="btn btn-ghost btn-sm" :disabled="telegramStatusLoading || !emailInput.trim()" @click="checkTelegramStatus">{{ telegramStatusLoading ? 'Проверка…' : 'Проверить статус' }}</button>
              <button type="button" class="btn btn-accent btn-sm" :disabled="telegramLoading || !emailInput.trim()" @click="requestTelegramLink">{{ telegramLoading ? 'Генерация…' : 'Получить ссылку' }}</button>
              <a v-if="telegramDeepLink" class="btn btn-primary btn-sm" :href="telegramDeepLink" target="_blank" rel="noopener noreferrer" @click="startTelegramStatusPolling">Открыть бота</a>
            </div>
            <p v-if="telegramStatusText" :class="telegramConfirmed ? 'form-success' : 'form-error'" style="margin-top:0.75rem">{{ telegramStatusText }}</p>
            <p v-if="telegramHint" class="orders-lead" style="font-size:0.8rem;margin-top:0.5rem">{{ telegramHint }}</p>
          </div>

          <div v-if="orders.length" style="margin-top:1.25rem">
            <h3 class="section-heading" style="font-size:1rem">История заказов</h3>
            <div class="order-filters">
              <select v-model="orderTypeFilter" class="form-input">
                <option value="all">Все типы</option><option value="free">Бесплатные</option><option value="paid">Платные</option>
              </select>
              <select v-model="periodFilter" class="form-input">
                <option value="all">За всё время</option><option value="30">30 дней</option><option value="90">90 дней</option><option value="365">365 дней</option>
              </select>
            </div>

            <div v-for="group in groupedOrders" :key="group.key" class="orders-group">
              <h4 class="text-mono orders-group-title">{{ group.label }} ({{ group.items.length }})</h4>
              <div v-for="(ord, idx) in group.items" :key="ord.orderId || ord.id || idx" class="order-result card">
                <h4 class="section-heading" style="font-size:0.95rem">Заказ {{ ord.orderId }}</h4>
                <dl class="order-dl">
                  <div><dt>Дата</dt><dd>{{ formatDate(ord.createdAt) }}</dd></div>
                  <div><dt>Статус</dt><dd>{{ statusLabel(ord.status) }}</dd></div>
                  <div><dt>Сумма</dt><dd>{{ formatMoney(ord.total, ord.currency) }}</dd></div>
                  <div><dt>Тип</dt><dd>{{ orderPricingType(ord) === 'paid' ? 'Платный' : 'Бесплатный' }}</dd></div>
                </dl>
                <div class="order-timeline">
                  <span v-for="s in orderTimelineState(ord.status)" :key="s.key" :class="['timeline-chip', s.state]">{{ s.label }}</span>
                </div>
              </div>
            </div>
          </div>
          <p v-if="emptyAfterVerify" class="form-error" style="margin-top:1rem">Заказы не найдены для этого email.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { javaApi } from '@/api/backend.js'
import { isClientLoggedIn } from '@/auth/clientAuth.js'
import { useGuestProgress } from '@/composables/useGuestProgress.js'
import { useFavorites } from '@/composables/useFavorites.js'

const emailInput = ref('')
const codeInput = ref('')
const codeRequested = ref(false)
const codeHint = ref('')
const requestLoading = ref(false)
const verifyLoading = ref(false)
const formError = ref('')
const networkError = ref('')
const orders = ref([])
const emptyAfterVerify = ref(false)
const telegramLoading = ref(false)
const telegramStatusLoading = ref(false)
const telegramConfirmed = ref(false)
const telegramDeepLink = ref('')
const telegramStatusText = ref('')
const telegramHint = ref('')
const orderTypeFilter = ref('all')
const periodFilter = ref('all')

const {
  visitedPoisCount,
  completedPaidRoutesCount,
  completedFreeRoutesCount,
  syncFromCounters,
  getSnapshot,
  mergeSnapshot,
} = useGuestProgress()
const { favoritePoisList } = useFavorites()

let lastAction = null
let telegramPollTimer = null

watch(emailInput, () => {
  telegramConfirmed.value = false
  telegramDeepLink.value = ''
  telegramStatusText.value = ''
  telegramHint.value = ''
})

function statusLabel(s) {
  const map = {
    processing: 'В обработке',
    confirmed: 'Подтверждён',
    shipped: 'Отправлен',
    delivered: 'Доставлен',
    cancelled: 'Отменён',
  }
  return map[s] || s || '—'
}

function formatDate(iso) {
  if (!iso) return '—'
  try {
    const d = new Date(iso)
    return d.toLocaleString('ru-RU', { dateStyle: 'medium', timeStyle: 'short' })
  } catch {
    return String(iso)
  }
}

function formatMoney(amount, currency) {
  const n = Number(amount)
  const cur = (currency || 'RUB').toUpperCase()
  if (Number.isNaN(n)) return '—'
  return `${n.toLocaleString('ru-RU', { maximumFractionDigits: 2 })} ${cur === 'RUB' ? '₽' : cur}`
}

function orderItems(ord) {
  const items = ord?.items
  return Array.isArray(items) ? items : []
}

function orderPricingType(ord) {
  const serverType = String(ord?.routeType || ord?.orderType || '').toLowerCase()
  if (serverType === 'paid' || serverType === 'free') return serverType
  const amount = Number(ord?.total)
  return Number.isFinite(amount) && amount > 0 ? 'paid' : 'free'
}

function isPaidOrder(ord) {
  return orderPricingType(ord) === 'paid'
}

const filteredOrders = computed(() => {
  const now = Date.now()
  return (orders.value || []).filter((ord) => {
    if (orderTypeFilter.value === 'free' && isPaidOrder(ord)) return false
    if (orderTypeFilter.value === 'paid' && !isPaidOrder(ord)) return false
    if (periodFilter.value !== 'all') {
      const days = Number(periodFilter.value)
      const created = new Date(ord?.createdAt || 0).getTime()
      if (Number.isFinite(days) && created && now - created > days * 24 * 60 * 60 * 1000) return false
    }
    return true
  })
})

const groupedOrders = computed(() => {
  const active = []
  const completed = []
  const cancelled = []
  for (const ord of filteredOrders.value) {
    const st = String(ord?.status || '').toLowerCase()
    if (st === 'cancelled') cancelled.push(ord)
    else if (st === 'delivered') completed.push(ord)
    else active.push(ord)
  }
  return [
    { key: 'active', label: 'Активные', items: active },
    { key: 'completed', label: 'Завершенные', items: completed },
    { key: 'cancelled', label: 'Отмененные', items: cancelled },
  ].filter((g) => g.items.length)
})

function orderTimelineState(statusRaw) {
  const status = String(statusRaw || '').toLowerCase()
  const flow = ['confirmed', 'processing', 'shipped', 'delivered']
  const currentIndex = flow.indexOf(status)
  const cancelled = status === 'cancelled'
  return [
    { key: 'confirmed', label: 'Подтвержден', state: cancelled ? 'done' : currentIndex >= 0 ? 'done' : 'pending' },
    { key: 'processing', label: 'В обработке', state: cancelled ? 'done' : currentIndex >= 1 ? 'done' : 'pending' },
    { key: 'shipped', label: 'Отправлен', state: cancelled ? 'done' : currentIndex >= 2 ? 'done' : 'pending' },
    { key: 'delivered', label: 'Доставлен', state: cancelled ? 'pending' : currentIndex >= 3 ? 'current' : 'pending' },
    { key: 'cancelled', label: 'Отменен', state: cancelled ? 'cancelled' : 'hidden' },
  ].filter((x) => x.state !== 'hidden')
}

function recalcProgressFromOrders() {
  const delivered = orders.value.filter((o) => String(o?.status || '').toLowerCase() === 'delivered')
  const paid = delivered.filter((o) => isPaidOrder(o)).length
  const free = delivered.length - paid
  syncFromCounters({
    visitedPois: visitedPoisCount.value,
    paidRoutes: Math.max(paid, completedPaidRoutesCount.value),
    freeRoutes: Math.max(free, completedFreeRoutesCount.value),
    favoritePois: favoritePoisList.value.length,
  })
}

async function syncProgressToServer() {
  if (!isClientLoggedIn()) return
  try {
    await javaApi.userProgress.sync(getSnapshot())
  } catch (_) {}
}

function itemName(it) {
  return it?.name || it?.title || 'Товар'
}

function itemQty(it) {
  const q = it?.qty ?? it?.quantity ?? 1
  return Number(q) || 1
}

function itemPrice(it) {
  const p = it?.price
  return p != null ? Number(p) : 0
}

async function sendCode() {
  formError.value = ''
  networkError.value = ''
  codeHint.value = ''
  emptyAfterVerify.value = false
  orders.value = []
  lastAction = 'send'

  const email = emailInput.value.trim()
  if (!email) {
    formError.value = 'Укажите email'
    return
  }

  requestLoading.value = true
  try {
    const msg = await javaApi.orders.requestLookupCode(email)
    codeHint.value = msg
    codeRequested.value = true
  } catch (e) {
    formError.value = e?.message || 'Не удалось отправить код'
  } finally {
    requestLoading.value = false
  }
}

async function verifyAndLoad() {
  formError.value = ''
  networkError.value = ''
  codeHint.value = ''
  emptyAfterVerify.value = false
  orders.value = []
  lastAction = 'verify'

  const email = emailInput.value.trim()
  const code = codeInput.value.trim()
  if (!email || !code) {
    formError.value = 'Укажите email и код'
    return
  }
  if (!telegramConfirmed.value) {
    formError.value = 'Сначала подтвердите Telegram-привязку. Без неё доступ к заказам заблокирован.'
    return
  }

  verifyLoading.value = true
  try {
    const list = await javaApi.orders.verifyLookupCode(email, code)
    localStorage.setItem('astra_guest_email', email)
    window.dispatchEvent(new CustomEvent('astra:guest-email-updated', { detail: { email } }))
    orders.value = Array.isArray(list) ? list : []
    emptyAfterVerify.value = orders.value.length === 0
    try {
      const remote = await javaApi.userProgress.getByEmail(email)
      if (remote?.snapshot) {
        mergeSnapshot(remote.snapshot)
      }
    } catch (_) {}
    recalcProgressFromOrders()
    await syncProgressToServer()
  } catch (e) {
    networkError.value = e?.message || 'Не удалось загрузить заказы'
  } finally {
    verifyLoading.value = false
  }
}

function retryLast() {
  networkError.value = ''
  if (lastAction === 'verify') verifyAndLoad()
  else if (lastAction === 'send') sendCode()
}

async function checkTelegramStatus() {
  telegramStatusLoading.value = true
  telegramStatusText.value = ''
  telegramHint.value = ''
  try {
    const data = await javaApi.telegram.getLinkStatus({ email: emailInput.value.trim() })
    telegramConfirmed.value = !!data?.linked
    if (telegramConfirmed.value) {
      const who = data?.username ? `@${data.username}` : (data?.chatId ? `chat_id ${data.chatId}` : 'подключен')
      telegramStatusText.value = `Telegram подтвержден (${who}).`
    } else {
      telegramStatusText.value = 'Telegram пока не подтвержден.'
    }
  } catch (e) {
    telegramStatusText.value = e?.message || 'Не удалось проверить статус Telegram.'
    telegramConfirmed.value = false
  } finally {
    telegramStatusLoading.value = false
  }
}

async function requestTelegramLink() {
  telegramLoading.value = true
  telegramStatusText.value = ''
  telegramHint.value = ''
  telegramDeepLink.value = ''
  try {
    const data = await javaApi.telegram.requestLink({ email: emailInput.value.trim() })
    telegramDeepLink.value = data?.botDeepLink || ''
    const ttl = Number(data?.expiresInSec || 0)
    telegramHint.value = ttl > 0
      ? `Ссылка активна около ${Math.round(ttl / 60)} мин. После подтверждения нажмите «Проверить статус».`
      : 'Ссылка создана. Подтвердите привязку в Telegram.'
    telegramStatusText.value = 'Ссылка для привязки создана.'
  } catch (e) {
    telegramStatusText.value = e?.message || 'Не удалось создать ссылку Telegram.'
  } finally {
    telegramLoading.value = false
  }
}

function startTelegramStatusPolling() {
  if (telegramPollTimer) clearInterval(telegramPollTimer)
  telegramPollTimer = setInterval(async () => {
    await checkTelegramStatus()
    if (telegramConfirmed.value && telegramPollTimer) {
      clearInterval(telegramPollTimer)
      telegramPollTimer = null
    }
  }, 5000)
  setTimeout(async () => { await checkTelegramStatus() }, 1500)
}

onUnmounted(() => {
  if (telegramPollTimer) {
    clearInterval(telegramPollTimer)
    telegramPollTimer = null
  }
})
</script>

<style scoped>
.orders-page { padding-bottom: var(--spacing-2xl); }
.orders-hero { padding: var(--spacing-2xl) 0 var(--spacing-lg); max-width: 640px; }
.orders-lead { color: var(--gray-400); font-size: 0.9rem; line-height: 1.7; margin-top: var(--spacing-md); }

.orders-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--spacing-xl);
  align-items: start;
  max-width: min(42rem, 100%);
}

.orders-form-card { padding: var(--spacing-xl); }
.orders-form-card--full { width: 100%; }

.order-dl {
  display: grid;
  gap: 0.5rem;
  margin: 0.75rem 0 0;
  font-size: 0.875rem;
}
.order-dl div { display: flex; gap: 0.75rem; }
.order-dl dt { color: var(--gray-400); min-width: 5rem; }
.order-dl dd { margin: 0; }

.order-items-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8rem;
}
.order-items-table th,
.order-items-table td {
  padding: 0.4rem 0.5rem;
  border-bottom: 1px solid var(--gray-800);
  text-align: left;
}
.order-items-table th { color: var(--gray-400); font-weight: normal; font-family: var(--font-mono); font-size: 0.65rem; }
.order-filters { display:flex; gap:0.5rem; margin:0.75rem 0 0.25rem; flex-wrap:wrap; }
.orders-group { margin-top:1rem; }
.orders-group-title { color: var(--gray-400); font-size: 0.72rem; margin-bottom: 0.4rem; }
.order-result { margin-top:0.75rem;padding:1rem;border:1px solid var(--gray-700); }
.order-timeline { display:flex; gap:0.35rem; flex-wrap:wrap; margin-top:0.5rem; }
.timeline-chip { padding:0.2rem 0.5rem; border:1px solid var(--gray-700); border-radius:999px; font-size:0.65rem; }
.timeline-chip.done { color:var(--gray-300); }
.timeline-chip.current { border-color:var(--accent); color:var(--accent); }
.timeline-chip.cancelled { border-color:#c55; color:#f09; }

.telegram-bind-block {
  border: 1px dashed var(--gray-700);
  border-radius: var(--radius-sm);
  padding: 0.9rem;
}

.telegram-bind-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 0.75rem;
}

</style>
