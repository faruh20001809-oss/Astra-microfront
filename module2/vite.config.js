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
      }
    }
  }
})