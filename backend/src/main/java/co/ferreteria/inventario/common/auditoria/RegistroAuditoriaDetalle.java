/**
 * Nombre: RegistroAuditoriaDetalle.java
 * Descripción: Detalle de un registro de auditoría con los valores antes y después.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonRawValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Registro de auditoría completo (RF-SEG-002, paso 4: valores antes y después, IP y fecha).
 *
 * @param id identificador
 * @param fecha fecha y hora del evento
 * @param usuarioId id del usuario que lo realizó
 * @param usuario nombre de usuario
 * @param ip dirección IP del cliente
 * @param accion acción auditada
 * @param entidad tabla afectada
 * @param entidadId id del registro afectado
 * @param valoresAntes fila antes del cambio, en JSON (sin hash de contraseña)
 * @param valoresDespues fila después del cambio, en JSON (sin hash de contraseña)
 */
public record RegistroAuditoriaDetalle(Long id, OffsetDateTime fecha, Long usuarioId, String usuario, String ip,
		String accion, String entidad, Long entidadId,
		@Schema(type = "object") @JsonRawValue String valoresAntes,
		@Schema(type = "object") @JsonRawValue String valoresDespues) {
}
