/**
 * Nombre: UsuarioRespuesta.java
 * Descripción: Vista de un usuario para la API de administración; nunca incluye el hash de la contraseña.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario.dto;

import java.time.Instant;
import java.util.Set;

import co.ferreteria.inventario.usuario.Usuario;

/**
 * Usuario visto por el administrador (RF-SEG-001).
 *
 * @param id identificador
 * @param usuario nombre de usuario
 * @param nombreCompleto nombre de la persona
 * @param correo correo
 * @param activo si puede iniciar sesión
 * @param roles códigos de rol
 * @param bloqueadoHasta fin del bloqueo por intentos fallidos, si lo hay
 * @param ultimoAcceso último inicio de sesión correcto
 * @param creadoEn fecha de creación
 */
public record UsuarioRespuesta(Long id, String usuario, String nombreCompleto, String correo, boolean activo,
		Set<String> roles, Instant bloqueadoHasta, Instant ultimoAcceso, Instant creadoEn) {

	/**
	 * Construye la vista a partir de la entidad.
	 *
	 * @param u entidad
	 * @return vista sin datos secretos
	 */
	public static UsuarioRespuesta desde(Usuario u) {
		return new UsuarioRespuesta(u.getId(), u.getUsuario(), u.getNombreCompleto(), u.getCorreo(), u.isActivo(),
				u.codigosRoles(), u.getBloqueadoHasta(), u.getUltimoAcceso(), u.getCreadoEn());
	}

}
