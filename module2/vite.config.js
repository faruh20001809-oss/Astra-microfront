// vite.config.js
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'
import { fileURLToPath, URL } from 'node:url'

const useHttpsDev = process.env.VITE_USE_HTTPS === '1'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue(), ...(useHttpsDev ? [basicSsl()] : [])],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: {
        // 🟡 Java API
        '/java-api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/java-api/, ''),
          timeout: 120_000,
          proxyTimeout: 120_000,
          configure: (proxy) => {
            proxy.on('error', (err, req) => {
              console.warn('[vite proxy /java-api]', req?.url, err?.message)
            })
          },
        },

        '/api/ai/chat': {
          target: 'https://openrouter.ai/api',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/api\/ai\/chat/, '/v1/chat'),
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              const apiKey = env.VITE_AI_API_KEY
              if (apiKey) {
                proxyReq.setHeader('Authorization', `Bearer ${apiKey}`)
                console.log('🔑 VITE_AI_API_KEY key attached (first 10 chars):', apiKey.slice(0, 10) + '...')
              } else {
                console.error('❌ VITE_AI_API_KEY not found in .env')
              }
            })
          }
        },
        '/api/node': {
          target: 'http://localhost:3001',
          changeOrigin: true,
        },

        /** 2GIS Routing API — если браузер блокирует CORS, задайте VITE_DGIS_ROUTING_BASE=/api/dgis-routing */
        '/api/dgis-routing': {
          target: 'https://routing.api.2gis.com',
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path.replace(/^\/api\/dgis-routing/, ''),
        },
        /** 2GIS Static API — превью карт в карточках; VITE_DGIS_STATIC_BASE=/api/dgis-static */
        '/api/dgis-static': {
          target: 'https://static.maps.2gis.com',
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path.replace(/^\/api\/dgis-static/, '/2.0'),
        },
      }
    }
  }
})