/**
 * Nombre: EstadoTercero.java
 * Descripción: Estado ACTIVO/INACTIVO de un tercero, guardado en la columna booleana tercero.activo.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

/**
 * Estado de un tercero. Los terceros no se eliminan, solo se desactivan (RF-TER-001).
 */
public enum EstadoTercero {

	/** Disponible para nuevos documentos. */
	ACTIVO,
	/** Desactivado: se conserva con su historial. */
	INACTIVO;

	/**
	 * Estado que corresponde a un valor de la columna {@code activo}.
	 *
	 * @param activo valor de la columna
	 * @return {@link #ACTIVO} si es verdadero; {@link #INACTIVO} en otro caso
	 */
	public static EstadoTercero desdeActivo(boolean activo) {
		return activo ? ACTIVO : INACTIVO;
	}

	/**
	 * Valor de la columna {@code activo} para este estado.
	 *
	 * @return {@code true} solo para {@link #ACTIVO}
	 */
	public boolean comoActivo() {
		return this == ACTIVO;
	}

}
