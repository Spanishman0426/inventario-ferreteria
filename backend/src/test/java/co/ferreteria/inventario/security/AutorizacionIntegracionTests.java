/**
 * Nombre: AutorizacionIntegracionTests.java
 * Descripción: Pruebas de la matriz de permisos 2.2 aplicada en el servidor (401/403 por rol), incluida
 *              la matriz de productos para los seis roles.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;

/**
 * Autorización por permisos (RF-SEG-001, RN-27, RNF-SEG-002).
 */
class AutorizacionIntegracionTests extends PruebaIntegracionApi {

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Un vendedor no gestiona usuarios por la API: 403 y no se crea nada")
	void vendedorNoGestionaUsuarios() throws Exception {
		String vendedor = usuarios.bearer("vendedor");

		mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

		mvc.perform(post("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, vendedor)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuario":"intruso","nombreCompleto":"Intruso","correo":"intruso@ferreteria.test",
								 "contrasena":"Clave12345","roles":["ADMIN"]}
								"""))
				.andExpect(status().isForbidden());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE usuario = 'intruso'", Integer.class))
				.isZero();
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Un vendedor no consulta la auditoría: 403")
	void vendedorNoConsultaAuditoria() throws Exception {
		mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	@Test
	@DisplayName("[CA-RNF-SEG-002-1] El gerente consulta auditoría pero no gestiona usuarios (matriz 2.2)")
	void gerenteConsultaAuditoriaPeroNoUsuarios() throws Exception {
		String gerente = usuarios.bearer("gerente");
		mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, gerente)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, gerente)).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("[CA-RNF-SEG-002-1] El auditor consulta la auditoría: 200")
	void auditorConsultaAuditoria() throws Exception {
		mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("auditor")))
				.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "{0}: consultar {1}, crear {2}, editar {3}, inactivar {4}")
	@CsvSource({
		"admin,     200, 201, 200, 204",
		"gerente,   200, 201, 200, 204",
		"jefe,      200, 201, 200, 204",
		"comprador, 200, 403, 403, 403",
		"vendedor,  200, 403, 403, 403",
		"auditor,   200, 403, 403, 403"})
	@DisplayName("[CA-RF-SEG-001-2][CA-RNF-SEG-002-1] Matriz 2.2 en productos por API: todos consultan; solo admin, gerente y jefe de inventario crean, editan e inactivan")
	void matrizProductos(String usuario, int consultar, int crear, int editar, int inactivar) throws Exception {
		long id = crearProducto(usuarios.bearer("jefe"), 201);
		String token = usuarios.bearer(usuario);

		mvc.perform(get("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().is(consultar));
		mvc.perform(get("/api/v1/productos/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().is(consultar));
		crearProducto(token, crear);
		mvc.perform(put("/api/v1/productos/{id}", id).header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON).content(producto("Editado " + UUID.randomUUID())))
				.andExpect(status().is(editar));
		mvc.perform(delete("/api/v1/productos/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().is(inactivar));

		boolean activo = Boolean.TRUE.equals(
				jdbc.queryForObject("SELECT activo FROM producto WHERE id = ?", Boolean.class, id));
		assertThat(activo).as("un rol sin permiso no deja rastro en el producto").isEqualTo(inactivar == 403);
	}

	@ParameterizedTest(name = "{0}: {1} permisos")
	@CsvSource({"admin, 26", "gerente, 22", "jefe, 8", "comprador, 7", "vendedor, 6", "auditor, 4"})
	@DisplayName("[CA-RNF-SEG-002-1] Cada rol recibe en su token exactamente los permisos de la matriz 2.2")
	void permisosPorRol(String usuario, int permisos) throws Exception {
		mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, usuarios.bearer(usuario)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.permisos", hasSize(permisos)));
	}

	@Test
	@DisplayName("[CA-RNF-SEG-002-1] El vendedor no tiene el permiso COSTOS_VER (matriz 2.2)")
	void vendedorSinCostos() throws Exception {
		mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(jsonPath("$.permisos", not(hasItem("COSTOS_VER"))));
	}

	private long crearProducto(String token, int estadoEsperado) throws Exception {
		String cuerpo = mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON).content(producto("Matriz " + UUID.randomUUID())))
				.andExpect(status().is(estadoEsperado))
				.andReturn().getResponse().getContentAsString();
		return estadoEsperado == 201 ? ((Number) JsonPath.read(cuerpo, "$.id")).longValue() : 0;
	}

	private static String producto(String nombre) {
		return """
				{"nombre":"%s","categoriaId":1,"marcaId":1,"unidadBase":"UND","tarifaIvaId":3,
				 "porcentajeIncremento":30,"control":"NINGUNO"}
				""".formatted(nombre);
	}

}
