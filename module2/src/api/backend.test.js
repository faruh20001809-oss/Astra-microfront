import { beforeEach, describe, expect, it, vi } from 'vitest'

async function loadApi() {
  vi.resetModules()
  return import('./backend.js')
}

function jsonResponse({ status = 200, body = {}, contentType = 'application/json' } = {}) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'ERR',
    url: 'http://test',
    headers: { get: (name) => (name.toLowerCase() === 'content-type' ? contentType : null) },
    json: async () => body,
    text: async () => JSON.stringify(body),
    clone() {
      return this
    },
  }
}

describe('module2 backend api client', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    globalThis.fetch = vi.fn()
  })

  it('requestLookupCode returns success message', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({ status: 200, body: { status: 'success', data: { message: 'Код отправлен' } } }),
    )

    const msg = await javaApi.orders.requestLookupCode('user@mail.com')
    expect(msg).toBe('Код отправлен')
  })

  it('requestLookupCode throws on 429', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({ status: 429, body: { message: 'Слишком часто' } }),
    )

    await expect(javaApi.orders.requestLookupCode('user@mail.com')).rejects.toThrow('Слишком часто')
  })

  it('verifyLookupCode returns order array on success', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({
        status: 200,
        body: { status: 'success', data: [{ orderId: 'ORD-1' }, { orderId: 'ORD-2' }] },
      }),
    )

    const orders = await javaApi.orders.verifyLookupCode('user@mail.com', '123456')
    expect(orders).toHaveLength(2)
    expect(orders[0].orderId).toBe('ORD-1')
  })

  it('getPaymentStatus returns safe fallback for non-OK response', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({ status: 500, body: { status: 'error' } }),
    )

    const status = await javaApi.orders.getPaymentStatus('ORD-X')
    expect(status).toEqual({ paid: false, status: '', orderId: 'ORD-X' })
  })

  it('profile register returns profile payload', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({
        status: 200,
        body: { status: 'success', data: { username: 'client1', email: 'c@x.ru', role: 'Клиент' } },
      }),
    )
    const profile = await javaApi.profile.register({ login: 'client1', email: 'c@x.ru', password: 'secret12' })
    expect(profile.username).toBe('client1')
  })

  it('profile login throws backend message on failure', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({
        status: 400,
        body: { status: 'error', message: 'Неверный пароль.' },
      }),
    )
    await expect(javaApi.profile.login({ loginOrEmail: 'client1', password: 'bad' })).rejects.toThrow('Неверный пароль.')
  })

  it('feedback send throws backend message on failure', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({
        status: 500,
        body: { status: 'error', message: 'Feedback unavailable' },
      }),
    )
    await expect(javaApi.feedback.send({ name: 'A', email: 'a@b.ru', message: 'Hi' })).rejects.toThrow('Feedback unavailable')
  })

  it('preorders.create returns preorderId on success', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({
        status: 200,
        body: { status: 'success', data: { preorderId: 'PRE-ABCD1234', status: 'NEW_PREORDER' } },
      }),
    )
    const data = await javaApi.preorders.create({
      customerName: 'Иван',
      telegramUsername: 'ivan',
      items: [{ id: 1, name: 'Кружка', price: 890, qty: 1 }],
    })
    expect(data.preorderId).toBe('PRE-ABCD1234')
    expect(data.status).toBe('NEW_PREORDER')
  })

  it('preorders.create surfaces validation field errors from backend', async () => {
    const { javaApi } = await loadApi()
    globalThis.fetch.mockResolvedValueOnce(
      jsonResponse({
        status: 400,
        body: {
          status: 'error',
          message: 'Ошибка валидации',
          errors: { contact: 'Заполните Telegram или MAX' },
        },
      }),
    )
    try {
      await javaApi.preorders.create({ items: [] })
      throw new Error('expected to throw')
    } catch (err) {
      expect(err.message).toBe('Ошибка валидации')
      expect(err.fieldErrors).toEqual({ contact: 'Заполните Telegram или MAX' })
    }
  })
})
