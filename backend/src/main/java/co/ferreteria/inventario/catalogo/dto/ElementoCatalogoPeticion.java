/**
 * Nombre: ElementoCatalogoPeticion.java
 * Descripción: Datos para crear o renombrar una marca o categoría (POST y PUT de /api/v1/marcas y /api/v1/categorias).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Elemento de catálogo que envía el cliente (RF-CFG-001). El estado se cambia con PATCH .../estado.
 *
 * @param nombre de 2 a 100 caracteres; único sin distinguir mayúsculas; se guarda recortado
 */
@Schema(name = "ElementoCatalogoPeticion", description = "Nombre de una marca o categoría (RF-CFG-001).")
public record ElementoCatalogoPeticion(
		@Schema(description = "Nombre único sin distinguir mayúsculas; se recortan los espacios de los extremos.",
				example = "Stanley", minLength = 2, maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(min = 2, max = 100)
		String nombre) {
}
