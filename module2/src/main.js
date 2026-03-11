import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'
import Button from 'primevue/button'
import Drawer from 'primevue/drawer'
import Dialog from 'primevue/dialog'
import Badge from 'primevue/badge'
import Ripple from 'primevue/ripple'
import App from './App.vue'
import router from './router/index.js'
import 'primeicons/primeicons.css'
import 'primeflex/primeflex.css'
import './assets/styles/main.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      darkModeSelector: '.app-dark'
    }
  },
  ripple: true
})
app.component('Button', Button)
app.component('Drawer', Drawer)
app.component('Dialog', Dialog)
app.component('Badge', Badge)
app.directive('ripple', Ripple)
app.mount('#app')
