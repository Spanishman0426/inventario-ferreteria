/**
 * Nombre: RolTercero.java
 * Descripción: Rol comercial de un tercero (cliente o proveedor), usado en filtros y permisos.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

/**
 * Rol de un tercero con el permiso que exige registrarlo o editarlo (matriz 2.2, fila "Registrar
 * terceros"). Un tercero puede tener los dos roles (RN-26).
 */
public enum RolTercero {

	/** Cliente: lo registran el vendedor, el gerente y el administrador. */
	CLIENTE("TERCERO_CLIENTE_EDITAR"),
	/** Proveedor: lo registran el comprador, el gerente y el administrador. */
	PROVEEDOR("TERCERO_PROVEEDOR_EDITAR");

	private final String permiso;

	RolTercero(String permiso) {
		this.permiso = permiso;
	}

	/**
	 * Permiso que exige crear o editar un tercero con este rol.
	 *
	 * @return código del permiso
	 */
	public String getPermiso() {
		return permiso;
	}

}
