/**
 * Nombre: ElementoCatalogoRespuesta.java
 * Descripción: Marca o categoría con su estado, tal como la devuelve la administración de catálogos.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.dto;

import co.ferreteria.inventario.catalogo.ElementoCatalogo;
import co.ferreteria.inventario.catalogo.EstadoCatalogo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Elemento de catálogo con estado (RF-CFG-001).
 *
 * @param id identificador
 * @param nombre nombre
 * @param estado ACTIVO o INACTIVO
 */
@Schema(name = "ElementoCatalogo", description = "Marca o categoría con su estado.")
public record ElementoCatalogoRespuesta(
		@Schema(example = "1") Long id,
		@Schema(example = "Stanley") String nombre,
		@Schema(example = "ACTIVO") EstadoCatalogo estado) {

	/**
	 * Vista de una entidad de catálogo.
	 *
	 * @param elemento marca o categoría
	 * @return respuesta
	 */
	public static ElementoCatalogoRespuesta desde(ElementoCatalogo elemento) {
		return new ElementoCatalogoRespuesta(elemento.getId(), elemento.getNombre(),
				EstadoCatalogo.desdeActivo(elemento.isActivo()));
	}

}
