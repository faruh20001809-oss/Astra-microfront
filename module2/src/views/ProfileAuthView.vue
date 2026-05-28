<template>
  <div class="page-wrapper auth-page">
    <div class="container auth-container">
      <section class="auth-shell motion-reveal" aria-labelledby="auth-page-title">
        <header class="auth-head">
          <p class="auth-eyebrow text-mono">Вход в сервис</p>
          <h1 id="auth-page-title" class="auth-title">Профиль клиента</h1>
          <p class="auth-lead">
            Войдите или зарегистрируйтесь, чтобы сохранять данные и быстрее оформлять заказы в «Астрахань. Живая история».
          </p>
        </header>

        <div
          class="auth-segmented"
          role="tablist"
          aria-label="Режим"
        >
          <button
            type="button"
            role="tab"
            class="auth-tab"
            :class="{ active: mode === 'login' }"
            :aria-selected="mode === 'login'"
            id="tab-login"
            aria-controls="panel-login"
            @click="mode = 'login'"
          >
            Вход
          </button>
          <button
            type="button"
            role="tab"
            class="auth-tab"
            :class="{ active: mode === 'register' }"
            :aria-selected="mode === 'register'"
            id="tab-register"
            aria-controls="panel-register"
            @click="mode = 'register'"
          >
            Регистрация
          </button>
        </div>

        <form
          v-if="mode === 'login'"
          id="panel-login"
          class="auth-form"
          role="tabpanel"
          aria-labelledby="tab-login"
          @submit.prevent="submitLogin"
        >
          <div class="form-group">
            <label class="form-label" for="auth-login-email">Логин или email</label>
            <input
              id="auth-login-email"
              v-model.trim="loginForm.loginOrEmail"
              class="form-input"
              autocomplete="username"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label" for="auth-login-pass">Пароль</label>
            <input
              id="auth-login-pass"
              v-model="loginForm.password"
              class="form-input"
              type="password"
              autocomplete="current-password"
              required
            />
          </div>
          <button type="submit" class="btn btn-primary btn-lg auth-submit" :disabled="busy">
            {{ busy ? 'Вход…' : 'Войти' }}
          </button>
        </form>

        <form
          v-else
          id="panel-register"
          class="auth-form"
          role="tabpanel"
          aria-labelledby="tab-register"
          @submit.prevent="submitRegister"
        >
          <div class="form-group">
            <label class="form-label" for="auth-reg-login">Логин</label>
            <input
              id="auth-reg-login"
              v-model.trim="registerForm.login"
              class="form-input"
              minlength="3"
              autocomplete="username"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label" for="auth-reg-email">Email</label>
            <input
              id="auth-reg-email"
              v-model.trim="registerForm.email"
              class="form-input"
              type="email"
              autocomplete="email"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label" for="auth-reg-pass">Пароль</label>
            <input
              id="auth-reg-pass"
              v-model="registerForm.password"
              class="form-input"
              type="password"
              minlength="6"
              autocomplete="new-password"
              required
            />
          </div>
          <button type="submit" class="btn btn-accent btn-lg auth-submit" :disabled="busy">
            {{ busy ? 'Создание…' : 'Создать профиль' }}
          </button>
        </form>

        <p v-if="errorText" class="form-error auth-feedback" role="alert">{{ errorText }}</p>
        <p v-if="okText" class="form-success auth-feedback" role="status">{{ okText }}</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { javaApi } from '@/api/backend.js'
import { saveClientSession } from '@/auth/clientAuth.js'

const router = useRouter()
const mode = ref('login')
const busy = ref(false)
const errorText = ref('')
const okText = ref('')
const loginForm = ref({ loginOrEmail: '', password: '' })
const registerForm = ref({ login: '', email: '', password: '' })

function saveProfile(profile) {
  saveClientSession(profile, profile?.accessToken)
}

async function submitLogin() {
  errorText.value = ''
  okText.value = ''
  busy.value = true
  try {
    const profile = await javaApi.profile.login(loginForm.value)
    saveProfile(profile)
    okText.value = 'Вход выполнен.'
    await router.push('/profile')
  } catch (e) {
    errorText.value = e?.message || 'Не удалось войти'
  } finally {
    busy.value = false
  }
}

async function submitRegister() {
  errorText.value = ''
  okText.value = ''
  busy.value = true
  try {
    const profile = await javaApi.profile.register(registerForm.value)
    saveProfile(profile)
    okText.value = 'Профиль создан.'
    await router.push('/profile')
  } catch (e) {
    errorText.value = e?.message || 'Не удалось создать профиль'
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.auth-page {
  --page-pad-top-extra: clamp(1.25rem, 3vw, 2rem);
}

.auth-container {
  max-width: 520px;
}

.auth-shell {
  padding: clamp(1.35rem, 3vw, 2rem);
  border-radius: var(--radius-md);
  border: 1px solid rgba(212, 184, 150, 0.22);
  background:
    linear-gradient(165deg, rgba(46, 38, 32, 0.97) 0%, rgba(18, 15, 12, 0.95) 100%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.05) inset,
    0 24px 56px rgba(0, 0, 0, 0.42);
}

.auth-eyebrow {
  color: var(--accent);
  margin: 0 0 0.5rem;
  letter-spacing: 0.1em;
}

.auth-title {
  font-family: var(--font-display);
  font-size: clamp(1.5rem, 4vw, 1.85rem);
  font-weight: 700;
  color: var(--cream);
  margin: 0 0 0.65rem;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.auth-lead {
  margin: 0;
  color: var(--gray-400);
  font-size: 0.95rem;
  line-height: 1.65;
  max-width: 38rem;
}

.auth-segmented {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.35rem;
  margin: clamp(1.15rem, 3vw, 1.5rem) 0;
  padding: 0.35rem;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(212, 184, 150, 0.12);
}

.auth-tab {
  min-height: 44px;
  border: none;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
  color: var(--gray-400);
  background: transparent;
  transition:
    color var(--transition),
    background var(--transition),
    box-shadow var(--transition);
}

.auth-tab:hover {
  color: var(--paper);
}

.auth-tab.active {
  color: var(--ink);
  background: linear-gradient(180deg, var(--accent) 0%, #c9a66b 100%);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
}

.auth-tab:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.auth-submit {
  margin-top: 0.35rem;
  width: 100%;
  justify-content: center;
  min-height: 48px;
}

.auth-feedback {
  margin: 1rem 0 0;
}

/* Customer reference: flat black/white auth screen */
.auth-page {
  background: #fff;
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
}

.auth-container {
  max-width: 620px;
}

.auth-shell {
  padding: clamp(1rem, 2.5vw, 1.5rem);
  border: 2px solid #1d1d1b;
  border-radius: 0;
  background: #fff;
  box-shadow: none;
}

.auth-eyebrow {
  margin: 0 0 0.45rem;
  color: #1d1d1b;
  letter-spacing: 0;
  text-transform: lowercase;
}

.auth-title {
  margin: 0 0 0.65rem;
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
  font-size: clamp(2.1rem, 4vw, 3.2rem);
  font-weight: 900;
  line-height: 0.98;
  letter-spacing: 0;
}

.auth-lead {
  color: #1d1d1b;
  font-size: 1rem;
  line-height: 1.18;
}

.auth-segmented {
  gap: 0;
  padding: 0;
  border: 2px solid #1d1d1b;
  border-radius: 999px;
  background: #fff;
  overflow: hidden;
}

.auth-tab {
  min-height: 44px;
  border-radius: 0;
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: lowercase;
}

.auth-tab:hover {
  color: #1d1d1b;
  background: #efefef;
}

.auth-tab.active {
  background: #1d1d1b;
  color: #fff;
  box-shadow: none;
}

.auth-tab:focus-visible {
  outline: 2px solid #1d1d1b;
  outline-offset: 2px;
}

.auth-form {
  gap: 0.95rem;
}

.auth-page :deep(.form-label) {
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: none;
}

.auth-page :deep(.form-input) {
  min-height: 48px;
  border: 2px solid #1d1d1b;
  border-radius: 0;
  background: #fff;
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
}

.auth-page :deep(.form-input:focus) {
  border-color: #1d1d1b;
  box-shadow: 0 0 0 3px rgba(29, 29, 27, 0.12);
}

.auth-submit {
  min-height: 48px;
  border-radius: 999px;
  background: #1d1d1b;
  border-color: #1d1d1b;
  color: #fff;
  font-family: Arial, Helvetica, sans-serif;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: none;
}

.auth-feedback {
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
}
</style>
