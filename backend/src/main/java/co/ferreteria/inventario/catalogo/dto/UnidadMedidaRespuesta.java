/**
 * Nombre: UnidadMedidaRespuesta.java
 * Descripción: Vista de una unidad de medida para selectores y respuestas.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.dto;

import co.ferreteria.inventario.catalogo.UnidadMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Unidad base de existencias (RN-05).
 *
 * @param codigo código de la unidad
 * @param nombre nombre legible
 * @param esDiscreta si solo admite cantidades enteras
 */
@Builder
@Schema(name = "UnidadMedida", description = "Unidad base (RN-05); las discretas solo admiten enteros.")
public record UnidadMedidaRespuesta(
		@Schema(example = "UND") String codigo,
		@Schema(example = "Unidad") String nombre,
		@Schema(example = "true") boolean esDiscreta) {

	/**
	 * Construye la vista a partir de la entidad.
	 *
	 * @param unidad entidad
	 * @return vista de la unidad
	 */
	public static UnidadMedidaRespuesta desde(UnidadMedida unidad) {
		return new UnidadMedidaRespuesta(unidad.getCodigo(), unidad.getNombre(), unidad.isEsDiscreta());
	}

}
