/**
 * Astrakhan Heritage — Node.js Backend (Module 2)
 * Express server: Groq proxy, routes API, products API,
 * orders, contact form
 */

import express from 'express'
import cors from 'cors'
import { readFileSync, existsSync } from 'fs'
import Groq from 'groq-sdk'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const app = express()
const PORT = process.env.PORT || 3001

// ─── Config ───────────────────────────────────────
const GROQ_API_KEY = process.env.GROQ_API_KEY || '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'
const groq = new Groq({ apiKey: GROQ_API_KEY })

// ─── Middleware ───────────────────────────────────
app.use(cors({ origin: ['http://localhost:5173', 'http://localhost:4173'] }))
app.use(express.json({ limit: '10mb' }))

// Request logger
app.use((req, _res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`)
  next()
})

// ─── Groq proxy ───────────────────────────────────

/**
 * POST /api/groq/generate
 * Body: { messages: [{role, content}] }
 * Proxies to Groq chat completions
 */
app.post('/api/groq/generate', async (req, res) => {
  try {
    const { messages } = req.body
    if (!messages?.length) {
      return res.status(400).json({ error: 'messages required' })
    }

    const completion = await groq.chat.completions.create({
      model: 'meta-llama/llama-4-scout-17b-16e-instruct',
      messages,
      max_tokens: 1024,
      temperature: 0.7
    })

    const content = completion.choices[0]?.message?.content || ''
    res.json({ content })
  } catch (err) {
    console.error('Groq generate error:', err.message)
    res.status(500).json({ error: err.message })
  }
})

/**
 * POST /api/node/ai/generate — для фронта (как fallback когда OpenRouter недоступен)
 * Body: { poi: { name, description, category, year, ... }, audience: 'default'|'children'|'academic' }
 * Returns: { status: 'success', data: { content: string } }
 */
const promptsByAudience = {
  default: (poi) => `Ты опытный экскурсовод Астрахани. Напиши маленькое увлекательное описание для объекта "${poi.name}". Факты: год — ${poi.year || 'неизвестен'}, категория — ${poi.category}. Базовое описание: ${poi.description || 'нет данных'}. Расскажи об архитектурных особенностях и интересных фактах. Пиши живо и эмоционально.`,
  children: (poi) => `Ты добрый гид для детей. Объясни простыми словами, что такое "${poi.name}" в Астрахани. Год: ${poi.year || 'давным-давно'}. Категория: ${poi.category}. Используй простые слова и интересные сравнения.`,
  academic: (poi) => `Напиши академическое описание объекта культурного наследия "${poi.name}" (г. Астрахань). Год: ${poi.year}. Архитектор: ${poi.architect || 'неизвестен'}. Категория: ${poi.category}. Опиши историко-архитектурную ценность и культурное значение.`
}

app.post('/api/node/ai/generate', async (req, res) => {
  try {
    const { poi, audience = 'default' } = req.body
    if (!poi?.name) {
      return res.status(400).json({ status: 'error', message: 'poi with name required' })
    }
    const promptFn = promptsByAudience[audience] || promptsByAudience.default
    const userContent = promptFn(poi)
    const completion = await groq.chat.completions.create({
      model: 'meta-llama/llama-4-scout-17b-16e-instruct',
      messages: [
        { role: 'system', content: 'Ты помощник-историк, специалист по архитектуре и истории Астрахани. Отвечай на русском языке.' },
        { role: 'user', content: userContent }
      ],
      max_tokens: 600,
      temperature: 0.3
    })
    const content = (completion.choices[0]?.message?.content || '').trim()
    res.json({ status: 'success', data: { content } })
  } catch (err) {
    console.error('AI generate error:', err.message)
    res.status(500).json({ status: 'error', message: err.message })
  }
})

/**
 * POST /api/groq/tts
 * Body: { text: string }
 * Returns WAV audio
 */
app.post('/api/groq/tts', async (req, res) => {
  try {
    const { text } = req.body
    if (!text) return res.status(400).json({ error: 'text required' })

    // Trim text to avoid excessive costs
    const trimmed = text.slice(0, 2000)

    const wav = await groq.audio.speech.create({
      model: 'playai-tts',
      voice: 'Celeste-PlayAI',
      response_format: 'wav',
      input: trimmed
    })

    const buffer = Buffer.from(await wav.arrayBuffer())
    res.set({
      'Content-Type': 'audio/wav',
      'Content-Length': buffer.length
    })
    res.send(buffer)
  } catch (err) {
    console.error('TTS error:', err.message)
    res.status(500).json({ error: err.message })
  }
})

/**
 * POST /api/node/ai/tts — для фронта. Пробует Yandex TTS (Python), иначе Groq.
 * Body: { text: string }
 * Returns: audio/mpeg (Yandex) или audio/wav (Groq)
 */
app.post('/api/node/ai/tts', async (req, res) => {
  try {
    const { text, voice, emotion } = req.body || {}
    if (!text) return res.status(400).json({ error: 'text required' })
    const trimmed = (text || '').slice(0, 5000).trim()
    if (!trimmed) return res.status(400).json({ error: 'text required' })

    // Авто-подключение Python-скрипта из репозитория, если переменная окружения не задана
    let yandexScript = process.env.YANDEX_TTS_SCRIPT || process.env.YANDEX_TTS_PYTHON
    if (!yandexScript) {
      const candidate = path.resolve(__dirname, '../../scripts/yandex_tts.py')
      if (existsSync(candidate)) {
        yandexScript = candidate
      }
    }

    if (yandexScript) {
      const { spawn } = await import('child_process')
      const args = [yandexScript]
      if (voice) {
        args.push('--voice', String(voice))
      }
      if (emotion) {
        args.push('--emotion', String(emotion))
      }
      const py = spawn(process.env.PYTHON_PATH || 'python3', args, { stdio: ['pipe', 'pipe', 'pipe'] })
      const chunks = []
      py.stdin.write(trimmed, () => py.stdin.end())
      py.stdout.on('data', (chunk) => chunks.push(chunk))
      py.stderr.on('data', (d) => console.error('[yandex-tts]', d.toString()))
      const code = await new Promise((resolve) => py.on('close', resolve))
      if (code === 0 && chunks.length > 0) {
        const buffer = Buffer.concat(chunks)
        res.set({ 'Content-Type': 'audio/mpeg', 'Content-Length': buffer.length })
        return res.send(buffer)
      }
    }

    // Fallback: Groq TTS
    const wav = await groq.audio.speech.create({
      model: 'playai-tts',
      voice: 'Celeste-PlayAI',
      response_format: 'wav',
      input: trimmed.slice(0, 2000)
    })
    const buffer = Buffer.from(await wav.arrayBuffer())
    res.set({ 'Content-Type': 'audio/wav', 'Content-Length': buffer.length })
    res.send(buffer)
  } catch (err) {
    console.error('TTS error:', err.message)
    res.status(500).json({ error: err.message })
  }
})

// ─── Routes API ───────────────────────────────────

// Try to fetch from Java API, fall back to mock
app.get('/api/routes', async (req, res) => {
  try {
    const javaRes = await fetch('http://localhost:8080/api/v1/routes?published=true', {
      signal: AbortSignal.timeout(3000)
    })
    if (javaRes.ok) {
      const data = await javaRes.json()
      return res.json(data)
    }
  } catch { /* Java not available */ }

  res.json(getMockRoutes())
})

// ─── Products API ─────────────────────────────────

app.get('/api/products', async (req, res) => {
  try {
    const javaRes = await fetch('http://localhost:8080/api/v1/products', {
      signal: AbortSignal.timeout(3000)
    })
    if (javaRes.ok) {
      const data = await javaRes.json()
      return res.json(data)
    }
  } catch { /* Java not available */ }

  res.json(getMockProducts())
})

// ─── Orders ───────────────────────────────────────

app.post('/api/orders', async (req, res) => {
  const { items, delivery, total } = req.body

  if (!items?.length) {
    return res.status(400).json({ error: 'items required' })
  }

  // Try to forward to Java
  try {
    const javaRes = await fetch('http://localhost:8080/api/v1/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ items, delivery, total }),
      signal: AbortSignal.timeout(3000)
    })
    if (javaRes.ok) {
      const data = await javaRes.json()
      return res.json(data)
    }
  } catch { /* Java not available */ }

  // Mock order creation
  const orderId = `AST-${Date.now()}`
  console.log(`[ORDER] ${orderId}:`, { items: items.length, delivery, total })
  res.json({ orderId, status: 'pending', message: 'Заказ принят' })
})

// ─── Contact form ─────────────────────────────────

app.post('/api/contact', async (req, res) => {
  const { name, email, subject, message } = req.body

  if (!name || !email || !message) {
    return res.status(400).json({ error: 'name, email, message required' })
  }

  // Log contact message (replace with email/Telegram integration)
  console.log('[CONTACT]', { name, email, subject, message: message.slice(0, 100) })

  // Optional: forward to Java module
  try {
    const javaRes = await fetch('http://localhost:8080/api/v1/feedback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, subject, message }),
      signal: AbortSignal.timeout(3000)
    })
    if (javaRes.ok) return res.json({ ok: true })
  } catch { /* Java not available */ }

  res.json({ ok: true, message: 'Сообщение получено' })
})

// ─── Health check ─────────────────────────────────

app.get('/api/health', (_req, res) => {
  res.json({ status: 'ok', ts: new Date().toISOString() })
})

// ─── Start ────────────────────────────────────────

app.listen(PORT, () => {
  console.log(`\n🟢 Astrakhan Heritage API running on http://localhost:${PORT}`)
  console.log(`   Groq TTS: /api/groq/tts`)
  console.log(`   Groq Gen: /api/groq/generate`)
  console.log(`   Routes:   /api/routes`)
  console.log(`   Products: /api/products`)
  console.log(`   Orders:   POST /api/orders`)
  console.log(`   Contact:  POST /api/contact\n`)
})

// ─── Mock data ────────────────────────────────────

function getMockRoutes() {
  return [
    {
      id: 1, title: 'Кремль и его окрестности',
      category: 'Архитектура', duration: '2–3 часа', distance: '2.5 км',
      isPaid: false, price: null, rating: 4.8,
      description: 'Прогулка по историческому центру Астрахани.',
      stops: [
        { name: 'Астраханский Кремль', description: 'Главная достопримечательность' },
        { name: 'Успенский собор', description: 'Шедевр барокко XVII в.' }
      ]
    },
    {
      id: 2, title: 'Купеческая Астрахань',
      category: 'История', duration: '3–4 часа', distance: '4 км',
      isPaid: true, price: 450, rating: 4.6,
      description: 'Маршрут по особнякам купцов XIX века.',
      stops: [
        { name: 'Гостиный двор', description: 'Центр торговли XIX в.' },
        { name: 'Индийское подворье', description: 'Шёлковый путь' }
      ]
    }
  ]
}

function getMockProducts() {
  return [
    { id: 1, name: 'Футболка «Кремль»', category: 'Одежда', price: 1490, emoji: '👕', description: 'Хлопковая футболка с силуэтом кремля.', variants: ['S', 'M', 'L', 'XL'] },
    { id: 2, name: 'Кружка «Лотос»', category: 'Посуда', price: 890, emoji: '☕', description: 'Керамическая кружка 350 мл.', variants: null },
    { id: 3, name: 'Открытки «Зодчество»', category: 'Открытки', price: 120, emoji: '✉', description: 'Набор из 5 открыток.', variants: null },
    { id: 4, name: 'Магнит «Кремль»', category: 'Сувениры', price: 290, emoji: '🧲', description: '3D-магнит из полирезины.', variants: null }
  ]
}
