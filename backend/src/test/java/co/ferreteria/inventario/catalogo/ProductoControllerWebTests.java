/**
 * Nombre: ProductoControllerWebTests.java
 * Descripción: Pruebas de capa web (MockMvc) del controlador de productos: validación con mensajes en
 *              español e inglés, 401 sin token, 403 sin permiso, forma del JSON y de Problem Details.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import co.ferreteria.inventario.catalogo.dto.ProductoPeticion;
import co.ferreteria.inventario.catalogo.dto.ProductoRespuesta;
import co.ferreteria.inventario.catalogo.dto.ReferenciaRespuesta;
import co.ferreteria.inventario.catalogo.dto.TarifaIvaRespuesta;
import co.ferreteria.inventario.catalogo.dto.UnidadMedidaRespuesta;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.common.i18n.Mensajes;
import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.config.LocaleConfig;
import co.ferreteria.inventario.security.ConfiguracionJwt;
import co.ferreteria.inventario.security.ManejadorErroresSeguridad;
import co.ferreteria.inventario.security.SecurityConfig;

/**
 * Capa web de {@code /api/v1/productos} con la seguridad real y el servicio simulado (RF-PRO-001,
 * RF-PRO-003, RF-SEG-001, RNF-SEG-001, RNF-INT-002).
 */
@WebMvcTest(controllers = ProductoController.class)
@ActiveProfiles("test")
@Import({ SecurityConfig.class, ConfiguracionJwt.class, ManejadorErroresSeguridad.class, LocaleConfig.class,
		Mensajes.class })
class ProductoControllerWebTests {

	private static final String PRODUCTOS = "/api/v1/productos";
	private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

	/** Cuerpo válido con todos los campos obligatorios. */
	private static final String VALIDO = """
			{"nombre":"Martillo","categoriaId":1,"marcaId":1,"unidadBase":"UND","tarifaIvaId":3,
			 "porcentajeIncremento":30,"control":"NINGUNO"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ProductoService servicio;

	// ------------------------------------------------------------------ autenticación y permisos

	@Test
	@DisplayName("[CA-RNF-SEG-001-1] Sin token, consultar, crear, editar e inactivar responden 401 en Problem Details")
	void sinTokenResponde401() throws Exception {
		mvc.perform(get(PRODUCTOS))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
		mvc.perform(get(PRODUCTOS + "/1")).andExpect(status().isUnauthorized());
		mvc.perform(post(PRODUCTOS).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnauthorized());
		mvc.perform(put(PRODUCTOS + "/1").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete(PRODUCTOS + "/1")).andExpect(status().isUnauthorized());
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Sin CATALOGO_EDITAR, crear, editar e inactivar responden 403 y el servicio no se invoca")
	void sinPermisoResponde403() throws Exception {
		JwtRequestPostProcessor vendedor = usuario("CATALOGO_CONSULTAR", "VENTA_REGISTRAR");

		mvc.perform(post(PRODUCTOS).with(vendedor).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
				.andExpect(jsonPath("$.title").value("Acceso denegado"));
		mvc.perform(put(PRODUCTOS + "/1").with(vendedor).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isForbidden());
		mvc.perform(delete(PRODUCTOS + "/1").with(vendedor)).andExpect(status().isForbidden());
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Sin CATALOGO_CONSULTAR, la consulta responde 403")
	void sinPermisoDeConsulta() throws Exception {
		mvc.perform(get(PRODUCTOS).with(usuario("AUDITORIA_CONSULTAR")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		verifyNoInteractions(servicio);
	}

	// ------------------------------------------------------------------ validación en español e inglés

	@Test
	@DisplayName("[CA-RF-PRO-001-2][CA-RNF-INT-002-1] Sin marca, sin unidad base y sin tarifa: 400 con campo y causa en español")
	void validacionEnEspanol() throws Exception {
		mvc.perform(post(PRODUCTOS).with(editor()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombre":"Martillo","categoriaId":1,"porcentajeIncremento":30,"control":"NINGUNO"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.title").value("Datos no válidos"))
				.andExpect(jsonPath("$.errores", hasSize(3)))
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("marcaId", "unidadBase", "tarifaIvaId")))
				.andExpect(jsonPath("$.errores[*].mensaje", containsInAnyOrder("es obligatorio", "es obligatorio",
						"es obligatorio")));
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-2][CA-RNF-INT-002-1] Sin marca: 400 con campo y causa en inglés con Accept-Language: en")
	void validacionEnIngles() throws Exception {
		mvc.perform(post(PRODUCTOS).with(editor()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALIDO.replace("\"marcaId\":1,", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid data"))
				.andExpect(jsonPath("$.errores[0].campo").value("marcaId"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("is required"));
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-2][CA-RNF-INT-002-1] Garantía sin plazo: el error queda en plazoGarantiaMeses, en es y en")
	void garantiaSinPlazo() throws Exception {
		String cuerpo = VALIDO.replace("\"NINGUNO\"", "\"GARANTIA\"");
		mvc.perform(post(PRODUCTOS).with(editor()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].campo").value("plazoGarantiaMeses"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("es obligatorio cuando el control es GARANTIA"));
		mvc.perform(post(PRODUCTOS).with(editor()).header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].mensaje").value("is required when control is GARANTIA"));
	}

	@Test
	@DisplayName("[CA-RNF-INT-002-1] Margen fuera de rango y nombre corto: un error por campo con su causa")
	void rangosYLongitudes() throws Exception {
		mvc.perform(post(PRODUCTOS).with(editor()).contentType(MediaType.APPLICATION_JSON)
						.content(VALIDO.replace("\"Martillo\"", "\"ab\"").replace(":30,", ":500.01,")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[?(@.campo == 'nombre')].mensaje").value("debe tener entre 3 y 150 caracteres"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'porcentajeIncremento')].mensaje")
						.value("debe ser menor o igual a 500"));
	}

	@Test
	@DisplayName("Un JSON mal formado responde 400 SOLICITUD_INVALIDA en Problem Details")
	void jsonMalFormado() throws Exception {
		mvc.perform(post(PRODUCTOS).with(editor()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
	}

	// ------------------------------------------------------------------ forma de las respuestas

	@Test
	@DisplayName("[CA-RF-PRO-001-1] Crear responde 201 con Location y el producto en JSON")
	void crearRespondeProducto() throws Exception {
		when(servicio.crear(any(ProductoPeticion.class))).thenReturn(producto(10L, true));

		mvc.perform(post(PRODUCTOS).with(editor()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/v1/productos/10")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.nombre").value("Martillo"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.marca.id").value(1))
				.andExpect(jsonPath("$.marca.nombre").value("Stanley"))
				.andExpect(jsonPath("$.categoria.nombre").value("Herramientas"))
				.andExpect(jsonPath("$.unidadBase.codigo").value("UND"))
				.andExpect(jsonPath("$.unidadBase.esDiscreta").value(true))
				.andExpect(jsonPath("$.tarifaIva.porcentaje").value(19.0))
				.andExpect(jsonPath("$.porcentajeIncremento").value(30.0))
				.andExpect(jsonPath("$.control").value("NINGUNO"))
				.andExpect(jsonPath("$.creadoEn").value("2026-10-20T15:30:00Z"));
	}

	@Test
	@DisplayName("[CA-RF-PRO-003-3] Sin costos en la respuesta, el JSON omite porcentajeIncremento y ultimoCostoBase")
	void respuestaSinCostosOmiteCampos() throws Exception {
		when(servicio.obtener(10L)).thenReturn(producto(10L, false));

		mvc.perform(get(PRODUCTOS + "/10").with(usuario("CATALOGO_CONSULTAR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Martillo"))
				.andExpect(jsonPath("$.porcentajeIncremento").doesNotExist())
				.andExpect(jsonPath("$.ultimoCostoBase").doesNotExist());
	}

	@Test
	@DisplayName("La búsqueda responde una página con contenido, pagina, tamano, totalElementos y totalPaginas")
	void formaDeLaPagina() throws Exception {
		when(servicio.listar(eq("mar"), eq(null), eq(EstadoProducto.ACTIVO), any()))
				.thenReturn(Pagina.de(List.of(producto(10L, false)), 0, 20, 21));

		mvc.perform(get(PRODUCTOS).param("q", "mar").param("estado", "ACTIVO").with(usuario("CATALOGO_CONSULTAR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido", hasSize(1)))
				.andExpect(jsonPath("$.contenido[0].id").value(10))
				.andExpect(jsonPath("$.pagina").value(0))
				.andExpect(jsonPath("$.tamano").value(20))
				.andExpect(jsonPath("$.totalElementos").value(21))
				.andExpect(jsonPath("$.totalPaginas").value(2));
	}

	@Test
	@DisplayName("Un texto de búsqueda de más de 150 caracteres responde 400 en el parámetro q")
	void busquedaDemasiadoLarga() throws Exception {
		mvc.perform(get(PRODUCTOS).param("q", "x".repeat(151)).with(usuario("CATALOGO_CONSULTAR")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].campo").value("q"));
	}

	@Test
	@DisplayName("Problem Details de 404: title, status, detail, instance, codigo y errores vacíos (type implícito about:blank)")
	void problemDetailsNoEncontrado() throws Exception {
		when(servicio.obtener(99L)).thenThrow(new RecursoNoEncontradoException("entidad.producto", 99L));

		mvc.perform(get(PRODUCTOS + "/99").with(usuario("CATALOGO_CONSULTAR")))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				// Sin "type", RFC 9457 lo interpreta como about:blank: el tipo lo da "codigo".
				.andExpect(jsonPath("$.type").doesNotExist())
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("No existe el producto con identificador 99."))
				.andExpect(jsonPath("$.instance").value(PRODUCTOS + "/99"))
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.errores", hasSize(0)));
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-3] Problem Details de 409: duplicado con la causa traducida en es y en")
	void problemDetailsDuplicado() throws Exception {
		when(servicio.crear(any(ProductoPeticion.class)))
				.thenThrow(new ConflictoException("producto.duplicado", "Martillo", "Stanley"));

		mvc.perform(post(PRODUCTOS).with(editor()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
				.andExpect(jsonPath("$.title").value("Registro duplicado"))
				.andExpect(jsonPath("$.detail").value("Ya existe el producto «Martillo» para la marca Stanley."));
		mvc.perform(post(PRODUCTOS).with(editor()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(jsonPath("$.title").value("Duplicate record"))
				.andExpect(jsonPath("$.detail").value("The product \"Martillo\" already exists for brand Stanley."));
	}

	@Test
	@DisplayName("Inactivar responde 204 sin cuerpo; uno inexistente responde 404")
	void inactivar() throws Exception {
		doThrow(new RecursoNoEncontradoException("entidad.producto", 99L)).when(servicio).inactivar(99L);

		mvc.perform(delete(PRODUCTOS + "/10").with(editor()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		mvc.perform(delete(PRODUCTOS + "/99").with(editor()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	@Test
	@DisplayName("Editar responde 200 con el producto actualizado")
	void editar() throws Exception {
		when(servicio.actualizar(anyLong(), any(ProductoPeticion.class))).thenReturn(producto(10L, true));

		mvc.perform(put(PRODUCTOS + "/10").with(editor()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(10));
	}

	// ------------------------------------------------------------------ utilidades

	private static JwtRequestPostProcessor editor() {
		return usuario("CATALOGO_CONSULTAR", "CATALOGO_EDITAR", "COSTOS_VER");
	}

	private static JwtRequestPostProcessor usuario(String... permisos) {
		return jwt().jwt(j -> j.subject("4")).authorities(
				java.util.Arrays.stream(permisos).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
	}

	private static ProductoRespuesta producto(long id, boolean conCostos) {
		return ProductoRespuesta.builder()
				.id(id)
				.nombre("Martillo")
				.estado(EstadoProducto.ACTIVO)
				.categoria(new ReferenciaRespuesta(1L, "Herramientas"))
				.marca(new ReferenciaRespuesta(1L, "Stanley"))
				.unidadBase(new UnidadMedidaRespuesta("UND", "Unidad", true))
				.tarifaIva(new TarifaIvaRespuesta((short) 3, "Tarifa general", new BigDecimal("19.00")))
				.porcentajeIncremento(conCostos ? new BigDecimal("30.00") : null)
				.control(ControlProducto.NINGUNO)
				.ultimoCostoBase(conCostos ? new BigDecimal("18000.0000") : null)
				.creadoEn(Instant.parse("2026-10-20T15:30:00Z"))
				.build();
	}

}
