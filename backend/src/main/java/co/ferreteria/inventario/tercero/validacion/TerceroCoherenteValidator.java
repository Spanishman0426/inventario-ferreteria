/**
 * Nombre: TerceroCoherenteValidator.java
 * Descripción: Validador de la restricción TerceroCoherente sobre TerceroPeticion.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero.validacion;

import java.util.OptionalInt;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import co.ferreteria.inventario.tercero.DigitoVerificacionNit;
import co.ferreteria.inventario.tercero.TipoIdentificacion;
import co.ferreteria.inventario.tercero.TipoPersona;
import co.ferreteria.inventario.tercero.dto.TerceroPeticion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Comprueba las reglas de RN-26 que combinan campos y registra cada error en el campo que lo causa.
 * Si falta el tipo de persona o de identificación no opina sobre lo que depende de ellos: ese error lo
 * reporta {@code @NotNull}. El dígito solo se compara cuando el número tiene un formato válido, para no
 * repetir el error del número.
 */
public class TerceroCoherenteValidator implements ConstraintValidator<TerceroCoherente, TerceroPeticion> {

	/** Mensaje de campo obligatorio, el mismo de {@code @NotBlank}. */
	private static final String OBLIGATORIO = "{jakarta.validation.constraints.NotBlank.message}";

	/**
	 * Constructor usado por Bean Validation.
	 */
	public TerceroCoherenteValidator() {
		// Sin estado.
	}

	/**
	 * Valida la combinación de campos.
	 *
	 * @param p tercero a validar
	 * @param contexto contexto para registrar los errores por campo
	 * @return {@code true} si no hay errores
	 */
	@Override
	public boolean isValid(TerceroPeticion p, ConstraintValidatorContext contexto) {
		if (p == null) {
			return true;
		}
		contexto.disableDefaultConstraintViolation();
		boolean valido = true;

		if (p.tipoPersona() == TipoPersona.NATURAL) {
			valido &= exigir(p.nombre(), "nombre", contexto);
			valido &= exigir(p.apellido(), "apellido", contexto);
		} else if (p.tipoPersona() == TipoPersona.JURIDICA) {
			valido &= exigir(p.razonSocial(), "razonSocial", contexto);
			if (p.tipoIdentificacion() == TipoIdentificacion.CC) {
				valido = error(contexto, "{tercero.juridica.solo_nit}", "tipoIdentificacion");
			}
		}

		if (p.tipoIdentificacion() == TipoIdentificacion.NIT) {
			valido &= validarDigito(p, contexto);
		} else if (p.tipoIdentificacion() == TipoIdentificacion.CC && p.digitoVerificacion() != null) {
			valido = error(contexto, "{tercero.dv.no_aplica}", "digitoVerificacion");
		}

		if (!p.esCliente() && !p.esProveedor()) {
			valido = error(contexto, "{tercero.rol.obligatorio}", "esCliente");
		}
		return valido;
	}

	private static boolean validarDigito(TerceroPeticion p, ConstraintValidatorContext contexto) {
		if (p.digitoVerificacion() == null) {
			return error(contexto, OBLIGATORIO, "digitoVerificacion");
		}
		String numero = p.numeroIdentificacion();
		if (numero == null || !numero.matches("[0-9]{5,15}")) {
			return true;
		}
		OptionalInt esperado = DigitoVerificacionNit.calcular(numero);
		if (esperado.isPresent() && esperado.getAsInt() != p.digitoVerificacion()) {
			contexto.unwrap(HibernateConstraintValidatorContext.class)
					.addMessageParameter("esperado", esperado.getAsInt());
			return error(contexto, "{tercero.dv.invalido}", "digitoVerificacion");
		}
		return true;
	}

	private static boolean exigir(String valor, String campo, ConstraintValidatorContext contexto) {
		return valor != null && !valor.isBlank() || error(contexto, OBLIGATORIO, campo);
	}

	private static boolean error(ConstraintValidatorContext contexto, String mensaje, String campo) {
		contexto.buildConstraintViolationWithTemplate(mensaje).addPropertyNode(campo).addConstraintViolation();
		return false;
	}

}
