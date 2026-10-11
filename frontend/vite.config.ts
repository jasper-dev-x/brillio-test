import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    // Dev only: forward API calls to Spring Boot so the browser sees a single origin.
    proxy: {
      '/api': process.env.API_URL ?? 'http://localhost:8080',
    },
  },
})
