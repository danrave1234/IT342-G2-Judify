import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  optimizeDeps: {
    exclude: ['flowbite-react'],
  },
  server: {
    proxy: {
      '/api': {
        target: 'https://judify-795422705086.asia-east1.run.app',
        changeOrigin: true,
        secure: true
      },
      '/ws': {
        target: 'https://judify-795422705086.asia-east1.run.app',
        ws: true,
        changeOrigin: true,
        secure: true,
      }
    }
  },
  build: {
    rollupOptions: {
      external: ['tailwindcss/version.js'],
    },
  },
})
