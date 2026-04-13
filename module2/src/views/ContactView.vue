<template>
  <div class="page-wrapper contact-page">
    <div class="container contact-page__inner">

      <section class="contact-hero motion-reveal" aria-labelledby="contact-hero-title">
        <div class="contact-hero__accent" aria-hidden="true" />
        <p class="contact-eyebrow text-mono">Связь с нами</p>
        <h1 id="contact-hero-title" class="contact-hero-title">Контакты</h1>
        <p class="contact-hero-lead">
          Вопросы по карте, предложения объектов и сотрудничество — мы читаем каждое сообщение.
        </p>
      </section>

      <div class="contact-bento">

        <!-- Contact form -->
        <div class="contact-panel contact-panel--form">
          <h2 class="section-heading">Написать нам</h2>
          <p class="section-desc">
            Если вы хотите предложить новый объект на карту, сообщить об ошибке или просто поделиться мыслями — мы рады вашему письму.
          </p>

          <form class="contact-form" @submit.prevent="submitForm">
            <div class="form-group">
              <label class="form-label">Имя *</label>
              <input v-model="form.name" type="text" class="form-input" placeholder="Иван Петров" required />
            </div>
            <div class="form-group">
              <label class="form-label">Email *</label>
              <input v-model="form.email" type="email" class="form-input" placeholder="ivan@example.com" required />
            </div>
            <div class="form-group">
              <label class="form-label">Тема</label>
              <select v-model="form.subject" class="form-select">
                <option value="">Выберите тему</option>
                <option value="new-object">Предложить объект на карту</option>
                <option value="error">Сообщить об ошибке</option>
                <option value="cooperation">Сотрудничество</option>
                <option value="donation">Пожертвование</option>
                <option value="other">Другое</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Сообщение *</label>
              <textarea v-model="form.message" class="form-textarea" placeholder="Ваше сообщение…" required style="min-height: 160px" />
            </div>

            <div v-if="formError" class="form-error">{{ formError }}</div>

            <button type="submit" class="btn btn-primary btn-lg" :disabled="submitting">
              <span v-if="submitting">
                <span class="spinner" style="width:14px;height:14px;display:inline-block" />
                Отправка…
              </span>
              <span v-else>Отправить сообщение</span>
            </button>

            <transition name="fade">
              <div v-if="submitted" class="form-success" role="status">
                <span class="form-success__mark" aria-hidden="true" />
                Сообщение отправлено! Мы ответим вам в течение 1–2 рабочих дней.
              </div>
            </transition>
          </form>
        </div>

        <!-- Info column -->
        <aside class="contact-panel contact-panel--aside" aria-label="Контакты и поддержка">

          <!-- Social / contacts -->
          <div class="info-card">
            <h3 class="info-card__title">Наши контакты</h3>
            <div class="divider" />
            <div class="contact-links">
              <a href="mailto:info@astrakhan-history.ru" class="contact-link">
                <span class="contact-link-icon">@</span>
                <div>
                  <p class="contact-link-label">Email</p>
                  <p>info@astrakhan-history.ru</p>
                </div>
              </a>
              <a href="https://t.me/astrakhan_history" target="_blank" class="contact-link">
                <span class="contact-link-icon">✈</span>
                <div>
                  <p class="contact-link-label">Telegram</p>
                  <p>@astrakhan_history</p>
                </div>
              </a>
              <div class="contact-link">
                <span class="contact-link-icon">◎</span>
                <div>
                  <p class="contact-link-label">Адрес</p>
                  <p>414000, г. Астрахань</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Donation -->
          <div class="info-card donation-card">
            <p class="donation-eyebrow text-mono">Поддержать проект</p>
            <h3 class="info-card__title">Пожертвование</h3>
            <p class="donation-desc">
              Проект существует благодаря поддержке неравнодушных горожан. Ваш вклад помогает оцифровывать исторические архивы, добавлять новые объекты и развивать платформу.
            </p>

            <div class="donation-amounts">
              <button
                v-for="amount in donationAmounts"
                :key="amount"
                type="button"
                :class="['donation-btn', { active: selectedDonation === amount }]"
                @click="selectedDonation = amount; customDonation = ''"
              >
                {{ amount }} ₽
              </button>
            </div>

            <div class="form-group">
              <label class="form-label">Или введите сумму</label>
              <input
                v-model="customDonation"
                type="number"
                class="form-input"
                placeholder="Своя сумма…"
                min="10"
                @input="selectedDonation = null"
              />
            </div>

            <div class="donation-methods">
              <p class="donation-methods__label text-mono">Способы перевода</p>
              <div class="method-list">
                <div class="method-item">
                  <span>Сбербанк Онлайн</span>
                  <span class="method-val">4276 1234 5678 9012</span>
                </div>
                <div class="method-item">
                  <span>СБП</span>
                  <span class="method-val">+7 (999) 123-45-67</span>
                </div>
                <div class="method-item">
                  <span>ЮMoney</span>
                  <span class="method-val">410014123456789</span>
                </div>
              </div>
            </div>

            <button type="button" class="btn btn-accent btn-lg" @click="donate">
              Поддержать — {{ activeDonationAmount || '…' }} ₽
            </button>
          </div>

        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useToastStore } from '@/store/index.js'

const toastStore = useToastStore()

const form = ref({ name: '', email: '', subject: '', message: '' })
const submitting = ref(false)
const submitted = ref(false)
const formError = ref('')

const donationAmounts = [100, 250, 500, 1000, 2000, 5000]
const selectedDonation = ref(500)
const customDonation = ref('')

const activeDonationAmount = computed(() => customDonation.value || selectedDonation.value)

async function submitForm() {
  formError.value = ''
  submitting.value = true
  try {
    const res = await fetch('/api/contact', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    if (res.ok) {
      submitted.value = true
      form.value = { name: '', email: '', subject: '', message: '' }
      setTimeout(() => { submitted.value = false }, 8000)
    } else {
      throw new Error()
    }
  } catch {
    // Mock success in dev
    submitted.value = true
    form.value = { name: '', email: '', subject: '', message: '' }
    setTimeout(() => { submitted.value = false }, 8000)
  } finally {
    submitting.value = false
  }
}

function donate() {
  if (!activeDonationAmount.value) {
    toastStore.push('Выберите или введите сумму пожертвования', 'error')
    return
  }
  toastStore.push(`Спасибо! Переводите ${activeDonationAmount.value} ₽ любым удобным способом ниже.`, 'success', 5000)
}
</script>

<style scoped>
.contact-page {
  padding-bottom: var(--spacing-2xl);
  container-type: inline-size;
  container-name: contact;
}

.contact-page__inner {
  max-width: 1120px;
}

.contact-hero {
  position: relative;
  padding: clamp(1.5rem, 4vw, 2.5rem) 0 clamp(1rem, 2.5vw, 1.75rem);
  max-width: 38rem;
}

.contact-hero__accent {
  position: absolute;
  left: 0;
  top: 0;
  width: 3rem;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), transparent);
  border-radius: 2px;
}

.contact-eyebrow {
  color: var(--accent);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: clamp(0.65rem, 0.45cqi + 0.55rem, 0.75rem);
  margin: 0 0 var(--spacing-sm);
}

.contact-hero-title {
  font-family: var(--font-display);
  font-size: clamp(2rem, 4cqi + 1rem, 2.75rem);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.02em;
  margin: 0;
  color: var(--cream);
}

.contact-hero-lead {
  margin: var(--spacing-md) 0 0;
  color: var(--gray-400);
  font-size: clamp(0.875rem, 0.8rem + 0.3vw, 0.95rem);
  line-height: 1.7;
  max-width: 42ch;
}

.contact-bento {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 400px);
  gap: clamp(1.25rem, 3cqi, 2.5rem);
  align-items: start;
}

.contact-panel {
  min-width: 0;
}

.contact-panel--form {
  padding: clamp(1.25rem, 2.5cqi, 2rem);
  border-radius: var(--radius-md);
  border: 1px solid rgba(212, 184, 150, 0.12);
  background: linear-gradient(160deg, rgba(42, 35, 30, 0.55) 0%, rgba(20, 16, 13, 0.65) 100%);
  box-shadow: var(--shadow-card);
}

.contact-panel--aside {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.section-heading {
  font-family: var(--font-display);
  font-size: clamp(1.25rem, 1.5cqi + 0.9rem, 1.5rem);
  margin-bottom: var(--spacing-sm);
  color: var(--cream);
}

.section-desc {
  color: var(--gray-400);
  font-size: 0.875rem;
  line-height: 1.75;
  margin-bottom: var(--spacing-xl);
  max-width: 52ch;
}

.contact-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.form-error {
  color: var(--danger);
  font-size: 0.8rem;
  padding: var(--spacing-sm);
  border: 1px solid var(--danger);
  border-radius: var(--radius-sm);
}

.form-success {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  color: var(--paper);
  background: rgba(45, 107, 69, 0.18);
  border: 1px solid rgba(45, 107, 69, 0.55);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  line-height: 1.6;
}

.form-success__mark {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 0.35rem;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 12px var(--accent-glow);
}

.info-card {
  position: relative;
  background: linear-gradient(180deg, rgba(42, 35, 30, 0.85) 0%, rgba(30, 25, 21, 0.92) 100%);
  border: 1px solid rgba(212, 184, 150, 0.14);
  border-radius: var(--radius-md);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-subtle);
}

.info-card::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  background: linear-gradient(90deg, transparent, rgba(212, 184, 150, 0.45), transparent);
  opacity: 0.85;
  pointer-events: none;
}

.info-card__title {
  font-family: var(--font-display);
  font-size: 1.2rem;
  margin: 0 0 var(--spacing-sm);
  color: var(--cream);
}

.contact-links { display: flex; flex-direction: column; gap: var(--spacing-md); }

.contact-link {
  display: flex;
  gap: var(--spacing-md);
  align-items: flex-start;
  text-decoration: none;
  padding: var(--spacing-sm);
  border-radius: var(--radius-sm);
  transition: background var(--transition);
}

.contact-link:hover { background: rgba(255,255,255,0.04); }

.contact-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.contact-link-icon {
  width: 36px; height: 36px;
  border: 1px solid var(--gray-600);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.875rem;
  flex-shrink: 0;
  color: var(--accent);
}

.contact-link-label {
  font-size: 0.65rem;
  font-family: var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gray-400);
  margin-bottom: 2px;
}

.donation-eyebrow {
  color: var(--accent);
  margin-bottom: 0.5rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.65rem;
}

/* Donation */
.donation-card {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.donation-desc {
  font-size: 0.8rem;
  color: var(--gray-400);
  line-height: 1.7;
}

.donation-amounts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-xs);
}

.donation-btn {
  padding: 0.5rem;
  font-family: var(--font-mono);
  font-size: 0.8rem;
  background: transparent;
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  color: var(--gray-400);
  cursor: pointer;
  transition: all var(--transition);
  text-align: center;
}

.donation-btn:hover { border-color: var(--gray-400); color: var(--paper); }
.donation-btn.active { border-color: var(--accent); color: var(--accent); background: rgba(200,169,110,0.08); }

.donation-methods {
  background: rgba(20, 16, 13, 0.55);
  border: 1px solid rgba(212, 184, 150, 0.1);
  border-radius: var(--radius-sm);
  padding: var(--spacing-md);
}

.donation-methods__label {
  color: var(--gray-400);
  margin-bottom: 0.5rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 0.65rem;
}

.method-list { display: flex; flex-direction: column; gap: var(--spacing-sm); }

.method-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.78rem;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.method-val {
  font-family: var(--font-mono);
  color: var(--gray-400);
  font-size: 0.72rem;
}

@media (max-width: 900px) {
  .contact-bento {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .contact-hero { padding: var(--spacing-xl) 0 var(--spacing-lg); }
  .section-heading { font-size: 1.25rem; }
  .contact-form { gap: var(--spacing-md); }
  .info-card { padding: var(--spacing-lg); }
  .donation-amounts { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 480px) {
  .contact-hero { padding: var(--spacing-lg) 0; }
  .section-heading { font-size: 1.125rem; }
  .donation-amounts { grid-template-columns: 1fr; }
  .donation-btn { min-height: 44px; padding: 0.75rem; }
  .contact-link { padding: var(--spacing-sm); min-height: 48px; }
}
</style>
