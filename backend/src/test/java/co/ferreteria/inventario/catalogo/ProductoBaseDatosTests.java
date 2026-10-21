/**
 * Nombre: ProductoBaseDatosTests.java
 * Descripción: Pruebas de integración que verifican que la base rechaza productos inválidos aunque se
 *              salte el servicio (JDBC directo y repositorio JPA).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;

/**
 * Defensa en profundidad del catálogo: las restricciones de la base (RF-PRO-001, RN-18) actúan aunque
 * el código de la aplicación no valide.
 */
class ProductoBaseDatosTests extends PruebaIntegracionApi {

	/** Inserción directa con los ids de V100: marca Stanley, categoría Herramientas, IVA 19 %. */
	private static final String INSERTAR = """
			INSERT INTO producto (nombre, marca_id, categoria_id, unidad_base, tarifa_iva_id, porcentaje_incremento,
			                      control, plazo_garantia_meses)
			VALUES (?, ?, 1, ?, ?, 30, ?, ?)
			""";

	@Autowired
	private ProductoRepository productos;
	@Autowired
	private MarcaRepository marcas;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private UnidadMedidaRepository unidades;
	@Autowired
	private TarifaIvaRepository tarifas;

	@Test
	@DisplayName("[CA-RF-PRO-001-3] Un duplicado de nombre y marca insertado por SQL, con otras mayúsculas, se rechaza con 23505")
	void duplicadoPorSql() {
		String nombre = nombreNuevo();
		jdbc.update(INSERTAR, nombre, 1L, "UND", (short) 3, "NINGUNO", null);

		assertThatThrownBy(() -> jdbc.update(INSERTAR, nombre.toUpperCase(), 1L, "UND", (short) 3, "NINGUNO", null))
				.satisfies(e -> {
					assertThat(sqlState(e)).isEqualTo("23505");
					assertThat(NestedExceptionUtils.getMostSpecificCause(e).getMessage())
							.contains("ux_producto_marca_nombre");
				});
		assertThat(jdbc.queryForObject("SELECT count(*) FROM producto WHERE lower(nombre) = lower(?)",
				Integer.class, nombre)).isEqualTo(1);
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-3] Un duplicado guardado con el repositorio JPA, sin pasar por el servicio, se rechaza con 23505")
	void duplicadoPorRepositorio() {
		String nombre = nombreNuevo();
		productos.saveAndFlush(nuevo(nombre));

		assertThatThrownBy(() -> productos.saveAndFlush(nuevo(nombre.toLowerCase())))
				.isInstanceOf(DataIntegrityViolationException.class)
				.satisfies(e -> assertThat(sqlState(e)).isEqualTo("23505"));
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-2] Por SQL, garantía sin plazo se rechaza con 23514 (ck_producto_garantia)")
	void garantiaSinPlazoPorSql() {
		assertThatThrownBy(() -> jdbc.update(INSERTAR, nombreNuevo(), 1L, "UND", (short) 3, "GARANTIA", null))
				.satisfies(e -> {
					assertThat(sqlState(e)).isEqualTo("23514");
					assertThat(NestedExceptionUtils.getMostSpecificCause(e).getMessage()).contains("ck_producto_garantia");
				});
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-2] Por SQL, un producto sin marca, sin unidad base o sin tarifa de IVA se rechaza con 23502")
	void obligatoriosPorSql() {
		assertThatThrownBy(() -> jdbc.update(INSERTAR, nombreNuevo(), null, "UND", (short) 3, "NINGUNO", null))
				.satisfies(e -> assertThat(sqlState(e)).isEqualTo("23502"));
		assertThatThrownBy(() -> jdbc.update(INSERTAR, nombreNuevo(), 1L, null, (short) 3, "NINGUNO", null))
				.satisfies(e -> assertThat(sqlState(e)).isEqualTo("23502"));
		assertThatThrownBy(() -> jdbc.update(INSERTAR, nombreNuevo(), 1L, "UND", null, "NINGUNO", null))
				.satisfies(e -> assertThat(sqlState(e)).isEqualTo("23502"));
	}

	private Producto nuevo(String nombre) {
		Producto producto = new Producto(nombre, marcas.findById(1L).orElseThrow(),
				categorias.findById(1L).orElseThrow(), unidades.findById("UND").orElseThrow(),
				tarifas.findById((short) 3).orElseThrow());
		producto.setPorcentajeIncremento(new BigDecimal("30"));
		producto.setControl(ControlProducto.NINGUNO);
		return producto;
	}

	private static String sqlState(Throwable error) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof SQLException sql && sql.getSQLState() != null) {
				return sql.getSQLState();
			}
		}
		return null;
	}

	private static String nombreNuevo() {
		return "Producto bd " + UUID.randomUUID().toString().substring(0, 8);
	}

}
