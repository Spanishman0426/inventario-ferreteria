/**
 * Nombre: RutasApp.jsx
 * Descripción: Definición de las rutas de la aplicación y de los permisos que exige cada una.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { Navigate, Route, Routes } from 'react-router';

import AppLayout from './components/AppLayout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import CatalogosPage from './pages/CatalogosPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import ProductosPage from './pages/ProductosPage.jsx';

/**
 * Rutas: login público; el resto exige sesión y el permiso indicado.
 *
 * @returns {import('react').JSX.Element} árbol de rutas
 */
export default function RutasApp() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/productos" replace />} />
        <Route
          path="productos"
          element={
            <ProtectedRoute permiso="CATALOGO_CONSULTAR">
              <ProductosPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="catalogos"
          element={
            <ProtectedRoute permiso="CONFIG_GESTIONAR">
              <CatalogosPage />
            </ProtectedRoute>
          }
        />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
