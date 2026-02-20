import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// When deployed under a subpath (e.g. /etlobtodayenginrepo-frontend/), set VITE_BASE_PATH at build time
const base = process.env.VITE_BASE_PATH || '/';

export default defineConfig({
  base,
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:3000',
        changeOrigin: true
      },
      '/uploads': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:3000',
        changeOrigin: true
      }
    }
  }
});

