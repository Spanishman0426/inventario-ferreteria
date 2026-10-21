/**
 * Nombre: AutenticacionIntegracionTests.java
 * Descripción: Pruebas de inicio de sesión, bloqueo por intentos, token JWT, cabeceras de seguridad y CORS.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.ResultActions;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;
import co.ferreteria.inventario.soporte.UsuariosPrueba;

/**
 * Autenticación por la API (RF-SEG-001, RNF-SEG-001, RNF-SEG-005, RNF-SEG-006).
 */
class AutenticacionIntegracionTests extends PruebaIntegracionApi {

	private static final String LOGIN = "/api/v1/auth/login";
	private static final String ME = "/api/v1/auth/me";
	private static final String FRONTEND = "http://localhost:5173";

	@Autowired
	private JwtEncoder jwtEncoder;

	// ------------------------------------------------------------------ inicio de sesión

	@Test
	@DisplayName("Login correcto: devuelve token de 30 minutos, roles y permisos, actualiza ultimo_acceso y audita INICIAR_SESION con la IP")
	void loginCorrecto() throws Exception {
		long antes = ultimoIdAuditoria();

		login("admin", UsuariosPrueba.CONTRASENA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresIn").value(1800))
				.andExpect(jsonPath("$.usuario.id").value(2))
				.andExpect(jsonPath("$.usuario.nombreCompleto").value("Ana Administradora"))
				.andExpect(jsonPath("$.usuario.roles", contains("ADMIN")))
				.andExpect(jsonPath("$.usuario.permisos", hasSize(26)))
				.andExpect(jsonPath("$.usuario.permisos", hasItem("USUARIOS_GESTIONAR")))
				.andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());

		Map<String, Object> fila = jdbc.queryForMap(
				"SELECT intentos_fallidos, ultimo_acceso FROM usuario WHERE usuario = 'admin'");
		assertThat(((Number) fila.get("intentos_fallidos")).intValue()).isZero();
		assertThat(fila.get("ultimo_acceso")).isNotNull();

		Map<String, Object> auditoria = jdbc.queryForMap(
				"SELECT accion, usuario_id, host(ip) AS ip FROM auditoria WHERE id > ? ORDER BY id DESC LIMIT 1", antes);
		assertThat(auditoria).containsEntry("accion", "INICIAR_SESION").containsEntry("ip", IP_LOCAL);
		assertThat(((Number) auditoria.get("usuario_id")).longValue()).isEqualTo(2L);
	}

	@Test
	@DisplayName("Contraseña incorrecta: 401 CREDENCIALES_INVALIDAS, suma un intento y audita SESION_FALLIDA")
	void contrasenaIncorrecta() throws Exception {
		long antes = ultimoIdAuditoria();

		login("gerente", "incorrecta123")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));

		assertThat(intentos("gerente")).isEqualTo(1);
		Map<String, Object> auditoria = jdbc.queryForMap(
				"SELECT accion, usuario_id, host(ip) AS ip FROM auditoria WHERE id > ? ORDER BY id DESC LIMIT 1", antes);
		assertThat(auditoria).containsEntry("accion", "SESION_FALLIDA").containsEntry("ip", IP_LOCAL);
		assertThat(((Number) auditoria.get("usuario_id")).longValue()).isEqualTo(3L);
	}

	@Test
	@DisplayName("Usuario inexistente: la misma respuesta genérica 401 y SESION_FALLIDA sin usuario")
	void usuarioInexistente() throws Exception {
		long antes = ultimoIdAuditoria();

		login("nadie", "cualquiera123")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));

		Map<String, Object> auditoria = jdbc.queryForMap(
				"SELECT accion, usuario_id, valores_despues->>'usuario' AS intentado FROM auditoria WHERE id > ?", antes);
		assertThat(auditoria).containsEntry("accion", "SESION_FALLIDA").containsEntry("intentado", "nadie");
		assertThat(auditoria.get("usuario_id")).isNull();
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-1] Tras 5 intentos fallidos la cuenta se bloquea 15 minutos, aun con la contraseña correcta")
	void bloqueoAlQuintoIntento() throws Exception {
		for (int intento = 1; intento <= 4; intento++) {
			login("comprador", "incorrecta123").andExpect(status().isUnauthorized());
			assertThat(intentos("comprador")).isEqualTo(intento);
		}

		Instant antesDelBloqueo = Instant.now();
		login("comprador", "incorrecta123")
				.andExpect(status().isLocked())
				.andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));

		OffsetDateTime hasta = jdbc.queryForObject(
				"SELECT bloqueado_hasta FROM usuario WHERE usuario = 'comprador'", OffsetDateTime.class);
		assertThat(hasta).isNotNull();
		assertThat(Duration.between(antesDelBloqueo, hasta.toInstant()))
				.isBetween(Duration.ofMinutes(14), Duration.ofMinutes(16));

		login("comprador", UsuariosPrueba.CONTRASENA)
				.andExpect(status().isLocked())
				.andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));
	}

	@Test
	@DisplayName("Un acceso correcto reinicia el contador de intentos fallidos")
	void accesoCorrectoReiniciaIntentos() throws Exception {
		login("jefe", "incorrecta123").andExpect(status().isUnauthorized());
		login("jefe", "incorrecta123").andExpect(status().isUnauthorized());
		assertThat(intentos("jefe")).isEqualTo(2);

		login("jefe", UsuariosPrueba.CONTRASENA).andExpect(status().isOk());
		assertThat(intentos("jefe")).isZero();
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-3] Una cuenta desactivada no inicia sesión")
	void cuentaInactivaNoIniciaSesion() throws Exception {
		jdbc.update("UPDATE usuario SET activo = false WHERE usuario = 'auditor'");

		login("auditor", UsuariosPrueba.CONTRASENA)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
	}

	@Test
	@DisplayName("El usuario Sistema no puede iniciar sesión")
	void usuarioSistemaNoIniciaSesion() throws Exception {
		login("sistema", "cualquiera123").andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Login con campos vacíos: 400 con un error por campo")
	void loginConCamposVacios() throws Exception {
		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\"\",\"contrasena\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores", hasSize(2)));
	}

	// ------------------------------------------------------------------ token

	@Test
	@DisplayName("[RNF-SEG-001] Sin token, una ruta protegida responde 401")
	void sinTokenResponde401() throws Exception {
		mvc.perform(get(ME))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
		mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("[RNF-SEG-001] Un token alterado responde 401")
	void tokenAlteradoResponde401() throws Exception {
		String token = usuarios.token("admin");
		// Se cambia un carácter en medio de la firma (los del final pueden ser solo relleno)
		int posicion = token.lastIndexOf('.') + 10;
		char nuevo = token.charAt(posicion) == 'A' ? 'B' : 'A';
		String alterado = token.substring(0, posicion) + nuevo + token.substring(posicion + 1);

		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + alterado))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
	}

	@Test
	@DisplayName("[RNF-SEG-006] Un token vencido responde 401")
	void tokenVencidoResponde401() throws Exception {
		Instant hace = Instant.now().minus(Duration.ofHours(1));
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("2").issuedAt(hace).expiresAt(hace.plus(Duration.ofMinutes(30)))
				.claim("permisos", List.of("USUARIOS_GESTIONAR")).build();
		String vencido = jwtEncoder.encode(
				JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + vencido))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("GET /auth/me devuelve el usuario del token con sus roles y permisos")
	void meDevuelveElUsuarioAutenticado() throws Exception {
		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(6))
				.andExpect(jsonPath("$.nombreCompleto").value("Valeria Vendedora"))
				.andExpect(jsonPath("$.roles", contains("VENDEDOR")))
				.andExpect(jsonPath("$.permisos", hasSize(6)));
	}

	// ------------------------------------------------------------------ cabeceras y CORS

	@Test
	@DisplayName("[RNF-SEG-006] Cabeceras de seguridad: HSTS en HTTPS, nosniff y X-Frame-Options DENY")
	void cabecerasDeSeguridad() throws Exception {
		mvc.perform(get(ME).secure(true).header(HttpHeaders.AUTHORIZATION, usuarios.bearer("admin")))
				.andExpect(status().isOk())
				.andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"));
	}

	@Test
	@DisplayName("CORS admite solo el origen de FRONTEND_URL")
	void corsSoloAdmiteElFrontend() throws Exception {
		mvc.perform(options(LOGIN)
						.header(HttpHeaders.ORIGIN, FRONTEND)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND));

		mvc.perform(options(LOGIN)
						.header(HttpHeaders.ORIGIN, "https://sitio-ajeno.test")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	// ------------------------------------------------------------------ utilidades

	private ResultActions login(String usuario, String contrasena) throws Exception {
		return mvc.perform(post(LOGIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(UsuariosPrueba.credenciales(usuario, contrasena)));
	}

	private int intentos(String usuario) {
		Integer valor = jdbc.queryForObject("SELECT intentos_fallidos FROM usuario WHERE usuario = ?", Integer.class,
				usuario);
		return valor == null ? -1 : valor;
	}

}
