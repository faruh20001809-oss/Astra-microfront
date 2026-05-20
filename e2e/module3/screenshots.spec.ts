import { test, expect } from '@playwright/test'

const module3Reachable = async (baseURL?: string) => {
  if (!baseURL) return false
  try {
    const res = await fetch(`${baseURL}/login`, { signal: AbortSignal.timeout(3000) })
    return res.ok
  } catch {
    return false
  }
}

test.describe('module3 — скриншоты', () => {
  test.beforeEach(async ({ baseURL }, testInfo) => {
    if (!(await module3Reachable(baseURL))) {
      testInfo.skip(true, `Flask module3 не запущен на ${baseURL ?? 'MODULE3_BASE_URL'}. Запустите: cd module3 && flask run`)
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
    await page.locator('#loginForm, .login-page').first().waitFor({ state: 'visible' })
    await expect(page).toHaveScreenshot('login.png', { fullPage: true })
  })
})
