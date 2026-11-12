import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Zapytania /api idą do backendu, więc w dev nie trzeba CORS
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
