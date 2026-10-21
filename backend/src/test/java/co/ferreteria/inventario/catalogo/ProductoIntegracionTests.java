/**
 * Nombre: ProductoIntegracionTests.java
 * Descripción: Pruebas de integración del CRUD de productos: alta, validación, duplicados, búsqueda,
 *              edición, borrado lógico, ciclo completo por HTTP, auditoría, permisos, costos por rol y
 *              mensajes en inglés.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import co.ferreteria.inventario.catalogo.dto.ProductoPeticion;
import co.ferreteria.inventario.soporte.PruebaIntegracionApi;
import tools.jackson.databind.json.JsonMapper;

/**
 * CRUD de productos (RF-PRO-001, RF-PRO-003, RNF-SEG-002, RNF-INT-002) sobre los datos de V100.
 * Cada prueba crea productos con nombre único para no depender del orden de ejecución.
 */
class ProductoIntegracionTests extends PruebaIntegracionApi {

	private static final String PRODUCTOS = "/api/v1/productos";

	/** Marca Stanley, categoría Herramientas, unidad UND e IVA 19 % de V100. */
	private static final long STANLEY = 1;
	private static final long TRUPER = 5;
	private static final long HERRAMIENTAS = 1;
	private static final long PINTURAS = 4;
	private static final short IVA_19 = 3;

	/** Martillo de V100: tiene presentaciones y lotes. */
	private static final long MARTILLO = 1;

	@Autowired
	private JsonMapper json;

	private String jefe;

	@BeforeEach
	void iniciarSesion() throws Exception {
		jefe = usuarios.bearer("jefe");
	}

	// ------------------------------------------------------------------ RF-PRO-001: registrar

	@Test
	@DisplayName("[CA-RF-PRO-001-1] Un producto válido queda registrado (201 con Location), puede consultarse y se audita una vez")
	void registrarProductoValido() throws Exception {
		String nombre = nombreNuevo("Martillo");
		long auditoriaAntes = ultimoIdAuditoria();

		String cuerpo = crear(jefe, peticion(nombre).control(ControlProducto.GARANTIA).plazoGarantiaMeses(6)
						.descripcion("  Mango de fibra  ").build())
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/api/v1/productos/\\d+$")))
				.andExpect(jsonPath("$.nombre").value(nombre))
				.andExpect(jsonPath("$.descripcion").value("Mango de fibra"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.marca.nombre").value("Stanley"))
				.andExpect(jsonPath("$.categoria.nombre").value("Herramientas"))
				.andExpect(jsonPath("$.unidadBase.codigo").value("UND"))
				.andExpect(jsonPath("$.unidadBase.esDiscreta").value(true))
				.andExpect(jsonPath("$.tarifaIva.porcentaje").value(19.0))
				.andExpect(jsonPath("$.porcentajeIncremento").value(30.0))
				.andExpect(jsonPath("$.control").value("GARANTIA"))
				.andExpect(jsonPath("$.plazoGarantiaMeses").value(6))
				.andExpect(jsonPath("$.creadoEn").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();

		mvc.perform(get(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nombre").value(nombre));

		Integer registros = jdbc.queryForObject("""
				SELECT count(*) FROM auditoria
				 WHERE id > ? AND entidad = 'producto' AND entidad_id = ? AND accion = 'CREAR' AND usuario_id = ?
				""", Integer.class, auditoriaAntes, id, idUsuario("jefe"));
		assertThat(registros).isEqualTo(1);
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-2] Se rechaza un producto sin marca, sin unidad base o sin tarifa de IVA, con el error en su campo")
	void rechazarSinObligatorios() throws Exception {
		crear(jefe, peticion(nombreNuevo("X")).marcaId(null).build())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", contains("marcaId")));
		crear(jefe, peticion(nombreNuevo("X")).unidadBase(" ").build())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", hasItem("unidadBase")));
		crear(jefe, peticion(nombreNuevo("X")).tarifaIvaId(null).build())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", contains("tarifaIvaId")));
		crear(jefe, peticion(nombreNuevo("X")).categoriaId(null).nombre("ab").porcentajeIncremento(new BigDecimal("500.01")).build())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)));
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-2] Se rechaza un producto con garantía sin plazo, y un plazo sin control de garantía")
	void rechazarGarantiaSinPlazo() throws Exception {
		crear(jefe, peticion(nombreNuevo("X")).control(ControlProducto.GARANTIA).build())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].campo").value("plazoGarantiaMeses"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("es obligatorio cuando el control es GARANTIA"));
		crear(jefe, peticion(nombreNuevo("X")).control(ControlProducto.VENCIMIENTO).plazoGarantiaMeses(12).build())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].campo").value("plazoGarantiaMeses"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("solo se admite cuando el control es GARANTIA"));
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-2] Marca, categoría, unidad o tarifa inexistentes responden 400 en su campo")
	void rechazarReferenciasInexistentes() throws Exception {
		crear(jefe, peticion(nombreNuevo("X")).marcaId(9999L).build())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[0].campo").value("marcaId"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("no existe o está inactivo (9999)"));
		crear(jefe, peticion(nombreNuevo("X")).categoriaId(9999L).build())
				.andExpect(jsonPath("$.errores[0].campo").value("categoriaId"));
		crear(jefe, peticion(nombreNuevo("X")).unidadBase("XYZ").build())
				.andExpect(jsonPath("$.errores[0].campo").value("unidadBase"));
		crear(jefe, peticion(nombreNuevo("X")).tarifaIvaId((short) 99).build())
				.andExpect(jsonPath("$.errores[0].campo").value("tarifaIvaId"));
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-3] Se rechaza un duplicado de nombre y marca sin distinguir mayúsculas (409), indicando la causa")
	void rechazarDuplicado() throws Exception {
		String nombre = nombreNuevo("Llave");
		crear(jefe, peticion(nombre).build()).andExpect(status().isCreated());

		crear(jefe, peticion(nombre.toUpperCase()).build())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
				.andExpect(jsonPath("$.detail").value("Ya existe el producto «" + nombre.toUpperCase()
						+ "» para la marca Stanley."));

		// El mismo nombre en otra marca sí se admite
		crear(jefe, peticion(nombre).marcaId(TRUPER).build()).andExpect(status().isCreated());
	}

	// ------------------------------------------------------------------ RF-PRO-003: buscar

	@Test
	@DisplayName("[CA-RF-PRO-003-3] Un vendedor no ve costos (margen ni último costo); un gerente sí")
	void vendedorNoVeCostos() throws Exception {
		mvc.perform(get(PRODUCTOS + "/" + MARTILLO).header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Martillo de uña 16 oz"))
				.andExpect(jsonPath("$.porcentajeIncremento").doesNotExist())
				.andExpect(jsonPath("$.ultimoCostoBase").doesNotExist());
		mvc.perform(get(PRODUCTOS).param("q", "martillo").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(jsonPath("$.contenido[*].ultimoCostoBase").isEmpty())
				.andExpect(jsonPath("$.contenido[*].porcentajeIncremento").isEmpty());

		mvc.perform(get(PRODUCTOS + "/" + MARTILLO).header(HttpHeaders.AUTHORIZATION, usuarios.bearer("gerente")))
				.andExpect(jsonPath("$.porcentajeIncremento").value(30.0))
				.andExpect(jsonPath("$.ultimoCostoBase").value(18000.0));
	}

	@Test
	@DisplayName("Búsqueda por nombre sin distinguir mayúsculas, por categoría y por estado, paginada y ordenada")
	void buscarConFiltros() throws Exception {
		String prefijo = "Brocha" + UUID.randomUUID().toString().substring(0, 8);
		crear(jefe, peticion(prefijo + " 1 pulgada").categoriaId(PINTURAS).build()).andExpect(status().isCreated());
		crear(jefe, peticion(prefijo + " 2 pulgadas").categoriaId(PINTURAS).build()).andExpect(status().isCreated());
		long tercera = idDe(crear(jefe, peticion(prefijo + " 3 pulgadas").categoriaId(PINTURAS).build()));
		mvc.perform(delete(PRODUCTOS + "/" + tercera).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isNoContent());

		String vendedor = usuarios.bearer("vendedor");
		mvc.perform(get(PRODUCTOS).param("q", prefijo.toUpperCase()).param("sort", "nombre,desc")
						.param("size", "2").header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(3))
				.andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.tamano").value(2))
				.andExpect(jsonPath("$.contenido[0].nombre").value(prefijo + " 3 pulgadas"))
				.andExpect(jsonPath("$.contenido[1].nombre").value(prefijo + " 2 pulgadas"));

		mvc.perform(get(PRODUCTOS).param("q", prefijo).param("estado", "ACTIVO").param("categoriaId", "" + PINTURAS)
						.header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(jsonPath("$.totalElementos").value(2))
				.andExpect(jsonPath("$.contenido[*].estado", everyItem(is("ACTIVO"))));

		mvc.perform(get(PRODUCTOS).param("q", prefijo).param("categoriaId", "" + HERRAMIENTAS)
						.header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(jsonPath("$.totalElementos").value(0));

		// Los comodines de LIKE se buscan literalmente
		mvc.perform(get(PRODUCTOS).param("q", "%").header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(jsonPath("$.totalElementos").value(0));
	}

	@Test
	@DisplayName("Orden por un campo desconocido o estado inválido responden 400")
	void ordenInvalido() throws Exception {
		mvc.perform(get(PRODUCTOS).param("sort", "costo,asc").header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[0].campo").value("sort"));
		mvc.perform(get(PRODUCTOS).param("estado", "BORRADO").header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------ editar e inactivar

	@Test
	@DisplayName("PUT reemplaza los datos; un nombre que ya usa otro producto de la marca responde 409")
	void editarProducto() throws Exception {
		String nombre = nombreNuevo("Alicate");
		long id = idDe(crear(jefe, peticion(nombre).build()));
		String otro = nombreNuevo("Pinza");
		crear(jefe, peticion(otro).build()).andExpect(status().isCreated());

		actualizar(id, peticion(nombre + " editado").categoriaId(PINTURAS).unidadBase("JGO")
						.porcentajeIncremento(new BigDecimal("45.5")).descripcion("").build())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value(nombre + " editado"))
				.andExpect(jsonPath("$.categoria.id").value(PINTURAS))
				.andExpect(jsonPath("$.unidadBase.codigo").value("JGO"))
				.andExpect(jsonPath("$.porcentajeIncremento").value(45.5))
				.andExpect(jsonPath("$.descripcion").value(nullValue()));

		actualizar(id, peticion(otro.toLowerCase()).build())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"));

		actualizar(999_999, peticion(nombreNuevo("X")).build())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	@Test
	@DisplayName("[RN-05] La unidad base de un producto con presentaciones o lotes no se puede cambiar (409)")
	void unidadBaseNoEditableConExistencias() throws Exception {
		String antes = jdbc.queryForObject("SELECT unidad_base FROM producto WHERE id = ?", String.class, MARTILLO);
		actualizar(MARTILLO, peticion("Martillo de uña 16 oz").unidadBase("KG")
						.control(ControlProducto.GARANTIA).plazoGarantiaMeses(6).build())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
		actualizar(MARTILLO, peticion("Martillo de uña 16 oz").control(ControlProducto.NINGUNO).build())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
		assertThat(jdbc.queryForObject("SELECT unidad_base FROM producto WHERE id = ?", String.class, MARTILLO))
				.isEqualTo(antes);
	}

	@Test
	@DisplayName("[RF-CFG-001] DELETE es borrado lógico: 204, el producto queda INACTIVO y consultable; repetirlo no audita de nuevo")
	void borradoLogico() throws Exception {
		long id = idDe(crear(jefe, peticion(nombreNuevo("Serrucho")).build()));

		long auditoriaAntes = ultimoIdAuditoria();
		mvc.perform(delete(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isNoContent());
		mvc.perform(delete(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isNoContent());

		mvc.perform(get(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"));
		assertThat(jdbc.queryForObject("SELECT activo FROM producto WHERE id = ?", Boolean.class, id)).isFalse();
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM auditoria WHERE id > ? AND entidad = 'producto' AND entidad_id = ?",
				Integer.class, auditoriaAntes, id)).isEqualTo(1);

		mvc.perform(delete(PRODUCTOS + "/999999").header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("[CA-RF-PRO-001-1] CRUD completo por HTTP con token real: crear, consultar, buscar, editar, inactivar y reactivar")
	void cicloCompleto() throws Exception {
		String nombre = nombreNuevo("Nivel");
		long id = idDe(crear(jefe, peticion(nombre).build()));

		mvc.perform(get(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value(nombre))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
		mvc.perform(get(PRODUCTOS).param("q", nombre).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.contenido[0].id").value(id));

		actualizar(id, peticion(nombre + " torpedo").control(ControlProducto.GARANTIA).plazoGarantiaMeses(3).build())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value(nombre + " torpedo"))
				.andExpect(jsonPath("$.plazoGarantiaMeses").value(3));

		mvc.perform(delete(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isNoContent());
		mvc.perform(get(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(jsonPath("$.estado").value("INACTIVO"));

		actualizar(id, peticion(nombre + " torpedo").estado(EstadoProducto.ACTIVO).build())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.plazoGarantiaMeses").value(nullValue()));
	}

	@Test
	@DisplayName("[CA-RF-SEG-002-1][CA-RNF-SEG-003-1] Editar e inactivar dejan un MODIFICAR cada uno con usuario_id del jefe, IP y valores antes y después")
	void editarEInactivarQuedanAuditados() throws Exception {
		String nombre = nombreNuevo("Escuadra");
		long id = idDe(crear(jefe, peticion(nombre).build()));
		long antes = ultimoIdAuditoria();

		actualizar(id, peticion(nombre + " 30 cm").build()).andExpect(status().isOk());
		mvc.perform(delete(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe))
				.andExpect(status().isNoContent());

		List<Map<String, Object>> filas = jdbc.queryForList("""
				SELECT accion, usuario_id, host(ip) AS ip, fecha,
				       valores_antes->>'nombre' AS nombre_antes, valores_despues->>'nombre' AS nombre_despues,
				       valores_antes->>'activo' AS activo_antes, valores_despues->>'activo' AS activo_despues
				  FROM auditoria
				 WHERE id > ? AND entidad = 'producto' AND entidad_id = ?
				 ORDER BY id
				""", antes, id);
		assertThat(filas).hasSize(2);
		assertThat(filas).allSatisfy(f -> {
			assertThat(f).containsEntry("accion", "MODIFICAR").containsEntry("ip", IP_LOCAL);
			assertThat(((Number) f.get("usuario_id")).longValue()).isEqualTo(idUsuario("jefe"));
			assertThat(f.get("fecha")).isNotNull();
		});
		assertThat(filas.get(0)).containsEntry("nombre_antes", nombre).containsEntry("nombre_despues", nombre + " 30 cm");
		assertThat(filas.get(1)).containsEntry("activo_antes", "true").containsEntry("activo_despues", "false");
	}

	// ------------------------------------------------------------------ seguridad e idioma

	@Test
	@DisplayName("[CA-RF-SEG-001-2][CA-RNF-SEG-002-1] Vendedor, auditor y comprador no crean, editan ni inactivan productos (403); sin token, 401")
	void permisos() throws Exception {
		for (String usuario : new String[] {"vendedor", "auditor", "comprador"}) {
			String token = usuarios.bearer(usuario);
			crear(token, peticion(nombreNuevo("X")).build()).andExpect(status().isForbidden())
					.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
			mvc.perform(put(PRODUCTOS + "/" + MARTILLO).header(HttpHeaders.AUTHORIZATION, token)
							.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(peticion(nombreNuevo("X")).build())))
					.andExpect(status().isForbidden());
			mvc.perform(delete(PRODUCTOS + "/" + MARTILLO).header(HttpHeaders.AUTHORIZATION, token))
					.andExpect(status().isForbidden());
		}
		mvc.perform(get(PRODUCTOS)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
		assertThat(jdbc.queryForObject("SELECT activo FROM producto WHERE id = ?", Boolean.class, MARTILLO)).isTrue();
	}

	@Test
	@DisplayName("[CA-RNF-INT-002-1] Los mensajes de validación y conflicto llegan en inglés con Accept-Language: en")
	void mensajesEnIngles() throws Exception {
		mvc.perform(post(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, jefe).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json.writeValueAsString(peticion(nombreNuevo("X")).control(ControlProducto.GARANTIA).build())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid data"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("is required when control is GARANTIA"));
		mvc.perform(post(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, jefe).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json.writeValueAsString(peticion("Martillo de uña 16 oz")
								.control(ControlProducto.GARANTIA).plazoGarantiaMeses(6).build())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("The product \"Martillo de uña 16 oz\" already exists for brand Stanley."));
	}

	// ------------------------------------------------------------------ utilidades

	private static ProductoPeticion.ProductoPeticionBuilder peticion(String nombre) {
		return ProductoPeticion.builder()
				.nombre(nombre)
				.categoriaId(HERRAMIENTAS)
				.marcaId(STANLEY)
				.unidadBase("UND")
				.tarifaIvaId(IVA_19)
				.porcentajeIncremento(new BigDecimal("30"))
				.control(ControlProducto.NINGUNO);
	}

	private ResultActions crear(String token, ProductoPeticion datos) throws Exception {
		return mvc.perform(post(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)));
	}

	private ResultActions actualizar(long id, ProductoPeticion datos) throws Exception {
		return mvc.perform(put(PRODUCTOS + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe)
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)));
	}

	private static long idDe(ResultActions resultado) throws Exception {
		String cuerpo = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
	}

	private static String nombreNuevo(String base) {
		return base + " " + UUID.randomUUID().toString().substring(0, 8);
	}

}
