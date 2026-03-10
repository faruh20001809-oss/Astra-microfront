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

// ===== УТИЛИТЫ =====

/**
 * Универсальный обработчик ответов Java API
 * @param {Response} response - fetch Response
 * @returns {Promise<any>} - данные (data) или fallback
 */
const handleJavaResponse = async (response) => {
  try {
    // 🔹 1. Проверка Content-Type
    const contentType = response.headers.get('content-type')
    if (!contentType || !contentType.includes('application/json')) {
      // Попробуем прочитать как текст для отладки
      const text = await response.clone().text().catch(() => '')
      console.error('❌ API returned non-JSON:', {
        url: response.url,
        status: response.status,
        contentType,
        preview: text.substring(0, 300)
      })

      // Если это HTML-страница ошибки Vite/Spring
      if (text.startsWith('<!DOCTYPE') || text.startsWith('<html')) {
        throw new Error('API returned HTML page instead of JSON. Check proxy configuration.')
      }

      throw new Error(`Expected JSON, got ${contentType || 'unknown'}`)
    }

    // 🔹 2. Проверка HTTP статуса
    if (!response.ok) {
      const error = await response.json().catch(() => ({
        message: response.statusText,
        status: response.status
      }))
      throw new Error(error.message || `HTTP ${response.status}: ${response.statusText}`)
    }

    // 🔹 3. Парсинг JSON
    const json = await response.json()

    // 🔹 4. Обработка формата Java API: { status, data }
    if (json?.status === 'success' && json.data !== undefined) {
      return json.data
    }

    // 🔹 5. Если ответ уже массив — возвращаем
    if (Array.isArray(json)) {
      return json
    }

    // 🔹 6. Если ответ — объект с данными
    if (json && typeof json === 'object' && !json.status) {
      return json
    }

    // 🔹 7. Fallback: предупреждение и пустой массив
    console.warn('⚠️ Unexpected API response format:', json)
    return []

  } catch (err) {
    console.error('❌ handleJavaResponse error:', err)
    // Возвращаем пустой массив для устойчивости UI
    return []
  }
}

/**
 * Базовый fetch-запрос с обработкой ошибок
 * @param {string} url
 * @param {RequestInit} options
 * @returns {Promise<Response>}
 */
const baseFetch = async (url, options = {}) => {
  // 🔹 Безопасное объединение headers (избегаем TypeScript конфликтов)
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('Accept', 'application/json')

  const fetchOptions = {
    ...options,
    headers
  }

  try {
    const response = await fetch(url, fetchOptions)
    return response
  } catch (networkError) {
    console.error('Network error:', networkError)
    throw new Error('Нет соединения с сервером. Проверьте, запущен ли backend.')
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

    /** @param {number} poiId @param {Object} review */
    addReview: async (poiId, review) => {
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
    getList: async (filters = {}) => {
      const params = new URLSearchParams({ published: 'true', ...filters })
      const res = await baseFetch(`${JAVA_API_BASE}/routes?${params}`)
      return handleJavaResponse(res)
    },

    /** @param {number} id */
    getById: async (id) => {
      const res = await baseFetch(`${JAVA_API_BASE}/routes/${id}`)
      return handleJavaResponse(res)
    }
  },

  /** Products (Товары) */
  products: {
    getList: async (filters = {}) => {
      const params = new URLSearchParams(filters)
      const res = await baseFetch(`${JAVA_API_BASE}/products?${params}`)
      return handleJavaResponse(res)
    },

    /** @param {number} id */
    getById: async (id) => {
      const res = await baseFetch(`${JAVA_API_BASE}/products/${id}`)
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

    /** Статус оплаты заказа: { paid: boolean, paidAt?: string } */
    getPaymentStatus: async (orderId) => {
      const res = await baseFetch(`${JAVA_API_BASE}/orders/${orderId}/payment-status`)
      const json = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(json.message || 'Ошибка запроса')
      return json.data
    }
  },

  /** Feedback (Обратная связь) */
  feedback: {
    /** @param {Object} feedbackData */
    send: async (feedbackData) => {
      const res = await baseFetch(`${JAVA_API_BASE}/feedback`, {
        method: 'POST',
        body: JSON.stringify(feedbackData)
      })
      return handleJavaResponse(res)
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

    /** @param {string} text */
    synthesizeSpeech: async (text) => {
      const res = await baseFetch(`${NODE_API_BASE}/ai/tts`, {
        method: 'POST',
        body: JSON.stringify({ text })
      })
      return res.blob() // TTS возвращает audio/blob
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