import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/aws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/aws/, ''),
      },
      '/api/aliyun': {
        target: 'http://localhost:8081', 
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/aliyun/, ''),
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
  publicDir: 'public', // Copy files from public/ to dist/
})