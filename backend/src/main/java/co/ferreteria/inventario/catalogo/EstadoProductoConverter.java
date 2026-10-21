/**
 * Nombre: EstadoProductoConverter.java
 * Descripción: Convierte EstadoProducto a la columna booleana activo de la tabla producto.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce {@link EstadoProducto} a {@code boolean}: ACTIVO ↔ {@code true}, INACTIVO ↔ {@code false}.
 * La columna es {@code NOT NULL}; un nulo en memoria se guarda como ACTIVO (valor por defecto de la tabla).
 */
@Converter
public class EstadoProductoConverter implements AttributeConverter<EstadoProducto, Boolean> {

	/**
	 * Constructor usado por JPA.
	 */
	public EstadoProductoConverter() {
		// Sin estado.
	}

	/**
	 * Valor de la columna para un estado.
	 *
	 * @param estado estado del producto
	 * @return {@code false} solo para INACTIVO
	 */
	@Override
	public Boolean convertToDatabaseColumn(EstadoProducto estado) {
		return estado != EstadoProducto.INACTIVO;
	}

	/**
	 * Estado para un valor de la columna.
	 *
	 * @param activo valor leído
	 * @return estado correspondiente; nulo si la columna es nula
	 */
	@Override
	public EstadoProducto convertToEntityAttribute(Boolean activo) {
		return activo == null ? null : EstadoProducto.desdeActivo(activo);
	}

}
