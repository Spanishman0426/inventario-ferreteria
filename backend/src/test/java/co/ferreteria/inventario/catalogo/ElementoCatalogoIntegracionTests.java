/**
 * Nombre: ElementoCatalogoIntegracionTests.java
 * Descripción: Pruebas de integración (PostgreSQL en Testcontainers) de la administración de marcas y
 *              categorías: alta, búsqueda, edición, desactivación sin borrado, selectores y auditoría.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;

/**
 * Administración de catálogos contra la base real (RF-CFG-001, RF-PRO-001, RN-28). Cada prueba se
 * ejecuta para marcas y para categorías; los nombres llevan un sufijo aleatorio porque los elementos no
 * se borran entre pruebas.
 */
class ElementoCatalogoIntegracionTests extends PruebaIntegracionApi {

	/** Catálogo bajo prueba: ruta, tabla y campo que lo referencia en el producto. */
	enum Catalogo {
		MARCA("/api/v1/marcas", "marca", "marcaId", "categoriaId"),
		CATEGORIA("/api/v1/categorias", "categoria", "categoriaId", "marcaId");

		final String ruta;
		final String tabla;
		final String campoProducto;
		final String otroCampo;

		Catalogo(String ruta, String tabla, String campoProducto, String otroCampo) {
			this.ruta = ruta;
			this.tabla = tabla;
			this.campoProducto = campoProducto;
			this.otroCampo = otroCampo;
		}
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Catalogo.class)
	@DisplayName("[CA-RF-CFG-001-3] Desactivar un elemento en uso no lo elimina: sigue consultable como INACTIVO y el producto lo conserva")
	void desactivarElementoEnUso(Catalogo catalogo) throws Exception {
		String admin = usuarios.bearer("admin");
		String jefe = usuarios.bearer("jefe");
		long id = crear(catalogo, admin, "Prueba uso " + sufijo());
		long productoId = crearProducto(catalogo, jefe, id);

		mvc.perform(patch(catalogo.ruta + "/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"));

		assertThat(jdbc.queryForObject("SELECT activo FROM " + catalogo.tabla + " WHERE id = ?", Boolean.class, id))
				.as("la fila sigue existiendo, inactiva").isFalse();
		mvc.perform(get(catalogo.ruta).param("estado", "INACTIVO").param("size", "100")
						.header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(jsonPath("$.contenido[*].id", hasItem((int) id)));
		mvc.perform(get("/api/v1/productos/{id}", productoId).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$." + catalogo.tabla + ".id").value(id));

		// Repetir la desactivación no escribe ni audita de nuevo.
		long auditoriaAntes = ultimoIdAuditoria();
		mvc.perform(patch(catalogo.ruta + "/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
				.andExpect(status().isOk());
		assertThat(ultimoIdAuditoria()).isEqualTo(auditoriaAntes);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Catalogo.class)
	@DisplayName("[CA-RF-CFG-001-D1] Un elemento inactivo sale de /activas y no se acepta en un producto nuevo, pero el producto existente lo conserva al editarse")
	void selectoresSoloActivos(Catalogo catalogo) throws Exception {
		String admin = usuarios.bearer("admin");
		String jefe = usuarios.bearer("jefe");
		long id = crear(catalogo, admin, "Prueba selector " + sufijo());
		mvc.perform(get(catalogo.ruta + "/activas").header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(jsonPath("$[*].id", hasItem((int) id)));
		long productoId = crearProducto(catalogo, jefe, id);

		mvc.perform(patch(catalogo.ruta + "/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, admin)
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}")).andExpect(status().isOk());

		mvc.perform(get(catalogo.ruta + "/activas").header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
		mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, jefe)
						.contentType(MediaType.APPLICATION_JSON).content(producto(catalogo, "Nuevo " + sufijo(), id)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].campo").value(catalogo.campoProducto));
		mvc.perform(put("/api/v1/productos/{id}", productoId).header(HttpHeaders.AUTHORIZATION, jefe)
						.contentType(MediaType.APPLICATION_JSON)
						.content(producto(catalogo, "Editado " + sufijo(), id)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$." + catalogo.tabla + ".id").value(id));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Catalogo.class)
	@DisplayName("Crear, buscar por texto y estado, renombrar y reactivar; cada escritura deja un registro de auditoría con el usuario")
	void cicloCompletoConAuditoria(Catalogo catalogo) throws Exception {
		String admin = usuarios.bearer("admin");
		String marcaUnica = sufijo();
		long auditoriaAntes = ultimoIdAuditoria();
		long id = crear(catalogo, admin, "  Prueba ciclo " + marcaUnica + "  ");

		List<Map<String, Object>> registros = jdbc.queryForList(
				"SELECT accion, entidad, entidad_id, usuario_id FROM auditoria WHERE id > ? ORDER BY id", auditoriaAntes);
		assertThat(registros).hasSize(1);
		assertThat(registros.getFirst()).containsEntry("accion", "CREAR").containsEntry("entidad", catalogo.tabla);
		assertThat(((Number) registros.getFirst().get("entidad_id")).longValue()).isEqualTo(id);
		assertThat(((Number) registros.getFirst().get("usuario_id")).longValue()).isEqualTo(idUsuario("admin"));

		mvc.perform(get(catalogo.ruta).param("q", "CICLO " + marcaUnica.toUpperCase()).param("estado", "ACTIVO")
						.header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.contenido[0].nombre").value("Prueba ciclo " + marcaUnica));

		long antesDeEditar = ultimoIdAuditoria();
		mvc.perform(put(catalogo.ruta + "/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Prueba renombrada " + marcaUnica + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Prueba renombrada " + marcaUnica));
		mvc.perform(patch(catalogo.ruta + "/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, admin)
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}")).andExpect(status().isOk());
		mvc.perform(patch(catalogo.ruta + "/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ACTIVO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
		assertThat(jdbc.queryForList("SELECT accion FROM auditoria WHERE id > ? ORDER BY id", String.class, antesDeEditar))
				.containsExactly("MODIFICAR", "MODIFICAR", "MODIFICAR");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Catalogo.class)
	@DisplayName("Un nombre repetido con otras mayúsculas responde 409 al crear y al renombrar, sin auditar")
	void duplicadoSinDistinguirMayusculas(Catalogo catalogo) throws Exception {
		String admin = usuarios.bearer("admin");
		String nombre = "Prueba duplicado " + sufijo();
		crear(catalogo, admin, nombre);
		long otro = crear(catalogo, admin, "Prueba otro " + sufijo());
		long auditoriaAntes = ultimoIdAuditoria();

		mvc.perform(post(catalogo.ruta).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre.toUpperCase() + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"));
		mvc.perform(put(catalogo.ruta + "/{id}", otro).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre.toLowerCase() + "\"}"))
				.andExpect(status().isConflict());
		assertThat(ultimoIdAuditoria()).isEqualTo(auditoriaAntes);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Catalogo.class)
	@DisplayName("Editar o cambiar el estado de un id inexistente responde 404")
	void noEncontrado(Catalogo catalogo) throws Exception {
		String admin = usuarios.bearer("admin");
		mvc.perform(put(catalogo.ruta + "/{id}", 999_999).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"No existe\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
		mvc.perform(patch(catalogo.ruta + "/{id}/estado", 999_999).header(HttpHeaders.AUTHORIZATION, admin)
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ACTIVO\"}")).andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------------ apoyo

	private long crear(Catalogo catalogo, String token, String nombre) throws Exception {
		String cuerpo = mvc.perform(post(catalogo.ruta).header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
	}

	private long crearProducto(Catalogo catalogo, String token, long id) throws Exception {
		String cuerpo = mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON).content(producto(catalogo, "Producto " + sufijo(), id)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
	}

	private static String producto(Catalogo catalogo, String nombre, long id) {
		return """
				{"nombre":"%s","%s":%d,"%s":1,"unidadBase":"UND","tarifaIvaId":3,
				 "porcentajeIncremento":30,"control":"NINGUNO"}""".formatted(nombre, catalogo.campoProducto, id,
				catalogo.otroCampo);
	}

	private static String sufijo() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

}
