<template>
  <div class="page-wrapper orders-page">
    <div class="container">
      <section class="orders-hero">
        <p class="text-mono" style="color:var(--accent)">◫ Кабинет гостя</p>
        <h1>Мои заказы</h1>
        <p class="orders-lead">
          Укажите email, который вы указывали при оформлении заказа. Мы отправим на него одноразовый код;
          после ввода кода покажем все ваши заказы с этим адресом.
        </p>
      </section>

      <div class="orders-layout">
        <div class="orders-form-card card">
          <h2 class="section-heading">Вход по email</h2>

          <form class="contact-form" @submit.prevent="sendCode">
            <div class="form-group">
              <label class="form-label">Email *</label>
              <input
                v-model.trim="emailInput"
                type="email"
                class="form-input"
                placeholder="you@example.com"
                required
                autocomplete="email"
                :disabled="verifyLoading"
              />
            </div>
            <button type="submit" class="btn btn-primary btn-lg" :disabled="requestLoading || verifyLoading">
              <span v-if="requestLoading"><span class="spinner" style="width:14px;height:14px;display:inline-block" /> Отправка…</span>
              <span v-else>Получить код на почту</span>
            </button>
          </form>

          <p v-if="codeHint" class="form-success" style="margin-top:1rem">{{ codeHint }}</p>
          <p v-if="formError" class="form-error" style="margin-top:0.75rem">{{ formError }}</p>

          <div v-if="codeRequested" class="divider" style="margin:1.25rem 0" />

          <form v-if="codeRequested" class="contact-form" @submit.prevent="verifyAndLoad">
            <div class="form-group">
              <label class="form-label">Код из письма *</label>
              <input
                v-model.trim="codeInput"
                type="text"
                inputmode="numeric"
                maxlength="8"
                class="form-input"
                placeholder="6 цифр"
                required
                autocomplete="one-time-code"
              />
            </div>
            <button type="submit" class="btn btn-accent btn-lg" :disabled="verifyLoading || requestLoading">
              <span v-if="verifyLoading"><span class="spinner" style="width:14px;height:14px;display:inline-block" /> Загрузка…</span>
              <span v-else>Показать заказы</span>
            </button>
          </form>

          <div v-if="networkError" class="form-error" style="margin-top:1rem">{{ networkError }}</div>
          <button
            v-if="networkError"
            type="button"
            class="btn btn-ghost"
            style="margin-top:0.5rem"
            @click="retryLast"
          >
            Попробовать снова
          </button>

          <div v-if="orders.length" class="orders-list" style="margin-top:1.5rem">
            <h3 class="section-heading" style="font-size:1rem">Ваши заказы ({{ orders.length }})</h3>
            <div
              v-for="(ord, idx) in orders"
              :key="ord.orderId || ord.id || idx"
              class="order-result card"
              style="margin-top:1rem;padding:1rem;border:1px solid var(--gray-700)"
            >
              <h4 class="section-heading" style="font-size:0.95rem">Заказ {{ ord.orderId }}</h4>
              <dl class="order-dl">
                <div><dt>Дата</dt><dd>{{ formatDate(ord.createdAt) }}</dd></div>
                <div><dt>Статус</dt><dd>{{ statusLabel(ord.status) }}</dd></div>
                <div><dt>Сумма</dt><dd>{{ formatMoney(ord.total, ord.currency) }}</dd></div>
              </dl>
              <div v-if="orderItems(ord).length" class="order-items">
                <p class="text-mono" style="color:var(--gray-400);font-size:0.7rem;margin-bottom:0.5rem">Состав заказа</p>
                <table class="order-items-table">
                  <thead><tr><th>Товар</th><th>Кол-во</th><th>Цена</th></tr></thead>
                  <tbody>
                    <tr v-for="(it, i) in orderItems(ord)" :key="i">
                      <td>{{ itemName(it) }}</td>
                      <td>{{ itemQty(it) }}</td>
                      <td>{{ formatMoney(itemPrice(it), ord.currency) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <p v-if="emptyAfterVerify" class="form-error" style="margin-top:1rem">
            Список пуст. Если заказ оформляли без email, свяжитесь с поддержкой через раздел «Контакты».
          </p>
        </div>

        <aside class="progress-card card">
          <h2 class="section-heading">Мой прогресс</h2>
          <p class="orders-lead" style="font-size:0.85rem">
            Считается локально в браузере: открытые точки на карте и отмеченные маршруты.
          </p>
          <ul class="progress-stats">
            <li>Пройдено маршрутов: <strong>{{ completedRoutesCount }}</strong></li>
            <li>Посещено точек: <strong>{{ visitedPoisCount }}</strong></li>
          </ul>
          <div class="tier-badges">
            <span :class="['tier-badge', routeTier === 'bronze' ? 'active' : '']">Bronze · от 1 маршрута</span>
            <span :class="['tier-badge', routeTier === 'silver' ? 'active' : '']">Silver · от 5</span>
            <span :class="['tier-badge', routeTier === 'gold' ? 'active' : '']">Gold · от 10</span>
          </div>
          <router-link to="/routes" class="btn btn-ghost btn-sm" style="margin-top:1rem;display:inline-block">К маршрутам</router-link>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { javaApi } from '@/api/backend.js'
import { useGuestProgress } from '@/composables/useGuestProgress.js'

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

const { visitedPoisCount, completedRoutesCount, routeTier } = useGuestProgress()

let lastAction = null

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

  verifyLoading.value = true
  try {
    const list = await javaApi.orders.verifyLookupCode(email, code)
    orders.value = Array.isArray(list) ? list : []
    emptyAfterVerify.value = orders.value.length === 0
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
</script>

<style scoped>
.orders-page { padding-bottom: var(--spacing-2xl); }
.orders-hero { padding: var(--spacing-2xl) 0 var(--spacing-lg); max-width: 640px; }
.orders-lead { color: var(--gray-400); font-size: 0.9rem; line-height: 1.7; margin-top: var(--spacing-md); }

.orders-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: var(--spacing-xl);
  align-items: start;
}

.orders-form-card { padding: var(--spacing-xl); }
.progress-card { padding: var(--spacing-xl); }

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

.progress-stats { list-style: none; padding: 0; margin: 1rem 0; color: var(--gray-300); font-size: 0.9rem; }
.progress-stats li { margin-bottom: 0.35rem; }

.tier-badges { display: flex; flex-direction: column; gap: 0.35rem; }
.tier-badge {
  font-size: 0.7rem;
  font-family: var(--font-mono);
  padding: 0.35rem 0.5rem;
  border: 1px solid var(--gray-700);
  border-radius: var(--radius-sm);
  color: var(--gray-500);
}
.tier-badge.active {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(200, 169, 110, 0.08);
}

@media (max-width: 900px) {
  .orders-layout { grid-template-columns: 1fr; }
}
</style>
