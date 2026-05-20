import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import VirtualMuseumView from '@/views/VirtualMuseumView.vue'
import MapView from '@/views/MapView.vue'
import RoutesView from '@/views/RoutesView.vue'
import RouteShareView from '@/views/RouteShareView.vue'
import PoiDetailsView from '@/views/PoiDetailsView.vue'
import ShopView from '@/views/ShopView.vue'
import ProductDetailsView from '@/views/ProductDetailsView.vue'
import ContactView from '@/views/ContactView.vue'
import PaymentSuccessView from '@/views/PaymentSuccessView.vue'
import OrdersView from '@/views/OrdersView.vue'
import CabinetView from '@/views/CabinetView.vue'
import ProfileAuthView from '@/views/ProfileAuthView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { title: 'Главная' }
  },
  {
    path: '/virtual-museum',
    name: 'virtual-museum',
    component: VirtualMuseumView,
    meta: { title: 'Виртуальный музей деревянной архитектуры' }
  },
  {
    path: '/map',
    name: 'map',
    component: MapView,
    meta: { title: 'Карта' }
  },
  {
    path: '/pois/:id',
    name: 'poi-details',
    component: PoiDetailsView,
    meta: { title: 'Точка интереса — полная информация' },
    props: true
  },
  {
    path: '/routes',
    name: 'routes',
    component: RoutesView,
    meta: { title: 'Маршруты' }
  },
  {
    path: '/routes/:id',
    name: 'route-share',
    component: RouteShareView,
    meta: { title: 'Маршрут' }
  },
  {
    path: '/profile',
    name: 'profile',
    component: CabinetView,
    meta: { title: 'Профиль' }
  },
  {
    path: '/profile/auth',
    name: 'profile-auth',
    component: ProfileAuthView,
    meta: { title: 'Вход в профиль' }
  },
  {
    path: '/cabinet',
    redirect: '/profile'
  },
  {
    path: '/orders',
    name: 'orders',
    component: OrdersView,
    meta: { title: 'Мои заказы' }
  },
  {
    path: '/shop',
    name: 'shop',
    component: ShopView,
    meta: { title: 'Магазин' }
  },
  {
    path: '/shop/:productId',
    name: 'product-details',
    component: ProductDetailsView,
    meta: { title: 'Товар' },
    props: true
  },
  {
    path: '/contact',
    name: 'contact',
    component: ContactView,
    meta: { title: 'Контакты' }
  },
  {
    path: '/payment/success',
    name: 'payment-success',
    component: PaymentSuccessView,
    meta: { title: 'Оплата успешна' }
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
  scrollBehavior(to) {
    if (to.hash) return { el: to.hash, top: 80, behavior: 'smooth' }
    return { top: 0 }
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title} — Астрахань. Живая История`
})

export default router
