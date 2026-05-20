import type { Page, Route } from '@playwright/test'
import {
  MOCK_POIS,
  MOCK_PRODUCTS,
  MOCK_ROUTES,
  javaSuccess,
} from './api-data.js'

function matchPath(pathname: string): string {
  const idx = pathname.indexOf('/api/v1')
  return idx >= 0 ? pathname.slice(idx + '/api/v1'.length) : pathname
}

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  })
}

async function handleJavaApi(route: Route) {
  const url = new URL(route.request().url())
  const path = matchPath(url.pathname)
  const method = route.request().method()

  if (path === '/pois' && method === 'GET') {
    return json(route, javaSuccess(MOCK_POIS))
  }
  if (path === '/pois/catalog-revision' && method === 'GET') {
    return json(route, javaSuccess({ revision: MOCK_POIS.length }))
  }
  if (path.startsWith('/pois/since') && method === 'GET') {
    return json(route, javaSuccess([]))
  }
  const poiMatch = path.match(/^\/pois\/(\d+)$/)
  if (poiMatch && method === 'GET') {
    const poi = MOCK_POIS.find((p) => String(p.id) === poiMatch[1])
    return json(route, javaSuccess(poi ?? MOCK_POIS[0]))
  }

  if (path.startsWith('/routes') && method === 'GET' && path === '/routes') {
    return json(route, javaSuccess(MOCK_ROUTES))
  }
  const routeMatch = path.match(/^\/routes\/(\d+)$/)
  if (routeMatch && method === 'GET') {
    const r = MOCK_ROUTES.find((item) => String(item.id) === routeMatch[1])
    return json(route, javaSuccess(r ?? MOCK_ROUTES[0]))
  }

  if (path === '/products' && method === 'GET') {
    return json(route, javaSuccess(MOCK_PRODUCTS))
  }
  const productMatch = path.match(/^\/products\/(\d+)$/)
  if (productMatch && method === 'GET') {
    const p = MOCK_PRODUCTS.find((item) => String(item.id) === productMatch[1])
    return json(route, javaSuccess(p ?? MOCK_PRODUCTS[0]))
  }

  if (path.startsWith('/telegram/link/status') && method === 'GET') {
    return json(route, javaSuccess({ linked: false }))
  }
  if (path.startsWith('/user-progress') && method === 'GET') {
    return json(route, javaSuccess({ achievements: [], events: [], rewards: [] }))
  }
  if (path.startsWith('/orders/') && path.includes('/payment-status') && method === 'GET') {
    return json(route, javaSuccess({ paid: true, status: 'PAID', orderId: 'ORD-E2E-1' }))
  }

  if (method === 'POST') {
    return json(route, javaSuccess({ ok: true }))
  }

  return json(route, javaSuccess([]))
}

/** Перехватывает `/java-api/api/v1/*` для стабильных UI-скриншотов без Spring Boot. */
export async function installJavaApiMocks(page: Page) {
  await page.route('**/java-api/api/v1/**', handleJavaApi)
}

/** Блокирует внешние картинки (Wikimedia, 2GIS static) — меньше шума в pixel-diff. */
export async function blockExternalImages(page: Page) {
  await page.route('**/*', async (route) => {
    const req = route.request()
    if (req.resourceType() !== 'image') {
      return route.continue()
    }
    const host = new URL(req.url()).hostname
    if (host === '127.0.0.1' || host === 'localhost') {
      return route.continue()
    }
    return route.abort()
  })
}
