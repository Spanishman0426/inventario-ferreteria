/**
 * Nombre: NotificacionContext.js
 * Descripción: Contexto de React para mostrar notificaciones breves (Snackbar). El proveedor es
 *              el componente Notificaciones y se consume con el hook useNotificacion.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { createContext } from 'react';

/**
 * Funciones que expone el contexto de notificaciones. Los mensajes llegan ya traducidos.
 *
 * @typedef {object} ValorNotificacion
 * @property {(mensaje: string, severidad?: 'success'|'info'|'warning'|'error') => void} notificar muestra un mensaje
 * @property {(mensaje: string) => void} exito muestra un mensaje de éxito
 * @property {(mensaje: string) => void} error muestra un mensaje de error
 */

/** Contexto de notificaciones; null fuera del proveedor. */
const NotificacionContext = createContext(/** @type {ValorNotificacion|null} */ (null));

export default NotificacionContext;
