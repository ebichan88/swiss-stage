/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // 開発時は /api をバックエンド(Spring Boot)へプロキシ
      // xfwd: true で X-Forwarded-Host 等を付与し、Spring側の {baseUrl}(OAuth2リダイレクトURI組み立て)を
      // localhost:5173 ベースで解決させる(付けないと localhost:8080 になり redirect_uri_mismatch になる)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './tests/setup.ts',
    include: ['tests/unit/**/*.test.{ts,tsx}'],
  },
});
