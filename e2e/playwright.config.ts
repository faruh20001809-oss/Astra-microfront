import { defineConfig, devices } from '@playwright/test'

const module2Base = process.env.MODULE2_BASE_URL || 'http://127.0.0.1:5173'
const adminBase = process.env.ADMIN_BASE_URL || 'http://127.0.0.1:8080'
const module3Base = process.env.MODULE3_BASE_URL || 'http://127.0.0.1:5000'

export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  timeout: 60_000,
  reporter: [['list'], ['html', { open: 'never' }]],
  snapshotPathTemplate: '{testDir}/{testFileDir}/__screenshots__/{projectName}/{arg}{ext}',
  expect: {
    toHaveScreenshot: {
      maxDiffPixelRatio: 0.02,
      animations: 'disabled',
    },
  },
  use: {
    trace: 'on-first-retry',
    locale: 'ru-RU',
    timezoneId: 'Europe/Astrakhan',
    colorScheme: 'light',
  },
  projects: [
    {
      name: 'module2-desktop',
      testMatch: /module2\/.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        baseURL: module2Base,
        viewport: { width: 1280, height: 900 },
      },
      webServer: process.env.SKIP_MODULE2_SERVER
        ? undefined
        : {
            command: 'npm run dev:e2e',
            cwd: '../module2',
            url: module2Base,
            reuseExistingServer: !process.env.CI,
            timeout: 120_000,
          },
    },
    {
      name: 'astrakhan-admin-desktop',
      testMatch: /astrakhan-admin\/.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        baseURL: adminBase,
        viewport: { width: 1280, height: 900 },
      },
    },
    {
      name: 'module3-desktop',
      testMatch: /module3\/.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        baseURL: module3Base,
        viewport: { width: 1280, height: 900 },
      },
    },
  ],
})
