/**
 * Nombre: setup.js
 * Descripción: Preparación común de las pruebas de Vitest: matchers de jest-dom, idioma español,
 *              espera de las consultas asíncronas y almacenamiento del navegador limpio antes de cada prueba.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterEach, beforeEach } from 'vitest';

import i18n from '../i18n/index.js';

// findBy y waitFor esperan hasta 5 s (1 s por defecto): con cobertura y todas las pruebas en
// paralelo, montar la aplicación completa con MUI puede tardar más de un segundo.
configure({ asyncUtilTimeout: 5000 });

beforeEach(async () => {
  await i18n.changeLanguage('es');
  // changeLanguage guarda el idioma; se limpia para que cada prueba empiece sin elección previa.
  localStorage.clear();
  sessionStorage.clear();
});

afterEach(() => {
  cleanup();
});
