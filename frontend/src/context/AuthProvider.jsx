/**
 * Nombre: AuthProvider.jsx
 * Descripción: Proveedor de la sesión: inicia y cierra sesión, restaura la sesión guardada al
 *              recargar, la cierra al vencer el token o ante un 401 de la API (RF-SEG-001).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useCallback, useEffect, useMemo, useState } from 'react';

import { registrarManejadorNoAutorizado } from '../services/apiClient.js';
import { iniciarSesion, obtenerUsuarioActual } from '../services/authService.js';
import { borrarSesion, guardarSesion, leerSesion } from '../utils/sesionStorage.js';
import AuthContext from './AuthContext.js';

/**
 * Provee el contexto de autenticación a la aplicación.
 *
 * @param {object} props propiedades del componente
 * @param {import('react').ReactNode} props.children contenido protegido por la sesión
 * @returns {import('react').JSX.Element} proveedor del contexto
 */
export default function AuthProvider({ children }) {
  // Sesión guardada al montar (por ejemplo, tras recargar la página); se lee una sola vez.
  const [sesionInicial] = useState(() => leerSesion());
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(sesionInicial !== null);
  const [expiraEn, setExpiraEn] = useState(sesionInicial?.expiraEn ?? null);
  const [sesionExpirada, setSesionExpirada] = useState(false);

  /** Cierra la sesión porque el token venció o la API lo rechazó. */
  const expirarSesion = useCallback(() => {
    borrarSesion();
    setUsuario(null);
    setExpiraEn(null);
    setSesionExpirada(true);
  }, []);

  // Restaura la sesión guardada en sessionStorage consultando los permisos vigentes.
  useEffect(() => {
    if (!sesionInicial) return undefined;
    let cancelado = false;
    obtenerUsuarioActual()
      .then((datos) => {
        if (!cancelado) setUsuario(datos);
      })
      .catch(() => {
        if (!cancelado) {
          borrarSesion();
          setExpiraEn(null);
        }
      })
      .finally(() => {
        if (!cancelado) setCargando(false);
      });
    return () => {
      cancelado = true;
    };
  }, [sesionInicial]);

  // Un 401 de cualquier servicio (salvo el login) cierra la sesión.
  useEffect(() => {
    registrarManejadorNoAutorizado(expirarSesion);
    return () => registrarManejadorNoAutorizado(null);
  }, [expirarSesion]);

  // Cierra la sesión cuando vence el token, aunque el usuario no haga ninguna solicitud.
  useEffect(() => {
    if (!expiraEn) return undefined;
    const temporizador = setTimeout(expirarSesion, Math.max(0, expiraEn - Date.now()));
    return () => clearTimeout(temporizador);
  }, [expiraEn, expirarSesion]);

  const login = useCallback(async (nombreUsuario, contrasena) => {
    const respuesta = await iniciarSesion(nombreUsuario, contrasena);
    const sesion = guardarSesion(respuesta.accessToken, respuesta.expiresIn);
    setExpiraEn(sesion.expiraEn);
    setUsuario(respuesta.usuario);
    setSesionExpirada(false);
    return respuesta.usuario;
  }, []);

  const logout = useCallback(() => {
    borrarSesion();
    setUsuario(null);
    setExpiraEn(null);
    setSesionExpirada(false);
  }, []);

  const valor = useMemo(() => {
    const permisos = usuario?.permisos ?? [];
    return {
      usuario,
      permisos,
      autenticado: usuario !== null,
      cargando,
      sesionExpirada,
      login,
      logout,
      // Con un arreglo basta cualquiera de los permisos (por ejemplo, los dos de terceros).
      tienePermiso: (permiso) =>
        Array.isArray(permiso)
          ? permiso.some((p) => permisos.includes(p))
          : permisos.includes(permiso),
    };
  }, [usuario, cargando, sesionExpirada, login, logout]);

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}
