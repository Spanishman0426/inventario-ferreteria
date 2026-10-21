/**
 * Nombre: ReferenciaRespuesta.java
 * Descripción: Par id-nombre de un elemento de catálogo (marca o categoría) para selectores y respuestas.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.dto;

import co.ferreteria.inventario.catalogo.Categoria;
import co.ferreteria.inventario.catalogo.Marca;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Elemento de catálogo identificado por id (marca o categoría).
 *
 * @param id identificador
 * @param nombre nombre legible
 */
@Builder
@Schema(name = "Referencia", description = "Marca o categoría.")
public record ReferenciaRespuesta(@Schema(example = "1") Long id, @Schema(example = "Stanley") String nombre) {

	/**
	 * Vista de una marca.
	 *
	 * @param marca entidad
	 * @return referencia
	 */
	public static ReferenciaRespuesta desde(Marca marca) {
		return new ReferenciaRespuesta(marca.getId(), marca.getNombre());
	}

	/**
	 * Vista de una categoría.
	 *
	 * @param categoria entidad
	 * @return referencia
	 */
	public static ReferenciaRespuesta desde(Categoria categoria) {
		return new ReferenciaRespuesta(categoria.getId(), categoria.getNombre());
	}

}
