/**
 * Nombre: UsuarioIntegracionTests.java
 * Descripción: Pruebas de la API de administración de usuarios (alta, validación, desactivación y
 *              protección del último administrador).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;
import co.ferreteria.inventario.soporte.UsuariosPrueba;

/**
 * Gestión de usuarios (RF-SEG-001, RNF-SEG-001, RNF-INT-002).
 */
class UsuarioIntegracionTests extends PruebaIntegracionApi {

	private static final String USUARIOS = "/api/v1/usuarios";

	private String admin;

	@BeforeEach
	void iniciarSesion() throws Exception {
		admin = usuarios.bearer("admin");
	}

	@Test
	@DisplayName("[CA-RNF-SEG-001-2] Crear un usuario: 201, sin hash en la respuesta y contraseña guardada con BCrypt fuerza 12")
	void crearUsuario() throws Exception {
		String nombre = nombreNuevo();
		String cuerpo = crear(nombre, "Clave12345", "[\"COMPRADOR\"]")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.usuario").value(nombre))
				.andExpect(jsonPath("$.activo").value(true))
				.andExpect(jsonPath("$.roles", contains("COMPRADOR")))
				.andExpect(jsonPath("$.creadoEn").isNotEmpty())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.contrasena").doesNotExist())
				.andReturn().getResponse().getContentAsString();

		assertThat(cuerpo).doesNotContain("$2a$");
		String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE usuario = ?", String.class, nombre);
		assertThat(hash).startsWith("$2a$12$").doesNotContain("Clave12345");

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(UsuariosPrueba.credenciales(nombre, "Clave12345")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Contraseña sin números o corta: 400 con el error en el campo contrasena")
	void contrasenaDebil() throws Exception {
		crear(nombreNuevo(), "soloLetras", "[\"VENDEDOR\"]")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", hasItem("contrasena")));
		crear(nombreNuevo(), "abc12", "[\"VENDEDOR\"]")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", hasItem("contrasena")));
	}

	@Test
	@DisplayName("Sin roles: 400; rol inexistente: 404")
	void rolesNoValidos() throws Exception {
		crear(nombreNuevo(), "Clave12345", "[]").andExpect(status().isBadRequest());
		crear(nombreNuevo(), "Clave12345", "[\"CAJERO\"]")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	@Test
	@DisplayName("Usuario duplicado: 409")
	void usuarioDuplicado() throws Exception {
		crear("vendedor", "Clave12345", "[\"VENDEDOR\"]")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"));
	}

	@Test
	@DisplayName("Un administrador no puede desactivarse a sí mismo")
	void administradorNoSeDesactivaASiMismo() throws Exception {
		mvc.perform(patch(USUARIOS + "/{id}/estado", idUsuario("admin")).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
		assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE usuario = 'admin'", Boolean.class)).isTrue();
	}

	@Test
	@DisplayName("El último administrador activo no puede quitarse el rol ADMIN")
	void ultimoAdministradorConservaElRol() throws Exception {
		mvc.perform(put(USUARIOS + "/{id}/roles", idUsuario("admin")).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"GERENTE\"]}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
		assertThat(jdbc.queryForObject("""
				SELECT count(*) FROM usuario_rol ur JOIN rol r ON r.id = ur.rol_id
				 WHERE ur.usuario_id = ? AND r.codigo = 'ADMIN'
				""", Integer.class, idUsuario("admin"))).isEqualTo(1);
	}

	@Test
	@DisplayName("Con otro administrador activo, sí se puede quitar el rol ADMIN a un usuario")
	void conOtroAdministradorSePuedeQuitarElRol() throws Exception {
		String nombre = nombreNuevo();
		long id = idCreado(crear(nombre, "Clave12345", "[\"ADMIN\"]").andExpect(status().isCreated()));

		mvc.perform(put(USUARIOS + "/{id}/roles", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"AUDITOR\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles", contains("AUDITOR")));
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-3] Un usuario desactivado por la API ya no inicia sesión")
	void usuarioDesactivadoNoIniciaSesion() throws Exception {
		String nombre = nombreNuevo();
		long id = idCreado(crear(nombre, "Clave12345", "[\"VENDEDOR\"]").andExpect(status().isCreated()));

		mvc.perform(patch(USUARIOS + "/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(UsuariosPrueba.credenciales(nombre, "Clave12345")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-3] Los usuarios no se eliminan: DELETE no existe")
	void noSeEliminanUsuarios() throws Exception {
		mvc.perform(delete(USUARIOS + "/{id}", idUsuario("vendedor")).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	@DisplayName("Editar nombre y correo; el usuario Sistema no se puede modificar")
	void editarYUsuarioSistema() throws Exception {
		String nombre = nombreNuevo();
		long id = idCreado(crear(nombre, "Clave12345", "[\"VENDEDOR\"]").andExpect(status().isCreated()));

		mvc.perform(put(USUARIOS + "/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombreCompleto\":\"Nombre Nuevo\",\"correo\":\"" + nombre + "-b@ferreteria.test\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombreCompleto").value("Nombre Nuevo"));

		long sistema = jdbc.queryForObject("SELECT id FROM usuario WHERE es_sistema", Long.class);
		mvc.perform(patch(USUARIOS + "/{id}/estado", sistema).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Listar usuarios y roles: no aparece el usuario Sistema ni ningún hash")
	void listarUsuariosYRoles() throws Exception {
		String cuerpo = mvc.perform(get(USUARIOS).header(HttpHeaders.AUTHORIZATION, admin).param("tamano", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[*].usuario", hasItem("vendedor")))
				.andReturn().getResponse().getContentAsString();
		assertThat(cuerpo).doesNotContain("\"sistema\"").doesNotContain("passwordHash");

		mvc.perform(get("/api/v1/roles").header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].codigo", hasItem("AUDITOR")));
	}

	private ResultActions crear(String usuario, String contrasena, String roles) throws Exception {
		return mvc.perform(post(USUARIOS).header(HttpHeaders.AUTHORIZATION, admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"usuario":"%s","nombreCompleto":"Persona de prueba","correo":"%s@ferreteria.test",
						 "contrasena":"%s","roles":%s}
						""".formatted(usuario, usuario + UUID.randomUUID().toString().substring(0, 4), contrasena, roles)));
	}

	private static String nombreNuevo() {
		return "u" + UUID.randomUUID().toString().substring(0, 8);
	}

	private static long idCreado(ResultActions resultado) throws Exception {
		return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

}
