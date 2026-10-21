/**
 * Nombre: FiltroAuditoria.java
 * Descripción: Filtros opcionales de la consulta de auditoría.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import java.time.LocalDate;

/**
 * Filtros de RF-SEG-002; cualquiera puede ir vacío.
 *
 * @param usuarioId autor del cambio
 * @param entidad tabla afectada (producto, documento, usuario, periodo...)
 * @param accion acción auditada
 * @param desde primer día del rango (hora de Colombia)
 * @param hasta último día del rango, incluido
 */
public record FiltroAuditoria(Long usuarioId, String entidad, String accion, LocalDate desde, LocalDate hasta) {
}
