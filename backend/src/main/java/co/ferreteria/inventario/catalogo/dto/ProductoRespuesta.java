/**
 * Nombre: ProductoRespuesta.java
 * Descripción: Vista de un producto en la API, con sus referencias anidadas y costos según permiso.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import co.ferreteria.inventario.catalogo.ControlProducto;
import co.ferreteria.inventario.catalogo.EstadoProducto;
import co.ferreteria.inventario.catalogo.Producto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Producto devuelto por la API (RF-PRO-001, RF-PRO-003).
 *
 * <p>{@code porcentajeIncremento} y {@code ultimoCostoBase} revelan costos y utilidad: solo se
 * incluyen si el usuario tiene el permiso {@code COSTOS_VER} (matriz 2.2; criterio de RF-PRO-003
 * "un vendedor no ve costos"). Sin ese permiso, las propiedades no aparecen en el JSON.</p>
 *
 * @param id identificador
 * @param nombre denominación
 * @param descripcion detalle adicional
 * @param estado ACTIVO o INACTIVO
 * @param categoria categoría
 * @param marca marca
 * @param unidadBase unidad base
 * @param tarifaIva tarifa de IVA
 * @param porcentajeIncremento margen sobre el costo; solo con {@code COSTOS_VER}
 * @param control tipo de control
 * @param plazoGarantiaMeses meses de garantía, si aplica
 * @param ultimoCostoBase costo unitario en unidad base de la última compra; solo con {@code COSTOS_VER}
 * @param creadoEn fecha de creación
 */
@Builder
@Schema(name = "ProductoRespuesta", description = "Producto del catálogo (RF-PRO-001, RF-PRO-003).")
public record ProductoRespuesta(
		@Schema(example = "10") Long id,
		@Schema(example = "Martillo de uña 16 oz") String nombre,
		@Schema(example = "Mango de fibra de vidrio") String descripcion,
		@Schema(example = "ACTIVO") EstadoProducto estado,
		ReferenciaRespuesta categoria,
		ReferenciaRespuesta marca,
		UnidadMedidaRespuesta unidadBase,
		TarifaIvaRespuesta tarifaIva,
		@Schema(description = "Solo si el usuario tiene COSTOS_VER.", example = "30.00")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		BigDecimal porcentajeIncremento,
		@Schema(example = "GARANTIA") ControlProducto control,
		@Schema(example = "12") Short plazoGarantiaMeses,
		@Schema(description = "Costo unitario en unidad base de la última compra (RN-20); "
				+ "solo si el usuario tiene COSTOS_VER y ya hubo una recepción.", example = "18000.0000")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		BigDecimal ultimoCostoBase,
		@Schema(example = "2026-10-20T15:30:00Z") Instant creadoEn) {

	/**
	 * Construye la vista a partir de la entidad con sus relaciones cargadas.
	 *
	 * @param p producto
	 * @param verCostos si el usuario tiene {@code COSTOS_VER}
	 * @return vista del producto
	 */
	public static ProductoRespuesta desde(Producto p, boolean verCostos) {
		return ProductoRespuesta.builder()
				.id(p.getId())
				.nombre(p.getNombre())
				.descripcion(p.getDescripcion())
				.estado(p.getEstado())
				.categoria(ReferenciaRespuesta.desde(p.getCategoria()))
				.marca(ReferenciaRespuesta.desde(p.getMarca()))
				.unidadBase(UnidadMedidaRespuesta.desde(p.getUnidadBase()))
				.tarifaIva(TarifaIvaRespuesta.desde(p.getTarifaIva()))
				.porcentajeIncremento(verCostos ? p.getPorcentajeIncremento() : null)
				.control(p.getControl())
				.plazoGarantiaMeses(p.getPlazoGarantiaMeses())
				.ultimoCostoBase(verCostos ? p.getUltimoCostoBase() : null)
				.creadoEn(p.getCreadoEn())
				.build();
	}

}
