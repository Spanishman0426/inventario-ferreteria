/**
 * Nombre: App.jsx
 * Descripción: Componente raíz: proveedores globales, enrutador del navegador, sesión y rutas.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { BrowserRouter } from 'react-router';

import AuthProvider from './context/AuthProvider.jsx';
import ProveedoresApp from './ProveedoresApp.jsx';
import RutasApp from './RutasApp.jsx';

/**
 * Aplicación completa.
 *
 * @returns {import('react').JSX.Element} aplicación
 */
export default function App() {
  return (
    <ProveedoresApp>
      <BrowserRouter>
        <AuthProvider>
          <RutasApp />
        </AuthProvider>
      </BrowserRouter>
    </ProveedoresApp>
  );
}
