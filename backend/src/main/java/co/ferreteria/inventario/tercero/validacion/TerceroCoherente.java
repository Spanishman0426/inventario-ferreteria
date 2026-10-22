/**
 * Nombre: TerceroCoherente.java
 * Descripción: Restricción de Bean Validation con las reglas de RN-26 que dependen de varios campos del tercero.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero.validacion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Reglas de RN-26 y de los CHECK de la tabla {@code tercero}: persona natural con nombre y apellido;
 * persona jurídica solo con NIT y con razón social; NIT con dígito de verificación correcto; cédula sin
 * dígito; al menos un rol. Cada error se reporta en su campo (RNF-INT-002).
 */
@Documented
@Constraint(validatedBy = TerceroCoherenteValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TerceroCoherente {

	/**
	 * Mensaje por defecto (se reemplaza por uno más preciso en cada campo).
	 *
	 * @return clave del mensaje
	 */
	String message() default "{tercero.datos.incoherentes}";

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
