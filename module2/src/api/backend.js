/**
 * API-клиент для модуля 2 (Vue + Node.js)
 *
 * Архитектура запросов:
 * - /java-api/api/v1/* → Spring Boot (Java) через прокси /java-api
 * - /api/node/* → Node.js Express сервер
 *
 * Формат ответа Java API: { status: "success", data: [...] }
 */

// ===== КОНФИГУРАЦИЯ =====
// В dev с Vite прокси: /java-api → localhost:8080. Если заказы не доходят — задайте VITE_JAVA_API_BASE=http://localhost:8080/api/v1
const JAVA_API_BASE = import.meta.env.VITE_JAVA_API_BASE || '/java-api/api/v1'
const NODE_API_BASE = '/api/node'          // Прокси на Node.js:3001
const IS_DEV = import.meta.env.DEV

/** Однократные предупреждения по URL, чтобы не засорять консоль. */
const apiWarnedUrls = new Set()

function warnApiOnce(url, message, detail) {
  const key = `${url}|${message}`
  if (apiWarnedUrls.has(key)) return
  apiWarnedUrls.add(key)
  if (IS_DEV) {
    console.warn(`[API] ${message}`, detail ?? '')
  }
}

function backendHintFromHtml(text, status) {
  if (status === 502 || status === 503 || status === 504) {
    return 'Сервер API недоступен (502/503/504). Проверьте, что Spring Boot запущен на :8080 и прокси /java-api настроен.'
  }
  if (text.includes('<!DOCTYPE') || text.includes('<html')) {
    return 'Вместо JSON пришла HTML-страница. Обычно backend не запущен или неверный прокси /java-api.'
  }
  return null
}

/**
 * Универсальный обработчик ответов Java API
 * @param {Response} response - fetch Response
 * @returns {Promise<any>} - данные (data) или fallback
 */
function isIncompleteResponseError(err) {
  const msg = String(err?.message || err)
  return (
    msg.includes('chunked') ||
    msg.includes('ERR_INCOMPLETE') ||
    msg.includes('network error') ||
    err?.name === 'TypeError'
  )
}

/** 502/503/504 — бэкенд не поднят, не маскируем пустым массивом. */
function isGatewayError(err) {
  const msg = String(err?.message || err)
  return /502|503|504|недоступен/i.test(msg)
}

const handleJavaResponse = async (response) => {
  const url = response.url || ''
  try {
    const contentType = response.headers.get('content-type') || ''
    const rawText = await response.text()
    const trimmed = rawText.trim()

    if (!response.ok) {
      const hint = backendHintFromHtml(trimmed, response.status)
      warnApiOnce(url, `HTTP ${response.status}`, hint || trimmed.slice(0, 200))
      throw new Error(hint || `HTTP ${response.status}: ${response.statusText}`)
    }

    if (!trimmed) {
      warnApiOnce(url, 'Пустой ответ API (возможен обрыв chunked-передачи)', null)
      throw new Error('Пустой ответ сервера')
    }

    let json
    const looksJson = trimmed.startsWith('{') || trimmed.startsWith('[')
    if (looksJson) {
      try {
        json = JSON.parse(trimmed)
      } catch (parseErr) {
        warnApiOnce(url, 'Не удалось разобрать JSON', parseErr?.message)
        throw parseErr
      }
    } else {
      const hint = backendHintFromHtml(trimmed, response.status)
        || `Ожидался JSON, получен ${contentType || 'неизвестный тип'}`
      warnApiOnce(url, hint, trimmed.slice(0, 200))
      throw new Error(hint)
    }

    if (json?.status === 'success' && json.data !== undefined) {
      return json.data
    }
    if (Array.isArray(json)) {
      return json
    }
    if (json && typeof json === 'object' && !json.status) {
      return json
    }

    warnApiOnce(url, 'Неожиданный формат ответа API', json)
    return []
  } catch (err) {
    if (isIncompleteResponseError(err)) {
      throw err
    }
    warnApiOnce(url, err?.message || 'Ошибка API', null)
    return []
  }
}

const RETRYABLE_STATUS = new Set([502, 503, 504])

/** Нормализует тело Java API в массив (список маршрутов, POI и т.д.). */
export function unwrapJavaList(data) {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object') {
    if (Array.isArray(data.items)) return data.items
    if (Array.isArray(data.data)) return data.data
  }
  return []
}

/**
 * Базовый fetch-запрос с обработкой ошибок
 * @param {string} url
 * @param {RequestInit} options
 * @returns {Promise<Response>}
 */
const baseFetch = async (url, options = {}, attempt = 0) => {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }
  headers.set('Accept', 'application/json')

  const fetchOptions = {
    ...options,
    headers,
  }

  try {
    const response = await fetch(url, fetchOptions)
    if (attempt < 1 && RETRYABLE_STATUS.has(response.status)) {
      await new Promise((r) => setTimeout(r, 400))
      return baseFetch(url, options, attempt + 1)
    }
    return response
  } catch (networkError) {
    if (attempt < 1) {
      await new Promise((r) => setTimeout(r, 400))
      return baseFetch(url, options, attempt + 1)
    }
    warnApiOnce(url, 'Нет соединения с API', networkError?.message)
    throw new Error('Нет соединения с сервером. Запустите Spring Boot (порт 8080) и dev-прокси /java-api.')
  }
}

// ===== JAVA API (Spring Boot) =====
// Все эндпоинты: /java-api/api/v1/* → http://localhost:8080/api/v1/*

export const javaApi = {
  /** POIs (Точки интереса) */
  pois: {
    /** @param {Object} filters - { status: 'PUBLISHED', category: '...', search: '...' } */
    getList: async (filters = {}) => {
      const params = new URLSearchParams({ status: 'PUBLISHED', ...filters })
      const res = await baseFetch(`${JAVA_API_BASE}/pois?${params}`)
      return handleJavaResponse(res)
    },

    /** @param {number} id */
    getById: async (id) => {
      const res = await baseFetch(`${JAVA_API_BASE}/pois/${id}`)
      return handleJavaResponse(res)
    },

    /** Max id среди сейчас видимых на карте ТОИ (для polling). */
    getCatalogRevision: async () => {
      try {
        const res = await baseFetch(`${JAVA_API_BASE}/pois/catalog-revision`)
        const data = await handleJavaResponse(res)
        const n = Number(data?.revision)
        return Number.isFinite(n) ? n : 0
      } catch {
        return 0
      }
    },

    /** Новые видимые ТОИ с id > afterId */
    listSince: async (afterId, limit = 10) => {
      try {
        const params = new URLSearchParams({
          afterId: String(afterId),
          limit: String(Math.min(Math.max(limit, 1), 20)),
        })
        const res = await baseFetch(`${JAVA_API_BASE}/pois/since?${params}`)
        const data = await handleJavaResponse(res)
        return Array.isArray(data) ? data : []
      } catch {
        return []
      }
    },

    /** Подписка на рассылку о новых точках */
    subscribeNewsletter: async (email) => {
      const res = await baseFetch(`${JAVA_API_BASE}/newsletter/subscribe`, {
        method: 'POST',
        body: JSON.stringify({ email: String(email || '').trim() }),
      })
      return handleJavaResponse(res)
    },

    /**
     * @deprecated По ТЗ (раздел 6) функционал оценки точек удаляется.
     * Метод оставлен для обратной совместимости первого этапа миграции
     * и НЕ должен вызываться из UI. На следующем этапе будет удален.
     * @param {number} poiId @param {Object} review
     */
    addReview: async (poiId, review) => {
      console.warn('[deprecated] javaApi.pois.addReview: оценки точек отключены в UI согласно ТЗ.')
      const res = await baseFetch(`${JAVA_API_BASE}/pois/${poiId}/reviews`, {
        method: 'POST',
        body: JSON.stringify(review)
      })
      return handleJavaResponse(res)
    }
  },

  /** Routes (Маршруты) */
  routes: {
    /** @param {Object} filters - { published: 'true', category: '...' } */
    getList: async (filters = {}, attempt = 0) => {
      const params = new URLSearchParams({ published: 'true', ...filters })
      const url = `${JAVA_API_BASE}/routes?${params}`
      try {
        const res = await baseFetch(url)
        return await handleJavaResponse(res)
      } catch (err) {
        if (attempt < 1 && isIncompleteResponseError(err)) {
          await new Promise((r) => setTimeout(r, 500))
          return javaApi.routes.getList(filters, attempt + 1)
        }
        warnApiOnce(url, err?.message || 'Ошибка загрузки маршрутов', null)
        throw err
      }
    },

    /** @param {number} id @param {string} [email] */
    getById: async (id, email) => {
      const params = email ? `?email=${encodeURIComponent(String(email).trim())}` : ''
      const res = await baseFetch(`${JAVA_API_BASE}/routes/${id}${params}`)
      return handleJavaResponse(res)
    },
    /** @param {number} id @param {string} email */
    checkAccess: async (id, email) => {
      const params = email ? `?email=${encodeURIComponent(String(email).trim())}` : ''
      const res = await baseFetch(`${JAVA_API_BASE}/routes/${id}/access${params}`)
      return handleJavaResponse(res)
    },
    markCompleted: async (id, email) => {
      const res = await baseFetch(`${JAVA_API_BASE}/routes/${id}/complete`, {
        method: 'POST',
        body: JSON.stringify({ email: String(email || '').trim() }),
      })
      return handleJavaResponse(res)
    },
  },

  /** Products (Товары) */
  products: {
    getList: async (filters = {}) => {
      const params = new URLSearchParams(filters)
      const url = `${JAVA_API_BASE}/products?${params}`
      try {
        const res = await baseFetch(url)
        return await handleJavaResponse(res)
      } catch (err) {
        warnApiOnce(url, err?.message || 'Ошибка загрузки товаров', null)
        if (isGatewayError(err)) throw err
        return []
      }
    },

    /** @param {number} id */
    getById: async (id) => {
      const res = await baseFetch(`${JAVA_API_BASE}/products/${id}`)
      return handleJavaResponse(res)
    }
  },

  /** Пункты самовывоза (тот же справочник, что модалка заказа в Java-админке) */
  pickupPoints: {
    getList: async () => {
      const res = await baseFetch(`${JAVA_API_BASE}/pickup-points`)
      return handleJavaResponse(res)
    }
  },

  /** Orders (Заказы) */
  orders: {
    /** @param {Object} orderData */
    create: async (orderData) => {
      const res = await baseFetch(`${JAVA_API_BASE}/orders`, {
        method: 'POST',
        body: JSON.stringify(orderData)
      })
      const data = await handleJavaResponse(res)
      if (Array.isArray(data) || !data || !data.orderId) {
        throw new Error('Заказ не создан: бэкенд не ответил. Запустите Java-приложение (astrakhan-admin) на порту 8080 и перезагрузите страницу.')
      }
      return data
    },

    /** @param {string} orderId */
    getById: async (orderId) => {
      const res = await baseFetch(`${JAVA_API_BASE}/orders/${orderId}`)
      return handleJavaResponse(res)
    },

    /**
     * Поиск заказа для гостя: null при 404, throw при сети/ошибке API.
     * @param {string} orderId
     * @returns {Promise<object|null>}
     */
    getByIdForGuest: async (orderId) => {
      const id = encodeURIComponent(String(orderId).trim())
      const res = await baseFetch(`${JAVA_API_BASE}/orders/${id}`)
      if (res.status === 404) return null
      const contentType = res.headers.get('content-type') || ''
      if (!contentType.includes('application/json')) {
        const text = await res.text().catch(() => '')
        if (!res.ok) {
          throw new Error(text?.slice(0, 200) || `Ошибка сервера (${res.status})`)
        }
        throw new Error('Сервер вернул не JSON')
      }
      const json = await res.json()
      if (!res.ok) {
        throw new Error(json?.message || `HTTP ${res.status}`)
      }
      if (json?.status === 'success' && json.data !== undefined && json.data !== null) {
        return typeof json.data === 'object' ? json.data : null
      }
      return null
    },

    /** @param {string} email */
    requestLookupCode: async (email) => {
      const res = await baseFetch(`${JAVA_API_BASE}/orders/lookup/request-code`, {
        method: 'POST',
        body: JSON.stringify({ email: String(email || '').trim() }),
      })
      const json = await res.json().catch(() => ({}))
      if (res.status === 429) {
        throw new Error(json.message || 'Новый код можно запросить не чаще раза в минуту')
      }
      if (!res.ok || json.status === 'error') {
        throw new Error(json.message || `Ошибка сервера (${res.status})`)
      }
      return json.data?.message || json.message || 'Готово'
    },

    /**
     * @param {string} email
     * @param {string} code
     * @returns {Promise<object[]>}
     */
    verifyLookupCode: async (email, code) => {
      const res = await baseFetch(`${JAVA_API_BASE}/orders/lookup/verify`, {
        method: 'POST',
        body: JSON.stringify({
          email: String(email || '').trim(),
          code: String(code ?? '').trim(),
        }),
      })
      const json = await res.json().catch(() => ({}))
      if (res.status === 401 || res.status === 403) {
        throw new Error(json.message || 'Неверный или просроченный код')
      }
      if (!res.ok || json.status === 'error') {
        throw new Error(json.message || `Ошибка (${res.status})`)
      }
      if (json.status === 'success' && Array.isArray(json.data)) {
        return json.data
      }
      return []
    },

    /**
     * Создать платёж и получить ссылку на оплату (онлайн-касса).
     * @param {string} orderId
     * @param {{ returnUrl: string, cancelUrl?: string }} urls
     * @returns {Promise<{ paymentId: string, redirectUrl: string, orderId: string, amount: number, currency: string }>}
     */
    createPaymentLink: async (orderId, urls) => {
      const res = await baseFetch(`${JAVA_API_BASE}/orders/${orderId}/payment`, {
        method: 'POST',
        body: JSON.stringify(urls || {})
      })
      const json = await res.json().catch(() => ({}))
      if (!res.ok || json.status === 'error') {
        throw new Error(json.message || 'Не удалось создать платёж')
      }
      return json.data
    },

    /** Статус оплаты заказа: { paid: boolean, paidAt?: string, status?: string }. При 404/5xx возвращает безопасный объект. */
    getPaymentStatus: async (orderId) => {
      try {
        const res = await baseFetch(`${JAVA_API_BASE}/orders/${orderId}/payment-status`)
        const json = await res.json().catch(() => ({}))
        if (!res.ok) return { paid: false, status: '', orderId }
        return json.data != null ? json.data : { paid: false, status: '', orderId }
      } catch (_) {
        return { paid: false, status: '', orderId }
      }
    }
  },

  /** Telegram link for guest order updates */
  telegram: {
    requestLink: async ({ email, phone } = {}) => {
      const res = await baseFetch(`${JAVA_API_BASE}/telegram/link/request`, {
        method: 'POST',
        body: JSON.stringify({
          email: String(email || '').trim(),
          phone: String(phone || '').trim(),
        }),
      })
      return handleJavaResponse(res)
    },

    getLinkStatus: async ({ email, phone } = {}) => {
      const params = new URLSearchParams()
      if (email) params.set('email', String(email).trim())
      if (phone) params.set('phone', String(phone).trim())
      const q = params.toString()
      const res = await baseFetch(`${JAVA_API_BASE}/telegram/link/status${q ? `?${q}` : ''}`)
      return handleJavaResponse(res)
    },
  },

  userProgress: {
    getByEmail: async (email) => {
      const params = new URLSearchParams({ email: String(email || '').trim() })
      const res = await baseFetch(`${JAVA_API_BASE}/user-progress?${params}`)
      return handleJavaResponse(res)
    },
    syncByEmail: async (email, snapshot) => {
      const res = await baseFetch(`${JAVA_API_BASE}/user-progress/sync`, {
        method: 'POST',
        body: JSON.stringify({
          email: String(email || '').trim(),
          snapshot: snapshot || {},
        }),
      })
      return handleJavaResponse(res)
    },
    getConfirmedByEmail: async (email) => {
      const params = new URLSearchParams({ email: String(email || '').trim() })
      const res = await baseFetch(`${JAVA_API_BASE}/user-progress/confirmed?${params}`)
      return handleJavaResponse(res)
    },
  },

  rewards: {
    redeemByEmail: async (email, routeId) => {
      const res = await baseFetch(`${JAVA_API_BASE}/rewards/redeem`, {
        method: 'POST',
        body: JSON.stringify({
          email: String(email || '').trim(),
          routeId: Number(routeId),
        }),
      })
      return handleJavaResponse(res)
    },
  },

  profile: {
    register: async ({ login, email, password } = {}) => {
      const res = await baseFetch(`${JAVA_API_BASE}/profile/register`, {
        method: 'POST',
        body: JSON.stringify({
          login: String(login || '').trim(),
          email: String(email || '').trim(),
          password: String(password || ''),
        }),
      })
      const json = await res.json().catch(() => ({}))
      if (!res.ok || json.status === 'error') {
        throw new Error(json.message || 'Не удалось зарегистрировать профиль')
      }
      return json.data
    },
    login: async ({ loginOrEmail, password } = {}) => {
      const res = await baseFetch(`${JAVA_API_BASE}/profile/login`, {
        method: 'POST',
        body: JSON.stringify({
          loginOrEmail: String(loginOrEmail || '').trim(),
          password: String(password || ''),
        }),
      })
      const json = await res.json().catch(() => ({}))
      if (!res.ok || json.status === 'error') {
        throw new Error(json.message || 'Не удалось войти в профиль')
      }
      return json.data
    },
  },

  /** Preorders (Предзаказы корзины) */
  preorders: {
    /**
     * Создает предзаказ. Сервер требует минимум один контакт (telegramUsername или maxUsername).
     * @param {{customerName?:string, phone?:string, email?:string, telegramUsername?:string, maxUsername?:string, comment?:string, items:Array<{id?:number,name:string,price:number,qty:number,category?:string}>}} payload
     * @returns {Promise<object>} карточка предзаказа (preorderId, items, status и т.д.)
     */
    create: async (payload) => {
      const res = await baseFetch(`${JAVA_API_BASE}/preorders`, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      const contentType = res.headers.get('content-type') || ''
      const json = contentType.includes('application/json')
        ? await res.json().catch(() => ({}))
        : {}
      if (!res.ok || json.status === 'error') {
        const err = new Error(json.message || `Не удалось отправить предзаказ (${res.status})`)
        if (json.errors && typeof json.errors === 'object') {
          err.fieldErrors = json.errors
        }
        throw err
      }
      return json?.status === 'success' && json.data !== undefined ? json.data : json
    },

    /** Список предзаказов для админки (опционально по статусу). */
    getList: async (status) => {
      const params = status ? `?status=${encodeURIComponent(status)}` : ''
      const res = await baseFetch(`${JAVA_API_BASE}/preorders${params}`)
      return handleJavaResponse(res)
    },

    getById: async (preorderId) => {
      const res = await baseFetch(`${JAVA_API_BASE}/preorders/${encodeURIComponent(preorderId)}`)
      return handleJavaResponse(res)
    },
  },

  /** Feedback (Обратная связь) */
  feedback: {
    /** @param {Object} feedbackData */
    send: async (feedbackData) => {
      const res = await baseFetch(`${JAVA_API_BASE}/feedback`, {
        method: 'POST',
        body: JSON.stringify(feedbackData)
      })
      const contentType = res.headers.get('content-type') || ''
      const json = contentType.includes('application/json') ? await res.json().catch(() => ({})) : {}
      if (!res.ok || json.status === 'error') {
        throw new Error(json.message || `Не удалось отправить сообщение (${res.status})`)
      }
      return json?.status === 'success' && json.data !== undefined ? json.data : json
    }
  },

  /** POI Suggestions (предложения точек на карту) */
  poiSuggestions: {
    /** @param {Object} data - { name, place, description?, whyAdd } */
    submit: async (data) => {
      const res = await baseFetch(`${JAVA_API_BASE}/poi-suggestions`, {
        method: 'POST',
        body: JSON.stringify(data)
      })
      const json = await res.json().catch(() => ({}))
      if (!res.ok || json.status === 'error') {
        throw new Error(json.message || 'Не удалось отправить предложение')
      }
      return json
    }
  },

  /** Analytics (События) */
  analytics: {
    /** @param {string} eventType @param {Object} data */
    trackEvent: async (eventType, data = {}) => {
      const res = await baseFetch(`${JAVA_API_BASE}/analytics/events`, {
        method: 'POST',
        body: JSON.stringify({ event: eventType, data })
      })
      return handleJavaResponse(res)
    }
  }
}

// ===== NODE.JS API (Express proxy) =====
// Все эндпоинты: /api/node/* → http://localhost:3001/*

export const nodeApi = {
  /** Groq AI / TTS proxy */
  ai: {
    /** @param {Object} poi @param {string} audience */
    generatePoiContent: async (poi, audience = 'default') => {
      const res = await baseFetch(`${NODE_API_BASE}/ai/generate`, {
        method: 'POST',
        body: JSON.stringify({ poi, audience })
      })
      return handleJavaResponse(res)
    },

    /**
     * Синтез речи через Node.js API (Yandex TTS или Groq). Ответ — бинарный audio.
     * @param {string} text
     * @param {{ voice?: string, emotion?: string }=} options
     * @returns {Promise<Blob>}
     */
    synthesizeSpeech: async (text, options = {}) => {
      const { signal, voice, emotion } = options
      const payload = {
        text,
        voice: voice ?? 'jane',
        emotion: emotion != null && emotion !== '' ? emotion : 'good',
      }
      const res = await fetch(`${NODE_API_BASE}/ai/tts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: '*/*' },
        body: JSON.stringify(payload),
        signal,
      })
      if (!res.ok) {
        const errText = await res.text().catch(() => '')
        throw new Error(errText || `Озвучка недоступна (${res.status})`)
      }
      return res.blob()
    }
  },

  /** Прокси для внешних API (2GIS, Яндекс и т.д.) */
  proxy: {
    get2gisPois: async (bounds) => {
      const res = await baseFetch(`${NODE_API_BASE}/proxy/2gis/pois`, {
        method: 'POST',
        body: JSON.stringify({ bounds })
      })
      return res.json()
    }
  }
}

// ===== LEGACY EXPORTS (для обратной совместимости) =====
export async function fetchPoisFromJava(filters = {}) {
  return javaApi.pois.getList(filters)
}

export async function fetchRoutesFromJava(filters = {}) {
  return javaApi.routes.getList(filters)
}

export async function fetchProducts() {
  return javaApi.products.getList()
}

export async function createOrder(payload) {
  return javaApi.orders.create(payload)
}

export async function sendContact(payload) {
  return javaApi.feedback.send(payload)
}

// ===== УДОБНЫЙ ЭКСПОРТ ДЛЯ БЫСТРОГО ДОСТУПА =====
export const apiService = {
  // Java API shortcuts
  getMarkers: javaApi.pois.getList,
  getMarkerById: javaApi.pois.getById,
  getRoutes: javaApi.routes.getList,
  getProducts: javaApi.products.getList,
  createOrder: javaApi.orders.create,
  createPreorder: javaApi.preorders.create,
  sendFeedback: javaApi.feedback.send,

  // Node.js API shortcuts
  generateAiContent: nodeApi.ai.generatePoiContent,
  synthesizeSpeech: nodeApi.ai.synthesizeSpeech,

  // Прямой доступ к полному API
  java: javaApi,
  node: nodeApi
}

// ===== ЭКСПОРТ ПО УМОЛЧАНИЮ =====
export default apiService
