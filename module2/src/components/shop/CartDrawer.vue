<template>
  <Drawer
      v-model:visible="cartStore.isOpen"
      position="right"
      :modal="true"
      :dismissable="true"
      :showCloseIcon="true"
      class="cart-drawer-pv"
  >
    <template #header>
      <span class="cart-title">Корзина</span>
    </template>

    <!-- Empty state -->
    <div v-if="!cartStore.items.length" class="cart-empty">
      <span class="cart-empty-icon" aria-hidden="true">◻</span>
      <p class="cart-empty-title">Корзина пуста</p>
      <p class="cart-empty-hint">Добавьте товары из магазина — сувениры и мерч с историческими мотивами Астрахани.</p>
      <Button label="Перейти в магазин" class="p-button-text p-button-secondary" @click="cartStore.isOpen = false; $router.push('/shop')" />
    </div>

    <!-- Cart with items: scrollable body -->
    <div v-else class="cart-body">
        <div class="cart-items">
              <div
                  v-for="item in cartStore.items"
                  :key="item.id"
                  class="cart-item"
              >
                <div class="cart-item-img">
                  <span>{{ item.emoji || '◻' }}</span>
                </div>
                <div class="cart-item-info">
                  <p class="cart-item-name">{{ item.name }}</p>
                  <p v-if="item.variant" class="cart-item-variant">{{ item.variant }}</p>
                  <p class="cart-item-price">{{ item.price }} ₽</p>
                </div>
                <div class="cart-item-controls">
                  <button class="qty-btn" @click="cartStore.updateQty(item.id, item.qty - 1)">−</button>
                  <span class="qty-val">{{ item.qty }}</span>
                  <button class="qty-btn" @click="cartStore.updateQty(item.id, item.qty + 1)">+</button>
                </div>
                <button class="cart-item-remove" @click="cartStore.removeItem(item.id)">✕</button>
              </div>
            </div>

            <div class="cart-footer">
              <p class="cart-section-label">Данные для доставки</p>
              <div class="customer-form">
            <div class="form-row">
              <div class="form-group">
                <label for="firstName" class="form-label">Имя *</label>
                <input
                    id="firstName"
                    v-model="customerForm.firstName"
                    type="text"
                    placeholder="Иван"
                    :class="{ error: errors.firstName }"
                    @blur="validateField('firstName')"
                />
                <span v-if="errors.firstName" class="error-msg">{{ errors.firstName }}</span>
              </div>
              <div class="form-group">
                <label for="lastName" class="form-label">Фамилия *</label>
                <input
                    id="lastName"
                    v-model="customerForm.lastName"
                    type="text"
                    placeholder="Иванов"
                    :class="{ error: errors.lastName }"
                    @blur="validateField('lastName')"
                />
                <span v-if="errors.lastName" class="error-msg">{{ errors.lastName }}</span>
              </div>
            </div>

            <div class="form-group">
              <label for="phone" class="form-label">Телефон *</label>
              <input
                  id="phone"
                  v-model="customerForm.phone"
                  type="tel"
                  placeholder="+7 (999) 123-45-67"
                  :class="{ error: errors.phone }"
                  @input="formatPhone"
                  @blur="validateField('phone')"
              />
              <span v-if="errors.phone" class="error-msg">{{ errors.phone }}</span>
            </div>

            <div class="form-group">
              <label for="email" class="form-label">Email</label>
              <input
                  id="email"
                  v-model="customerForm.email"
                  type="email"
                  placeholder="example@mail.ru"
                  :class="{ error: errors.email }"
              />
            </div>

            <div class="form-group newsletter-opt">
              <label class="checkbox-inline">
                <input v-model="newsletterSubscribe" type="checkbox" />
                <span>Уведомлять о новых точках на карте по email</span>
              </label>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label for="city" class="form-label">Город *</label>
                <input
                    id="city"
                    v-model="customerForm.city"
                    type="text"
                    placeholder="Астрахань"
                    :class="{ error: errors.city }"
                    @blur="validateField('city')"
                />
                <span v-if="errors.city" class="error-msg">{{ errors.city }}</span>
              </div>
              <div class="form-group">
                <label for="zip" class="form-label">Индекс</label>
                <input
                    id="zip"
                    v-model="customerForm.zip"
                    type="text"
                    placeholder="414000"
                    maxlength="6"
                />
              </div>
            </div>

            <div v-if="delivery !== 'pickup'" class="form-group">
              <label for="address" class="form-label">Адрес доставки *</label>
              <input
                  id="address"
                  v-model="customerForm.address"
                  type="text"
                  placeholder="ул. Кремлёвская, д. 1, кв. 10"
                  :class="{ error: errors.address }"
                  @blur="validateField('address')"
              />
              <span v-if="errors.address" class="error-msg">{{ errors.address }}</span>
            </div>

            <div v-else class="form-group pickup-block">
              <label for="pickupPoint" class="form-label">Пункт самовывоза *</label>
              <select
                  v-if="!pickupPointsLoading && pickupPoints.length"
                  id="pickupPoint"
                  v-model="selectedPickupId"
                  class="form-select"
                  :class="{ error: errors.pickupPointId }"
                  @change="validateField('pickupPointId')"
              >
                <option v-for="p in pickupPoints" :key="p.id" :value="p.id">{{ p.label }}</option>
              </select>
              <p v-else-if="pickupPointsLoading" class="pickup-hint text-mono">Загрузка пунктов…</p>
              <p v-else class="error-msg">Пункты самовывоза недоступны. Проверьте подключение к серверу.</p>
              <template v-if="!pickupPointsLoading && pickupPoints.length">
                <p class="pickup-hint text-mono">
                  Забрать заказ можно по адресу: {{ selectedPickupAddress }}
                </p>
                <p v-if="selectedPickupHours" class="pickup-hint text-mono">{{ selectedPickupHours }}</p>
              </template>
              <span v-if="errors.pickupPointId" class="error-msg">{{ errors.pickupPointId }}</span>
            </div>
          </div>

          <!-- Delivery & totals -->
          <div class="delivery-info">
            <span class="text-mono" style="color:var(--gray-400)">Доставка</span>
            <span>{{ deliveryPrice === 0 ? 'Бесплатно' : deliveryPrice + ' ₽' }}</span>
          </div>

          <div class="cart-total-row">
            <span class="cart-total-label">Итого</span>
            <span class="cart-total-price">{{ cartStore.totalPrice + deliveryPrice }} ₽</span>
          </div>

          <!-- Delivery method -->
          <div class="form-group">
            <label class="form-label">Способ доставки</label>
            <select v-model="delivery" class="form-select">
              <option value="pickup">Самовывоз — бесплатно</option>
              <option value="courier">Курьер по городу — 300 ₽</option>
              <option value="post">Почта России — 450 ₽</option>
            </select>
          </div>

          <!-- Payment method (опционально) -->
          <div class="form-group">
            <label class="form-label">Оплата</label>
            <div class="radio-inline">
              <label :class="{ active: paymentMethod === 'card' }">
                <input type="radio" v-model="paymentMethod" value="card" />
                <span>💳 Карта</span>
              </label>
              <label :class="{ active: paymentMethod === 'cash' }">
                <input type="radio" v-model="paymentMethod" value="cash" />
                <span>💵 При получении</span>
              </label>
            </div>
          </div>

              <Button label="Очистить корзину" class="p-button-text p-button-secondary p-button-sm" @click="cartStore.clearCart()" />

              <p class="consent-text">
                Нажимая кнопку ниже, вы соглашаетесь с
                <a href="/privacy" target="_blank">политикой конфиденциальности</a>
              </p>
            </div>
      </div>

    <!-- Кнопка оплаты в футере Drawer — всегда видна -->
    <template #footer>
      <Button
          v-if="cartStore.items.length"
          :label="checkoutLoading ? 'Обработка…' : `Оформить заказ — ${cartStore.totalPrice + deliveryPrice} ₽`"
          :loading="checkoutLoading"
          :disabled="!isFormValid"
          class="w-full cart-checkout-btn"
          @click="checkout"
      />
    </template>
  </Drawer>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useCartStore, useToastStore } from '@/store/index.js'
import { javaApi } from '@/api/backend'

/** Пункты выдачи с бэкенда (тот же список, что в модалке заказа Java-админки) */
const pickupPoints = ref([])
const pickupPointsLoading = ref(false)

const cartStore = useCartStore()
const toastStore = useToastStore()

// Delivery & payment
const delivery = ref('pickup')
const selectedPickupId = ref('')
const paymentMethod = ref('card')
const checkoutLoading = ref(false)
const newsletterSubscribe = ref(false)

// Customer form
const customerForm = ref({
  firstName: '',
  lastName: '',
  phone: '',
  email: '',
  city: 'Астрахань',
  address: '',
  zip: ''
})

// Validation errors
const errors = ref({})

// Computed
const deliveryPrice = computed(() => {
  if (delivery.value === 'courier') return 300
  if (delivery.value === 'post') return 450
  return 0
})

const selectedPickupAddress = computed(() => {
  const p = pickupPoints.value.find((x) => x.id === selectedPickupId.value)
  return p?.address || ''
})

const selectedPickupHours = computed(() => {
  const p = pickupPoints.value.find((x) => x.id === selectedPickupId.value)
  return p?.hours || ''
})

function syncPickupSelection() {
  if (!pickupPoints.value.length) return
  if (!pickupPoints.value.some((x) => x.id === selectedPickupId.value)) {
    selectedPickupId.value = pickupPoints.value[0].id
  }
}

async function loadPickupPoints() {
  pickupPointsLoading.value = true
  try {
    const list = await javaApi.pickupPoints.getList()
    if (Array.isArray(list) && list.length > 0) {
      pickupPoints.value = list.map((p) => ({
        id: p.id,
        label: p.label,
        address: p.address || '',
        hours: p.hours || ''
      }))
      syncPickupSelection()
    }
  } catch (e) {
    console.warn('pickup points:', e)
    toastStore.push('Не удалось загрузить пункты самовывоза', 'error')
  } finally {
    pickupPointsLoading.value = false
  }
}

onMounted(() => {
  loadPickupPoints()
})

watch(
  () => cartStore.isOpen,
  (open) => {
    if (open && !pickupPoints.value.length && !pickupPointsLoading.value) {
      loadPickupPoints()
    }
  }
)

const isFormValid = computed(() => {
  const c = customerForm.value
  if (!c.firstName.trim() || !c.lastName.trim() || !c.phone.trim() || !c.city.trim()) {
    return false
  }
  if (delivery.value === 'pickup') {
    if (pickupPointsLoading.value || !pickupPoints.value.length) return false
    return !!selectedPickupId.value
  }
  return !!c.address.trim()
})

watch(delivery, (v) => {
  if (v === 'pickup') {
    errors.value.address = ''
  }
})

// Phone formatting: +7 (XXX) XXX-XX-XX
function formatPhone() {
  let digits = customerForm.value.phone.replace(/\D/g, '')
  if (digits.startsWith('8')) digits = '7' + digits.slice(1)
  if (!digits.startsWith('7')) digits = '7' + digits

  let f = '+' + digits
  if (digits.length > 1) f = '+7 (' + digits.slice(1, 4)
  if (digits.length > 4) f = '+7 (' + digits.slice(1, 4) + ') ' + digits.slice(4, 7)
  if (digits.length > 7) f += '-' + digits.slice(7, 9)
  if (digits.length > 9) f += '-' + digits.slice(9, 11)

  customerForm.value.phone = f.slice(0, 18)
}

// Validate single field
function validateField(field) {
  const value = customerForm.value[field]?.trim()

  switch (field) {
    case 'firstName':
    case 'lastName':
      errors.value[field] = !value ? 'Обязательное поле' : ''
      break
    case 'phone':
      if (!value) errors.value.phone = 'Обязательное поле'
      else if (value.replace(/\D/g, '').length < 11) errors.value.phone = 'Неверный формат'
      else errors.value.phone = ''
      break
    case 'city':
      errors.value[field] = !value ? 'Обязательное поле' : ''
      break
    case 'address':
      if (delivery.value === 'pickup') {
        errors.value.address = ''
      } else {
        errors.value[field] = !value ? 'Обязательное поле' : ''
      }
      break
    case 'pickupPointId':
      if (delivery.value === 'pickup' && !selectedPickupId.value) {
        errors.value.pickupPointId = 'Выберите пункт самовывоза'
      } else {
        errors.value.pickupPointId = ''
      }
      break
    case 'email':
      errors.value.email = value && !/^\S+@\S+\.\S+$/.test(value) ? 'Некорректный email' : ''
      break
  }
}

// Full form validation
function validateForm() {
  ;['firstName', 'lastName', 'phone', 'city'].forEach(validateField)
  if (delivery.value === 'pickup') {
    validateField('pickupPointId')
    errors.value.address = ''
  } else {
    validateField('address')
    errors.value.pickupPointId = ''
  }
  validateField('email')
  return !Object.values(errors.value).some(e => e)
}

// Checkout handler
async function checkout() {
  if (!validateForm()) {
    toastStore.push('Заполните обязательные поля', 'error')
    // Scroll to first error
    const firstError = Object.entries(errors.value).find(([_, msg]) => msg)
    if (firstError) {
      document.getElementById(firstError[0])?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    return
  }

  if (cartStore.items.length === 0) {
    toastStore.push('Корзина пуста', 'error')
    return
  }

  checkoutLoading.value = true

  const ORDER_TIMEOUT_MS = 20000
  const timeoutPromise = new Promise((_, reject) =>
    setTimeout(() => reject(new Error('Сервер не ответил. Проверьте подключение и попробуйте снова.')), ORDER_TIMEOUT_MS)
  )

  try {
    const isPickup = delivery.value === 'pickup'
    const pickupAddr = selectedPickupAddress.value
    const orderPayload = {
      userId: 'guest-' + Date.now(),
      shippingAddress: {
        firstName: customerForm.value.firstName.trim(),
        lastName: customerForm.value.lastName.trim(),
        phone: customerForm.value.phone.trim(),
        email: customerForm.value.email?.trim() || null,
        city: customerForm.value.city.trim(),
        address: isPickup ? '' : customerForm.value.address.trim(),
        zip: customerForm.value.zip?.trim() || null,
        ...(isPickup
          ? {
              pickupPointId: selectedPickupId.value,
              pickupAddress: pickupAddr,
            }
          : {}),
      },
      shippingMethod: delivery.value,
      pickupPointId: isPickup ? selectedPickupId.value : undefined,
      pickupAddress: isPickup ? pickupAddr : undefined,
      paymentMethod: paymentMethod.value,
      items: cartStore.items.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        qty: item.qty || 1,
        category: item.category
      })),
      newsletterSubscribe:
        newsletterSubscribe.value && !!(customerForm.value.email && customerForm.value.email.trim()),
    }

    const result = await Promise.race([
      javaApi.orders.create(orderPayload),
      timeoutPromise
    ])
    const orderId = result?.orderId ?? result?.data?.orderId ?? ''

    // Оплата картой: редирект на страницу оплаты или на страницу успеха с номером заказа (если касса не привязана)
    if (paymentMethod.value === 'card' && orderId) {
      try {
        const base = window.location.origin + (import.meta.env.BASE_URL || '/').replace(/\/$/, '')
        const paymentData = await javaApi.orders.createPaymentLink(orderId, {
          returnUrl: `${base}/payment/success`,
          cancelUrl: `${base}/shop`
        })
        cartStore.clearCart()
        cartStore.isOpen = false
        customerForm.value = { firstName: '', lastName: '', phone: '', email: '', city: 'Астрахань', address: '', zip: '' }
        selectedPickupId.value = pickupPoints.value[0]?.id || ''
        newsletterSubscribe.value = false
        delivery.value = 'pickup'
        paymentMethod.value = 'card'
        window.location.href = paymentData.redirectUrl
        return
      } catch (payErr) {
        console.warn('Payment link failed, order created:', payErr)
        // Пока касса не привязана — показываем страницу успешной оплаты с id заказа
        cartStore.clearCart()
        cartStore.isOpen = false
        customerForm.value = { firstName: '', lastName: '', phone: '', email: '', city: 'Астрахань', address: '', zip: '' }
        selectedPickupId.value = pickupPoints.value[0]?.id || ''
        newsletterSubscribe.value = false
        delivery.value = 'pickup'
        paymentMethod.value = 'card'
        const base = (import.meta.env.BASE_URL || '/').replace(/\/$/, '') || ''
        window.location.href = `${base ? base + '/' : '/'}payment/success?orderId=${encodeURIComponent(orderId)}`
        return
      }
    }

    toastStore.push(orderId ? `✅ Заказ #${orderId} оформлен!` : '✅ Заказ оформлен!', 'success', 6000)
    cartStore.clearCart()
    cartStore.isOpen = false

    // Reset form
    customerForm.value = {
      firstName: '', lastName: '', phone: '', email: '',
      city: 'Астрахань', address: '', zip: ''
    }
    selectedPickupId.value = pickupPoints.value[0]?.id || ''
    newsletterSubscribe.value = false
    delivery.value = 'pickup'
    paymentMethod.value = 'card'

  } catch (err) {
    console.error('Order error:', err)
    toastStore.push('❌ Ошибка: ' + err.message, 'error')
  } finally {
    checkoutLoading.value = false
  }
}
</script>

<style scoped>
.cart-drawer-pv .p-drawer-content { display: flex; flex-direction: column; overflow: hidden; }
.cart-drawer-pv .cart-body { flex: 1; min-height: 0; overflow-y: auto; -webkit-overflow-scrolling: touch; display: flex; flex-direction: column; }
.cart-drawer-pv .cart-checkout-btn { width: 100%; }
.cart-drawer-pv .p-drawer-footer { padding: var(--spacing-md); padding-bottom: calc(var(--spacing-md) + env(safe-area-inset-bottom, 0)); border-top: 1px solid var(--gray-600); }

.cart-title { font-family: var(--font-display); font-size: 1.25rem; }
.cart-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: var(--spacing-md); color: var(--gray-400);
}
.cart-empty-icon { font-size: 3rem; opacity: 0.35; }
.cart-empty-title { font-family: var(--font-display); font-size: 1.1rem; margin: 0; color: var(--paper); }
.cart-empty-hint { font-size: 0.8rem; color: var(--gray-400); max-width: 260px; margin: 0; line-height: 1.5; }
.cart-section-label {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gray-400);
  margin-bottom: var(--spacing-sm);
  margin-top: 0;
}

.cart-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  display: flex;
  flex-direction: column;
}

.cart-items {
  padding: var(--spacing-md) var(--spacing-xl);
  display: flex; flex-direction: column; gap: var(--spacing-md);
  flex-shrink: 0;
}

.cart-item {
  display: flex; align-items: center; gap: var(--spacing-sm);
  padding: var(--spacing-md) 0; border-bottom: 1px solid var(--gray-800);
}
.cart-item-img {
  width: 48px; height: 48px; background: var(--ink);
  border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.5rem; flex-shrink: 0;
}
.cart-item-info { flex: 1; min-width: 0; }
.cart-item-name { font-size: 0.8rem; line-height: 1.3; }
.cart-item-variant { font-size: 0.65rem; color: var(--gray-400); margin-top: 2px; }
.cart-item-price { font-family: var(--font-mono); font-size: 0.8rem; color: var(--accent); margin-top: 4px; }

.cart-item-controls { display: flex; align-items: center; gap: 6px; }
.qty-btn {
  width: 24px; height: 24px; background: var(--ink);
  border: 1px solid var(--gray-600); color: var(--paper);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; border-radius: var(--radius-sm); font-size: 0.85rem;
  transition: all var(--transition);
}
.qty-btn:hover { border-color: var(--accent); color: var(--accent); }
.qty-val { font-family: var(--font-mono); font-size: 0.8rem; min-width: 20px; text-align: center; }

.cart-item-remove {
  background: transparent; border: none; color: var(--gray-600);
  cursor: pointer; font-size: 0.75rem; padding: 4px;
  transition: color var(--transition);
}
.cart-item-remove:hover { color: var(--danger); }

.cart-footer {
  padding: var(--spacing-lg) var(--spacing-xl);
  border-top: 1px solid var(--gray-600);
  display: flex; flex-direction: column; gap: var(--spacing-md);
  flex-shrink: 0;
}
.cart-checkout-btn { width: 100%; justify-content: center; }
.delivery-info {
  display: flex; justify-content: space-between;
  font-size: 0.75rem; color: var(--gray-400);
}
.cart-total-row {
  display: flex; justify-content: space-between; align-items: baseline;
}
.cart-total-label { font-family: var(--font-mono); font-size: 0.8rem; color: var(--gray-400); }
.cart-total-price { font-family: var(--font-mono); font-size: 1.5rem; color: var(--paper); }
.w-full { width: 100%; justify-content: center; }

/* ===== Customer form styles ===== */
.customer-form {
  padding: var(--spacing-md) 0;
  border-top: 1px solid var(--gray-700);
  border-bottom: 1px solid var(--gray-700);
  margin: var(--spacing-md) 0;
}

.form-row {
  display: grid; grid-template-columns: 1fr 1fr; gap: var(--spacing-sm);
}

.form-group { margin-bottom: var(--spacing-sm); }

.form-label {
  display: block; font-family: var(--font-mono);
  font-size: 0.65rem; color: var(--gray-400);
  margin-bottom: 0.25rem; text-transform: uppercase; letter-spacing: 0.04em;
}

.form-group input, .form-select {
  width: 100%; padding: 0.5rem 0.75rem;
  background: var(--gray-900); border: 1px solid var(--gray-700);
  border-radius: var(--radius-sm); color: var(--paper);
  font-size: 0.85rem; transition: border-color 0.2s;
}
.form-group input:focus, .form-select:focus {
  outline: none; border-color: var(--accent);
}
.form-group input.error { border-color: #ef4444; }

.error-msg {
  display: block; font-size: 0.65rem; color: #ef4444;
  margin-top: 0.2rem; min-height: 1em;
}

/* Radio inline for payment */
.radio-inline {
  display: flex; gap: var(--spacing-sm);
}
.radio-inline label {
  display: flex; align-items: center; gap: 0.35rem;
  padding: 0.4rem 0.75rem; background: var(--gray-900);
  border: 1px solid var(--gray-700); border-radius: var(--radius-sm);
  font-size: 0.75rem; cursor: pointer; transition: all 0.2s;
}
.radio-inline label.active {
  border-color: var(--accent); background: rgba(200,169,110,0.08);
}
.radio-inline input { display: none; }

/* Consent text */
.consent-text {
  font-size: 0.65rem; color: var(--gray-500);
  text-align: center; margin-top: var(--spacing-sm);
}
.consent-text a { color: var(--gray-400); text-decoration: underline; }

/* Desktop: PrimeVue default is 20rem — widen so line items + two-column form fit */
@media (min-width: 769px) {
  .cart-drawer-pv .p-drawer {
    width: min(32rem, 90vw);
  }
}

/* Mobile responsive */
@media (max-width: 768px) {
  .cart-drawer-pv .p-drawer { width: 100%; max-width: 100vw; }
  .cart-body { min-height: 0; }
  .cart-items {
    padding: var(--spacing-md) var(--spacing-lg);
    padding-left: env(safe-area-inset-left, var(--spacing-lg));
    padding-right: env(safe-area-inset-right, var(--spacing-lg));
  }
  .cart-footer {
    padding: var(--spacing-lg);
    padding-left: env(safe-area-inset-left, var(--spacing-lg));
    padding-right: env(safe-area-inset-right, var(--spacing-lg));
  }
  .cart-drawer-pv .p-drawer-footer {
    padding-left: env(safe-area-inset-left, var(--spacing-lg));
    padding-right: env(safe-area-inset-right, var(--spacing-lg));
  }
  .cart-item { padding: var(--spacing-sm) 0; }
  .qty-btn {
    min-width: 36px;
    min-height: 36px;
    width: 36px;
    height: 36px;
  }
  .form-group input,
  .form-select {
    min-height: 44px;
    font-size: 16px;
  }
  .radio-inline { flex-wrap: wrap; }
  .radio-inline label { min-height: 44px; }
}

@media (max-width: 480px) {
  .form-row { grid-template-columns: 1fr; }
  .cart-footer { padding: var(--spacing-md); }
}

.newsletter-opt { margin-bottom: 0.5rem; }
.checkbox-inline {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--gray-300);
  cursor: pointer;
  line-height: 1.4;
}
.checkbox-inline input { margin-top: 0.2rem; flex-shrink: 0; }

.pickup-block { margin-top: var(--spacing-sm); }
.pickup-hint {
  margin: var(--spacing-sm) 0 0;
  font-size: 0.72rem;
  line-height: 1.45;
  color: var(--gray-400);
}
</style>