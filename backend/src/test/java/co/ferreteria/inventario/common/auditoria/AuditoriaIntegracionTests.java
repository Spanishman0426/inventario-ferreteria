/**
 * Nombre: AuditoriaIntegracionTests.java
 * Descripción: Pruebas de la auditoría de accesos y de cambios hechos por la API, y de su consulta.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;
import co.ferreteria.inventario.soporte.UsuariosPrueba;

/**
 * Auditoría de punta a punta (RF-SEG-002, RN-28, RNF-SEG-003).
 */
class AuditoriaIntegracionTests extends PruebaIntegracionApi {

	@Test
	@DisplayName("[CA-RF-SEG-002-1] Un inicio de sesión correcto genera exactamente un registro de auditoría")
	void loginGeneraUnSoloRegistro() throws Exception {
		long antes = ultimoIdAuditoria();

		usuarios.token("gerente");

		List<String> acciones = jdbc.queryForList("SELECT accion FROM auditoria WHERE id > ?", String.class, antes);
		assertThat(acciones).containsExactly("INICIAR_SESION");
	}

	@Test
	@DisplayName("[CA-RF-SEG-002-1] Un inicio de sesión fallido genera exactamente un registro de auditoría")
	void loginFallidoGeneraUnSoloRegistro() throws Exception {
		long antes = ultimoIdAuditoria();

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(UsuariosPrueba.credenciales("gerente", "incorrecta123")))
				.andExpect(status().isUnauthorized());

		List<String> acciones = jdbc.queryForList("SELECT accion FROM auditoria WHERE id > ?", String.class, antes);
		assertThat(acciones).containsExactly("SESION_FALLIDA");
	}

	@Test
	@DisplayName("[CA-RF-SEG-002-1] Desactivar un usuario por la API deja un MODIFICAR con el usuario_id del administrador y su IP")
	void desactivarUsuarioQuedaAuditadoConElAdministrador() throws Exception {
		String admin = usuarios.bearer("admin");
		long id = crearUsuario(admin);
		long antes = ultimoIdAuditoria();

		mvc.perform(patch("/api/v1/usuarios/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(false));

		List<Map<String, Object>> filas = jdbc.queryForList("""
				SELECT accion, entidad, entidad_id, usuario_id, host(ip) AS ip,
				       valores_antes->>'activo' AS antes, valores_despues->>'activo' AS despues
				  FROM auditoria WHERE id > ?
				""", antes);
		assertThat(filas).hasSize(1);
		Map<String, Object> fila = filas.getFirst();
		assertThat(fila).containsEntry("accion", "MODIFICAR").containsEntry("entidad", "usuario")
				.containsEntry("ip", IP_LOCAL).containsEntry("antes", "true").containsEntry("despues", "false");
		assertThat(((Number) fila.get("entidad_id")).longValue()).isEqualTo(id);
		assertThat(((Number) fila.get("usuario_id")).longValue()).isEqualTo(idUsuario("admin"));
	}

	@Test
	@DisplayName("Crear un usuario por la API audita el usuario y su rol a nombre del administrador")
	void crearUsuarioQuedaAuditado() throws Exception {
		long antes = ultimoIdAuditoria();
		long id = crearUsuario(usuarios.bearer("admin"));

		List<Map<String, Object>> filas = jdbc.queryForList(
				"SELECT accion, entidad, usuario_id FROM auditoria WHERE id > ? AND accion <> 'INICIAR_SESION' ORDER BY id",
				antes);
		assertThat(filas).extracting(f -> f.get("entidad")).containsExactly("usuario", "usuario_rol");
		assertThat(filas).allSatisfy(f -> {
			assertThat(f.get("accion")).isEqualTo("CREAR");
			assertThat(((Number) f.get("usuario_id")).longValue()).isEqualTo(idUsuario("admin"));
		});
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM auditoria WHERE id > ? AND (valores_despues -> 'password_hash') IS NOT NULL", Integer.class, antes))
				.as("ningún registro guarda el hash").isZero();
		assertThat(id).isPositive();
	}

	@Test
	@DisplayName("Cambiar la contraseña por la API deja un MODIFICAR marcado, sin el hash")
	void cambioDeContrasenaQuedaAuditadoSinHash() throws Exception {
		String admin = usuarios.bearer("admin");
		long id = crearUsuario(admin);
		long antes = ultimoIdAuditoria();

		mvc.perform(put("/api/v1/usuarios/{id}/contrasena", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"contrasena\":\"OtraClave99\"}"))
				.andExpect(status().isNoContent());

		Map<String, Object> fila = jdbc.queryForMap("""
				SELECT accion, usuario_id, valores_despues->>'contrasena_cambiada' AS marca,
				       (valores_antes -> 'password_hash') IS NOT NULL OR (valores_despues -> 'password_hash') IS NOT NULL AS tiene_hash
				  FROM auditoria WHERE id > ?
				""", antes);
		assertThat(fila).containsEntry("accion", "MODIFICAR").containsEntry("marca", "true")
				.containsEntry("tiene_hash", false);
		assertThat(((Number) fila.get("usuario_id")).longValue()).isEqualTo(idUsuario("admin"));
	}

	@Test
	@DisplayName("La consulta filtra por usuario, acción y fechas, y muestra el detalle con antes y después")
	void consultaConFiltrosYDetalle() throws Exception {
		usuarios.token("comprador");
		String auditor = usuarios.bearer("auditor");
		String hoy = LocalDate.now().toString();
		long comprador = idUsuario("comprador");

		String cuerpo = mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, auditor)
						.param("usuarioId", Long.toString(comprador)).param("accion", "INICIAR_SESION")
						.param("desde", hoy).param("hasta", hoy).param("tamano", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos", greaterThan(0)))
				.andExpect(jsonPath("$.contenido[*].accion", everyItem(is("INICIAR_SESION"))))
				.andExpect(jsonPath("$.contenido[0].usuario").value("comprador"))
				.andExpect(jsonPath("$.contenido[0].ip").value(IP_LOCAL))
				.andReturn().getResponse().getContentAsString();

		Number id = JsonPath.read(cuerpo, "$.contenido[0].id");
		mvc.perform(get("/api/v1/auditoria/{id}", id.longValue()).header(HttpHeaders.AUTHORIZATION, auditor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valoresDespues.usuario").value("comprador"));

		mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, auditor)
						.param("desde", LocalDate.now().plusDays(1).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(0));
	}

	@Test
	@DisplayName("Un filtro de acción no válido responde 400")
	void filtroNoValido() throws Exception {
		mvc.perform(get("/api/v1/auditoria").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("auditor"))
						.param("accion", "BORRAR_TODO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	private long crearUsuario(String bearer) throws Exception {
		String nombre = "aud" + UUID.randomUUID().toString().substring(0, 8);
		String cuerpo = mvc.perform(post("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, bearer)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuario":"%s","nombreCompleto":"Usuario de auditoría","correo":"%s@ferreteria.test",
								 "contrasena":"Clave12345","roles":["VENDEDOR"]}
								""".formatted(nombre, nombre)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
	}

}
