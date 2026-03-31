<template>
  <div class="page-wrapper profile-auth-page">
    <div class="container">
      <section class="auth-shell card">
        <div class="auth-head">
          <p class="text-mono auth-kicker">◫ Client Identity</p>
          <h1>Профиль клиента</h1>
          <p class="auth-lead">Создайте профиль или войдите, чтобы сохранять персональные данные и быстрее работать с заказами.</p>
        </div>

        <div class="auth-tabs">
          <button type="button" class="auth-tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">Вход</button>
          <button type="button" class="auth-tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">Регистрация</button>
        </div>

        <form v-if="mode === 'login'" class="auth-form" @submit.prevent="submitLogin">
          <div class="form-group">
            <label class="form-label">Логин или email</label>
            <input v-model.trim="loginForm.loginOrEmail" class="form-input" required />
          </div>
          <div class="form-group">
            <label class="form-label">Пароль</label>
            <input v-model="loginForm.password" type="password" class="form-input" required />
          </div>
          <button type="submit" class="btn btn-primary btn-lg" :disabled="busy">{{ busy ? 'Вход...' : 'Войти' }}</button>
        </form>

        <form v-else class="auth-form" @submit.prevent="submitRegister">
          <div class="form-group">
            <label class="form-label">Логин</label>
            <input v-model.trim="registerForm.login" class="form-input" minlength="3" required />
          </div>
          <div class="form-group">
            <label class="form-label">Email</label>
            <input v-model.trim="registerForm.email" type="email" class="form-input" required />
          </div>
          <div class="form-group">
            <label class="form-label">Пароль</label>
            <input v-model="registerForm.password" type="password" class="form-input" minlength="6" required />
          </div>
          <button type="submit" class="btn btn-accent btn-lg" :disabled="busy">{{ busy ? 'Создание...' : 'Создать профиль' }}</button>
        </form>

        <p v-if="errorText" class="form-error" style="margin-top:0.75rem">{{ errorText }}</p>
        <p v-if="okText" class="form-success" style="margin-top:0.75rem">{{ okText }}</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { javaApi } from '@/api/backend.js'

const router = useRouter()
const mode = ref('login')
const busy = ref(false)
const errorText = ref('')
const okText = ref('')
const loginForm = ref({ loginOrEmail: '', password: '' })
const registerForm = ref({ login: '', email: '', password: '' })

function saveProfile(profile) {
  localStorage.setItem('astra_client_profile', JSON.stringify(profile || {}))
  if (profile?.email) localStorage.setItem('astra_guest_email', String(profile.email).trim())
  window.dispatchEvent(new CustomEvent('astra:client-profile-updated', { detail: { profile } }))
  if (profile?.email) {
    window.dispatchEvent(new CustomEvent('astra:guest-email-updated', { detail: { email: profile.email } }))
  }
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
.profile-auth-page { padding-top: 2rem; }
.auth-shell {
  max-width: 680px;
  margin: 0 auto;
  background: linear-gradient(145deg, rgba(245, 240, 232, 0.06), rgba(245, 240, 232, 0.02));
  border: 1px solid rgba(200, 169, 110, 0.25);
  border-radius: 20px;
  padding: 1.25rem;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
}
.auth-kicker { color: var(--accent); margin-bottom: 0.4rem; }
.auth-lead { color: var(--gray-300); margin-top: 0.5rem; }
.auth-tabs { display: flex; gap: 0.5rem; margin: 1rem 0; }
.auth-tab {
  border: 1px solid var(--gray-700);
  background: var(--gray-900);
  color: var(--gray-300);
  border-radius: 999px;
  padding: 0.45rem 0.9rem;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.auth-tab.active {
  color: var(--ink);
  background: var(--accent);
  border-color: var(--accent);
}
.auth-form { display: grid; gap: 0.5rem; }
</style>
