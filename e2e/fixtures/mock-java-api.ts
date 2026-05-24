import type { Page, Route } from '@playwright/test'
import {
  MOCK_POIS,
  MOCK_PRODUCTS,
  MOCK_ROUTES,
  javaSuccess,
} from './api-data.js'

export type JavaMockState = {
  completions: Map<string, Set<number>>
  redemptions: Map<string, number>
  ordersByEmail: Map<string, Array<Record<string, unknown>>>
}

export function createJavaMockState(): JavaMockState {
  return {
    completions: new Map(),
    redemptions: new Map(),
    ordersByEmail: new Map(),
  }
}

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

export function progressForEmail(email: string, state: JavaMockState) {
  const set = state.completions.get(email) ?? new Set<number>()
  const completedRoutes = set.size
  const paidIds = MOCK_ROUTES.filter((r) => (r.price ?? 0) > 0).map((r) => r.id)
  const freeIds = MOCK_ROUTES.filter((r) => !(r.price ?? 0)).map((r) => r.id)
  let completedPaid = 0
  let completedFree = 0
  for (const id of set) {
    if (paidIds.includes(id)) completedPaid += 1
    if (freeIds.includes(id)) completedFree += 1
  }
  const eligiblePacks = Math.min(Math.floor(completedPaid / 2), Math.floor(completedFree / 3))
  const used = state.redemptions.get(email) ?? 0
  const availableRewards = Math.max(eligiblePacks - used, 0)
  return {
    email,
    completedRoutes,
    completedPaidRoutes: completedPaid,
    completedFreeRoutes: completedFree,
    earnedRewards: eligiblePacks,
    availableRewards,
    usedRewards: used,
  }
}

async function handleJavaApi(route: Route, state: JavaMockState) {
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
    const base = poi ?? MOCK_POIS[0]
    return json(route, javaSuccess({
      ...base,
      description: base.shortDescription,
      lat: base.latitude,
      lng: base.longitude,
    }))
  }

  if (path === '/routes' && method === 'GET') {
    return json(route, javaSuccess(MOCK_ROUTES))
  }
  const routeMatch = path.match(/^\/routes\/(\d+)$/)
  if (routeMatch && method === 'GET') {
    const r = MOCK_ROUTES.find((item) => String(item.id) === routeMatch[1])
    return json(route, javaSuccess(r ?? MOCK_ROUTES[0]))
  }

  const routeCompleteMatch = path.match(/^\/routes\/(\d+)\/complete$/)
  if (routeCompleteMatch && method === 'POST') {
    let email = ''
    try {
      const body = route.request().postDataJSON() as { email?: string }
      email = String(body?.email || '').trim().toLowerCase()
    } catch {
      email = 'guest@e2e.test'
    }
    const routeId = Number(routeCompleteMatch[1])
    if (!state.completions.has(email)) state.completions.set(email, new Set())
    state.completions.get(email)!.add(routeId)
    return json(route, javaSuccess(progressForEmail(email, state)))
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
    return json(route, javaSuccess({ linked: true, username: 'bog_e2e_user', chatId: '12345' }))
  }
  if (path === '/telegram/link/request' && method === 'POST') {
    return json(route, javaSuccess({ botDeepLink: 'https://t.me/astra_e2e_bot', expiresInSec: 600 }))
  }

  if (path.startsWith('/user-progress/confirmed') && method === 'GET') {
    const email = (url.searchParams.get('email') || 'guest@e2e.test').trim().toLowerCase()
    return json(route, javaSuccess(progressForEmail(email, state)))
  }

  if (path.startsWith('/user-progress/sync') && method === 'POST') {
    return json(route, javaSuccess({ synced: true }))
  }
  if (path === '/user-progress' && method === 'GET') {
    const email = (url.searchParams.get('email') || 'guest@e2e.test').trim().toLowerCase()
    return json(route, javaSuccess({ snapshot: null, ...progressForEmail(email, state) }))
  }

  if (path === '/orders/lookup/request-code' && method === 'POST') {
    return json(route, javaSuccess({ message: 'Код отправлен на email (E2E mock)' }))
  }

  if (path === '/orders/lookup/verify' && method === 'POST') {
    let email = 'guest@e2e.test'
    try {
      const body = route.request().postDataJSON() as { email?: string }
      email = String(body?.email || email).trim().toLowerCase()
    } catch {
      /* ignore */
    }
    if (!state.ordersByEmail.has(email)) {
      state.ordersByEmail.set(email, [
        {
          orderId: 'ORD-BOG-E2E-001',
          status: 'CONFIRMED',
          total: 1290,
          currency: 'RUB',
          createdAt: '2025-05-20T10:00:00Z',
          items: [{ productId: 1, name: 'Футболка «Астрахань»', qty: 1, price: 1290 }],
        },
      ])
    }
    return json(route, javaSuccess(state.ordersByEmail.get(email) ?? []))
  }

  if (path === '/orders' && method === 'POST') {
    return json(route, javaSuccess({
      orderId: 'ORD-BOG-CREATE-001',
      status: 'PENDING',
      total: 1290,
      currency: 'RUB',
    }))
  }

  if (path.startsWith('/orders/') && path.includes('/payment-status') && method === 'GET') {
    return json(route, javaSuccess({ paid: true, status: 'PAID', orderId: 'ORD-BOG-E2E-001' }))
  }

  if (path === '/rewards/redeem' && method === 'POST') {
    let email = ''
    try {
      const body = route.request().postDataJSON() as { email?: string; routeId?: number }
      email = String(body?.email || '').trim().toLowerCase()
      const stats = progressForEmail(email, state)
      if ((stats.availableRewards ?? 0) < 1) {
        return json(
          route,
          { status: 'error', message: 'Недостаточно наград для списания' },
          400,
        )
      }
      state.redemptions.set(email, (state.redemptions.get(email) ?? 0) + 1)
      return json(route, javaSuccess(progressForEmail(email, state)))
    } catch {
      return json(route, { status: 'error', message: 'Недостаточно наград' }, 400)
    }
  }

  if (path === '/preorders' && method === 'POST') {
    return json(route, javaSuccess({ preorderId: 'PRE-BOG-001', status: 'NEW' }))
  }

  if (path === '/profile/login' && method === 'POST') {
    return json(route, javaSuccess({
      login: 'bog_e2e',
      email: 'bog.e2e@example.com',
      displayName: 'Тестовый пользователь ВКР',
    }))
  }

  if (path === '/profile/register' && method === 'POST') {
    return json(route, javaSuccess({
      login: 'bog_e2e',
      email: 'bog.e2e@example.com',
      displayName: 'Тестовый пользователь ВКР',
    }))
  }

  if (method === 'POST') {
    return json(route, javaSuccess({ ok: true }))
  }

  return json(route, javaSuccess([]))
}

/** Перехватывает Java API и внешние картинки одним обработчиком (стабильнее, чем два route). */
export async function installJavaApiMocks(page: Page, state = createJavaMockState()) {
  await page.route('**/*', async (route) => {
    const req = route.request()
    const url = req.url()

    if (url.includes('/java-api/api/v1')) {
      return handleJavaApi(route, state)
    }

    if (req.resourceType() === 'image') {
      const host = new URL(url).hostname
      if (host !== '127.0.0.1' && host !== 'localhost') {
        return route.abort()
      }
    }

    return route.continue()
  })
}

/** @deprecated Используйте installJavaApiMocks — блокировка изображений встроена. */
export async function blockExternalImages(page: Page) {
  await installJavaApiMocks(page)
}
