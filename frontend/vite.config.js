/**
 * Nombre: vite.config.js
 * Descripción: Configuración de Vite (servidor de desarrollo con proxy HTTPS al backend) y de Vitest.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

/** Backend de desarrollo (perfil dev de Spring Boot, HTTPS con certificado autofirmado). */
const BACKEND_DEV_POR_DEFECTO = 'https://localhost:8443';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_');
  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      // El navegador llama a /api en el mismo origen y Vite lo reenvía al backend: no depende de CORS.
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || BACKEND_DEV_POR_DEFECTO,
          changeOrigin: true,
          // Acepta el certificado autofirmado del backend local (solo en desarrollo).
          secure: false,
        },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.js'],
      css: false,
      restoreMocks: true,
    },
  };
});
