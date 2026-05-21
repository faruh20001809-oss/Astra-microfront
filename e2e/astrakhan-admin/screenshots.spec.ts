import { expect, test } from '@playwright/test'
import {
  adminReachable,
  adminScreenshot,
  loginAsAdmin,
  mockPoiAiDescription,
  prepareAdminPage,
} from './helpers/admin.js'

test.describe('astrakhan-admin — визуальные тесты (KOS-UI)', () => {
  test.beforeEach(async ({ baseURL }, testInfo) => {
    if (!(await adminReachable(baseURL))) {
      testInfo.skip(
        true,
        `Spring Admin не запущен (${baseURL}). Запуск: cd astrakhan-admin && mvn spring-boot:run -Dspring-boot.run.profiles=e2e`,
      )
    }
  })

  test('KOS-UI-01 — страница авторизации @diploma', async ({ page }) => {
    await prepareAdminPage(page)
    await page.goto('/login', { waitUntil: 'load' })
    await page.locator('#username').waitFor({ state: 'visible' })
    await page.locator('#password').waitFor({ state: 'visible' })
    await page.locator('button[type="submit"]').waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-01', 'login', { diploma: true })
  })

  test('KOS-UI-02 — интерфейс после входа', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin', { waitUntil: 'load' })
    await page.locator('h1').filter({ hasText: 'Дашборд' }).waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-02', 'dashboard-after-login')
  })

  test('KOS-UI-03 — список POI @diploma', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/poi', { waitUntil: 'load' })
    await page.locator('table').waitFor({ state: 'visible' })
    await page.locator('a[href*="/admin/poi/create"]').waitFor({ state: 'visible' })
    await page.locator('.filter-bar').waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-03', 'poi-list', { diploma: true })
  })

  test('KOS-UI-04 — форма создания POI @diploma', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/poi/create', { waitUntil: 'load' })
    await page.locator('#poiForm').waitFor({ state: 'visible' })
    await page.locator('[name="name"]').fill('E2E Визуальная точка')
    await page.locator('[name="category"]').selectOption('История')
    await page.locator('#latInput').fill('46.3497')
    await page.locator('#lngInput').fill('48.0408')
    await page.locator('#poi-description-field').fill(
      'Описание тестовой точки для скриншота формы редактирования в админке сотрудника.',
    )
    await adminScreenshot(page, 'KOS-UI-04', 'poi-form', {
      diploma: true,
      mask: ['#dgisCoordMap'],
    })
  })

  test('KOS-UI-05 — AI-описание POI @diploma', async ({ page }) => {
    await loginAsAdmin(page)
    await mockPoiAiDescription(page)
    await page.goto('/admin/poi/edit/1', { waitUntil: 'load' })
    const aiBtn = page.locator('#poi-ai-generate-btn')
    if (await aiBtn.count()) {
      await page.locator('#poi-description-field').evaluate((el) => {
        ;(el as HTMLTextAreaElement).value = ''
      })
      page.once('dialog', (dialog) => dialog.accept())
      const aiResponse = page.waitForResponse(
        (r) => r.url().includes('/admin/api/poi/ai-description') && r.status() === 200,
        { timeout: 20_000 },
      )
      await aiBtn.click()
      await aiResponse
      await expect(page.locator('#poi-ai-msg')).toContainText('Готово', { timeout: 10_000 })
    }
    await adminScreenshot(page, 'KOS-UI-05', 'poi-ai-description', {
      diploma: true,
      mask: ['#dgisCoordMap'],
    })
  })

  test('KOS-UI-06 — preview карточки POI', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/poi/edit/1', { waitUntil: 'load' })
    await page.locator('#pvPoiName').waitFor({ state: 'visible' })
    await page.locator('h4').filter({ hasText: 'Preview карточки' }).waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-06', 'poi-preview', {
      mask: ['#dgisCoordMap'],
    })
  })

  test('KOS-UI-07 — список маршрутов', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/routes', { waitUntil: 'load' })
    await page.locator('table').waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-07', 'routes-list')
  })

  test('KOS-UI-08 — форма маршрута', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/routes/edit/1', { waitUntil: 'load' })
    await page.locator('form').first().waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-08', 'route-form')
  })

  test('KOS-UI-09 — список товаров', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/products', { waitUntil: 'load' })
    await page.locator('table').waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-09', 'products-list')
  })

  test('KOS-UI-10 — список заказов @diploma', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/orders', { waitUntil: 'load' })
    await page.locator('table').waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-10', 'orders-list', { diploma: true })
  })

  test('KOS-UI-11 — карточка заказа @diploma', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/orders/view/2', { waitUntil: 'commit' })
    await page.getByText('Timeline статусов').waitFor({ state: 'visible', timeout: 45_000 })
    await page.locator('button').filter({ hasText: 'Подтвердить' }).first().waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-11', 'order-card', { diploma: true })
  })

  test('KOS-UI-12 — смена статуса заказа (форма действий)', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/orders/view/2', { waitUntil: 'commit' })
    await page.locator('form[action*="/admin/orders/status/"]').first().waitFor({
      state: 'visible',
      timeout: 45_000,
    })
    await adminScreenshot(page, 'KOS-UI-12', 'order-status-actions')
  })

  test('KOS-UI-13 — обратная связь', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/feedback', { waitUntil: 'load' })
    await page.locator('table, .card').first().waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-13', 'feedback-list')
  })

  test('KOS-UI-14 — предложения POI @diploma', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/poi-suggestions', { waitUntil: 'load' })
    await page.locator('table').waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-14', 'poi-suggestions', { diploma: true })
  })

  test('KOS-UI-15 — аналитика @diploma', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/analytics', { waitUntil: 'load' })
    await page.locator('h1').filter({ hasText: 'Аналитика' }).waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-15', 'analytics', { diploma: true })
  })

  test('KOS-UI-16 — профиль сотрудника', async ({ page }) => {
    await loginAsAdmin(page)
    await page.goto('/admin/profile', { waitUntil: 'load' })
    await page.locator('form').first().waitFor({ state: 'visible' })
    await adminScreenshot(page, 'KOS-UI-16', 'profile')
  })
})
