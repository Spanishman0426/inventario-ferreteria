/**
 * Nombre: AutorizacionIntegracionTests.java
 * Descripción: Pruebas de la matriz de permisos 2.2 aplicada en el servidor (401/403 por rol).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
	@DisplayName("El gerente consulta auditoría pero no gestiona usuarios (matriz 2.2)")
	void gerenteConsultaAuditoriaPeroNoUsuarios() throws Exception {
		String gerente = usuarios.bearer("gerente");
		mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, gerente)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, gerente)).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("El auditor consulta la auditoría: 200")
	void auditorConsultaAuditoria() throws Exception {
		mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("auditor")))
				.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "{0}: {1} permisos")
	@CsvSource({"admin, 26", "gerente, 22", "jefe, 8", "comprador, 7", "vendedor, 6", "auditor, 4"})
	@DisplayName("[RNF-SEG-002] Cada rol recibe en su token exactamente los permisos de la matriz 2.2")
	void permisosPorRol(String usuario, int permisos) throws Exception {
		mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, usuarios.bearer(usuario)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.permisos", hasSize(permisos)));
	}

	@Test
	@DisplayName("[RNF-SEG-002] El vendedor no tiene el permiso COSTOS_VER (matriz 2.2)")
	void vendedorSinCostos() throws Exception {
		mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(jsonPath("$.permisos", not(hasItem("COSTOS_VER"))));
	}

}
