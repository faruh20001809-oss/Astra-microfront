import { test } from '@playwright/test'
import { gotoModule2, screenshotPage } from '../helpers/module2.js'

/**
 * Визуальные регрессионные тесты пользовательского сайта (module2).
 * Эталонные скриншоты: e2e/module2/__screenshots__/module2-desktop/
 *
 * Обновить эталоны: npm run test:update (из каталога e2e)
 */
test.describe('module2 — скриншоты разделов', () => {
  test('главная', async ({ page }) => {
    await gotoModule2(page, '/', {
      waitSelector: '#museum-hero-title',
      settleMs: 400,
    })
    await screenshotPage(page, 'home', { maxDiffPixelRatio: 0.04 })
  })

  test('виртуальный музей', async ({ page }) => {
    await gotoModule2(page, '/virtual-museum', {
      waitSelector: '#museum-virtual-title',
      settleMs: 600,
    })
    await screenshotPage(page, 'virtual-museum', {
      mask: ['.museum-feature-map', '.museum-feature-map__fallback'],
    })
  })

  test('карта (боковая панель, карта замаскирована)', async ({ page }) => {
    await gotoModule2(page, '/map', {
      waitSelector: '.map-sidebar .sidebar-title',
      settleMs: 800,
    })
    await screenshotPage(page, 'map', {
      mask: ['.map-wrapper'],
      maxDiffPixelRatio: 0.03,
    })
  })

  test('онлайн-маршруты', async ({ page }) => {
    await gotoModule2(page, '/routes', {
      waitSelector: '#routes-hero-title',
      settleMs: 500,
    })
    await screenshotPage(page, 'routes')
  })

  test('магазин', async ({ page }) => {
    await gotoModule2(page, '/shop', {
      waitSelector: '.shop-page, .page-wrapper',
      settleMs: 500,
    })
    await screenshotPage(page, 'shop')
  })

  test('карточка товара', async ({ page }) => {
    await gotoModule2(page, '/shop/1', {
      waitSelector: '.product-details-page, .page-wrapper',
      settleMs: 500,
    })
    await screenshotPage(page, 'product-details')
  })

  test('контакты / помощь проекту', async ({ page }) => {
    await gotoModule2(page, '/contact', {
      waitSelector: '.contact-page, .page-wrapper',
      settleMs: 400,
    })
    await screenshotPage(page, 'contact')
  })

  test('личный кабинет', async ({ page }) => {
    await gotoModule2(page, '/profile', {
      waitSelector: '.cabinet-page, .page-wrapper',
      settleMs: 400,
    })
    await screenshotPage(page, 'profile')
  })

  test('вход в профиль', async ({ page }) => {
    await gotoModule2(page, '/profile/auth', {
      waitSelector: '.auth-page, .page-wrapper, form',
      settleMs: 400,
    })
    await screenshotPage(page, 'profile-auth')
  })

  test('мои заказы', async ({ page }) => {
    await gotoModule2(page, '/orders', {
      waitSelector: '.orders-page, .page-wrapper',
      settleMs: 400,
    })
    await screenshotPage(page, 'orders')
  })

  test('успешная оплата', async ({ page }) => {
    await gotoModule2(page, '/payment/success?orderId=ORD-E2E-1', {
      waitSelector: '.payment-success-page, .page-wrapper',
      settleMs: 400,
    })
    await screenshotPage(page, 'payment-success')
  })

  test('карточка POI', async ({ page }) => {
    await gotoModule2(page, '/pois/1', {
      waitSelector: '.poi-details-page, .page-wrapper',
      settleMs: 500,
    })
    await screenshotPage(page, 'poi-details', {
      mask: ['.poi-static-map, .poi-map-preview'],
    })
  })
})
