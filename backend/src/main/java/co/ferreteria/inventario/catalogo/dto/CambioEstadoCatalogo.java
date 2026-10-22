/**
 * Nombre: CambioEstadoCatalogo.java
 * Descripción: Activación o desactivación de una marca o categoría (PATCH .../{id}/estado).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.dto;

import co.ferreteria.inventario.catalogo.EstadoCatalogo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado del elemento. Un elemento en uso no se elimina, solo se desactiva (RF-CFG-001).
 *
 * @param estado ACTIVO o INACTIVO
 */
@Schema(name = "CambioEstadoCatalogo", description = "Estado nuevo de una marca o categoría.")
public record CambioEstadoCatalogo(
		@Schema(example = "INACTIVO", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull EstadoCatalogo estado) {
}
