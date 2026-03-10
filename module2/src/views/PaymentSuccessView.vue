<template>
  <div class="page-wrapper payment-success-page">
    <div class="container narrow">
      <section class="success-block">
        <p class="text-mono" style="color:var(--accent)">◇ Оплата</p>
        <h1>Спасибо за заказ</h1>
        <p v-if="orderId" class="order-id">Заказ <strong>{{ orderId }}</strong></p>
        <div v-if="paid" class="status-badge paid">Оплачено</div>
        <p v-else-if="orderId" class="status-note">Оплата зарегистрирована. Заказ передан в обработку.</p>
        <router-link to="/shop" class="btn btn-primary">Вернуться в магазин</router-link>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { javaApi } from '@/api/backend.js'

const route = useRoute()
const orderId = ref(route.query.orderId || '')
const paid = ref(false)

onMounted(async () => {
  if (!orderId.value) return
  // В режиме stub можно симулировать успешную оплату через webhook
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
.payment-success-page { padding: var(--spacing-2xl) 0; }
.narrow { max-width: 480px; margin: 0 auto; }
.success-block {
  text-align: center;
  padding: var(--spacing-2xl);
  background: var(--gray-800);
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-md);
}
.success-block h1 { margin: var(--spacing-sm) 0 var(--spacing-md); }
.order-id { font-size: 1.1rem; margin-bottom: var(--spacing-md); }
.status-badge.paid {
  display: inline-block;
  padding: 0.35rem 0.75rem;
  background: rgba(76, 175, 80, 0.2);
  color: var(--success, #4caf50);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  margin-bottom: var(--spacing-lg);
}
.status-note { color: var(--gray-400); margin-bottom: var(--spacing-lg); }
</style>
