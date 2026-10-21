/**
 * Nombre: SeguridadNoFuncionalIntegracionTests.java
 * Descripción: Pruebas no funcionales de seguridad: contraseñas guardadas con BCrypt, cabecera HSTS en
 *              toda respuesta HTTPS y triggers de auditoría que registran la creación y la modificación
 *              de productos, por la API y por SQL directo.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;
import co.ferreteria.inventario.soporte.UsuariosPrueba;

/**
 * Seguridad no funcional (RNF-SEG-001, RNF-SEG-003, RNF-SEG-006, RF-SEG-002, RN-28).
 */
class SeguridadNoFuncionalIntegracionTests extends PruebaIntegracionApi {

	/** Valor esperado de la cabecera HSTS: un año con subdominios (SecurityConfig). */
	private static final String HSTS = "max-age=" + SecurityConfig.HSTS_SEGUNDOS + " ; includeSubDomains";

	/** Hash BCrypt con costo 12: prefijo $2a$, $2b$ o $2y$ y 60 caracteres en total. */
	private static final String PATRON_BCRYPT_12 = "^\\$2[aby]\\$12\\$[./A-Za-z0-9]{53}$";

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private PlatformTransactionManager transactionManager;

	// ------------------------------------------------------------------ BCrypt

	@Test
	@DisplayName("[CA-RNF-SEG-001-2] Crear un usuario y cambiarle la contraseña por la API guarda siempre un hash BCrypt de costo 12, nunca el texto plano")
	void contrasenasGuardadasConBCrypt() throws Exception {
		String admin = usuarios.bearer("admin");
		String unico = UUID.randomUUID().toString().substring(0, 8);
		String inicial = "Inicial" + unico + "9";
		String nueva = "Nueva" + unico + "7";

		String cuerpo = mvc.perform(post("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuario":"bc%s","nombreCompleto":"Prueba BCrypt","correo":"bc%s@ferreteria.test",
								 "contrasena":"%s","roles":["VENDEDOR"]}
								""".formatted(unico, unico, inicial)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
		assertThat(cuerpo).doesNotContain(inicial).doesNotContain("$2");

		String hashInicial = hashDe(id);
		assertThat(hashInicial).matches(PATRON_BCRYPT_12).doesNotContain(inicial);
		assertThat(new BCryptPasswordEncoder().matches(inicial, hashInicial))
				.as("el hash es BCrypt estándar, verificable por cualquier implementación").isTrue();

		mvc.perform(put("/api/v1/usuarios/{id}/contrasena", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"contrasena\":\"" + nueva + "\"}"))
				.andExpect(status().is2xxSuccessful());

		String hashNuevo = hashDe(id);
		assertThat(hashNuevo).matches(PATRON_BCRYPT_12).isNotEqualTo(hashInicial);
		assertThat(passwordEncoder.matches(nueva, hashNuevo)).isTrue();
		assertThat(passwordEncoder.matches(inicial, hashNuevo)).isFalse();

		Integer conTextoPlano = jdbc.queryForObject("""
				SELECT count(*) FROM usuario u
				 WHERE to_jsonb(u)::text LIKE '%' || ? || '%' OR to_jsonb(u)::text LIKE '%' || ? || '%'
				""", Integer.class, inicial, nueva);
		assertThat(conTextoPlano).as("ninguna columna de usuario guarda la contraseña en texto plano").isZero();
	}

	@Test
	@DisplayName("[CA-RNF-SEG-001-2] Dos usuarios con la misma contraseña tienen hashes distintos (BCrypt con sal aleatoria)")
	void bcryptUsaSalPorUsuario() throws Exception {
		String admin = usuarios.bearer("admin");
		String misma = "Compartida" + UUID.randomUUID().toString().substring(0, 8) + "1";
		long primero = crearUsuario(admin, misma);
		long segundo = crearUsuario(admin, misma);

		String hashPrimero = hashDe(primero);
		String hashSegundo = hashDe(segundo);
		assertThat(hashPrimero).matches(PATRON_BCRYPT_12).isNotEqualTo(hashSegundo);
		assertThat(hashSegundo).matches(PATRON_BCRYPT_12);
		assertThat(passwordEncoder.matches(misma, hashPrimero)).isTrue();
		assertThat(passwordEncoder.matches(misma, hashSegundo)).isTrue();
	}

	// ------------------------------------------------------------------ HSTS

	@ParameterizedTest(name = "{0} {1} con token {2}: {3}")
	@CsvSource({
		"GET,  /api/v1/auth/me,    admin,    200",
		"GET,  /api/v1/productos,  vendedor, 200",
		"GET,  /api/v1/usuarios,   vendedor, 403",
		"GET,  /api/v1/productos,  ninguno,  401",
		"POST, /api/v1/auth/login, ninguno,  401",
		"GET,  /actuator/health,   ninguno,  200"})
	@DisplayName("[CA-RNF-SEG-006-1] Toda respuesta por HTTPS lleva la cabecera HSTS de un año con subdominios, también las de error")
	void hstsEnTodaRespuestaHttps(String metodo, String ruta, String rol, int esperado) throws Exception {
		mvc.perform(solicitud(metodo, ruta, rol).secure(true))
				.andExpect(status().is(esperado))
				.andExpect(header().string("Strict-Transport-Security", HSTS));
	}

	@Test
	@DisplayName("[CA-RNF-SEG-006-1] Por HTTP plano no se envía HSTS (la norma RFC 6797 solo la admite en HTTPS)")
	void sinHstsEnHttpPlano() throws Exception {
		mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("admin")))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Strict-Transport-Security"));
	}

	// ------------------------------------------------------------------ triggers de auditoría

	@Test
	@DisplayName("[CA-RF-SEG-002-1][CA-RNF-SEG-003-1] Crear y modificar un producto por la API: el trigger registra un CREAR y un MODIFICAR con usuario, IP y valores antes y después")
	void triggerAuditaCreacionYModificacionPorApi() throws Exception {
		String jefe = usuarios.bearer("jefe");
		String nombre = "Auditado " + UUID.randomUUID();
		long antes = ultimoIdAuditoria();

		String cuerpo = mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, jefe)
						.contentType(MediaType.APPLICATION_JSON).content(producto(nombre, 30)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
		mvc.perform(put("/api/v1/productos/{id}", id).header(HttpHeaders.AUTHORIZATION, jefe)
						.contentType(MediaType.APPLICATION_JSON).content(producto(nombre, 45)))
				.andExpect(status().isOk());

		List<Map<String, Object>> registros = jdbc.queryForList("""
				SELECT accion, usuario_id, host(ip) AS ip,
				       valores_antes ->> 'porcentaje_incremento' AS margen_antes,
				       valores_despues ->> 'porcentaje_incremento' AS margen_despues,
				       valores_despues ->> 'nombre' AS nombre
				  FROM auditoria WHERE id > ? AND entidad = 'producto' AND entidad_id = ? ORDER BY id
				""", antes, id);

		assertThat(registros).extracting(r -> r.get("accion")).containsExactly("CREAR", "MODIFICAR");
		assertThat(registros).allSatisfy(r -> {
			assertThat(((Number) r.get("usuario_id")).longValue()).isEqualTo(idUsuario("jefe"));
			assertThat(r.get("ip")).isEqualTo(IP_LOCAL);
			assertThat(r.get("nombre")).isEqualTo(nombre);
		});
		assertThat(registros.get(0).get("margen_antes")).as("CREAR no tiene valores antes").isNull();
		assertThat(registros.get(0).get("margen_despues")).isEqualTo("30.00");
		assertThat(registros.get(1).get("margen_antes")).isEqualTo("30.00");
		assertThat(registros.get(1).get("margen_despues")).isEqualTo("45.00");
	}

	@Test
	@DisplayName("[CA-RF-SEG-002-1] Por SQL directo, sin pasar por la aplicación, el trigger también audita la creación y la modificación de un producto en la misma transacción")
	void triggerAuditaCreacionYModificacionPorSql() {
		TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
		long gerente = idUsuario("gerente");

		List<Map<String, Object>> registros = transaccion.execute(estado -> {
			estado.setRollbackOnly();
			jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)", String.class, String.valueOf(gerente));
			Long id = jdbc.queryForObject("""
					INSERT INTO producto (nombre, marca_id, categoria_id, unidad_base, tarifa_iva_id, porcentaje_incremento)
					VALUES ('Auditado por SQL', 1, 1, 'UND', 3, 20) RETURNING id
					""", Long.class);
			jdbc.update("UPDATE producto SET nombre = 'Auditado por SQL editado' WHERE id = ?", id);
			jdbc.update("UPDATE producto SET nombre = nombre WHERE id = ?", id);
			return jdbc.queryForList("""
					SELECT accion, usuario_id, valores_antes ->> 'nombre' AS antes, valores_despues ->> 'nombre' AS despues
					  FROM auditoria WHERE entidad = 'producto' AND entidad_id = ? ORDER BY id
					""", id);
		});

		assertThat(registros).extracting(r -> r.get("accion")).as("un UPDATE sin cambios no se audita")
				.containsExactly("CREAR", "MODIFICAR");
		assertThat(registros).allSatisfy(r ->
				assertThat(((Number) r.get("usuario_id")).longValue()).isEqualTo(gerente));
		assertThat(registros.get(1)).containsEntry("antes", "Auditado por SQL")
				.containsEntry("despues", "Auditado por SQL editado");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM producto WHERE nombre LIKE 'Auditado por SQL%'", Integer.class))
				.as("al revertir la transacción no queda el producto (ni su auditoría)").isZero();
	}

	// ------------------------------------------------------------------ apoyo

	private MockHttpServletRequestBuilder solicitud(String metodo, String ruta, String rol) throws Exception {
		MockHttpServletRequestBuilder solicitud = "POST".equals(metodo)
				? post(ruta).contentType(MediaType.APPLICATION_JSON)
						.content(UsuariosPrueba.credenciales("gerente", "incorrecta123"))
				: get(ruta);
		return "ninguno".equals(rol) ? solicitud : solicitud.header(HttpHeaders.AUTHORIZATION, usuarios.bearer(rol));
	}

	private long crearUsuario(String admin, String contrasena) throws Exception {
		String unico = UUID.randomUUID().toString().substring(0, 8);
		String cuerpo = mvc.perform(post("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuario":"sal%s","nombreCompleto":"Prueba sal","correo":"sal%s@ferreteria.test",
								 "contrasena":"%s","roles":["VENDEDOR"]}
								""".formatted(unico, unico, contrasena)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
	}

	private String hashDe(long id) {
		return jdbc.queryForObject("SELECT password_hash FROM usuario WHERE id = ?", String.class, id);
	}

	private static String producto(String nombre, int margen) {
		return """
				{"nombre":"%s","categoriaId":1,"marcaId":1,"unidadBase":"UND","tarifaIvaId":3,
				 "porcentajeIncremento":%d,"control":"NINGUNO"}
				""".formatted(nombre, margen);
	}

}
