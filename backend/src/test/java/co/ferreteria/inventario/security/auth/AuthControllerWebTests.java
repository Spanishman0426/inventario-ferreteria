/**
 * Nombre: AuthControllerWebTests.java
 * Descripción: Pruebas de capa web (MockMvc) del inicio de sesión: forma de la respuesta, validación en
 *              español e inglés, credenciales inválidas, cuenta bloqueada y /auth/me sin token.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security.auth;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.OperacionRechazadaException;
import co.ferreteria.inventario.common.i18n.Mensajes;
import co.ferreteria.inventario.config.LocaleConfig;
import co.ferreteria.inventario.security.ConfiguracionJwt;
import co.ferreteria.inventario.security.ManejadorErroresSeguridad;
import co.ferreteria.inventario.security.SecurityConfig;

/**
 * Capa web de {@code /api/v1/auth} con la seguridad real y el servicio simulado (RF-SEG-001,
 * RNF-SEG-001, RNF-INT-002).
 */
@WebMvcTest(controllers = AuthController.class)
@ActiveProfiles("test")
@Import({ SecurityConfig.class, ConfiguracionJwt.class, ManejadorErroresSeguridad.class, LocaleConfig.class,
		Mensajes.class })
class AuthControllerWebTests {

	private static final String LOGIN = "/api/v1/auth/login";
	private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
	private static final String CREDENCIALES = "{\"usuario\":\"jefe\",\"contrasena\":\"Clave12345\"}";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ServicioAutenticacion servicio;

	@Test
	@DisplayName("Login correcto: 200 con accessToken, expiresIn y el usuario con roles y permisos")
	void loginCorrecto() throws Exception {
		when(servicio.iniciarSesion(any())).thenReturn(new RespuestaInicioSesion("token-firmado", 1800,
				new UsuarioSesion(4L, "Juana Jefe", Set.of("JEFE_INVENTARIO"), Set.of("CATALOGO_EDITAR"))));

		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(CREDENCIALES))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("token-firmado"))
				.andExpect(jsonPath("$.expiresIn").value(1800))
				.andExpect(jsonPath("$.usuario.id").value(4))
				.andExpect(jsonPath("$.usuario.nombreCompleto").value("Juana Jefe"))
				.andExpect(jsonPath("$.usuario.roles[0]").value("JEFE_INVENTARIO"))
				.andExpect(jsonPath("$.usuario.permisos[0]").value("CATALOGO_EDITAR"))
				.andExpect(jsonPath("$.contrasena").doesNotExist());
	}

	@Test
	@DisplayName("[CA-RNF-INT-002-1] Login con campos vacíos: 400 con un error por campo, en español y en inglés")
	void loginCamposVacios() throws Exception {
		String vacio = "{\"usuario\":\" \",\"contrasena\":\"\"}";
		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(vacio))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores", hasSize(2)))
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("usuario", "contrasena")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("es obligatorio"));
		mvc.perform(post(LOGIN).header(HttpHeaders.ACCEPT_LANGUAGE, "en").contentType(MediaType.APPLICATION_JSON)
						.content(vacio))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid data"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("is required"));
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("Credenciales inválidas: 401 CREDENCIALES_INVALIDAS en Problem Details con mensaje genérico")
	void credencialesInvalidas() throws Exception {
		when(servicio.iniciarSesion(any())).thenThrow(new OperacionRechazadaException(
				CodigoError.CREDENCIALES_INVALIDAS, CodigoError.CREDENCIALES_INVALIDAS.claveDetalle()));

		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(CREDENCIALES))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
				.andExpect(jsonPath("$.title").value("Credenciales inválidas"))
				.andExpect(jsonPath("$.detail")
						.value("El usuario o la contraseña no son correctos, o la cuenta está inactiva."));
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-1] Cuenta bloqueada: 423 CUENTA_BLOQUEADA con la hora de desbloqueo, en es y en")
	void cuentaBloqueada() throws Exception {
		when(servicio.iniciarSesion(any())).thenThrow(new OperacionRechazadaException(CodigoError.CUENTA_BLOQUEADA,
				CodigoError.CUENTA_BLOQUEADA.claveDetalle(), "20/10/2026 10:15"));

		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(CREDENCIALES))
				.andExpect(status().isLocked())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"))
				.andExpect(jsonPath("$.detail")
						.value("La cuenta está bloqueada por intentos fallidos hasta el 20/10/2026 10:15."));
		mvc.perform(post(LOGIN).header(HttpHeaders.ACCEPT_LANGUAGE, "en").contentType(MediaType.APPLICATION_JSON)
						.content(CREDENCIALES))
				.andExpect(jsonPath("$.title").value("Account locked"))
				.andExpect(jsonPath("$.detail")
						.value("The account is locked due to failed attempts until 20/10/2026 10:15."));
	}

	@Test
	@DisplayName("[CA-RNF-SEG-001-1] GET /auth/me sin token responde 401 NO_AUTENTICADO")
	void meSinToken() throws Exception {
		mvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("GET /auth/me con token devuelve el usuario del claim sub")
	void meConToken() throws Exception {
		when(servicio.usuarioActual(4L))
				.thenReturn(new UsuarioSesion(4L, "Juana Jefe", Set.of("JEFE_INVENTARIO"), Set.of()));

		mvc.perform(get("/api/v1/auth/me").with(jwt().jwt(j -> j.subject("4"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(4))
				.andExpect(jsonPath("$.nombreCompleto").value("Juana Jefe"));
	}

}
