/**
 * Nombre: UsuarioSesion.java
 * Descripción: Datos del usuario autenticado que recibe el frontend al iniciar sesión y en /auth/me.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security.auth;

import java.util.Set;

import co.ferreteria.inventario.usuario.Usuario;

/**
 * Usuario de la sesión con sus roles y permisos (RF-SEG-001, matriz 2.2).
 *
 * @param id id del usuario
 * @param nombreCompleto nombre de la persona
 * @param roles códigos de rol
 * @param permisos códigos de permiso; el frontend los usa para mostrar u ocultar opciones
 */
public record UsuarioSesion(Long id, String nombreCompleto, Set<String> roles, Set<String> permisos) {

	/**
	 * Construye la vista a partir de la entidad (con roles y permisos cargados).
	 *
	 * @param usuario entidad
	 * @return datos de sesión
	 */
	public static UsuarioSesion desde(Usuario usuario) {
		return new UsuarioSesion(usuario.getId(), usuario.getNombreCompleto(), usuario.codigosRoles(),
				usuario.codigosPermisos());
	}

}
