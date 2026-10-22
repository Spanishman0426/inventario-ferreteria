/**
 * Nombre: DigitoVerificacionNitTests.java
 * Descripción: Pruebas unitarias del cálculo del dígito de verificación del NIT con NIT reales.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Dígito de verificación del NIT (RN-26), mismo algoritmo de {@code fn_dv_nit}.
 */
class DigitoVerificacionNitTests {

	@ParameterizedTest(name = "{0}-{1}")
	@CsvSource({
		// NIT públicos reales.
		"890903938, 8",
		"899999068, 1",
		"800197268, 4",
		// NIT de los datos de prueba (02_datos_prueba.sql), aceptados por el CHECK ck_tercero_dv.
		"900123456, 8",
		"830045678, 8",
		"900987654, 7",
		"800555123, 1",
		"901234567, 7"
	})
	@DisplayName("[CA-RF-TER-001-3] Calcula el dígito de verificación de NIT reales y de los datos de prueba")
	void calculaNitReales(String nit, int esperado) {
		assertThat(DigitoVerificacionNit.calcular(nit)).hasValue(esperado);
		assertThat(DigitoVerificacionNit.esValido(nit, esperado)).isTrue();
	}

	@Test
	@DisplayName("[CA-RF-TER-001-3] Un dígito distinto del calculado no es válido")
	void digitoIncorrecto() {
		assertThat(DigitoVerificacionNit.esValido("890903938", 7)).isFalse();
		assertThat(DigitoVerificacionNit.esValido("890903938", null)).isFalse();
	}

	@Test
	@DisplayName("Los residuos 0 y 1 son el propio dígito; el resto es 11 menos el residuo")
	void residuosBajos() {
		// 1 × 3 = 3 → 11 − 3 = 8; 4 × 3 = 12 → residuo 1 → 1; "0" → residuo 0 → 0.
		assertThat(DigitoVerificacionNit.calcular("1")).hasValue(8);
		assertThat(DigitoVerificacionNit.calcular("4")).hasValue(1);
		assertThat(DigitoVerificacionNit.calcular("0")).hasValue(0);
	}

	@ParameterizedTest(name = "«{0}»")
	@NullAndEmptySource
	@ValueSource(strings = { "89090393A", "890.903.938", "1234567890123456" })
	@DisplayName("Sin 1 a 15 dígitos no hay dígito de verificación")
	void entradasNoValidas(String nit) {
		assertThat(DigitoVerificacionNit.calcular(nit)).isEmpty();
		assertThat(DigitoVerificacionNit.esValido(nit, 0)).isFalse();
	}

}
