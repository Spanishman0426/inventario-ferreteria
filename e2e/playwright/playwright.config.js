/**
 * Nombre: playwright.config.js
 * Descripción: Configuración de Playwright: frontend del entorno docker-compose.e2e.yml, Chromium,
 *              reporte HTML y JUnit XML, y captura de pantalla y traza solo cuando una prueba falla.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { defineConfig, devices } from '@playwright/test';

import { BASE_URL } from '../comun/entorno.js';

const enCI = Boolean(process.env.CI);

export default defineConfig({
  testDir: './tests',
  outputDir: './reportes/artefactos',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  forbidOnly: enCI,
  retries: enCI ? 1 : 0,
  // Dos navegadores a la vez: el login bloquea la fila del usuario mientras calcula BCrypt, y con más
  // navegadores los inicios de sesión simultáneos del mismo usuario se encolan y superan las esperas.
  workers: 2,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'reportes/html', open: 'never' }],
    ['junit', { outputFile: 'reportes/junit/resultados-playwright.xml' }],
  ],
  use: {
    baseURL: BASE_URL,
    locale: 'es-CO',
    timezoneId: 'America/Bogota',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off',
    actionTimeout: 15_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1366, height: 900 } },
    },
  ],
});
