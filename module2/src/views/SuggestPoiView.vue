<template>
  <div class="page-wrapper suggest-poi-page">
    <div class="container suggest-container">
      <section class="suggest-hero" aria-label="О разделе">
        <p class="suggest-hero-label text-mono">◇ Предложить объект</p>
        <h1 class="suggest-hero-title">Предложить точку на карту</h1>
        <p class="suggest-desc">Знаете интересное место в Астрахани? Расскажите о нём — мы рассмотрим заявку и добавим точку на карту.</p>
      </section>

      <div class="suggest-form-wrap">
        <form class="suggest-form" @submit.prevent="submitForm">
          <div class="form-group">
            <label class="form-label">Название *</label>
            <input v-model="form.name" type="text" class="form-input" placeholder="Например: Дом-музей Кустодиева" required />
          </div>
          <div class="form-group">
            <label class="form-label">Место *</label>
            <input v-model="form.place" type="text" class="form-input" placeholder="Адрес или описание места (улица, район, ориентир)" required />
          </div>
          <div class="form-group">
            <label class="form-label">Описание</label>
            <textarea v-model="form.description" class="form-textarea" placeholder="Краткое описание объекта, история, что там можно увидеть…" rows="4" />
          </div>
          <div class="form-group">
            <label class="form-label">Почему мы должны добавить *</label>
            <textarea v-model="form.whyAdd" class="form-textarea" placeholder="Почему это место важно для истории или культуры города? Чем оно интересно?" required rows="4" />
          </div>

          <div v-if="formError" class="form-error">{{ formError }}</div>

          <button type="submit" class="btn btn-primary btn-lg" :disabled="submitting">
            <span v-if="submitting">
              <span class="spinner" style="width:14px;height:14px;display:inline-block" />
              Отправка…
            </span>
            <span v-else>Отправить предложение</span>
          </button>

          <transition name="fade">
            <div v-if="submitted" class="form-success">
              <span style="color:var(--accent)">✦</span>
              Предложение отправлено! Мы рассмотрим его и при одобрении добавим точку на карту.
            </div>
          </transition>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { javaApi } from '@/api/backend.js'

const form = ref({
  name: '',
  place: '',
  description: '',
  whyAdd: ''
})
const submitting = ref(false)
const submitted = ref(false)
const formError = ref('')

async function submitForm() {
  formError.value = ''
  submitting.value = true
  try {
    await javaApi.poiSuggestions.submit({
      name: form.value.name.trim(),
      place: form.value.place.trim(),
      description: form.value.description?.trim() || undefined,
      whyAdd: form.value.whyAdd.trim()
    })
    submitted.value = true
    form.value = { name: '', place: '', description: '', whyAdd: '' }
    setTimeout(() => { submitted.value = false }, 8000)
  } catch (e) {
    formError.value = e.message || 'Не удалось отправить. Попробуйте позже.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.suggest-poi-page {
  padding-bottom: var(--spacing-2xl);
  padding-top: 0;
}

.suggest-container {
  position: relative;
  z-index: 0;
}

.suggest-hero {
  padding: var(--spacing-xl) 0 var(--spacing-lg);
  margin-top: 0;
}

.suggest-hero-label {
  color: var(--accent);
  margin-bottom: var(--spacing-xs);
}

.suggest-hero-title {
  font-size: clamp(1.5rem, 4vw, 2.25rem);
  line-height: 1.2;
  margin-bottom: var(--spacing-sm);
}

.suggest-desc {
  color: var(--gray-400);
  font-size: 0.95rem;
  line-height: 1.7;
  margin-top: 0;
  max-width: 560px;
}

@media (max-width: 768px) {
  .suggest-hero { padding: var(--spacing-lg) 0; }
  .suggest-desc, .suggest-form-wrap { max-width: 100%; }
}

@media (max-width: 480px) {
  .suggest-hero { padding: var(--spacing-md) 0; }
  .suggest-hero-title { font-size: 1.35rem; }
  .form-input, .form-textarea { min-height: 44px; font-size: 16px; }
}

.suggest-form-wrap {
  max-width: 560px;
}

.suggest-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.form-group { display: flex; flex-direction: column; gap: var(--spacing-xs); }
.form-label {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gray-400);
}
.form-input, .form-textarea {
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: 0.95rem;
  background: var(--gray-800);
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  color: var(--paper);
  transition: border-color var(--transition);
}
.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: var(--accent);
}
.form-textarea { min-height: 100px; resize: vertical; }

.form-error {
  color: var(--danger, #e57373);
  font-size: 0.85rem;
  padding: var(--spacing-sm);
  border: 1px solid var(--danger, #e57373);
  border-radius: var(--radius-sm);
}

.form-success {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  color: var(--paper);
  background: rgba(26, 107, 58, 0.15);
  border: 1px solid var(--success, #4caf50);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  line-height: 1.6;
}

.spinner {
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
