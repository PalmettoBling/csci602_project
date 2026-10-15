import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

/**
 * Backend origin used by the dev server only. The browser never sees this: it
 * calls same-origin paths and the dev server forwards them, which keeps CORS
 * out of the development loop entirely.
 *
 * For a deployed frontend, set VITE_API_BASE_URL at build time instead (see
 * src/api/client.ts) -- the proxy below does not apply to `vite build` output.
 */
const DEV_API_TARGET = 'http://localhost:5001'

/**
 * Path prefixes owned by the Spring Boot API. Listed explicitly rather than
 * proxying everything, so Vite keeps serving the app shell and its assets.
 */
const API_PATHS = [
  '/auth',
  '/users',
  '/activities',
  '/account',
  '/verification',
  '/health',
  '/info',
  '/v3/api-docs',
  '/swagger-ui',
]

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: Object.fromEntries(
      API_PATHS.map((path) => [path, { target: DEV_API_TARGET, changeOrigin: true }]),
    ),
  },
})
