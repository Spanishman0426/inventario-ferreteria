/**
 * Nombre: ProductoPeticion.java
 * Descripción: Datos para crear o editar un producto (POST y PUT /api/v1/productos).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.dto;

import java.math.BigDecimal;

import co.ferreteria.inventario.catalogo.ControlProducto;
import co.ferreteria.inventario.catalogo.EstadoProducto;
import co.ferreteria.inventario.catalogo.validacion.GarantiaConPlazo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Producto que envía el cliente (RF-PRO-001). Las reglas de campo se validan con Bean Validation;
 * {@link GarantiaConPlazo} exige {@code plazoGarantiaMeses} si y solo si {@code control} es GARANTIA.
 * La existencia y el estado activo de marca, categoría, unidad e IVA los valida el servicio.
 *
 * @param nombre denominación, de 3 a 150 caracteres; única por marca sin distinguir mayúsculas
 * @param descripcion detalle opcional, hasta 500 caracteres
 * @param categoriaId id de una categoría activa
 * @param marcaId id de una marca activa
 * @param unidadBase código de la unidad base (RN-05)
 * @param tarifaIvaId id de una tarifa de IVA activa (RN-08)
 * @param porcentajeIncremento margen sobre el costo, entre 0 y 500, con hasta 2 decimales (RN-20)
 * @param control tipo de control (RN-18)
 * @param plazoGarantiaMeses meses de garantía; solo con control GARANTIA
 * @param estado ACTIVO o INACTIVO; si se omite, ACTIVO al crear y sin cambio al editar
 */
@Builder
@GarantiaConPlazo
@Schema(name = "ProductoPeticion", description = "Datos para crear o reemplazar un producto (RF-PRO-001).")
public record ProductoPeticion(
		@Schema(description = "Denominación; única por marca sin distinguir mayúsculas.", example = "Martillo de uña 16 oz",
				minLength = 3, maxLength = 150, requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(min = 3, max = 150)
		String nombre,

		@Schema(description = "Detalle adicional.", example = "Mango de fibra de vidrio", maxLength = 500)
		@Size(max = 500)
		String descripcion,

		@Schema(description = "Id de una categoría activa (GET /api/v1/categorias).", example = "1",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull
		Long categoriaId,

		@Schema(description = "Id de una marca activa (GET /api/v1/marcas).", example = "1",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull
		Long marcaId,

		@Schema(description = "Código de la unidad base (GET /api/v1/unidades-medida).", example = "UND",
				maxLength = 4, requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 4)
		String unidadBase,

		@Schema(description = "Id de una tarifa de IVA activa (GET /api/v1/tarifas-iva).", example = "3",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull
		Short tarifaIvaId,

		@Schema(description = "Margen sobre el costo de la última compra, entre 0 y 500.", example = "30.00",
				minimum = "0", maximum = "500", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull @DecimalMin("0") @DecimalMax("500") @Digits(integer = 3, fraction = 2)
		BigDecimal porcentajeIncremento,

		@Schema(description = "Tipo de control.", example = "GARANTIA", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull
		ControlProducto control,

		@Schema(description = "Meses de garantía; obligatorio si y solo si control = GARANTIA.", example = "12",
				minimum = "1", maximum = "32767")
		@Positive @Max(Short.MAX_VALUE)
		Integer plazoGarantiaMeses,

		@Schema(description = "Estado; si se omite, ACTIVO al crear y sin cambio al editar.", example = "ACTIVO")
		EstadoProducto estado) {
}
