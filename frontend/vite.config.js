import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': 'http://localhost:9100',
      '/uploads': 'http://localhost:9100',
      '/ws': {
        target: 'ws://localhost:9100',
        ws: true
      }
    }
  }
})
