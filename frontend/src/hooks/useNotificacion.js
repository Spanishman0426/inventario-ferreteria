/**
 * Nombre: useNotificacion.js
 * Descripción: Hook para mostrar notificaciones breves desde cualquier componente.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useContext } from 'react';

import NotificacionContext from '../context/NotificacionContext.js';

/**
 * Devuelve las funciones para notificar.
 *
 * @returns {import('../context/NotificacionContext.js').ValorNotificacion} notificar, exito y error
 * @throws {Error} si se usa fuera del proveedor Notificaciones
 */
export default function useNotificacion() {
  const contexto = useContext(NotificacionContext);
  if (!contexto) {
    throw new Error('useNotificacion debe usarse dentro de Notificaciones');
  }
  return contexto;
}
