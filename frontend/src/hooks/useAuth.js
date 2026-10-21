/**
 * Nombre: useAuth.js
 * Descripción: Hook para leer la sesión (usuario, permisos, login, logout) desde AuthContext.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useContext } from 'react';

import AuthContext from '../context/AuthContext.js';

/**
 * Devuelve el contexto de autenticación.
 *
 * @returns {import('../context/AuthContext.js').ValorAuth} sesión actual y sus acciones
 * @throws {Error} si se usa fuera de AuthProvider
 */
export default function useAuth() {
  const contexto = useContext(AuthContext);
  if (!contexto) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return contexto;
}
