/**
 * Nombre: GarantiaConPlazoValidator.java
 * Descripción: Validador de la restricción GarantiaConPlazo sobre ProductoPeticion.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.validacion;

import co.ferreteria.inventario.catalogo.ControlProducto;
import co.ferreteria.inventario.catalogo.dto.ProductoPeticion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Comprueba que {@code plazoGarantiaMeses} venga si y solo si {@code control} es GARANTIA.
 * Si falta {@code control}, no opina: ese error lo reporta {@code @NotNull}.
 */
public class GarantiaConPlazoValidator implements ConstraintValidator<GarantiaConPlazo, ProductoPeticion> {

	/** Campo en el que se reporta el error. */
	public static final String CAMPO = "plazoGarantiaMeses";

	/**
	 * Constructor usado por Bean Validation.
	 */
	public GarantiaConPlazoValidator() {
		// Sin estado.
	}

	/**
	 * Valida la combinación de control y plazo.
	 *
	 * @param peticion producto a validar
	 * @param contexto contexto para registrar el error en el campo
	 * @return {@code true} si la combinación es válida
	 */
	@Override
	public boolean isValid(ProductoPeticion peticion, ConstraintValidatorContext contexto) {
		if (peticion == null || peticion.control() == null) {
			return true;
		}
		boolean esGarantia = peticion.control() == ControlProducto.GARANTIA;
		boolean tienePlazo = peticion.plazoGarantiaMeses() != null;
		if (esGarantia == tienePlazo) {
			return true;
		}
		String mensaje = esGarantia ? "{producto.garantia.plazo_obligatorio}" : "{producto.garantia.plazo_no_aplica}";
		contexto.disableDefaultConstraintViolation();
		contexto.buildConstraintViolationWithTemplate(mensaje).addPropertyNode(CAMPO).addConstraintViolation();
		return false;
	}

}
