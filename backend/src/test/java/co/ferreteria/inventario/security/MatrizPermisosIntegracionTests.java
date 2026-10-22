/**
 * Nombre: MatrizPermisosIntegracionTests.java
 * Descripción: Prueba parametrizada de la matriz de permisos 2.2: recorre cada endpoint de la API contra
 *              cada uno de los 6 roles (y sin token) y comprueba 2xx, 403 o 401; además verifica que la
 *              matriz cubra todos los endpoints publicados.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;

/**
 * Matriz de permisos de la especificación, sección 2.2, aplicada en el servidor (RF-SEG-001, RN-27,
 * RNF-SEG-001, RNF-SEG-002).
 *
 * <p>Cada {@link Endpoint} declara qué roles lo pueden usar según la fila de la matriz que le
 * corresponde. Se ejecuta cada endpoint con el token de cada rol: los roles permitidos reciben una
 * respuesta 2xx y los demás 403 {@code ACCESO_DENEGADO}, sin dejar registro de auditoría. Las
 * solicitudes de escritura llevan un cuerpo válido, para que el 403 no se confunda con un 400 de
 * validación, y actúan sobre un producto o un usuario creado solo para ese caso, así no alteran los
 * usuarios del escenario.</p>
 */
class MatrizPermisosIntegracionTests extends PruebaIntegracionApi {

	/** Los seis roles humanos de la matriz, por su usuario del escenario de prueba. */
	private static final Set<String> TODOS = Set.of("admin", "gerente", "jefe", "comprador", "vendedor", "auditor");

	/** Matriz 2.2, fila "Crear y editar productos y presentaciones". */
	private static final Set<String> CATALOGO_EDITAR = Set.of("admin", "gerente", "jefe");

	/** Matriz 2.2, fila "Consultar auditoría". */
	private static final Set<String> AUDITORIA = Set.of("admin", "gerente", "auditor");

	/** Matriz 2.2, fila "Usuarios, roles y configuración". */
	private static final Set<String> USUARIOS = Set.of("admin");

	/** Matriz 2.2, fila "Usuarios, roles y configuración" (catálogos de RF-CFG-001: CONFIG_GESTIONAR). */
	private static final Set<String> CONFIGURACION = Set.of("admin");

	/** Única ruta de la API que no exige token (RNF-SEG-001). */
	private static final String LOGIN = "POST /api/v1/auth/login";

	/** Tokens por usuario; los JWT valen 30 minutos y no dependen de la contraseña guardada. */
	private static final Map<String, String> TOKENS = new HashMap<>();

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping mapeos;

	/** Sobre qué registro actúa un endpoint con {@code {id}}. */
	private enum Objetivo {
		/** El endpoint no tiene {@code {id}}. */
		NINGUNO,
		/** Un producto nuevo creado para el caso. */
		PRODUCTO,
		/** Un usuario nuevo con rol VENDEDOR creado para el caso. */
		USUARIO,
		/** Una marca nueva creada para el caso. */
		MARCA,
		/** Una categoría nueva creada para el caso. */
		CATEGORIA,
		/** El último registro de auditoría. */
		AUDITORIA
	}

	/** Endpoints de la API con los roles que los pueden usar según la matriz 2.2. */
	private enum Endpoint {
		AUTH_ME(HttpMethod.GET, "/api/v1/auth/me", TODOS, Objetivo.NINGUNO),
		PRODUCTOS_LISTAR(HttpMethod.GET, "/api/v1/productos", TODOS, Objetivo.NINGUNO),
		PRODUCTOS_OBTENER(HttpMethod.GET, "/api/v1/productos/{id}", TODOS, Objetivo.PRODUCTO),
		PRODUCTOS_CREAR(HttpMethod.POST, "/api/v1/productos", CATALOGO_EDITAR, Objetivo.NINGUNO),
		PRODUCTOS_EDITAR(HttpMethod.PUT, "/api/v1/productos/{id}", CATALOGO_EDITAR, Objetivo.PRODUCTO),
		PRODUCTOS_INACTIVAR(HttpMethod.DELETE, "/api/v1/productos/{id}", CATALOGO_EDITAR, Objetivo.PRODUCTO),
		MARCAS_LISTAR(HttpMethod.GET, "/api/v1/marcas", TODOS, Objetivo.NINGUNO),
		MARCAS_ACTIVAS(HttpMethod.GET, "/api/v1/marcas/activas", TODOS, Objetivo.NINGUNO),
		MARCAS_CREAR(HttpMethod.POST, "/api/v1/marcas", CONFIGURACION, Objetivo.NINGUNO),
		MARCAS_EDITAR(HttpMethod.PUT, "/api/v1/marcas/{id}", CONFIGURACION, Objetivo.MARCA),
		MARCAS_ESTADO(HttpMethod.PATCH, "/api/v1/marcas/{id}/estado", CONFIGURACION, Objetivo.MARCA),
		CATEGORIAS_LISTAR(HttpMethod.GET, "/api/v1/categorias", TODOS, Objetivo.NINGUNO),
		CATEGORIAS_ACTIVAS(HttpMethod.GET, "/api/v1/categorias/activas", TODOS, Objetivo.NINGUNO),
		CATEGORIAS_CREAR(HttpMethod.POST, "/api/v1/categorias", CONFIGURACION, Objetivo.NINGUNO),
		CATEGORIAS_EDITAR(HttpMethod.PUT, "/api/v1/categorias/{id}", CONFIGURACION, Objetivo.CATEGORIA),
		CATEGORIAS_ESTADO(HttpMethod.PATCH, "/api/v1/categorias/{id}/estado", CONFIGURACION, Objetivo.CATEGORIA),
		UNIDADES_MEDIDA(HttpMethod.GET, "/api/v1/unidades-medida", TODOS, Objetivo.NINGUNO),
		TARIFAS_IVA(HttpMethod.GET, "/api/v1/tarifas-iva", TODOS, Objetivo.NINGUNO),
		AUDITORIA_LISTAR(HttpMethod.GET, "/api/v1/auditoria", AUDITORIA, Objetivo.NINGUNO),
		AUDITORIA_OBTENER(HttpMethod.GET, "/api/v1/auditoria/{id}", AUDITORIA, Objetivo.AUDITORIA),
		USUARIOS_LISTAR(HttpMethod.GET, "/api/v1/usuarios", USUARIOS, Objetivo.NINGUNO),
		USUARIOS_OBTENER(HttpMethod.GET, "/api/v1/usuarios/{id}", USUARIOS, Objetivo.USUARIO),
		USUARIOS_CREAR(HttpMethod.POST, "/api/v1/usuarios", USUARIOS, Objetivo.NINGUNO),
		USUARIOS_EDITAR(HttpMethod.PUT, "/api/v1/usuarios/{id}", USUARIOS, Objetivo.USUARIO),
		USUARIOS_ROLES(HttpMethod.PUT, "/api/v1/usuarios/{id}/roles", USUARIOS, Objetivo.USUARIO),
		USUARIOS_ESTADO(HttpMethod.PATCH, "/api/v1/usuarios/{id}/estado", USUARIOS, Objetivo.USUARIO),
		USUARIOS_CONTRASENA(HttpMethod.PUT, "/api/v1/usuarios/{id}/contrasena", USUARIOS, Objetivo.USUARIO),
		ROLES_LISTAR(HttpMethod.GET, "/api/v1/roles", USUARIOS, Objetivo.NINGUNO);

		private final HttpMethod metodo;
		private final String ruta;
		private final Set<String> permitidos;
		private final Objetivo objetivo;

		Endpoint(HttpMethod metodo, String ruta, Set<String> permitidos, Objetivo objetivo) {
			this.metodo = metodo;
			this.ruta = ruta;
			this.permitidos = permitidos;
			this.objetivo = objetivo;
		}

		/** @return método y ruta, por ejemplo {@code GET /api/v1/productos} */
		String firma() {
			return metodo.name() + " " + ruta;
		}

		/**
		 * Cuerpo JSON válido de la solicitud, o {@code null} si no lleva.
		 *
		 * @param unico sufijo para que nombres, usuarios y correos no se repitan
		 * @return JSON o {@code null}
		 */
		String cuerpo(String unico) {
			return switch (this) {
				case PRODUCTOS_CREAR, PRODUCTOS_EDITAR -> """
						{"nombre":"Matriz %s","categoriaId":1,"marcaId":1,"unidadBase":"UND","tarifaIvaId":3,
						 "porcentajeIncremento":30,"control":"NINGUNO"}""".formatted(unico);
				case USUARIOS_CREAR -> """
						{"usuario":"m%s","nombreCompleto":"Matriz","correo":"m%s@ferreteria.test",
						 "contrasena":"Clave12345","roles":["VENDEDOR"]}""".formatted(unico, unico);
				case USUARIOS_EDITAR -> """
						{"nombreCompleto":"Matriz editado","correo":"e%s@ferreteria.test"}""".formatted(unico);
				case USUARIOS_ROLES -> "{\"roles\":[\"COMPRADOR\"]}";
				case USUARIOS_ESTADO -> "{\"activo\":false}";
				case MARCAS_CREAR, CATEGORIAS_CREAR -> "{\"nombre\":\"Matriz %s\"}".formatted(unico);
				case MARCAS_EDITAR, CATEGORIAS_EDITAR -> "{\"nombre\":\"Matriz editada %s\"}".formatted(unico);
				case MARCAS_ESTADO, CATEGORIAS_ESTADO -> "{\"estado\":\"INACTIVO\"}";
				case USUARIOS_CONTRASENA -> "{\"contrasena\":\"Nueva12345\"}";
				default -> null;
			};
		}
	}

	/**
	 * Combinaciones endpoint × rol con el resultado esperado.
	 *
	 * @return argumentos (endpoint, rol, esperado)
	 */
	static Stream<Arguments> endpointPorRol() {
		return Arrays.stream(Endpoint.values()).flatMap(endpoint -> Stream.of(
						"admin", "gerente", "jefe", "comprador", "vendedor", "auditor")
				.map(rol -> Arguments.of(endpoint, rol, endpoint.permitidos.contains(rol) ? "2xx" : "403")));
	}

	@AfterAll
	static void olvidarTokens() {
		TOKENS.clear();
	}

	@ParameterizedTest(name = "{0} con {1}: {2}")
	@MethodSource("endpointPorRol")
	@DisplayName("[CA-RF-SEG-001-2][CA-RNF-SEG-002-1] Matriz 2.2 por API: cada endpoint contra cada rol responde 2xx si el rol tiene el permiso y 403 si no")
	void matrizPorEndpointYRol(Endpoint endpoint, String rol, String esperado) throws Exception {
		String unico = UUID.randomUUID().toString().substring(0, 8);
		long id = prepararObjetivo(endpoint.objetivo, unico);
		long auditoriaAntes = ultimoIdAuditoria();

		MvcResult resultado = mvc.perform(solicitud(endpoint, id, unico).header(HttpHeaders.AUTHORIZATION, bearer(rol)))
				.andReturn();
		int codigo = resultado.getResponse().getStatus();

		if ("2xx".equals(esperado)) {
			assertThat(codigo).as("%s con %s: %s", endpoint.firma(), rol, resultado.getResponse().getContentAsString())
					.isBetween(200, 299);
		} else {
			assertThat(codigo).as("%s con %s", endpoint.firma(), rol).isEqualTo(403);
			assertThat(resultado.getResponse().getContentAsString()).contains("\"codigo\":\"ACCESO_DENEGADO\"");
			assertThat(ultimoIdAuditoria()).as("una operación denegada no deja registro de auditoría")
					.isEqualTo(auditoriaAntes);
		}
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Endpoint.class)
	@DisplayName("[CA-RNF-SEG-001-1] Sin token, cada endpoint protegido de la API responde 401 NO_AUTENTICADO")
	void sinTokenResponde401(Endpoint endpoint) throws Exception {
		mvc.perform(solicitud(endpoint, 1, UUID.randomUUID().toString().substring(0, 8)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
	}

	@Test
	@DisplayName("[CA-RNF-SEG-002-1] La matriz cubre todos los endpoints publicados en /api (ninguno queda sin probar)")
	void laMatrizCubreTodosLosEndpoints() {
		Set<String> publicados = mapeos.getHandlerMethods().keySet().stream()
				.flatMap(info -> info.getPatternValues().stream()
						.filter(ruta -> ruta.startsWith("/api/"))
						.flatMap(ruta -> info.getMethodsCondition().getMethods().stream()
								.map(metodo -> metodo.name() + " " + ruta)))
				.collect(Collectors.toCollection(TreeSet::new));
		Set<String> probados = Arrays.stream(Endpoint.values()).map(Endpoint::firma)
				.collect(Collectors.toCollection(TreeSet::new));
		probados.add(LOGIN);

		assertThat(publicados).containsExactlyInAnyOrderElementsOf(probados);
	}

	// ------------------------------------------------------------------ apoyo

	private MockHttpServletRequestBuilder solicitud(Endpoint endpoint, long id, String unico) {
		MockHttpServletRequestBuilder solicitud = endpoint.ruta.contains("{id}")
				? request(endpoint.metodo, endpoint.ruta, id)
				: request(endpoint.metodo, endpoint.ruta);
		String cuerpo = endpoint.cuerpo(unico);
		return cuerpo == null ? solicitud : solicitud.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
	}

	private long prepararObjetivo(Objetivo objetivo, String unico) {
		Long id = switch (objetivo) {
			case NINGUNO -> 0L;
			case PRODUCTO -> jdbc.queryForObject("""
					INSERT INTO producto (nombre, marca_id, categoria_id, unidad_base, tarifa_iva_id, porcentaje_incremento)
					VALUES (?, 1, 1, 'UND', 3, 30) RETURNING id
					""", Long.class, "Objetivo matriz " + unico);
			case USUARIO -> {
				Long nuevo = jdbc.queryForObject("""
						INSERT INTO usuario (usuario, nombre_completo, correo, password_hash)
						VALUES (?, 'Objetivo matriz', ?, '$2a$12$hash.de.prueba.no.valido.para.login') RETURNING id
						""", Long.class, "objetivo" + unico, "objetivo" + unico + "@ferreteria.test");
				jdbc.update("INSERT INTO usuario_rol (usuario_id, rol_id) SELECT ?, id FROM rol WHERE codigo = 'VENDEDOR'",
						nuevo);
				yield nuevo;
			}
			case MARCA -> jdbc.queryForObject("INSERT INTO marca (nombre) VALUES (?) RETURNING id", Long.class,
					"Objetivo matriz " + unico);
			case CATEGORIA -> jdbc.queryForObject("INSERT INTO categoria (nombre) VALUES (?) RETURNING id", Long.class,
					"Objetivo matriz " + unico);
			case AUDITORIA -> ultimoIdAuditoria();
		};
		return id == null ? 0 : id;
	}

	private String bearer(String rol) throws Exception {
		String token = TOKENS.get(rol);
		if (token == null) {
			token = usuarios.bearer(rol);
			TOKENS.put(rol, token);
		}
		return token;
	}

}
