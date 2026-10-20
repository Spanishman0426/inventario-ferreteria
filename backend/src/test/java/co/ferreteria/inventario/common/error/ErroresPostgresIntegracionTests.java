/**
 * Nombre: ErroresPostgresIntegracionTests.java
 * Descripción: Verifica la traducción de errores reales de PostgreSQL (CHECK y trigger) a Problem Details.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import co.ferreteria.inventario.TestcontainersConfiguration;

/**
 * Provoca en la base las mismas violaciones de PB-01 (CHECK, RN-03) y PB-19 (trigger, RN-02) de
 * {@code docs/plan-pruebas-bd.md} y comprueba que el manejador las responde con 422.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ErroresPostgresIntegracionTests {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ManejadorGlobalErrores manejador;

	@Test
	@DisplayName("CHECK de saldo negativo (PB-01): SQLSTATE 23514 se responde 422 REGLA_INTEGRIDAD")
	void checkRealSeTraduceA422() {
		DataAccessException error = catchThrowableOfType(DataAccessException.class,
				() -> jdbc.update("UPDATE lote SET saldo_disponible = saldo_disponible - 1000 WHERE id = 1"));

		assertThat(ErrorBaseDatos.desde(error)).get()
				.extracting(ErrorBaseDatos::sqlState).isEqualTo(ErrorBaseDatos.CHECK);

		ProblemDetail problema = cuerpo(manejador.manejarAccesoDatos(error, solicitud()));
		assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
		assertThat(problema.getProperties()).containsEntry(ManejadorGlobalErrores.PROPIEDAD_CODIGO, "REGLA_INTEGRIDAD");
		assertThat(problema.getProperties()).containsKey(ManejadorGlobalErrores.PROPIEDAD_RESTRICCION);
	}

	@Test
	@DisplayName("Trigger que impide borrar un lote (PB-19): SQLSTATE P0001 se responde 422 con su mensaje")
	void triggerRealSeTraduceA422ConSuMensaje() {
		DataAccessException error = catchThrowableOfType(DataAccessException.class,
				() -> jdbc.update("DELETE FROM lote WHERE id = 1"));

		ErrorBaseDatos datos = ErrorBaseDatos.desde(error).orElseThrow();
		assertThat(datos.sqlState()).isEqualTo(ErrorBaseDatos.TRIGGER_NEGOCIO);
		assertThat(datos.mensaje()).isNotBlank();

		ProblemDetail problema = cuerpo(manejador.manejarAccesoDatos(error, solicitud()));
		assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
		assertThat(problema.getProperties()).containsEntry(ManejadorGlobalErrores.PROPIEDAD_CODIGO, "REGLA_NEGOCIO");
		assertThat(problema.getDetail()).isEqualTo(datos.mensaje());
	}

	private static ServletWebRequest solicitud() {
		return new ServletWebRequest(new MockHttpServletRequest("PUT", "/api/lotes/1"));
	}

	private static ProblemDetail cuerpo(ResponseEntity<Object> respuesta) {
		assertThat(respuesta.getBody()).isInstanceOf(ProblemDetail.class);
		return (ProblemDetail) respuesta.getBody();
	}

}
