import { test, expect } from '@playwright/test'

const adminReachable = async (baseURL?: string) => {
  if (!baseURL) return false
  try {
    const res = await fetch(`${baseURL}/login`, { signal: AbortSignal.timeout(3000) })
    return res.ok
  } catch {
    return false
  }
}

test.describe('astrakhan-admin — скриншоты', () => {
  test.beforeEach(async ({ baseURL }, testInfo) => {
    if (!(await adminReachable(baseURL))) {
      testInfo.skip(true, `Spring Admin не запущен на ${baseURL ?? 'ADMIN_BASE_URL'}. Запустите: cd astrakhan-admin && mvn spring-boot:run`)
    }
  })

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('astra-theme', 'light')
    })
    await page.emulateMedia({ reducedMotion: 'reduce' })
  })

  test('страница входа', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'domcontentloaded' })
    await page.locator('form, .login-page').first().waitFor({ state: 'visible' })
    await expect(page).toHaveScreenshot('login.png', { fullPage: true })
  })
})
