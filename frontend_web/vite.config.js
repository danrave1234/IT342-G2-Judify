import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  optimizeDeps: {
    exclude: ['flowbite-react'],
  },
  build: {
    rollupOptions: {
      external: ['tailwindcss/version.js'],
    },
  },
})
