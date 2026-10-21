/**
 * Nombre: AuthContext.js
 * Descripción: Contexto de React con la sesión del usuario (usuario, permisos, login y logout).
 *              El proveedor está en AuthProvider.jsx y se consume con el hook useAuth.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { createContext } from 'react';

/**
 * Valor que expone el contexto de autenticación.
 *
 * @typedef {object} ValorAuth
 * @property {import('../services/authService.js').UsuarioSesion|null} usuario usuario autenticado
 * @property {string[]} permisos códigos de permiso del usuario
 * @property {boolean} autenticado verdadero si hay usuario
 * @property {boolean} cargando verdadero mientras se restaura una sesión guardada
 * @property {boolean} sesionExpirada verdadero si la sesión terminó por vencimiento o por un 401
 * @property {(usuario: string, contrasena: string) => Promise<object>} login inicia sesión
 * @property {() => void} logout cierra la sesión
 * @property {(permiso: string) => boolean} tienePermiso indica si el usuario tiene un permiso
 */

/** Contexto de autenticación; null fuera de AuthProvider. */
const AuthContext = createContext(/** @type {ValorAuth|null} */ (null));

export default AuthContext;
