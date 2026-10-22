/**
 * Nombre: CambioEstadoTercero.java
 * Descripción: Estado nuevo de un tercero (PATCH /api/v1/terceros/{id}/estado).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero.dto;

import co.ferreteria.inventario.tercero.EstadoTercero;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado del tercero. Los terceros no se eliminan, solo se desactivan (RF-TER-001).
 *
 * @param estado ACTIVO o INACTIVO
 */
@Schema(name = "CambioEstadoTercero", description = "Estado nuevo de un tercero.")
public record CambioEstadoTercero(
		@Schema(example = "INACTIVO", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull EstadoTercero estado) {
}
