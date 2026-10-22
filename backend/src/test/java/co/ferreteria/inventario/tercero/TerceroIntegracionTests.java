/**
 * Nombre: TerceroIntegracionTests.java
 * Descripción: Pruebas de integración (PostgreSQL en Testcontainers) de clientes y proveedores: alta de
 *              personas naturales y jurídicas, doble rol, duplicados, dígito de verificación, permisos por
 *              rol, Consumidor final protegido, filtros y auditoría.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;

/**
 * Terceros contra la base real (RF-TER-001, RN-26, RN-27, RN-28). Las identificaciones son aleatorias
 * porque los terceros no se borran entre pruebas.
 */
class TerceroIntegracionTests extends PruebaIntegracionApi {

	private static final String RUTA = "/api/v1/terceros";

	/** Id del Consumidor final sembrado por V1. */
	private static final long CONSUMIDOR_FINAL = 1L;

	@Test
	@DisplayName("[CA-RF-TER-001-1] Se registran una persona natural con cédula, nombre y apellido y una jurídica con NIT y razón social")
	void registraNaturalYJuridica() throws Exception {
		String vendedor = usuarios.bearer("vendedor");
		String cc = numeroUnico(10);
		long natural = crear(vendedor, natural(cc, "  Ana ", " Rojas "));
		mvc.perform(get(RUTA + "/{id}", natural).header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoIdentificacion").value("CC"))
				.andExpect(jsonPath("$.numeroIdentificacion").value(cc))
				.andExpect(jsonPath("$.digitoVerificacion").doesNotExist())
				.andExpect(jsonPath("$.nombreMostrado").value("Ana Rojas"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));

		String comprador = usuarios.bearer("comprador");
		String nit = numeroUnico(9);
		int dv = DigitoVerificacionNit.calcular(nit).orElseThrow();
		long juridica = crear(comprador, juridica(nit, dv, false, true));
		Map<String, Object> fila = jdbc.queryForMap(
				"SELECT tipo_persona, digito_verificacion, razon_social, nombre FROM tercero WHERE id = ?", juridica);
		assertThat(((Number) fila.get("digito_verificacion")).intValue()).isEqualTo(dv);
		assertThat(fila).containsEntry("tipo_persona", "JURIDICA")
				.containsEntry("razon_social", "Proveedora " + nit + " S.A.S.").containsEntry("nombre", null);
	}

	@Test
	@DisplayName("[CA-RF-TER-001-2] Un mismo tercero se registra como cliente y proveedor y aparece con ambos filtros de rol")
	void clienteYProveedor() throws Exception {
		String gerente = usuarios.bearer("gerente");
		String nit = numeroUnico(9);
		long id = crear(gerente, juridica(nit, DigitoVerificacionNit.calcular(nit).orElseThrow(), true, true));

		for (String rol : List.of("CLIENTE", "PROVEEDOR")) {
			mvc.perform(get(RUTA).param("q", nit).param("rol", rol).header(HttpHeaders.AUTHORIZATION, gerente))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.contenido[*].id", hasItem((int) id)));
		}
		assertThat(jdbc.queryForObject("SELECT es_cliente AND es_proveedor FROM tercero WHERE id = ?", Boolean.class, id))
				.isTrue();
	}

	@Test
	@DisplayName("[CA-RF-TER-001-3] Se rechazan una identificación duplicada (409) y un NIT con dígito de verificación incorrecto (400)")
	void rechazaDuplicadoYDigitoIncorrecto() throws Exception {
		String admin = usuarios.bearer("admin");
		// Distribuidora Pavco Neiva (datos de prueba): NIT 900123456-8.
		mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON)
						.content(juridica("900123456", 8, false, true)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
				.andExpect(jsonPath("$.detail").value("Ya existe un tercero con identificación NIT 900123456."));

		String nit = numeroUnico(9);
		int incorrecto = (DigitoVerificacionNit.calcular(nit).orElseThrow() + 1) % 10;
		mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON)
						.content(juridica(nit, incorrecto, false, true)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].campo").value("digitoVerificacion"));
		assertThat(jdbc.queryForObject("SELECT count(*) FROM tercero WHERE numero_identificacion = ?", Integer.class, nit))
				.isZero();

		// La misma cifra con otro tipo de identificación es otra identificación.
		long id = crear(admin, natural("900123456", "Pedro", "Pavón"));
		assertThat(id).isPositive();
	}

	@Test
	@DisplayName("[CA-RF-TER-001-3] Editar con la identificación de otro tercero responde 409")
	void editarConIdentificacionDeOtro() throws Exception {
		String vendedor = usuarios.bearer("vendedor");
		long id = crear(vendedor, natural(numeroUnico(10), "Luis", "Mora"));

		// Juan Pérez Gómez (datos de prueba): CC 1075234567.
		mvc.perform(put(RUTA + "/{id}", id).header(HttpHeaders.AUTHORIZATION, vendedor)
						.contentType(MediaType.APPLICATION_JSON).content(natural("1075234567", "Luis", "Mora")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"));
	}

	@Test
	@DisplayName("[CA-RF-TER-001-4] El Consumidor final existe, se lista y no se puede editar, desactivar ni eliminar")
	void consumidorFinal() throws Exception {
		String admin = usuarios.bearer("admin");
		String vendedor = usuarios.bearer("vendedor");
		mvc.perform(get(RUTA).param("q", "222222222222").header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[0].id").value(CONSUMIDOR_FINAL))
				.andExpect(jsonPath("$.contenido[0].esConsumidorFinal").value(true));

		long auditoriaAntes = ultimoIdAuditoria();
		mvc.perform(put(RUTA + "/{id}", CONSUMIDOR_FINAL).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content(natural("222222222222", "Otro", "Nombre")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"))
				.andExpect(jsonPath("$.detail").value(
						"El Consumidor final viene predefinido y no se puede editar ni desactivar (RN-26)."));
		mvc.perform(patch(RUTA + "/{id}/estado", CONSUMIDOR_FINAL).header(HttpHeaders.AUTHORIZATION, vendedor)
						.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(delete(RUTA + "/{id}", CONSUMIDOR_FINAL).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isMethodNotAllowed());

		Map<String, Object> fila = jdbc.queryForMap("SELECT nombre, activo FROM tercero WHERE id = ?", CONSUMIDOR_FINAL);
		assertThat(fila).containsEntry("nombre", "Consumidor").containsEntry("activo", true);
		assertThat(ultimoIdAuditoria()).isEqualTo(auditoriaAntes);
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Vendedor y comprador solo registran y editan terceros de su rol; el jefe de inventario no accede")
	void permisosPorRol() throws Exception {
		String vendedor = usuarios.bearer("vendedor");
		String comprador = usuarios.bearer("comprador");
		String nit = numeroUnico(9);
		int dv = DigitoVerificacionNit.calcular(nit).orElseThrow();

		mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, vendedor).contentType(MediaType.APPLICATION_JSON)
						.content(juridica(nit, dv, true, true)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, comprador).contentType(MediaType.APPLICATION_JSON)
						.content(natural(numeroUnico(10), "Rosa", "Díaz")))
				.andExpect(status().isForbidden());

		long proveedor = crear(comprador, juridica(nit, dv, false, true));
		mvc.perform(put(RUTA + "/{id}", proveedor).header(HttpHeaders.AUTHORIZATION, vendedor)
						.contentType(MediaType.APPLICATION_JSON).content(juridica(nit, dv, true, false)))
				.andExpect(status().isForbidden());
		mvc.perform(patch(RUTA + "/{id}/estado", proveedor).header(HttpHeaders.AUTHORIZATION, vendedor)
						.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
				.andExpect(status().isForbidden());
		mvc.perform(get(RUTA + "/{id}", proveedor).header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(status().isOk());
		mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, usuarios.bearer("jefe")))
				.andExpect(status().isForbidden());
		assertThat(jdbc.queryForObject("SELECT es_cliente FROM tercero WHERE id = ?", Boolean.class, proveedor)).isFalse();
	}

	@Test
	@DisplayName("Filtros por tipo de persona y estado; desactivar es idempotente y cada escritura deja un registro de auditoría")
	void filtrosEstadoYAuditoria() throws Exception {
		String vendedor = usuarios.bearer("vendedor");
		String cc = numeroUnico(10);
		long auditoriaAntes = ultimoIdAuditoria();
		long id = crear(vendedor, natural(cc, "Marta", "Lugo"));
		assertThat(registrosDesde(auditoriaAntes)).singleElement().satisfies(r -> assertThat(r)
				.containsEntry("accion", "CREAR").containsEntry("entidad", "tercero")
				.containsEntry("usuario_id", idUsuario("vendedor")));

		mvc.perform(get(RUTA).param("tipoPersona", "NATURAL").param("q", "marta lu").param("size", "100")
						.header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(jsonPath("$.contenido[*].id", hasItem((int) id)))
				.andExpect(jsonPath("$.contenido[*].tipoPersona", everyItem(is("NATURAL"))));

		long antesEditar = ultimoIdAuditoria();
		mvc.perform(put(RUTA + "/{id}", id).header(HttpHeaders.AUTHORIZATION, vendedor)
						.contentType(MediaType.APPLICATION_JSON).content(natural(cc, "Marta", "Lugo Paz")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.apellido").value("Lugo Paz"));
		assertThat(registrosDesde(antesEditar)).singleElement()
				.satisfies(r -> assertThat(r).containsEntry("accion", "MODIFICAR"));

		long antesEstado = ultimoIdAuditoria();
		for (int i = 0; i < 2; i++) {
			mvc.perform(patch(RUTA + "/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, vendedor)
							.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.estado").value("INACTIVO"));
		}
		assertThat(registrosDesde(antesEstado)).hasSize(1);
		mvc.perform(get(RUTA).param("estado", "ACTIVO").param("q", cc).header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(jsonPath("$.contenido[*].id", not(hasItem((int) id))));
		mvc.perform(get(RUTA).param("estado", "INACTIVO").param("q", cc).header(HttpHeaders.AUTHORIZATION, vendedor))
				.andExpect(jsonPath("$.contenido[*].id", hasItem((int) id)));
	}

	// ------------------------------------------------------------------ apoyo

	private long crear(String token, String cuerpo) throws Exception {
		String respuesta = mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
	}

	private List<Map<String, Object>> registrosDesde(long id) {
		return jdbc.queryForList("SELECT accion, entidad, usuario_id FROM auditoria WHERE id > ? AND entidad = 'tercero'"
				+ " ORDER BY id", id);
	}

	/** Número aleatorio de {@code digitos} cifras que no empieza por 0. */
	private static String numeroUnico(int digitos) {
		StringBuilder numero = new StringBuilder().append(ThreadLocalRandom.current().nextInt(1, 10));
		for (int i = 1; i < digitos; i++) {
			numero.append(ThreadLocalRandom.current().nextInt(10));
		}
		return numero.toString();
	}

	private static String natural(String cc, String nombre, String apellido) {
		return """
				{"tipoPersona":"NATURAL","tipoIdentificacion":"CC","numeroIdentificacion":"%s","nombre":"%s",
				 "apellido":"%s","direccion":"Calle 10 # 5-20, Neiva","telefono":"3157654321","esCliente":true}"""
				.formatted(cc, nombre, apellido);
	}

	private static String juridica(String nit, int dv, boolean cliente, boolean proveedor) {
		return """
				{"tipoPersona":"JURIDICA","tipoIdentificacion":"NIT","numeroIdentificacion":"%s",
				 "digitoVerificacion":%d,"razonSocial":"Proveedora %s S.A.S.","direccion":"Neiva",
				 "telefono":"6088712345","correo":"ventas@proveedora.test","esCliente":%b,"esProveedor":%b}"""
				.formatted(nit, dv, nit, cliente, proveedor);
	}

}
