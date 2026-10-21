/**
 * Nombre: setup.js
 * Descripción: Preparación común de las pruebas de Vitest: matchers de jest-dom, idioma español y
 *              almacenamiento del navegador limpio antes de cada prueba.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, beforeEach } from 'vitest';

import i18n from '../i18n/index.js';

beforeEach(async () => {
  await i18n.changeLanguage('es');
  // changeLanguage guarda el idioma; se limpia para que cada prueba empiece sin elección previa.
  localStorage.clear();
  sessionStorage.clear();
});

afterEach(() => {
  cleanup();
});
