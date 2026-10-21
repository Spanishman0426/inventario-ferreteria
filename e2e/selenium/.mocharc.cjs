/**
 * Nombre: .mocharc.cjs
 * Descripción: Configuración de Mocha para las pruebas E2E con Selenium WebDriver: archivos de prueba,
 *              tiempo máximo por escenario y reportero (consola + JUnit XML).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
const path = require('node:path');

module.exports = {
  spec: ['tests/**/*.spec.js'],
  // Cada escenario abre su propio Chrome; las esperas internas son de 10 s.
  timeout: 120000,
  slow: 20000,
  // Un navegador a la vez: los escenarios comparten la base del entorno e2e.
  parallel: false,
  reporter: path.join(__dirname, 'support', 'reportero.cjs'),
};
