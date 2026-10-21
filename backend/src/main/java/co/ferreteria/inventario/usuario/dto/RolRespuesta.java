/**
 * Nombre: RolRespuesta.java
 * Descripción: Vista de un rol con sus permisos (GET /api/v1/roles).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario.dto;

import java.util.Set;
import java.util.TreeSet;

import co.ferreteria.inventario.usuario.Rol;

/**
 * Rol de la matriz 2.2 con sus permisos.
 *
 * @param codigo código del rol
 * @param nombre nombre legible
 * @param permisos códigos de permiso, ordenados
 */
public record RolRespuesta(String codigo, String nombre, Set<String> permisos) {

	/**
	 * Construye la vista a partir de la entidad.
	 *
	 * @param rol entidad con permisos cargados
	 * @return vista del rol
	 */
	public static RolRespuesta desde(Rol rol) {
		Set<String> permisos = new TreeSet<>();
		rol.getPermisos().forEach(p -> permisos.add(p.getCodigo()));
		return new RolRespuesta(rol.getCodigo(), rol.getNombre(), permisos);
	}

}
