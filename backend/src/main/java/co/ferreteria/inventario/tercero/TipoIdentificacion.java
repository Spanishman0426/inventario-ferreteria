/**
 * Nombre: TipoIdentificacion.java
 * Descripción: Tipo de identificación de un tercero (columna tercero.tipo_identificacion).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

/**
 * Cédula de ciudadanía o NIT (RF-TER-001). El NIT lleva dígito de verificación (RN-26).
 */
public enum TipoIdentificacion {

	/** Cédula de ciudadanía; sin dígito de verificación. */
	CC,
	/** Número de identificación tributaria; exige dígito de verificación. */
	NIT

}
