<template>
  <transition name="slide-drawer">
    <div v-if="cartStore.isOpen" class="cart-backdrop" @click.self="cartStore.isOpen = false">
      <aside class="cart-drawer">

        <!-- Header -->
        <div class="cart-header">
          <h3 class="cart-title">Корзина</h3>
          <button class="cart-close" @click="cartStore.isOpen = false">✕</button>
        </div>

        <!-- Empty state -->
        <div v-if="!cartStore.items.length" class="cart-empty">
          <span class="cart-empty-icon">◻</span>
          <p>Корзина пуста</p>
          <button class="btn btn-ghost btn-sm" @click="cartStore.isOpen = false; $router.push('/shop')">
            Перейти в магазин
          </button>
        </div>

        <!-- Cart items -->
        <div v-else class="cart-items">
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

        <!-- Footer with form and checkout -->
        <div v-if="cartStore.items.length" class="cart-footer">

          <!-- 👇 ФОРМА КЛИЕНТА (добавлено) -->
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

            <div class="form-group">
              <label for="address" class="form-label">Адрес *</label>
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

          <!-- Checkout button -->
          <button
              class="btn btn-primary btn-lg w-full"
              @click="checkout"
              :disabled="checkoutLoading || !isFormValid"
          >
            <span v-if="checkoutLoading">
              <span class="spinner" style="width:14px;height:14px;display:inline-block" />
              Обработка…
            </span>
            <span v-else>Оформить заказ — {{ cartStore.totalPrice + deliveryPrice }} ₽</span>
          </button>

          <button class="btn btn-ghost btn-sm" @click="cartStore.clearCart()">
            Очистить корзину
          </button>

          <p class="consent-text">
            Нажимая кнопку, вы соглашаетесь с
            <a href="/privacy" target="_blank">политикой конфиденциальности</a>
          </p>
        </div>

      </aside>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useCartStore, useToastStore } from '@/store/index.js'
import { javaApi } from '@/api/backend'

const cartStore = useCartStore()
const toastStore = useToastStore()

// Delivery & payment
const delivery = ref('pickup')
const paymentMethod = ref('card')
const checkoutLoading = ref(false)

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

const isFormValid = computed(() => {
  return (
      customerForm.value.firstName.trim() &&
      customerForm.value.lastName.trim() &&
      customerForm.value.phone.trim() &&
      customerForm.value.city.trim() &&
      customerForm.value.address.trim()
  )
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
    case 'address':
      errors.value[field] = !value ? 'Обязательное поле' : ''
      break
    case 'email':
      errors.value.email = value && !/^\S+@\S+\.\S+$/.test(value) ? 'Некорректный email' : ''
      break
  }
}

// Full form validation
function validateForm() {
  ;['firstName', 'lastName', 'phone', 'city', 'address'].forEach(validateField)
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

  try {
    const orderPayload = {
      userId: 'guest-' + Date.now(),
      shippingAddress: {
        firstName: customerForm.value.firstName.trim(),
        lastName: customerForm.value.lastName.trim(),
        phone: customerForm.value.phone.trim(),
        email: customerForm.value.email?.trim() || null,
        city: customerForm.value.city.trim(),
        address: customerForm.value.address.trim(),
        zip: customerForm.value.zip?.trim() || null
      },
      shippingMethod: delivery.value,
      paymentMethod: paymentMethod.value,
      items: cartStore.items.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        qty: item.qty || 1,
        category: item.category
      }))
    }

    const result = await javaApi.orders.create(orderPayload)
    const orderId = result?.orderId ?? result?.data?.orderId ?? ''

    // Оплата картой: редирект на страницу оплаты (онлайн-касса)
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
        delivery.value = 'pickup'
        paymentMethod.value = 'card'
        window.location.href = paymentData.redirectUrl
        return
      } catch (payErr) {
        console.warn('Payment link failed, order created:', payErr)
        toastStore.push(`Заказ #${orderId} создан. Оплата временно недоступна — с вами свяжутся.`, 'warning', 6000)
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
/* ===== Existing cart styles (unchanged) ===== */
.cart-backdrop {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(4px);
  z-index: 900;
  display: flex; justify-content: flex-end;
}

.cart-drawer {
  width: 400px; max-width: 100vw; height: 100vh;
  background: var(--gray-800);
  border-left: 1px solid var(--gray-600);
  display: flex; flex-direction: column; overflow: hidden;
}

.cart-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--gray-600); flex-shrink: 0;
}
.cart-title { font-family: var(--font-display); font-size: 1.25rem; }
.cart-close {
  background: transparent; border: 1px solid var(--gray-600);
  color: var(--paper); width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; border-radius: var(--radius-sm);
  transition: all var(--transition);
}
.cart-close:hover { border-color: var(--paper); }

.cart-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: var(--spacing-md); color: var(--gray-400);
}
.cart-empty-icon { font-size: 3rem; opacity: 0.3; }

.cart-items {
  flex: 1; overflow-y: auto;
  padding: var(--spacing-md) var(--spacing-xl);
  display: flex; flex-direction: column; gap: var(--spacing-md);
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

/* Slide transition */
.slide-drawer-enter-active, .slide-drawer-leave-active { transition: opacity var(--transition-slow); }
.slide-drawer-enter-active .cart-drawer, .slide-drawer-leave-active .cart-drawer { transition: transform var(--transition-slow); }
.slide-drawer-enter-from { opacity: 0; }
.slide-drawer-leave-to { opacity: 0; }
.slide-drawer-enter-from .cart-drawer { transform: translateX(100%); }
.slide-drawer-leave-to .cart-drawer { transform: translateX(100%); }

/* ===== NEW: Customer form styles ===== */
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

/* Mobile responsive */
@media (max-width: 480px) {
  .form-row { grid-template-columns: 1fr; }
  .cart-drawer { width: 100vw; }
  .cart-footer { padding: var(--spacing-md); }
}

.form-group input,
.form-select {
  width: 100%;
  padding: 0.5rem 0.75rem;
  background: var(--gray-900);  /* 👈 было: var(--gray-800) */
  border: 1px solid var(--gray-700);  /* 👈 было: var(--gray-600) */
  border-radius: var(--radius-sm);
  color: var(--paper);
  font-size: 0.85rem;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--accent);
  background: var(--gray-900);
}

/* Ошибки валидации */
.form-group input.error {
  border-color: #ef4444;
  background: rgba(239, 68, 68, 0.05);
}

/* Текст ошибок */
.error-msg {
  display: block;
  font-size: 0.65rem;
  color: #ef4444;
  margin-top: 0.2rem;
  min-height: 1em;
}

/* Радио-кнопки оплаты */
.radio-inline label {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.75rem;
  background: var(--gray-900);  /* 👈 было: var(--gray-800) */
  border: 1px solid var(--gray-700);  /* 👈 было: var(--gray-600) */
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--gray-300);
}

.radio-inline label.active {
  border-color: var(--accent);
  background: rgba(200, 169, 110, 0.08);
  color: var(--paper);
}

/* Текст согласия */
.consent-text {
  font-size: 0.65rem;
  color: var(--gray-500);  /* 👈 было: var(--gray-600) */
  text-align: center;
  margin-top: var(--spacing-sm);
}

.consent-text a {
  color: var(--gray-400);
  text-decoration: underline;
}

.consent-text a:hover {
  color: var(--accent);
}
</style>