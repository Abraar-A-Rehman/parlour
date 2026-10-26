import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The dev server forwards /api and /uploads to the Spring Boot backend, so the browser
// only talks to one origin (http://localhost:5173) and no CORS setup is needed in development.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },
})
