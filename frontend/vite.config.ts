import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

import fs from 'fs';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    {
      name: 'copy-to-spring-static',
      closeBundle() {
        const distDir = path.resolve(__dirname, 'dist');
        const staticDir = path.resolve(__dirname, 'static');
        try {
          if (fs.existsSync(distDir)) {
            const distHtml = path.join(distDir, 'index.html');
            if (fs.existsSync(distHtml)) {
              fs.copyFileSync(distHtml, path.join(staticDir, 'index.html'));
            }
            const distAssets = path.join(distDir, 'assets');
            const staticAssets = path.join(staticDir, 'assets');
            if (fs.existsSync(distAssets)) {
              fs.cpSync(distAssets, staticAssets, { recursive: true });
            }
          }
        } catch (err) {
          console.warn('[vite-copy-plugin] Could not sync to static:', err);
        }
      },
    },
  ],

  resolve: {
    alias: {
      // Allows imports like: import { apiClient } from '@/shared/api/client'
      '@': path.resolve(__dirname, './src'),
    },
  },

  server: {
    port: 5173,
    strictPort: true,

    /**
     * Development proxy — all /api requests are forwarded to the Spring Boot
     * backend. This means the browser never makes a cross-origin request,
     * so no CORS configuration is needed during development.
     *
     * The session cookie Spring Boot sets will be sent back with every
     * subsequent request transparently.
     */
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        secure: false,
        configure: (proxy) => {
          proxy.on('error', (err) => {
            console.warn('[vite-proxy] /api error:', err.message);
          });
        },
      },
    },
  },

  build: {
    outDir: 'dist',
    sourcemap: true,
    // Ensure assets reference a relative base so the build can be served
    // from /react/ when embedded in Spring Boot static resources.
    assetsDir: 'assets',
    rollupOptions: {
      output: {
        // Code-splitting: vendor libs into a separate chunk for better caching.
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          http: ['axios'],
        },
      },
    },
  },

  preview: {
    port: 4173,
  },
});
