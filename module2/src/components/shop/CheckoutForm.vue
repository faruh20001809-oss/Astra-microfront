<template>
  <div class="checkout-form">
    <h3 class="checkout-title">📦 Оформление заказа</h3>

    <!-- Контакты -->
    <section class="form-section">
      <h4 class="section-title">Контактные данные</h4>

      <div class="form-row">
        <div class="form-group">
          <label for="firstName">Имя *</label>
          <input
              id="firstName"
              v-model="form.firstName"
              type="text"
              placeholder="Иван"
              required
              :class="{ error: errors.firstName }"
          />
          <span v-if="errors.firstName" class="error-msg">{{ errors.firstName }}</span>
        </div>

        <div class="form-group">
          <label for="lastName">Фамилия *</label>
          <input
              id="lastName"
              v-model="form.lastName"
              type="text"
              placeholder="Иванов"
              required
              :class="{ error: errors.lastName }"
          />
          <span v-if="errors.lastName" class="error-msg">{{ errors.lastName }}</span>
        </div>
      </div>

      <div class="form-group">
        <label for="phone">Телефон *</label>
        <input
            id="phone"
            v-model="form.phone"
            type="tel"
            placeholder="+7 (999) 123-45-67"
            required
            :class="{ error: errors.phone }"
            @input="formatPhone"
        />
        <span v-if="errors.phone" class="error-msg">{{ errors.phone }}</span>
      </div>

      <div class="form-group">
        <label for="email">Email</label>
        <input
            id="email"
            v-model="form.email"
            type="email"
            placeholder="example@mail.ru"
            :class="{ error: errors.email }"
        />
        <span v-if="errors.email" class="error-msg">{{ errors.email }}</span>
      </div>
    </section>

    <!-- Адрес доставки -->
    <section class="form-section">
      <h4 class="section-title">Адрес доставки</h4>

      <div class="form-group">
        <label for="city">Город *</label>
        <input
            id="city"
            v-model="form.city"
            type="text"
            placeholder="Астрахань"
            required
            :class="{ error: errors.city }"
        />
        <span v-if="errors.city" class="error-msg">{{ errors.city }}</span>
      </div>

      <div class="form-group">
        <label for="address">Улица, дом, квартира *</label>
        <input
            id="address"
            v-model="form.address"
            type="text"
            placeholder="ул. Кремлёвская, д. 1, кв. 10"
            required
            :class="{ error: errors.address }"
        />
        <span v-if="errors.address" class="error-msg">{{ errors.address }}</span>
      </div>

      <div class="form-group">
        <label for="zip">Индекс</label>
        <input
            id="zip"
            v-model="form.zip"
            type="text"
            placeholder="414000"
        />
      </div>
    </section>

    <!-- Способ доставки -->
    <section class="form-section">
      <h4 class="section-title">Способ доставки</h4>

      <div class="radio-group">
        <label class="radio-card" :class="{ active: form.shippingMethod === 'courier' }">
          <input
              type="radio"
              v-model="form.shippingMethod"
              value="courier"
              name="shipping"
          />
          <div class="radio-content">
            <span class="radio-icon">🚚</span>
            <div>
              <p class="radio-title">Курьером</p>
              <p class="radio-desc">300 ₽ · 1-2 дня</p>
            </div>
          </div>
        </label>

        <label class="radio-card" :class="{ active: form.shippingMethod === 'pickup' }">
          <input
              type="radio"
              v-model="form.shippingMethod"
              value="pickup"
              name="shipping"
          />
          <div class="radio-content">
            <span class="radio-icon">📍</span>
            <div>
              <p class="radio-title">Самовывоз</p>
              <p class="radio-desc">Бесплатно · Сегодня</p>
            </div>
          </div>
        </label>

        <label class="radio-card" :class="{ active: form.shippingMethod === 'post' }">
          <input
              type="radio"
              v-model="form.shippingMethod"
              value="post"
              name="shipping"
          />
          <div class="radio-content">
            <span class="radio-icon">📮</span>
            <div>
              <p class="radio-title">Почта России</p>
              <p class="radio-desc">500 ₽ · 3-7 дней</p>
            </div>
          </div>
        </label>
      </div>
    </section>

    <!-- Способ оплаты -->
    <section class="form-section">
      <h4 class="section-title">Способ оплаты</h4>

      <div class="radio-group">
        <label class="radio-card" :class="{ active: form.paymentMethod === 'card' }">
          <input
              type="radio"
              v-model="form.paymentMethod"
              value="card"
              name="payment"
          />
          <div class="radio-content">
            <span class="radio-icon">💳</span>
            <div>
              <p class="radio-title">Картой онлайн</p>
              <p class="radio-desc">Visa, MasterCard, MIR</p>
            </div>
          </div>
        </label>

        <label class="radio-card" :class="{ active: form.paymentMethod === 'cash' }">
          <input
              type="radio"
              v-model="form.paymentMethod"
              value="cash"
              name="payment"
          />
          <div class="radio-content">
            <span class="radio-icon">💵</span>
            <div>
              <p class="radio-title">При получении</p>
              <p class="radio-desc">Наличные или карта</p>
            </div>
          </div>
        </label>
      </div>
    </section>

    <!-- Комментарий -->
    <section class="form-section">
      <h4 class="section-title">Комментарий к заказу</h4>

      <textarea
          v-model="form.customerComment"
          placeholder="Например: позвонить за 30 минут до доставки"
          rows="3"
          class="form-textarea"
      ></textarea>
    </section>

    <!-- Итого -->
    <div class="order-summary">
      <div class="summary-row">
        <span>Товары ({{ cartStore.items.length }}):</span>
        <span>{{ cartTotal }} ₽</span>
      </div>
      <div class="summary-row">
        <span>Доставка:</span>
        <span>{{ shippingCost }} ₽</span>
      </div>
      <div class="summary-row total">
        <span>Итого:</span>
        <span>{{ orderTotal }} ₽</span>
      </div>
    </div>
    <!-- Данные клиента -->
    <div class="customer-form" v-if="cartStore.items.length">
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
            @blur="validateField('email')"
        />
        <span v-if="errors.email" class="error-msg">{{ errors.email }}</span>
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

    <!-- Кнопка заказа -->
    <button
        class="btn btn-primary btn-lg btn-block"
        :disabled="isSubmitting || !isValid"
        @click="submitOrder"
    >
      <span v-if="isSubmitting">
        <span class="spinner" style="width:16px;height:16px;display:inline-block;margin-right:8px"></span>
        Оформляем...
      </span>
      <span v-else>
        ✅ Оформить заказ — {{ orderTotal }} ₽
      </span>
    </button>

    <!-- Согласие -->
    <p class="consent-text">
      Нажимая кнопку, вы соглашаетесь с
      <a href="/privacy" target="_blank">политикой конфиденциальности</a>
    </p>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useCartStore, useToastStore } from '@/store/index.js'
import { javaApi } from '@/api/backend'  // 👈 Новый API-клиент

const cartStore = useCartStore()
const toastStore = useToastStore()

const delivery = ref('pickup')
const checkoutLoading = ref(false)

// 👇 Данные клиента
const customerForm = ref({
  firstName: '',
  lastName: '',
  phone: '',
  email: '',
  city: 'Астрахань',
  address: '',
  zip: ''
})

// 👇 Ошибки валидации
const errors = ref({})

const deliveryPrice = computed(() => {
  if (delivery.value === 'courier') return 300
  if (delivery.value === 'post') return 450
  return 0
})

// 👇 Форматирование телефона: +7 (XXX) XXX-XX-XX
function formatPhone() {
  let digits = customerForm.value.phone.replace(/\D/g, '')
  if (digits.startsWith('8')) digits = '7' + digits.slice(1)
  if (!digits.startsWith('7')) digits = '7' + digits

  let f = '+' + digits
  if (digits.length > 1) f = '+7 (' + digits.slice(1, 4)
  if (digits.length > 4) f = '+7 (' + digits.slice(1, 4) + ') ' + digits.slice(4, 7)
  if (digits.length > 7) f = '+7 (' + digits.slice(1, 4) + ') ' + digits.slice(4, 7) + '-' + digits.slice(7, 9)
  if (digits.length > 9) f = '+7 (' + digits.slice(1, 4) + ') ' + digits.slice(4, 7) + '-' + digits.slice(7, 9) + '-' + digits.slice(9, 11)

  customerForm.value.phone = f.slice(0, 18)
}

// 👇 Валидация одного поля
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
    case 'email':
      errors.value.email = value && !/^\S+@\S+\.\S+$/.test(value) ? 'Некорректный email' : ''
      break
    case 'city':
    case 'address':
      errors.value[field] = !value ? 'Обязательное поле' : ''
      break
  }
}

// 👇 Полная валидация перед отправкой
function validateForm() {
  ;['firstName', 'lastName', 'phone', 'city', 'address'].forEach(validateField)
  return !Object.values(errors.value).some(e => e)
}

// 👇 Обновлённая функция оформления заказа
async function checkout() {
  // 1. Валидация
  if (!validateForm()) {
    toastStore.push('Пожалуйста, заполните все обязательные поля', 'error')
    // Прокрутка к первой ошибке
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
    // 2. Формируем payload для Spring Boot
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
      paymentMethod: 'card', // 👈 Можно добавить выбор оплаты позже
      items: cartStore.items.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        qty: item.qty || 1,  // 👈 Важно: qty, а не quantity!
        category: item.category
      }))
    }

    // 3. Отправляем через новый API-клиент
    const result = await javaApi.orders.create(orderPayload)

    // 4. Успех
    toastStore.push(`✅ Заказ #${result.orderId} оформлен!`, 'success', 6000)
    cartStore.clearCart()
    cartStore.isOpen = false

    // 5. Сброс формы (опционально)
    customerForm.value = {
      firstName: '', lastName: '', phone: '', email: '',
      city: 'Астрахань', address: '', zip: ''
    }
    delivery.value = 'pickup'

  } catch (err) {
    console.error('Order error:', err)
    toastStore.push('❌ Ошибка: ' + err.message, 'error')
  } finally {
    checkoutLoading.value = false
  }
}
</script>

<style scoped>
.checkout-form {
  max-width: 520px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}

.checkout-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  margin-bottom: var(--spacing-lg);
  color: var(--paper);
}

.form-section {
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--gray-800);
}

.section-title {
  font-family: var(--font-mono);
  font-size: 0.75rem;
  color: var(--gray-400);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: var(--spacing-md);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
}

.form-group {
  margin-bottom: var(--spacing-md);
}

.form-group label {
  display: block;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  color: var(--gray-400);
  margin-bottom: 0.35rem;
}

.form-group input,
.form-group textarea,
.form-select {
  width: 100%;
  padding: 0.65rem 0.75rem;
  background: var(--gray-800);
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  color: var(--paper);
  font-size: 0.9rem;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--accent);
}

.form-group input.error {
  border-color: #ef4444;
}

.error-msg {
  display: block;
  font-size: 0.7rem;
  color: #ef4444;
  margin-top: 0.25rem;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

/* Radio cards */
.radio-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.radio-card {
  display: block;
  padding: var(--spacing-md);
  background: var(--gray-800);
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
}

.radio-card:hover {
  border-color: var(--gray-800);
}

.radio-card.active {
  border-color: var(--accent);
  background: rgba(200, 169, 110, 0.08);
}

.radio-card input {
  display: none;
}

.radio-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.radio-icon {
  font-size: 1.5rem;
}

.radio-title {
  font-weight: 600;
  color: var(--paper);
  margin: 0;
}

.radio-desc {
  font-size: 0.75rem;
  color: var(--gray-400);
  margin: 0.25rem 0 0;
}

/* Order summary */
.order-summary {
  background: var(--gray-600);
  border: 1px solid var(--gray-800);
  border-radius: var(--radius-sm);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--spacing-sm);
  font-size: 0.9rem;
  color: var(--gray-200);
}

.summary-row.total {
  border-top: 1px solid var(--gray-600);
  padding-top: var(--spacing-sm);
  margin-top: var(--spacing-sm);
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--paper);
}

.summary-row.total span:last-child {
  color: var(--accent);
}

/* Button */
.btn-block {
  width: 100%;
}

/* Consent */
.consent-text {
  font-size: 0.7rem;
  color: var(--gray-600);
  text-align: center;
  margin-top: var(--spacing-md);
}

.consent-text a {
  color: var(--gray-400);
  text-decoration: underline;
}

/* Responsive */
@media (max-width: 480px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .checkout-form {
    padding: var(--spacing-md);
  }
}
</style>