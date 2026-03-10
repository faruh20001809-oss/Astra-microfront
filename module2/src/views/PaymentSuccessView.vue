<template>
  <div class="page-wrapper payment-success-page">
    <div class="success-container">
      <!-- Иконка успеха -->
      <div class="success-icon-wrap" :class="{ done: iconDone }">
        <svg class="success-icon" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle class="circle" cx="40" cy="40" r="36" stroke-width="3" />
          <path class="check" d="M22 40 L34 52 L58 26" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </div>

      <p class="success-label text-mono">Оплата</p>
      <h1 class="success-title">Спасибо за заказ</h1>
      <p class="success-lead">Ваш платёж принят. Заказ передан в обработку.</p>

      <!-- Карточка с номером заказа -->
      <div v-if="orderId" class="order-card">
        <span class="order-card-label text-mono">Номер заказа</span>
        <span class="order-id">{{ orderId }}</span>
        <button type="button" class="copy-btn text-mono" @click="copyOrderId" :aria-label="copyLabel">
          {{ copyLabel }}
        </button>
      </div>

      <!-- Статус оплаты -->
      <div v-if="orderId" class="status-row">
        <span v-if="paid" class="status-badge paid">Оплачено</span>
        <span v-else class="status-badge processing">Обрабатывается</span>
      </div>

      <!-- Что дальше -->
      <div class="next-steps">
        <p class="text-mono next-steps-label">Что дальше?</p>
        <ul class="next-steps-list">
          <li>Подтверждение придёт на указанную почту</li>
          <li>Заказ соберут и отправят в течение 1–3 рабочих дней</li>
        </ul>
      </div>

      <!-- Кнопки -->
      <div class="actions">
        <router-link to="/shop" class="btn btn-primary btn-lg">Вернуться в магазин</router-link>
        <router-link to="/" class="btn btn-secondary">На карту</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { javaApi } from '@/api/backend.js'

const route = useRoute()
const orderId = ref(route.query.orderId || '')
const paid = ref(false)
const iconDone = ref(false)
const copied = ref(false)

const copyLabel = computed(() => copied.value ? 'Скопировано' : 'Копировать')

function copyOrderId() {
  if (!orderId.value) return
  navigator.clipboard?.writeText(orderId.value).then(() => {
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  })
}

onMounted(async () => {
  setTimeout(() => { iconDone.value = true }, 100)
  if (!orderId.value) return
  const isStub = route.query.stub === '1'
  if (isStub) {
    try {
      await fetch('/java-api/api/v1/payment/webhook', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ orderId: orderId.value, success: true })
      })
    } catch (_) { /* ignore */ }
  }
  try {
    const status = await javaApi.orders.getPaymentStatus(orderId.value)
    paid.value = status.paid === true
  } catch (_) { /* ignore */ }
})
</script>

<style scoped>
.payment-success-page {
  min-height: calc(100dvh - var(--nav-h, 64px));
  padding: var(--spacing-2xl) var(--spacing-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, var(--paper) 0%, var(--gray-50) 100%);
}

.success-container {
  max-width: 420px;
  margin: 0 auto;
  text-align: center;
}

/* Иконка */
.success-icon-wrap {
  margin: 0 auto var(--spacing-xl);
  width: 88px;
  height: 88px;
}

.success-icon {
  width: 100%;
  height: 100%;
}

.success-icon .circle {
  stroke: var(--accent);
  stroke-dasharray: 226;
  stroke-dashoffset: 226;
  transition: stroke-dashoffset 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.success-icon-wrap.done .circle {
  stroke-dashoffset: 0;
}

.success-icon .check {
  stroke: var(--success, #1a6b3a);
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  transition: stroke-dashoffset 0.35s cubic-bezier(0.4, 0, 0.2, 1) 0.2s;
}

.success-icon-wrap.done .check {
  stroke-dashoffset: 0;
}

.success-label {
  color: var(--accent);
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: var(--spacing-xs);
}

.success-title {
  font-family: var(--font-display);
  font-size: clamp(1.75rem, 5vw, 2.25rem);
  font-weight: 600;
  color: var(--ink);
  margin-bottom: var(--spacing-sm);
  line-height: 1.2;
}

.success-lead {
  color: var(--gray-600);
  font-size: 1rem;
  margin-bottom: var(--spacing-xl);
  line-height: 1.5;
}

/* Карточка заказа */
.order-card {
  background: var(--white);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  box-shadow: var(--shadow-subtle);
}

.order-card-label {
  display: block;
  font-size: 0.7rem;
  color: var(--gray-500);
  letter-spacing: 0.06em;
  margin-bottom: var(--spacing-xs);
}

.order-id {
  display: block;
  font-family: var(--font-mono);
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: var(--spacing-sm);
}

.copy-btn {
  background: none;
  border: none;
  color: var(--accent);
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0;
  text-underline-offset: 2px;
}

.copy-btn:hover {
  text-decoration: underline;
}

/* Статус */
.status-row {
  margin-bottom: var(--spacing-xl);
}

.status-badge {
  display: inline-block;
  padding: 0.4rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  font-weight: 500;
}

.status-badge.paid {
  background: rgba(26, 107, 58, 0.12);
  color: var(--success, #1a6b3a);
}

.status-badge.processing {
  background: rgba(200, 169, 110, 0.15);
  color: var(--accent-dark, #8a6e3a);
}

/* Что дальше */
.next-steps {
  text-align: left;
  background: var(--gray-50);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.next-steps-label {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin-bottom: var(--spacing-sm);
}

.next-steps-list {
  list-style: none;
  font-size: 0.9rem;
  color: var(--gray-700);
  line-height: 1.7;
}

.next-steps-list li {
  padding-left: 1.25rem;
  position: relative;
}

.next-steps-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.55em;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--accent);
}

/* Кнопки */
.actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  align-items: center;
}

.actions .btn-lg {
  min-width: 220px;
}

/* Вторичная кнопка на светлом фоне — не сливается с фоном */
.actions .btn-secondary {
  background: transparent;
  color: var(--ink);
  border: 1px solid var(--gray-400);
}

.actions .btn-secondary:hover {
  border-color: var(--ink);
  color: var(--ink);
}

@media (max-width: 480px) {
  .payment-success-page { padding: var(--spacing-lg) var(--spacing-md); padding-bottom: calc(var(--spacing-lg) + env(safe-area-inset-bottom, 0)); }
  .success-container { width: 100%; padding: 0; }
  .success-icon-wrap { width: 72px; height: 72px; margin-bottom: var(--spacing-lg); }
  .success-title { font-size: 1.5rem; }
  .actions { width: 100%; }
  .actions .btn { width: 100%; max-width: 100%; min-width: 0; }
  .copy-btn { min-height: 44px; padding: 0.5rem 0; display: inline-flex; align-items: center; justify-content: center; }
}
</style>
