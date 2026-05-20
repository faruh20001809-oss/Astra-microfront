import { expect, type Page } from '@playwright/test'
import { blockExternalImages, installJavaApiMocks } from '../fixtures/mock-java-api.js'

export async function prepareModule2Page(page: Page) {
  await installJavaApiMocks(page)
  await blockExternalImages(page)
  await page.addInitScript(() => {
    localStorage.setItem('astra-theme', 'light')
    localStorage.removeItem('astra-cart')
    localStorage.removeItem('astra_client_profile')
    localStorage.removeItem('astra_guest_email')
  })
  await page.emulateMedia({ reducedMotion: 'reduce' })
}

export async function gotoModule2(
  page: Page,
  path: string,
  options?: { waitSelector?: string; settleMs?: number },
) {
  await prepareModule2Page(page)
  await page.goto(path, { waitUntil: 'load' })
  if (options?.waitSelector) {
    await page.locator(options.waitSelector).first().waitFor({ state: 'visible', timeout: 30_000 })
  }
  await page.waitForLoadState('networkidle').catch(() => {})
  if (options?.settleMs) {
    await page.waitForTimeout(options.settleMs)
  }
}

export async function screenshotPage(
  page: Page,
  name: string,
  options?: { mask?: string[]; maxDiffPixelRatio?: number },
) {
  const masks = (options?.mask ?? []).map((sel) => page.locator(sel))
  await expect(page).toHaveScreenshot(`${name}.png`, {
    fullPage: true,
    mask: masks.length ? masks : undefined,
    maxDiffPixelRatio: options?.maxDiffPixelRatio,
  })
}
