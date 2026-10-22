/**
 * Nombre: TerceroControllerWebTests.java
 * Descripción: Pruebas de capa web (MockMvc) de /api/v1/terceros: validación por campo en español e inglés,
 *              401 sin token, 403 sin permisos de terceros, 409, 422 del Consumidor final y 405 en DELETE.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import java.time.Instant;
import java.util.Arrays;
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

import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.OperacionRechazadaException;
import co.ferreteria.inventario.common.i18n.Mensajes;
import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.config.LocaleConfig;
import co.ferreteria.inventario.security.ConfiguracionJwt;
import co.ferreteria.inventario.security.ManejadorErroresSeguridad;
import co.ferreteria.inventario.security.SecurityConfig;
import co.ferreteria.inventario.tercero.dto.TerceroPeticion;
import co.ferreteria.inventario.tercero.dto.TerceroRespuesta;

/**
 * Capa web de terceros con la seguridad real y el servicio simulado (RF-TER-001, RNF-SEG-001,
 * RNF-INT-002).
 */
@WebMvcTest(controllers = TerceroController.class)
@ActiveProfiles("test")
@Import({ SecurityConfig.class, ConfiguracionJwt.class, ManejadorErroresSeguridad.class, LocaleConfig.class,
		Mensajes.class })
class TerceroControllerWebTests {

	private static final String RUTA = "/api/v1/terceros";

	private static final String JURIDICA = """
			{"tipoPersona":"JURIDICA","tipoIdentificacion":"NIT","numeroIdentificacion":"890903938",
			 "digitoVerificacion":%d,"razonSocial":"Bancolombia S.A.","direccion":"Medellín",
			 "telefono":"6045101000","esProveedor":true}""";

	private static final String VALIDO = JURIDICA.formatted(8);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private TerceroService servicio;

	// ------------------------------------------------------------------ autenticación y permisos

	@Test
	@DisplayName("[CA-RNF-SEG-001-1] Sin token, consultar, crear, editar y cambiar estado responden 401")
	void sinToken() throws Exception {
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
		mvc.perform(get(RUTA + "/1")).andExpect(status().isUnauthorized());
		mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnauthorized());
		mvc.perform(put(RUTA + "/1").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnauthorized());
		mvc.perform(patch(RUTA + "/1/estado").contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"INACTIVO\"}")).andExpect(status().isUnauthorized());
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Sin permisos de terceros (jefe de inventario, auditor), toda operación responde 403")
	void sinPermisos() throws Exception {
		JwtRequestPostProcessor jefe = usuario("CATALOGO_CONSULTAR", "CATALOGO_EDITAR", "RECEPCION_REGISTRAR");

		mvc.perform(get(RUTA).with(jefe)).andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		mvc.perform(get(RUTA + "/1").with(usuario("AUDITORIA_VER"))).andExpect(status().isForbidden());
		mvc.perform(post(RUTA).with(jefe).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isForbidden());
		mvc.perform(put(RUTA + "/1").with(jefe).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isForbidden());
		mvc.perform(patch(RUTA + "/1/estado").with(jefe).contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"INACTIVO\"}")).andExpect(status().isForbidden());
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Si el servicio niega el permiso de un rol, la respuesta es 403 ACCESO_DENEGADO")
	void permisoDeRolDenegado() throws Exception {
		when(servicio.crear(any(TerceroPeticion.class)))
				.thenThrow(new org.springframework.security.access.AccessDeniedException("TERCERO_PROVEEDOR_EDITAR"));

		mvc.perform(post(RUTA).with(usuario("TERCERO_CLIENTE_EDITAR")).contentType(MediaType.APPLICATION_JSON)
						.content(VALIDO))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	// ------------------------------------------------------------------ validación

	@Test
	@DisplayName("[CA-RF-TER-001-3] NIT con dígito de verificación incorrecto: 400 en digitoVerificacion con el dígito correcto")
	void digitoIncorrecto() throws Exception {
		mvc.perform(post(RUTA).with(admin()).contentType(MediaType.APPLICATION_JSON).content(JURIDICA.formatted(3)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[0].campo").value("digitoVerificacion"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("no corresponde al NIT; el dígito correcto es 8"));
		mvc.perform(post(RUTA).with(admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON).content(JURIDICA.formatted(3)))
				.andExpect(jsonPath("$.errores[0].mensaje").value("does not match the NIT; the correct digit is 8"));
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("[CA-RNF-INT-002-1] Persona natural vacía: 400 con un error por campo, en español")
	void validacionEnEspanol() throws Exception {
		mvc.perform(post(RUTA).with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
						{"tipoPersona":"NATURAL","tipoIdentificacion":"CC","numeroIdentificacion":"12a",
						 "direccion":" ","telefono":"123","correo":"x@"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[?(@.campo == 'nombre')].mensaje").value(hasItem("es obligatorio")))
				.andExpect(jsonPath("$.errores[?(@.campo == 'apellido')].mensaje").value(hasItem("es obligatorio")))
				.andExpect(jsonPath("$.errores[?(@.campo == 'numeroIdentificacion')].mensaje")
						.value(hasItem("debe tener entre 5 y 15 dígitos, sin puntos ni guiones")))
				.andExpect(jsonPath("$.errores[?(@.campo == 'telefono')].mensaje")
						.value(hasItem("debe tener entre 7 y 10 dígitos")))
				.andExpect(jsonPath("$.errores[?(@.campo == 'direccion')].mensaje").value(hasItem("es obligatorio")))
				.andExpect(jsonPath("$.errores[?(@.campo == 'correo')].mensaje").value(hasItem("no es un correo válido")))
				.andExpect(jsonPath("$.errores[?(@.campo == 'esCliente')].mensaje")
						.value(hasItem("marque al menos un rol: cliente o proveedor")));
		verifyNoInteractions(servicio);
	}

	@Test
	@DisplayName("[CA-RNF-INT-002-1] Persona jurídica con cédula y sin razón social: 400 con la causa en inglés")
	void validacionEnIngles() throws Exception {
		mvc.perform(post(RUTA).with(admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON).content("""
						{"tipoPersona":"JURIDICA","tipoIdentificacion":"CC","numeroIdentificacion":"12345678",
						 "direccion":"Neiva","telefono":"3001234567","esCliente":true}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid data"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'razonSocial')].mensaje").value(hasItem("is required")))
				.andExpect(jsonPath("$.errores[?(@.campo == 'tipoIdentificacion')].mensaje")
						.value(hasItem("a legal entity can only be identified with a NIT")));
		verifyNoInteractions(servicio);
	}

	// ------------------------------------------------------------------ respuestas

	@Test
	@DisplayName("[CA-RF-TER-001-1] Crear responde 201 con Location y el tercero ACTIVO")
	void crear() throws Exception {
		when(servicio.crear(any(TerceroPeticion.class))).thenReturn(respuesta(30L, false));

		mvc.perform(post(RUTA).with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, endsWith(RUTA + "/30")))
				.andExpect(jsonPath("$.digitoVerificacion").value(8))
				.andExpect(jsonPath("$.nombreMostrado").value("Bancolombia S.A."))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@Test
	@DisplayName("[CA-RF-TER-001-3] Identificación duplicada: 409 CONFLICTO_UNICIDAD con la causa traducida")
	void duplicado() throws Exception {
		when(servicio.crear(any(TerceroPeticion.class)))
				.thenThrow(new ConflictoException("tercero.identificacion_duplicada", "NIT", "890903938"));

		mvc.perform(post(RUTA).with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
				.andExpect(jsonPath("$.detail").value("Ya existe un tercero con identificación NIT 890903938."));
		mvc.perform(post(RUTA).with(admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(jsonPath("$.detail").value("A third party with identification NIT 890903938 already exists."));
	}

	@Test
	@DisplayName("[CA-RF-TER-001-4] Editar o desactivar el Consumidor final: 422 REGLA_NEGOCIO con mensaje claro; no hay DELETE (405)")
	void consumidorFinal() throws Exception {
		OperacionRechazadaException rechazo = new OperacionRechazadaException(CodigoError.REGLA_NEGOCIO,
				"tercero.consumidor_final_no_editable");
		when(servicio.actualizar(eq(1L), any(TerceroPeticion.class))).thenThrow(rechazo);
		when(servicio.cambiarEstado(1L, EstadoTercero.INACTIVO)).thenThrow(rechazo);

		mvc.perform(put(RUTA + "/1").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"))
				.andExpect(jsonPath("$.detail").value(
						"El Consumidor final viene predefinido y no se puede editar ni desactivar (RN-26)."));
		mvc.perform(patch(RUTA + "/1/estado").with(admin()).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(
						"The final consumer is predefined and cannot be edited or deactivated (RN-26)."));
		mvc.perform(delete(RUTA + "/1").with(admin()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
	}

	@Test
	@DisplayName("La búsqueda pasa q, rol, tipo de persona y estado al servicio; un solo permiso de terceros basta para consultar")
	void consultas() throws Exception {
		when(servicio.listar(eq("andina"), eq(RolTercero.PROVEEDOR), eq(TipoPersona.JURIDICA),
				eq(EstadoTercero.ACTIVO), any())).thenReturn(Pagina.de(List.of(respuesta(4L, false)), 0, 20, 1));
		when(servicio.obtener(anyLong())).thenReturn(respuesta(4L, false));

		mvc.perform(get(RUTA).param("q", "andina").param("rol", "PROVEEDOR").param("tipoPersona", "JURIDICA")
						.param("estado", "ACTIVO").with(usuario("TERCERO_PROVEEDOR_EDITAR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.contenido[0].esProveedor").value(true));
		mvc.perform(get(RUTA + "/4").with(usuario("TERCERO_CLIENTE_EDITAR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(4));
	}

	// ------------------------------------------------------------------ utilidades

	private static TerceroRespuesta respuesta(long id, boolean consumidorFinal) {
		return new TerceroRespuesta(id, TipoPersona.JURIDICA, TipoIdentificacion.NIT, "890903938", (short) 8, null,
				null, "Bancolombia S.A.", "Bancolombia S.A.", "Medellín", "6045101000", null, false, true,
				consumidorFinal, EstadoTercero.ACTIVO, Instant.parse("2026-10-22T15:00:00Z"));
	}

	private static JwtRequestPostProcessor admin() {
		return usuario("TERCERO_CLIENTE_EDITAR", "TERCERO_PROVEEDOR_EDITAR");
	}

	private static JwtRequestPostProcessor usuario(String... permisos) {
		return jwt().jwt(j -> j.subject("2")).authorities(
				Arrays.stream(permisos).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
	}

}
