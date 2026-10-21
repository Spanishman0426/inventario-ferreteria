/**
 * Nombre: EstadoProductoConverterTests.java
 * Descripción: Pruebas unitarias de la conversión entre EstadoProducto y la columna booleana activo.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ACTIVO ↔ {@code true}, INACTIVO ↔ {@code false}.
 */
class EstadoProductoConverterTests {

	private final EstadoProductoConverter convertidor = new EstadoProductoConverter();

	@Test
	@DisplayName("ACTIVO y INACTIVO se guardan como true y false, y se leen de vuelta")
	void idaYVuelta() {
		assertThat(convertidor.convertToDatabaseColumn(EstadoProducto.ACTIVO)).isTrue();
		assertThat(convertidor.convertToDatabaseColumn(EstadoProducto.INACTIVO)).isFalse();
		assertThat(convertidor.convertToEntityAttribute(true)).isEqualTo(EstadoProducto.ACTIVO);
		assertThat(convertidor.convertToEntityAttribute(false)).isEqualTo(EstadoProducto.INACTIVO);
	}

	@Test
	@DisplayName("Un estado nulo se guarda como ACTIVO (valor por defecto de la tabla) y una columna nula se lee como nulo")
	void nulos() {
		assertThat(convertidor.convertToDatabaseColumn(null)).isTrue();
		assertThat(convertidor.convertToEntityAttribute(null)).isNull();
	}

}
