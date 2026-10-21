/**
 * Nombre: RegistroAuditoriaResumen.java
 * Descripción: Fila del listado de auditoría (sin los valores antes y después).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import java.time.OffsetDateTime;

/**
 * Registro de auditoría en el listado (RF-SEG-002).
 *
 * @param id identificador
 * @param fecha fecha y hora del evento
 * @param usuarioId id del usuario que lo realizó (vacío si no se identificó)
 * @param usuario nombre de usuario
 * @param ip dirección IP del cliente
 * @param accion CREAR, MODIFICAR, ELIMINAR, ANULAR, APROBAR, CAMBIAR_ESTADO, INICIAR_SESION, SESION_FALLIDA o DEPURAR
 * @param entidad tabla afectada
 * @param entidadId id del registro afectado
 */
public record RegistroAuditoriaResumen(Long id, OffsetDateTime fecha, Long usuarioId, String usuario, String ip,
		String accion, String entidad, Long entidadId) {
}
