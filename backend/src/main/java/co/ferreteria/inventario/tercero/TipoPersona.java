/**
 * Nombre: TipoPersona.java
 * Descripción: Tipo de persona de un tercero (columna tercero.tipo_persona).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

/**
 * Persona natural o jurídica (RF-TER-001, RN-26). Define qué campos son obligatorios.
 */
public enum TipoPersona {

	/** Persona natural: cédula o NIT, nombre y apellido. */
	NATURAL,
	/** Persona jurídica: solo NIT, con razón social. */
	JURIDICA

}
