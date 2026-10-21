/**
 * Nombre: GarantiaConPlazoValidatorTests.java
 * Descripción: Pruebas unitarias de la restricción GarantiaConPlazo sobre ProductoPeticion.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.validacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.ferreteria.inventario.catalogo.ControlProducto;
import co.ferreteria.inventario.catalogo.dto.ProductoPeticion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Regla de RF-PRO-001: {@code plazoGarantiaMeses} obligatorio si y solo si {@code control} es GARANTIA.
 */
class GarantiaConPlazoValidatorTests {

	private static ValidatorFactory fabrica;
	private static Validator validador;

	@BeforeAll
	static void crearValidador() {
		fabrica = Validation.buildDefaultValidatorFactory();
		validador = fabrica.getValidator();
	}

	@AfterAll
	static void cerrar() {
		fabrica.close();
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-2] GARANTIA sin plazo es inválido y el error queda en plazoGarantiaMeses")
	void garantiaSinPlazo() {
		assertThat(camposConError(peticion(ControlProducto.GARANTIA, null))).containsExactly("plazoGarantiaMeses");
	}

	@Test
	@DisplayName("Un plazo con control distinto de GARANTIA es inválido")
	void plazoSinGarantia() {
		assertThat(camposConError(peticion(ControlProducto.NINGUNO, 12))).containsExactly("plazoGarantiaMeses");
		assertThat(camposConError(peticion(ControlProducto.VENCIMIENTO, 12))).containsExactly("plazoGarantiaMeses");
	}

	@Test
	@DisplayName("Combinaciones válidas y control nulo no generan error de esta regla")
	void combinacionesValidas() {
		assertThat(camposConError(peticion(ControlProducto.GARANTIA, 12))).isEmpty();
		assertThat(camposConError(peticion(ControlProducto.NINGUNO, null))).isEmpty();
		assertThat(camposConError(peticion(null, 12))).containsExactly("control");
	}

	private static Set<String> camposConError(ProductoPeticion peticion) {
		Set<ConstraintViolation<ProductoPeticion>> violaciones = validador.validate(peticion);
		return violaciones.stream().map(v -> v.getPropertyPath().toString())
				.collect(java.util.stream.Collectors.toSet());
	}

	private static ProductoPeticion peticion(ControlProducto control, Integer plazo) {
		return ProductoPeticion.builder()
				.nombre("Taladro percutor")
				.categoriaId(1L)
				.marcaId(1L)
				.unidadBase("UND")
				.tarifaIvaId((short) 3)
				.porcentajeIncremento(BigDecimal.TEN)
				.control(control)
				.plazoGarantiaMeses(plazo)
				.build();
	}

}
