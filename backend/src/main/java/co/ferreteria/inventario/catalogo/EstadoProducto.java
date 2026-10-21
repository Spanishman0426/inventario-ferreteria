/**
 * Nombre: EstadoProducto.java
 * Descripción: Estado de un producto en la API (ACTIVO o INACTIVO).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

/**
 * Estado de un producto. Se guarda en la columna booleana {@code producto.activo}
 * mediante {@link EstadoProductoConverter}. Un producto inactivo no se elimina (RF-CFG-001).
 */
public enum EstadoProducto {

	/** Se puede comprar y vender. */
	ACTIVO,
	/** Dado de baja del catálogo por borrado lógico; se conserva para el historial. */
	INACTIVO;

	/**
	 * Estado que corresponde a un valor de la columna {@code activo}.
	 *
	 * @param activo valor de la columna
	 * @return {@link #ACTIVO} si es verdadero; {@link #INACTIVO} en otro caso
	 */
	public static EstadoProducto desdeActivo(boolean activo) {
		return activo ? ACTIVO : INACTIVO;
	}

}
