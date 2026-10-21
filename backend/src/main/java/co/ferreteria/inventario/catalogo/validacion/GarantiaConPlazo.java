/**
 * Nombre: GarantiaConPlazo.java
 * Descripción: Restricción de Bean Validation: el plazo de garantía es obligatorio si y solo si el control es GARANTIA.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo.validacion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Regla de RF-PRO-001 y del CHECK {@code ck_producto_garantia}: un producto con control GARANTIA exige
 * {@code plazoGarantiaMeses}, y uno sin ese control no lo admite. El error se reporta en el campo
 * {@code plazoGarantiaMeses} para mostrarlo junto a él (RNF-INT-002).
 */
@Documented
@Constraint(validatedBy = GarantiaConPlazoValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GarantiaConPlazo {

	/**
	 * Mensaje por defecto (se reemplaza por uno más preciso al validar).
	 *
	 * @return clave del mensaje
	 */
	String message() default "{producto.garantia.plazo_obligatorio}";

	/**
	 * Grupos de validación.
	 *
	 * @return grupos
	 */
	Class<?>[] groups() default {};

	/**
	 * Carga útil de la restricción.
	 *
	 * @return carga útil
	 */
	Class<? extends Payload>[] payload() default {};

}
