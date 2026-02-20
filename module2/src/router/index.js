import { createRouter, createWebHistory } from 'vue-router'
import MapView from '@/views/MapView.vue'
import RoutesView from '@/views/RoutesView.vue'
import ShopView from '@/views/ShopView.vue'
import ContactView from '@/views/ContactView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: MapView,
    meta: { title: 'Карта' }
  },
  {
    path: '/routes',
    name: 'routes',
    component: RoutesView,
    meta: { title: 'Маршруты' }
  },
  {
    path: '/shop',
    name: 'shop',
    component: ShopView,
    meta: { title: 'Магазин' }
  },
  {
    path: '/contact',
    name: 'contact',
    component: ContactView,
    meta: { title: 'Контакты' }
  },
  // 👇 Catch-all для неизвестных путей (опционально)
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),  // 👈 Добавьте BASE_URL
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title} — Астрахань. Живая История`
})

export default router