/**
 * Nombre: ElementoCatalogoControllerWebTests.java
 * Descripción: Pruebas de capa web (MockMvc) de /api/v1/marcas y /api/v1/categorias: validación con mensajes
 *              en español e inglés, 401 sin token, 403 sin CONFIG_GESTIONAR, 409 por duplicado y 405 en DELETE.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.ferreteria.inventario.catalogo.dto.ElementoCatalogoPeticion;
import co.ferreteria.inventario.catalogo.dto.ElementoCatalogoRespuesta;
import co.ferreteria.inventario.catalogo.dto.ReferenciaRespuesta;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.i18n.Mensajes;
import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.config.LocaleConfig;
import co.ferreteria.inventario.security.ConfiguracionJwt;
import co.ferreteria.inventario.security.ManejadorErroresSeguridad;
import co.ferreteria.inventario.security.SecurityConfig;

/**
 * Capa web de los catálogos de marcas y categorías con la seguridad real y los servicios simulados
 * (RF-CFG-001, RF-SEG-001, RNF-SEG-001, RNF-INT-002). Cada prueba se ejecuta para las dos rutas.
 */
@WebMvcTest(controllers = { MarcaController.class, CategoriaController.class })
@ActiveProfiles("test")
@Import({ SecurityConfig.class, ConfiguracionJwt.class, ManejadorErroresSeguridad.class, LocaleConfig.class,
		Mensajes.class })
class ElementoCatalogoControllerWebTests {

	private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
	private static final String VALIDO = "{\"nombre\":\"Bosch\"}";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private MarcaService marcas;

	@MockitoBean
	private CategoriaService categorias;

	// ------------------------------------------------------------------ autenticación y permisos

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("[CA-RNF-SEG-001-1] Sin token, consultar, crear, editar y cambiar estado responden 401")
	void sinTokenResponde401(String ruta) throws Exception {
		mvc.perform(get(ruta))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
		mvc.perform(get(ruta + "/activas")).andExpect(status().isUnauthorized());
		mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnauthorized());
		mvc.perform(put(ruta + "/1").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnauthorized());
		mvc.perform(patch(ruta + "/1/estado").contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"INACTIVO\"}")).andExpect(status().isUnauthorized());
		verifyNoInteractions(marcas, categorias);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("[CA-RF-SEG-001-2] Sin CONFIG_GESTIONAR (jefe de inventario), crear, editar y cambiar estado responden 403")
	void sinPermisoResponde403(String ruta) throws Exception {
		JwtRequestPostProcessor jefe = usuario("CATALOGO_CONSULTAR", "CATALOGO_EDITAR");

		mvc.perform(post(ruta).with(jefe).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		mvc.perform(put(ruta + "/1").with(jefe).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isForbidden());
		mvc.perform(patch(ruta + "/1/estado").with(jefe).contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"INACTIVO\"}")).andExpect(status().isForbidden());
		verifyNoInteractions(marcas, categorias);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("[CA-RF-SEG-001-2] Sin CATALOGO_CONSULTAR, la búsqueda y las activas responden 403")
	void sinPermisoDeConsulta(String ruta) throws Exception {
		mvc.perform(get(ruta).with(usuario("AUDITORIA_VER"))).andExpect(status().isForbidden());
		mvc.perform(get(ruta + "/activas").with(usuario("AUDITORIA_VER"))).andExpect(status().isForbidden());
		verifyNoInteractions(marcas, categorias);
	}

	// ------------------------------------------------------------------ validación

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("[CA-RNF-INT-002-1] Nombre vacío, de 1 o de 101 caracteres: 400 con campo y causa en español")
	void validacionEnEspanol(String ruta) throws Exception {
		mvc.perform(post(ruta).with(admin()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'nombre')].mensaje").value(
						org.hamcrest.Matchers.hasItem("es obligatorio")));
		mvc.perform(post(ruta).with(admin()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"B\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0].campo").value("nombre"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("debe tener entre 2 y 100 caracteres"));
		mvc.perform(put(ruta + "/1").with(admin()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + "x".repeat(101) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].mensaje").value("debe tener entre 2 y 100 caracteres"));
		verifyNoInteractions(marcas, categorias);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("[CA-RNF-INT-002-1] Sin nombre y con estado no válido: 400 con la causa en inglés")
	void validacionEnIngles(String ruta) throws Exception {
		mvc.perform(post(ruta).with(admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid data"))
				.andExpect(jsonPath("$.errores[0].campo").value("nombre"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("is required"));
		mvc.perform(patch(ruta + "/1/estado").with(admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].campo").value("estado"));
		verifyNoInteractions(marcas, categorias);
	}

	// ------------------------------------------------------------------ respuestas

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("Crear responde 201 con Location y el elemento ACTIVO")
	void crear(String ruta) throws Exception {
		when(servicio(ruta).crear(any(ElementoCatalogoPeticion.class)))
				.thenReturn(new ElementoCatalogoRespuesta(30L, "Bosch", EstadoCatalogo.ACTIVO));

		mvc.perform(post(ruta).with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, endsWith(ruta + "/30")))
				.andExpect(jsonPath("$.id").value(30))
				.andExpect(jsonPath("$.nombre").value("Bosch"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("Problem Details de 409: nombre duplicado con la causa traducida en es y en")
	void duplicado(String ruta) throws Exception {
		String clave = ruta.endsWith("marcas") ? "marca.duplicada" : "categoria.duplicada";
		when(servicio(ruta).crear(any(ElementoCatalogoPeticion.class)))
				.thenThrow(new ConflictoException(clave, "Bosch"));
		String entidadEs = ruta.endsWith("marcas") ? "la marca" : "la categoría";
		String entidadEn = ruta.endsWith("marcas") ? "brand" : "category";

		mvc.perform(post(ruta).with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
				.andExpect(jsonPath("$.detail").value("Ya existe " + entidadEs + " «Bosch»."));
		mvc.perform(post(ruta).with(admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(jsonPath("$.detail").value("The " + entidadEn + " \"Bosch\" already exists."));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("[CA-RF-CFG-001-3] No existe DELETE: responde 405; desactivar se hace con PATCH .../estado")
	void sinBorrado(String ruta) throws Exception {
		when(servicio(ruta).cambiarEstado(1L, EstadoCatalogo.INACTIVO))
				.thenReturn(new ElementoCatalogoRespuesta(1L, "Stanley", EstadoCatalogo.INACTIVO));

		mvc.perform(delete(ruta + "/1").with(admin()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
		mvc.perform(patch(ruta + "/1/estado").with(admin()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"estado\":\"INACTIVO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/v1/marcas", "/api/v1/categorias" })
	@DisplayName("La búsqueda pasa q y estado al servicio y responde una página; las activas, una lista")
	void consultas(String ruta) throws Exception {
		when(servicio(ruta).listar(eq("bo"), eq(EstadoCatalogo.INACTIVO), any()))
				.thenReturn(Pagina.de(List.of(new ElementoCatalogoRespuesta(5L, "Bosch", EstadoCatalogo.INACTIVO)), 0,
						20, 1));
		when(servicio(ruta).listarActivos()).thenReturn(List.of(new ReferenciaRespuesta(1L, "Stanley")));

		mvc.perform(get(ruta).param("q", "bo").param("estado", "INACTIVO").with(usuario("CATALOGO_CONSULTAR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[0].estado").value("INACTIVO"))
				.andExpect(jsonPath("$.totalElementos").value(1));
		mvc.perform(get(ruta + "/activas").with(usuario("CATALOGO_CONSULTAR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].nombre").value("Stanley"));
	}

	// ------------------------------------------------------------------ utilidades

	private ElementoCatalogoService<?, ?> servicio(String ruta) {
		return ruta.endsWith("marcas") ? marcas : categorias;
	}

	private static JwtRequestPostProcessor admin() {
		return usuario("CATALOGO_CONSULTAR", "CONFIG_GESTIONAR");
	}

	private static JwtRequestPostProcessor usuario(String... permisos) {
		return jwt().jwt(j -> j.subject("2")).authorities(
				Arrays.stream(permisos).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
	}

}
