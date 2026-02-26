import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import fs from 'fs';
import path from 'path';

// When deployed under a subpath (e.g. /etlobtodayenginrepo-frontend/), set VITE_BASE_PATH at build time
const base = process.env.VITE_BASE_PATH || '/';

/** Plugin: copy index.html to 404.html after build so SPAs work when host serves 404.html for unknown paths */
function copyIndexTo404() {
  return {
    name: 'copy-index-to-404',
    closeBundle() {
      const outDir = path.resolve(process.cwd(), 'dist');
      const indexPath = path.join(outDir, 'index.html');
      const notFoundPath = path.join(outDir, '404.html');
      if (fs.existsSync(indexPath)) {
        fs.copyFileSync(indexPath, notFoundPath);
        console.log('Copied index.html to 404.html for SPA fallback');
      }
    },
  };
}

export default defineConfig({
  base,
  plugins: [vue(), copyIndexTo404()],
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

