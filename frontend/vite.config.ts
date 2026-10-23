import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Em dev, /api é repassado para o Spring Boot (evita CORS).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080' },
  },
})
