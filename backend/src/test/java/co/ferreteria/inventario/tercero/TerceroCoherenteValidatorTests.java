/**
 * Nombre: TerceroCoherenteValidatorTests.java
 * Descripción: Pruebas unitarias de las validaciones de TerceroPeticion que dependen del tipo de persona.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.ferreteria.inventario.tercero.dto.TerceroPeticion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Reglas de RN-26 en la petición, sin Spring: cada error se reporta en su campo (RNF-INT-002).
 */
class TerceroCoherenteValidatorTests {

	private static final String OBLIGATORIO = "{jakarta.validation.constraints.NotBlank.message}";

	private static ValidatorFactory fabrica;
	private static Validator validador;

	@BeforeAll
	static void crearValidador() {
		fabrica = Validation.buildDefaultValidatorFactory();
		validador = fabrica.getValidator();
	}

	@AfterAll
	static void cerrar() {
		fabrica.close();
	}

	@Test
	@DisplayName("[CA-RF-TER-001-1] Persona natural con cédula, nombre y apellido es válida")
	void naturalValida() {
		assertThat(errores(natural().build())).isEmpty();
	}

	@Test
	@DisplayName("[CA-RF-TER-001-1] Persona jurídica con NIT, dígito correcto y razón social es válida")
	void juridicaValida() {
		assertThat(errores(juridica().build())).isEmpty();
	}

	@Test
	@DisplayName("[CA-RF-TER-001-2] Un tercero puede ser cliente y proveedor a la vez")
	void ambosRoles() {
		assertThat(errores(juridica().esCliente(true).esProveedor(true).build())).isEmpty();
	}

	@Test
	@DisplayName("[CA-RF-TER-001-1] Persona natural sin nombre ni apellido: error en cada campo")
	void naturalSinNombres() {
		assertThat(errores(natural().nombre(" ").apellido(null).build()))
				.containsEntry("nombre", OBLIGATORIO)
				.containsEntry("apellido", OBLIGATORIO);
	}

	@Test
	@DisplayName("[CA-RF-TER-001-1] Persona jurídica sin razón social o con cédula: error en su campo")
	void juridicaIncompleta() {
		assertThat(errores(juridica().razonSocial("").build())).containsEntry("razonSocial", OBLIGATORIO);
		assertThat(errores(juridica().tipoIdentificacion(TipoIdentificacion.CC).digitoVerificacion(null).build()))
				.containsEntry("tipoIdentificacion", "{tercero.juridica.solo_nit}");
	}

	@Test
	@DisplayName("[CA-RF-TER-001-3] NIT con dígito de verificación incorrecto o ausente: error en digitoVerificacion")
	void nitConDigitoIncorrecto() {
		assertThat(errores(juridica().digitoVerificacion(3).build()))
				.containsEntry("digitoVerificacion", "{tercero.dv.invalido}");
		assertThat(errores(juridica().digitoVerificacion(null).build()))
				.containsEntry("digitoVerificacion", OBLIGATORIO);
	}

	@Test
	@DisplayName("Persona natural con NIT exige el dígito; con cédula no lo admite")
	void digitoSegunTipo() {
		assertThat(errores(natural().tipoIdentificacion(TipoIdentificacion.NIT).numeroIdentificacion("899999068")
				.digitoVerificacion(1).build())).isEmpty();
		assertThat(errores(natural().digitoVerificacion(5).build()))
				.containsEntry("digitoVerificacion", "{tercero.dv.no_aplica}");
	}

	@Test
	@DisplayName("Un número con formato no válido no compara el dígito (solo se informa el número)")
	void numeroNoValidoNoComparaDigito() {
		assertThat(errores(juridica().numeroIdentificacion("890-903").build()))
				.containsOnlyKeys("numeroIdentificacion");
	}

	@Test
	@DisplayName("Sin roles, teléfono de 6 dígitos y correo mal formado: un error por campo")
	void rolTelefonoCorreo() {
		Map<String, String> errores = errores(natural().esCliente(false).telefono("123456").correo("sin-arroba").build());
		assertThat(errores).containsEntry("esCliente", "{tercero.rol.obligatorio}")
				.containsEntry("telefono", "{tercero.telefono.formato}")
				.containsKey("correo");
	}

	// ------------------------------------------------------------------ apoyo

	private static Map<String, String> errores(TerceroPeticion peticion) {
		return validador.validate(peticion).stream().collect(Collectors.toMap(
				v -> v.getPropertyPath().toString(), ConstraintViolation::getMessageTemplate, (a, b) -> a));
	}

	static TerceroPeticion.TerceroPeticionBuilder natural() {
		return TerceroPeticion.builder().tipoPersona(TipoPersona.NATURAL).tipoIdentificacion(TipoIdentificacion.CC)
				.numeroIdentificacion("1075234567").nombre("Juan").apellido("Pérez Gómez").direccion("Neiva")
				.telefono("3157654321").esCliente(true);
	}

	static TerceroPeticion.TerceroPeticionBuilder juridica() {
		return TerceroPeticion.builder().tipoPersona(TipoPersona.JURIDICA).tipoIdentificacion(TipoIdentificacion.NIT)
				.numeroIdentificacion("890903938").digitoVerificacion(8).razonSocial("Bancolombia S.A.")
				.direccion("Medellín").telefono("6045101000").correo("contacto@banco.test").esProveedor(true);
	}

}
