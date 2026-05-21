import { defineConfig, devices } from '@playwright/test'

const module2Base = process.env.MODULE2_BASE_URL || 'http://127.0.0.1:5173'
const adminBase = process.env.ADMIN_BASE_URL || 'http://127.0.0.1:8080'
const module3Base = process.env.MODULE3_BASE_URL || 'http://127.0.0.1:5000'

const startModule2Server = !process.env.SKIP_MODULE2_SERVER
const startAdminServer = !process.env.SKIP_ADMIN_SERVER
const isWin = process.platform === 'win32'

export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  timeout: 60_000,
  reporter: [['list'], ['html', { open: 'never' }]],
  snapshotPathTemplate: '{testDir}/{testFileDir}/__screenshots__/{arg}{ext}',
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
      snapshotPathTemplate: '{testDir}/{testFileDir}/__screenshots__/{projectName}/{arg}{ext}',
      use: {
        ...devices['Desktop Chrome'],
        baseURL: module2Base,
        viewport: { width: 1280, height: 900 },
      },
      webServer: startModule2Server
        ? {
            command: 'npm run dev:e2e',
            cwd: '../module2',
            url: module2Base,
            reuseExistingServer: !process.env.CI,
            timeout: 120_000,
          }
        : undefined,
    },
    {
      name: 'astrakhan-admin-desktop',
      testMatch: /astrakhan-admin\/.*\.spec\.ts/,
      fullyParallel: false,
      use: {
        ...devices['Desktop Chrome'],
        baseURL: adminBase,
        viewport: { width: 1280, height: 900 },
      },
      webServer: startAdminServer
        ? {
            command: isWin
              ? 'mvn.cmd -q spring-boot:run -Dspring-boot.run.profiles=e2e'
              : 'mvn -q spring-boot:run -Dspring-boot.run.profiles=e2e',
            cwd: '../astrakhan-admin',
            url: `${adminBase}/login`,
            reuseExistingServer: !process.env.CI,
            timeout: 240_000,
          }
        : undefined,
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
