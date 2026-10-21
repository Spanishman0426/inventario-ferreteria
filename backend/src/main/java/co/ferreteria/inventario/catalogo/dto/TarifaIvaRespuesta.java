/**
 * Nombre: TarifaIvaRespuesta.java
 * Descripción: Vista de una tarifa de IVA para selectores y respuestas.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.dto;

import java.math.BigDecimal;

import co.ferreteria.inventario.catalogo.TarifaIva;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Tarifa de IVA (RN-08).
 *
 * @param id identificador
 * @param nombre nombre legible
 * @param porcentaje porcentaje de IVA
 */
@Builder
@Schema(name = "TarifaIva", description = "Tarifa de IVA (RN-08).")
public record TarifaIvaRespuesta(
		@Schema(example = "3") Short id,
		@Schema(example = "Tarifa general") String nombre,
		@Schema(example = "19.00") BigDecimal porcentaje) {

	/**
	 * Construye la vista a partir de la entidad.
	 *
	 * @param tarifa entidad
	 * @return vista de la tarifa
	 */
	public static TarifaIvaRespuesta desde(TarifaIva tarifa) {
		return new TarifaIvaRespuesta(tarifa.getId(), tarifa.getNombre(), tarifa.getPorcentaje());
	}

}
