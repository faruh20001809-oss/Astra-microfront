<template>
  <Drawer
      ref="cartDrawerRootRef"
      id="astra-cart-drawer"
      v-model:visible="cartStore.isOpen"
      position="right"
      :modal="true"
      :dismissable="true"
      :showCloseIcon="true"
      class="cart-drawer-pv"
  >
    <template #header>
      <span class="cart-title">Корзина · Предзаказ</span>
    </template>

    <div ref="cartDrawerIntrinsicRef" class="cart-drawer-intrinsic">

      <!-- Empty state -->
      <div v-if="!cartStore.items.length" class="cart-empty">
        <span class="cart-empty-icon" aria-hidden="true">◻</span>
        <p class="cart-empty-title">Корзина пуста</p>
        <p class="cart-empty-hint">
          Добавьте товары из магазина и оставьте предзаказ — сотрудник свяжется с вами по Telegram или MAX.
        </p>
        <Button
            label="Перейти в магазин"
            class="p-button-text p-button-secondary"
            @click="cartStore.isOpen = false; $router.push('/shop')" />
      </div>

      <!-- Cart with items: scrollable body -->
      <div v-else class="cart-body">

        <p class="cart-section-label cart-info text-mono">
          Корзина формирует предзаказ. Доставка и оплата согласуются с сотрудником.
        </p>

        <ul class="cart-items" aria-label="Состав предзаказа">
          <li
              v-for="item in cartStore.items"
              :key="`${item.id}-${item.variant || ''}`"
              class="cart-item"
          >
            <div class="cart-item__thumb">
              <CartItemThumbnail :item="item" />
            </div>
            <div class="cart-item__info">
              <p class="cart-item__name">{{ item.name }}</p>
              <p v-if="item.variant" class="cart-item__variant">{{ item.variant }}</p>
              <p class="cart-item__price">{{ item.price }} ₽</p>
            </div>
            <div class="cart-item__controls" role="group" :aria-label="`Количество — ${item.name}`">
              <button
                type="button"
                class="qty-btn"
                :aria-label="`Уменьшить количество товара ${item.name}`"
                @click="cartStore.updateQty(item.id, item.qty - 1)"
              >−</button>
              <span class="qty-val" aria-live="polite">{{ item.qty }}</span>
              <button
                type="button"
                class="qty-btn"
                :aria-label="`Увеличить количество товара ${item.name}`"
                @click="cartStore.updateQty(item.id, item.qty + 1)"
              >+</button>
            </div>
            <button
              type="button"
              class="cart-item__remove"
              :aria-label="`Удалить из предзаказа: ${item.name}`"
              @click="cartStore.removeItem(item.id)"
            >✕</button>
          </li>
        </ul>

        <div class="cart-footer">

          <p class="cart-section-label">Контакты для связи</p>
          <p class="cart-hint">
            Заполните минимум один контакт — Telegram или MAX. По нему сотрудник подтвердит предзаказ.
          </p>

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
                <label for="lastName" class="form-label">Фамилия</label>
                <input
                    id="lastName"
                    v-model="customerForm.lastName"
                    type="text"
                    placeholder="Иванов"
                />
              </div>
            </div>

            <div class="form-group">
              <label for="telegramUsername" class="form-label">Telegram</label>
              <div class="input-with-prefix">
                <span class="input-prefix" aria-hidden="true">@</span>
                <input
                    id="telegramUsername"
                    v-model="customerForm.telegramUsername"
                    type="text"
                    placeholder="username"
                    autocomplete="off"
                    spellcheck="false"
                    aria-describedby="telegramUsernameHelp"
                    :aria-invalid="!!(errors.telegramUsername || errors.contact)"
                    :class="{ error: errors.telegramUsername || errors.contact }"
                    @blur="validateField('telegramUsername'); validateContacts()"
                />
              </div>
              <span id="telegramUsernameHelp" class="form-hint">
                Логин без символа «@» — например, <code>ivan_petrov</code>.
              </span>
              <span v-if="errors.telegramUsername" class="error-msg">{{ errors.telegramUsername }}</span>
            </div>

            <div class="form-group">
              <label for="maxUsername" class="form-label">MAX</label>
              <div class="input-with-prefix">
                <span class="input-prefix" aria-hidden="true">@</span>
                <input
                    id="maxUsername"
                    v-model="customerForm.maxUsername"
                    type="text"
                    placeholder="username"
                    autocomplete="off"
                    spellcheck="false"
                    aria-describedby="maxUsernameHelp"
                    :aria-invalid="!!(errors.maxUsername || errors.contact)"
                    :class="{ error: errors.maxUsername || errors.contact }"
                    @blur="validateField('maxUsername'); validateContacts()"
                />
              </div>
              <span id="maxUsernameHelp" class="form-hint">
                Логин без символа «@».
              </span>
              <span v-if="errors.maxUsername" class="error-msg">{{ errors.maxUsername }}</span>
            </div>

            <p v-if="errors.contact" class="error-msg error-msg--block" role="alert">
              {{ errors.contact }}
            </p>

            <div class="form-group">
              <label for="phone" class="form-label">Телефон</label>
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

            <div class="form-group">
              <label for="comment" class="form-label">Комментарий</label>
              <textarea
                  id="comment"
                  v-model="customerForm.comment"
                  rows="3"
                  placeholder="Если есть пожелания по составу или времени связи"
                  class="form-textarea"
              />
            </div>

          </div>

          <div class="cart-total-row">
            <span class="cart-total-label">Сумма позиций</span>
            <span class="cart-total-price">{{ cartStore.totalPrice }} ₽</span>
          </div>

          <Button
              label="Очистить корзину"
              class="p-button-text p-button-secondary p-button-sm"
              @click="cartStore.clearCart()" />

          <p class="consent-text">
            Отправляя предзаказ, вы соглашаетесь с
            <a href="/privacy" target="_blank" rel="noopener">политикой конфиденциальности</a>.
            Сумма доставки уточняется сотрудником и не входит в эту сумму.
          </p>
        </div>
      </div>
    </div>

    <template #footer>
      <Button
          v-if="cartStore.items.length"
          :label="checkoutLoading ? 'Отправляем…' : `Оставить предзаказ — ${cartStore.totalPrice} ₽`"
          :loading="checkoutLoading"
          :disabled="!isFormValid"
          class="w-full cart-checkout-btn"
          @click="submitPreorder"
      />
    </template>
  </Drawer>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import { useCartStore, useToastStore } from '@/store/index.js'
import { javaApi } from '@/api/backend'
import CartItemThumbnail from '@/components/shop/CartItemThumbnail.vue'

/**
 * CartDrawer — корзина модуля 2 в режиме PREORDER (ТЗ, раздел 5).
 *
 * Что изменилось относительно прошлой версии корзины (заказ + онлайн-оплата):
 *  - убран блок «Способ доставки»: предзаказ не считает доставку (5.4),
 *    сотрудник согласует её отдельно;
 *  - убран блок «Оплата»: онлайн-кассы тут больше нет;
 *  - убраны поля адреса/индекса/города/пункта самовывоза;
 *  - добавлены поля telegramUsername и maxUsername; обязателен ХОТЯ БЫ один;
 *  - submitPreorder отправляет POST /api/v1/preorders.
 */
const cartStore = useCartStore()
const toastStore = useToastStore()

const cartDrawerRootRef = ref(null)
const cartDrawerIntrinsicRef = ref(null)

function asElement(node) {
  return node && typeof node === 'object' && node.nodeType === Node.ELEMENT_NODE ? node : null
}

function drawerSlidePanelEl() {
  if (typeof document === 'undefined') return null
  const byId = asElement(document.getElementById('astra-cart-drawer'))
  const fromRef = asElement(cartDrawerRootRef.value?.$el)
  let root = byId ?? fromRef
  if (!root) {
    root = asElement(document.querySelector('.p-drawer-right .p-drawer.cart-drawer-pv'))
  }
  if (!root) return null
  if (root.classList?.contains('p-drawer')) return root
  const inner = asElement(root.querySelector?.('.p-drawer'))
  return inner ?? root
}

const CART_DRAWER_PANEL_W = '29rem'
const CART_DRAWER_PANEL_MAX = 'min(29rem, 100vw)'

let cartDrawerWidthTimer = null

function applyCartDrawerDesktopWidth() {
  if (typeof window === 'undefined' || window.innerWidth <= 768) return
  const el = drawerSlidePanelEl()
  if (!el) return
  el.style.setProperty('width', CART_DRAWER_PANEL_W, 'important')
  el.style.setProperty('min-width', CART_DRAWER_PANEL_W, 'important')
  el.style.setProperty('max-width', CART_DRAWER_PANEL_MAX, 'important')
}

function clearCartDrawerDesktopWidth() {
  const el = drawerSlidePanelEl()
  if (!el) return
  el.style.removeProperty('width')
  el.style.removeProperty('min-width')
  el.style.removeProperty('max-width')
}

function scheduleApplyCartDrawerDesktopWidth() {
  if (cartDrawerWidthTimer) clearTimeout(cartDrawerWidthTimer)
  const run = () => {
    void nextTick(() => {
      applyCartDrawerDesktopWidth()
      requestAnimationFrame(() => {
        applyCartDrawerDesktopWidth()
        requestAnimationFrame(() => applyCartDrawerDesktopWidth())
      })
    })
  }
  run()
  cartDrawerWidthTimer = window.setTimeout(() => {
    cartDrawerWidthTimer = null
    applyCartDrawerDesktopWidth()
  }, 120)
}

const checkoutLoading = ref(false)

const customerForm = ref({
  firstName: '',
  lastName: '',
  phone: '',
  email: '',
  telegramUsername: '',
  maxUsername: '',
  comment: '',
})

const errors = ref({})

const USERNAME_PATTERN = /^[A-Za-z0-9_.\-]{3,64}$/

function normalizeUsername(value) {
  if (!value) return ''
  const v = String(value).trim()
  return v.startsWith('@') ? v.slice(1).trim() : v
}

function hasAnyContact() {
  const tg = normalizeUsername(customerForm.value.telegramUsername)
  const mx = normalizeUsername(customerForm.value.maxUsername)
  return Boolean(tg || mx)
}

const isFormValid = computed(() => {
  if (!cartStore.items.length) return false
  if (!customerForm.value.firstName.trim()) return false
  if (!hasAnyContact()) return false

  const tg = normalizeUsername(customerForm.value.telegramUsername)
  const mx = normalizeUsername(customerForm.value.maxUsername)
  if (tg && !USERNAME_PATTERN.test(tg)) return false
  if (mx && !USERNAME_PATTERN.test(mx)) return false

  const email = customerForm.value.email?.trim() || ''
  if (email && !/^\S+@\S+\.\S+$/.test(email)) return false

  return true
})

onBeforeUnmount(() => {
  if (cartDrawerWidthTimer) clearTimeout(cartDrawerWidthTimer)
  clearCartDrawerDesktopWidth()
})

watch(
  () => cartStore.isOpen,
  async (open) => {
    if (open) {
      await nextTick()
      scheduleApplyCartDrawerDesktopWidth()
    } else {
      clearCartDrawerDesktopWidth()
    }
  }
)

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

function validateField(field) {
  const value = customerForm.value[field]?.trim()
  switch (field) {
    case 'firstName':
      errors.value.firstName = !value ? 'Обязательное поле' : ''
      break
    case 'phone':
      if (!value) {
        errors.value.phone = ''
      } else if (value.replace(/\D/g, '').length < 11) {
        errors.value.phone = 'Введите телефон в формате +7 (___) ___-__-__'
      } else {
        errors.value.phone = ''
      }
      break
    case 'email':
      errors.value.email = value && !/^\S+@\S+\.\S+$/.test(value) ? 'Некорректный email' : ''
      break
    case 'telegramUsername': {
      const v = normalizeUsername(customerForm.value.telegramUsername)
      errors.value.telegramUsername = v && !USERNAME_PATTERN.test(v)
        ? '3–64 символа: латиница, цифры, _ . -'
        : ''
      break
    }
    case 'maxUsername': {
      const v = normalizeUsername(customerForm.value.maxUsername)
      errors.value.maxUsername = v && !USERNAME_PATTERN.test(v)
        ? '3–64 символа: латиница, цифры, _ . -'
        : ''
      break
    }
  }
}

function validateContacts() {
  errors.value.contact = hasAnyContact()
    ? ''
    : 'Укажите Telegram или MAX — сотрудник свяжется по нему'
}

function validateAll() {
  ['firstName', 'phone', 'email', 'telegramUsername', 'maxUsername'].forEach(validateField)
  validateContacts()
  return !Object.values(errors.value).some(Boolean)
}

async function submitPreorder() {
  if (!validateAll()) {
    toastStore.push('Заполните поля корректно', 'error')
    const firstError = Object.entries(errors.value).find(([_, msg]) => msg)
    if (firstError) {
      const el = document.getElementById(firstError[0])
      if (el?.scrollIntoView) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    return
  }
  if (!cartStore.items.length) {
    toastStore.push('Корзина пуста', 'error')
    return
  }

  checkoutLoading.value = true

  try {
    const payload = {
      customerName: [customerForm.value.firstName.trim(), customerForm.value.lastName.trim()]
        .filter(Boolean).join(' '),
      phone: customerForm.value.phone?.trim() || null,
      email: customerForm.value.email?.trim() || null,
      telegramUsername: normalizeUsername(customerForm.value.telegramUsername) || null,
      maxUsername: normalizeUsername(customerForm.value.maxUsername) || null,
      comment: customerForm.value.comment?.trim() || null,
      items: cartStore.items.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        qty: item.qty || 1,
        category: item.category,
      })),
    }

    const data = await javaApi.preorders.create(payload)
    const preorderId = data?.preorderId || data?.data?.preorderId
    toastStore.push(
      preorderId ? `Предзаказ #${preorderId} создан. Сотрудник свяжется с вами.` : 'Предзаказ отправлен',
      'success',
      6000
    )

    cartStore.clearCart()
    cartStore.isOpen = false
    customerForm.value = {
      firstName: '', lastName: '', phone: '', email: '',
      telegramUsername: '', maxUsername: '', comment: '',
    }
    errors.value = {}
  } catch (err) {
    console.error('Preorder error:', err)
    if (err?.fieldErrors && typeof err.fieldErrors === 'object') {
      Object.assign(errors.value, err.fieldErrors)
    }
    toastStore.push(err?.message || 'Не удалось отправить предзаказ', 'error')
  } finally {
    checkoutLoading.value = false
  }
}
</script>

<style scoped>
.cart-drawer-intrinsic {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
}

.cart-drawer-pv :deep(.p-drawer-content) {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.cart-drawer-pv :deep(.p-drawer-header),
.cart-drawer-pv :deep(.p-drawer-content),
.cart-drawer-pv :deep(.p-drawer-footer) {
  width: 100%;
  max-width: none;
  box-sizing: border-box;
}

.cart-title {
  font-family: var(--font-display);
  font-size: 1.1rem;
  letter-spacing: 0.02em;
}

/* Empty state */
.cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-2xl) var(--spacing-md);
  text-align: center;
  color: var(--gray-400);
}

.cart-empty-icon {
  font-size: 3rem;
  color: var(--gray-600);
}

.cart-empty-title {
  font-family: var(--font-display);
  font-size: 1.1rem;
  color: var(--paper);
  margin: 0;
}

.cart-empty-hint {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.55;
  max-width: 32ch;
}

.cart-body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  flex: 1 1 auto;
  overflow-y: auto;
  min-height: 0;
}

.cart-info {
  margin: 0;
  padding: var(--spacing-sm);
  border-radius: var(--radius-sm);
  background: rgba(200, 169, 110, 0.08);
  border: 1px solid rgba(212, 184, 150, 0.2);
  color: var(--paper, #f0e5cd);
  font-size: 0.72rem;
  letter-spacing: 0.04em;
  line-height: 1.5;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  list-style: none;
  padding: 0;
  margin: 0;
}

/**
 * Карточка позиции корзины.
 * Desktop / wide drawer: thumb | info | controls | remove (1 ряд).
 * Mobile (<= 480px):       thumb | info
 *                          (заполняется)| controls + remove (второй ряд).
 * Используется flex-wrap вместо grid-template-areas — это проще
 * поддерживать и не зависит от точного именования внутренних компонентов.
 */
.cart-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  border: 1px solid rgba(212, 184, 150, 0.14);
  border-radius: var(--radius-sm);
  background: rgba(20, 16, 13, 0.45);
}

.cart-item__thumb {
  flex: 0 0 auto;
  width: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-item__info {
  flex: 1 1 12rem;
  min-width: 0;
}

.cart-item__name {
  margin: 0;
  font-size: 0.92rem;
  color: var(--paper);
  font-weight: 500;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
}

.cart-item__variant {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  color: var(--gray-400);
}

.cart-item__price {
  margin: 0.2rem 0 0;
  font-family: var(--font-mono);
  font-size: 0.85rem;
  color: var(--accent);
}

.cart-item__controls {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex: 0 0 auto;
}

/**
 * Кнопки изменения количества и удаления — touch-target ≥ 40×40 на десктопе
 * и ≥ 44×44 на мобильных (см. responsive-design skill: «Maintain 44×44 minimum on mobile»).
 */
.qty-btn {
  width: 40px;
  height: 40px;
  background: var(--gray-800, #2a231e);
  border: 1px solid var(--gray-600);
  color: var(--paper);
  cursor: pointer;
  border-radius: var(--radius-sm);
  font-size: 1.05rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.qty-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.qty-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.qty-val {
  font-family: var(--font-mono);
  min-width: 28px;
  text-align: center;
  font-size: 0.95rem;
}

.cart-item__remove {
  width: 40px;
  height: 40px;
  background: transparent;
  border: 1px solid transparent;
  color: var(--gray-400);
  cursor: pointer;
  font-size: 1rem;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  transition: color 0.15s ease, border-color 0.15s ease;
}

.cart-item__remove:hover {
  color: var(--danger, #b84a3c);
  border-color: rgba(184, 74, 60, 0.45);
}

.cart-item__remove:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.cart-footer {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid rgba(212, 184, 150, 0.14);
}

.cart-section-label {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent);
}

.cart-hint {
  margin: 0;
  font-size: 0.78rem;
  color: var(--gray-400);
  line-height: 1.45;
}

.customer-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-sm);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-label {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.04em;
  color: var(--gray-400);
}

.customer-form input,
.customer-form textarea {
  width: 100%;
  min-height: 44px;
  padding: 0.55rem 0.75rem;
  background: var(--gray-800, #2a231e);
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  color: var(--paper);
  font-size: 0.92rem;
  transition: border-color 0.15s ease;
}

.customer-form textarea {
  resize: vertical;
  min-height: 78px;
}

.customer-form input:focus,
.customer-form textarea:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(200, 169, 110, 0.18);
}

.customer-form input.error,
.customer-form textarea.error {
  border-color: var(--danger, #b84a3c);
}

.input-with-prefix {
  position: relative;
  display: flex;
  align-items: stretch;
}

.input-prefix {
  display: inline-flex;
  align-items: center;
  padding: 0 0.65rem;
  background: var(--gray-800, #2a231e);
  border: 1px solid var(--gray-600);
  border-right: none;
  border-radius: var(--radius-sm) 0 0 var(--radius-sm);
  color: var(--gray-400);
  font-family: var(--font-mono);
  font-size: 0.92rem;
}

.input-with-prefix input {
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  flex: 1 1 auto;
  min-width: 0;
}

.form-hint {
  font-size: 0.72rem;
  line-height: 1.45;
  color: var(--gray-400);
}

.form-hint code {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  padding: 0.05rem 0.3rem;
  border-radius: 4px;
  background: rgba(212, 184, 150, 0.12);
  color: var(--accent);
}

.error-msg {
  font-size: 0.7rem;
  color: var(--danger, #b84a3c);
  line-height: 1.3;
}

.error-msg--block {
  margin: 0;
  padding: 0.5rem 0.65rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--danger, #b84a3c);
  background: rgba(184, 74, 60, 0.08);
  color: var(--danger, #b84a3c);
  font-size: 0.78rem;
}

.cart-total-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding-top: var(--spacing-sm);
  border-top: 1px solid rgba(212, 184, 150, 0.14);
}

.cart-total-label {
  font-family: var(--font-mono);
  font-size: 0.75rem;
  color: var(--gray-400);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.cart-total-price {
  font-family: var(--font-mono);
  font-size: 1.25rem;
  color: var(--accent);
}

.consent-text {
  font-size: 0.7rem;
  color: var(--gray-500, #786454);
  line-height: 1.55;
  margin: 0;
}

.consent-text a {
  color: var(--gray-300);
  text-decoration: underline;
}

.cart-checkout-btn {
  min-height: 48px;
}

@media (max-width: 480px) {
  .form-row {
    grid-template-columns: 1fr;
  }
  .cart-item__info {
    /* На узких — info занимает оставшуюся строку рядом с thumb,
       а controls + remove переходят на новый ряд (flex-wrap). */
    flex-basis: calc(100% - 60px - var(--spacing-sm));
  }
  .qty-btn,
  .cart-item__remove {
    width: 44px;
    height: 44px;
  }
}

/* Light-тема: переопределяем тёмные поверхности корзины. */
:root.app-light .cart-item {
  background: #ffffff;
  border-color: rgba(0, 0, 0, 0.08);
}

:root.app-light .customer-form input,
:root.app-light .customer-form textarea,
:root.app-light .input-prefix {
  background: #ffffff;
  border-color: rgba(0, 0, 0, 0.18);
  color: var(--paper);
}

:root.app-light .qty-btn {
  background: #f5f5f5;
}

:root.app-light .cart-info {
  background: rgba(184, 148, 94, 0.1);
  border-color: rgba(184, 148, 94, 0.35);
}

:root.app-light .error-msg--block {
  background: rgba(192, 57, 43, 0.05);
}

@media (prefers-reduced-motion: reduce) {
  .customer-form input,
  .customer-form textarea,
  .qty-btn,
  .cart-item__remove {
    transition: none;
  }
}
</style>
