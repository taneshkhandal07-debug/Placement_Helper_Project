import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
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
                configure: function (proxy) {
                    proxy.on('error', function (err) {
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
