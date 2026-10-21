/**
 * Nombre: AuditoriaIntegracionTests.java
 * Descripción: Pruebas de la auditoría de accesos y de cambios hechos por la API, de su consulta, de su
 *              inmutabilidad por SQL y por API, de la ausencia de contraseñas y de la depuración a 24 meses.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

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

	@Test
	@DisplayName("[CA-RF-SEG-002-2][CA-RNF-SEG-004-1] Por SQL con el usuario de la aplicación, modificar o borrar un registro de auditoría se rechaza (P0001)")
	void auditoriaInmutablePorSql() throws Exception {
		usuarios.token("gerente");
		long id = ultimoIdAuditoria();

		assertThatThrownBy(() -> jdbc.update("UPDATE auditoria SET accion = 'CREAR' WHERE id = ?", id))
				.satisfies(e -> assertThat(sqlState(e)).isEqualTo("P0001"));
		assertThatThrownBy(() -> jdbc.update("DELETE FROM auditoria WHERE id = ?", id))
				.satisfies(e -> assertThat(sqlState(e)).isEqualTo("P0001"));

		assertThat(jdbc.queryForObject("SELECT accion FROM auditoria WHERE id = ?", String.class, id))
				.isEqualTo("INICIAR_SESION");
	}

	@Test
	@DisplayName("[CA-RF-SEG-002-2][CA-RNF-SEG-004-1] Por API no hay forma de modificar ni borrar la auditoría: 405 y el registro queda igual")
	void auditoriaInmutablePorApi() throws Exception {
		String admin = usuarios.bearer("admin");
		long id = ultimoIdAuditoria();

		mvc.perform(put("/api/v1/auditoria/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"CREAR\"}"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
		mvc.perform(delete("/api/v1/auditoria/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isMethodNotAllowed());

		assertThat(jdbc.queryForObject("SELECT accion FROM auditoria WHERE id = ?", String.class, id))
				.isEqualTo("INICIAR_SESION");
	}

	@Test
	@DisplayName("[CA-RNF-SEG-001-2] Ni la contraseña ni su hash aparecen en la auditoría, y en usuario solo se guarda el hash BCrypt")
	void sinContrasenasEnTextoPlano() throws Exception {
		crearUsuario(usuarios.bearer("admin"));
		usuarios.token("vendedor");

		assertThat(jdbc.queryForObject("""
				SELECT count(*) FROM auditoria
				 WHERE coalesce(valores_antes::text, '') || coalesce(valores_despues::text, '') LIKE '%' || ? || '%'
				    OR coalesce(valores_antes::text, '') || coalesce(valores_despues::text, '') LIKE '%Clave12345%'
				    OR coalesce(valores_antes::text, '') || coalesce(valores_despues::text, '') ~ '\\$2[aby]\\$'
				""", Integer.class, UsuariosPrueba.CONTRASENA)).isZero();
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM usuario WHERE NOT es_sistema AND password_hash !~ '^\\$2[aby]\\$12\\$'", Integer.class))
				.as("toda contraseña guardada es un hash BCrypt de fuerza 12").isZero();
	}

	@Test
	@Transactional
	@DisplayName("[CA-RF-SEG-002-3][CA-RNF-SEG-003-2] La depuración elimina el registro de 25 meses, conserva el de 23 y deja constancia DEPURAR")
	void depuracionAVeinticuatroMeses() {
		// Se revierte al terminar: las particiones creadas y borradas vuelven a como estaban.
		jdbc.execute("SELECT fn_crear_particion_auditoria((now() - interval '25 months')::date)");
		jdbc.execute("SELECT fn_crear_particion_auditoria((now() - interval '23 months')::date)");
		jdbc.update("""
				INSERT INTO auditoria (fecha, accion, entidad, entidad_id)
				VALUES (now() - interval '25 months', 'CREAR', 'prueba_depuracion', 25),
				       (now() - interval '23 months', 'CREAR', 'prueba_depuracion', 23)
				""");

		Integer borradas = jdbc.queryForObject("SELECT fn_depurar_auditoria()", Integer.class);

		assertThat(borradas).isPositive();
		assertThat(jdbc.queryForList("SELECT entidad_id FROM auditoria WHERE entidad = 'prueba_depuracion'", Long.class))
				.containsExactly(23L);
		assertThat(jdbc.queryForObject("""
				SELECT (valores_despues->>'particiones_eliminadas')::int FROM auditoria
				 WHERE accion = 'DEPURAR' ORDER BY id DESC LIMIT 1
				""", Integer.class)).isEqualTo(borradas);
	}

	private static String sqlState(Throwable error) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof SQLException sql && sql.getSQLState() != null) {
				return sql.getSQLState();
			}
		}
		return null;
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
