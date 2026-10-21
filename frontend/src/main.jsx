/**
 * Nombre: main.jsx
 * Descripción: Punto de entrada: inicializa i18n y monta la aplicación en #root.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import './i18n/index.js';
import App from './App.jsx';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
