/**
 * Nombre: ManejadorGlobalErroresTests.java
 * Descripción: Pruebas del formato Problem Details, los códigos HTTP y la traducción por Accept-Language.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.ferreteria.inventario.common.i18n.Mensajes;
import co.ferreteria.inventario.config.LocaleConfig;
import co.ferreteria.inventario.security.ConfiguracionJwt;
import co.ferreteria.inventario.security.ManejadorErroresSeguridad;
import co.ferreteria.inventario.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Verifica cada traducción del manejador global con un controlador de prueba que lanza las
 * excepciones; los errores de PostgreSQL se simulan con el mismo formato que envía el servidor.
 */
@WebMvcTest(controllers = ManejadorGlobalErroresTests.ControladorPrueba.class)
@ActiveProfiles("test")
@Import({ ManejadorGlobalErroresTests.ControladorPrueba.class, SecurityConfig.class, ConfiguracionJwt.class,
		ManejadorErroresSeguridad.class, LocaleConfig.class, Mensajes.class })
class ManejadorGlobalErroresTests {

	private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

	@Autowired
	private MockMvc mvc;

	@Test
	@WithMockUser
	@DisplayName("Validación: 400 con un error por campo, en español por defecto")
	void validacionDevuelve400ConErroresPorCampo() throws Exception {
		mvc.perform(post("/prueba-errores/validacion")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\" \",\"cantidad\":-3}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.title").value("Datos no válidos"))
				.andExpect(jsonPath("$.instance").value("/prueba-errores/validacion"))
				.andExpect(jsonPath("$.errores", hasSize(2)))
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("nombre", "cantidad")))
				.andExpect(jsonPath("$.errores[?(@.campo == 'nombre')].mensaje").value("es obligatorio"));
	}

	@Test
	@WithMockUser
	@DisplayName("Validación: título y mensajes de campo en inglés con Accept-Language: en")
	void validacionEnIngles() throws Exception {
		mvc.perform(post("/prueba-errores/validacion")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"\",\"cantidad\":5}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid data"))
				.andExpect(jsonPath("$.errores[0].campo").value("nombre"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("is required"));
	}

	@Test
	@WithMockUser
	@DisplayName("Idioma no soportado: se responde en español")
	void idiomaNoSoportadoUsaEspanol() throws Exception {
		mvc.perform(get("/prueba-errores/no-encontrado").header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"));
	}

	@Test
	@WithMockUser
	@DisplayName("Recurso inexistente: 404 NO_ENCONTRADO con el nombre del recurso")
	void recursoInexistenteDevuelve404() throws Exception {
		mvc.perform(get("/prueba-errores/no-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("No existe el producto con identificador 99."))
				.andExpect(jsonPath("$.errores", hasSize(0)));
	}

	@Test
	@WithMockUser
	@DisplayName("Ruta inexistente: 404 NO_ENCONTRADO")
	void rutaInexistenteDevuelve404() throws Exception {
		mvc.perform(get("/no-existe"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	@Test
	@WithMockUser
	@DisplayName("Conflicto de la aplicación: 409 CONFLICTO_UNICIDAD")
	void conflictoDeAplicacionDevuelve409() throws Exception {
		mvc.perform(get("/prueba-errores/conflicto"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"));
	}

	@Test
	@WithMockUser
	@DisplayName("SQLSTATE 23505: 409 CONFLICTO_UNICIDAD con la restricción")
	void unicidadDeBaseDatosDevuelve409() throws Exception {
		mvc.perform(get("/prueba-errores/unicidad"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
				.andExpect(jsonPath("$.restriccion").value("presentacion_codigo_barras_key"));
	}

	@Test
	@WithMockUser
	@DisplayName("SQLSTATE 23514: 422 REGLA_INTEGRIDAD con la restricción en el detalle")
	void checkDeBaseDatosDevuelve422() throws Exception {
		mvc.perform(get("/prueba-errores/check"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("REGLA_INTEGRIDAD"))
				.andExpect(jsonPath("$.restriccion").value("lote_saldo_disponible_check"))
				.andExpect(jsonPath("$.detail").value(
						"Los datos no cumplen una regla de integridad de la base de datos (lote_saldo_disponible_check)."));
	}

	@Test
	@WithMockUser
	@DisplayName("SQLSTATE P0001: 422 REGLA_NEGOCIO con el mensaje del trigger, aunque venga envuelto en JPA")
	void triggerDeNegocioDevuelve422ConSuMensaje() throws Exception {
		mvc.perform(get("/prueba-errores/trigger"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"))
				.andExpect(jsonPath("$.detail").value("El período 2026-08 está cerrado"));
	}

	@Test
	@WithMockUser
	@DisplayName("Error no previsto: 500 sin exponer el mensaje interno")
	void errorNoPrevistoDevuelve500() throws Exception {
		mvc.perform(get("/prueba-errores/interno"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
				.andExpect(jsonPath("$.detail").value(
						"Ocurrió un error inesperado. Intente de nuevo o contacte al administrador."));
	}

	@Test
	@DisplayName("Sin autenticación: 401 NO_AUTENTICADO en Problem Details e idioma de la solicitud")
	void sinAutenticacionDevuelve401() throws Exception {
		mvc.perform(get("/prueba-errores/no-encontrado").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
				.andExpect(jsonPath("$.title").value("Not authenticated"));
	}

	// ------------------------------------------------------------------ apoyo

	/**
	 * Crea la excepción del controlador JDBC tal como llega desde PostgreSQL.
	 *
	 * @param sqlState código SQLSTATE
	 * @param mensaje mensaje primario
	 * @param restriccion restricción violada o {@code null}
	 * @return excepción con el mensaje de servidor
	 */
	static PSQLException errorPostgres(String sqlState, String mensaje, String restriccion) {
		String campos = "SERROR\0C" + sqlState + "\0M" + mensaje + "\0"
				+ (restriccion == null ? "" : "n" + restriccion + "\0");
		return new PSQLException(new ServerErrorMessage(campos));
	}

	/** Datos de entrada con reglas de validación. */
	record Datos(@NotBlank String nombre, @Positive Integer cantidad) {
	}

	/** Controlador de prueba que lanza cada tipo de excepción. */
	@RestController
	@RequestMapping("/prueba-errores")
	static class ControladorPrueba {

		@PostMapping("/validacion")
		void validacion(@Valid @RequestBody Datos datos) {
			// La validación ocurre antes de entrar.
		}

		@GetMapping("/no-encontrado")
		void noEncontrado() {
			throw new RecursoNoEncontradoException("entidad.producto", 99);
		}

		@GetMapping("/conflicto")
		void conflicto() {
			throw new ConflictoException("error.conflicto_unicidad.detalle");
		}

		@GetMapping("/unicidad")
		void unicidad() {
			throw new DataIntegrityViolationException("duplicado",
					errorPostgres("23505", "duplicate key value", "presentacion_codigo_barras_key"));
		}

		@GetMapping("/check")
		void check() {
			throw new DataIntegrityViolationException("check",
					errorPostgres("23514", "new row violates check constraint", "lote_saldo_disponible_check"));
		}

		@GetMapping("/trigger")
		void trigger() {
			throw new JpaSystemException(new RuntimeException("envoltura de Hibernate",
					errorPostgres("P0001", "El período 2026-08 está cerrado", null)));
		}

		@GetMapping("/interno")
		void interno() {
			throw new IllegalStateException("detalle interno que no debe salir");
		}

	}

}
