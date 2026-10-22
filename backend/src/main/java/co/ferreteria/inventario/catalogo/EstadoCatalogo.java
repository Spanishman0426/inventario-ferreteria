/**
 * Nombre: EstadoCatalogo.java
 * Descripción: Estado de un elemento de catálogo (marca o categoría) en la API: ACTIVO o INACTIVO.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

/**
 * Estado de una marca o categoría (RF-CFG-001). Se guarda en la columna booleana {@code activo}; un
 * elemento en uso no se elimina, solo se desactiva.
 */
public enum EstadoCatalogo {

	/** Se ofrece en los selectores de productos nuevos. */
	ACTIVO,
	/** Desactivado: los productos que ya lo usan lo conservan, pero no se ofrece a productos nuevos. */
	INACTIVO;

	/**
	 * Estado que corresponde a un valor de la columna {@code activo}.
	 *
	 * @param activo valor de la columna
	 * @return {@link #ACTIVO} si es verdadero; {@link #INACTIVO} en otro caso
	 */
	public static EstadoCatalogo desdeActivo(boolean activo) {
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
