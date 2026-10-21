/**
 * Nombre: RespuestaInicioSesion.java
 * Descripción: Respuesta de POST /api/v1/auth/login con el token de acceso.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security.auth;

/**
 * Token de acceso emitido (RF-SEG-001).
 *
 * @param accessToken JWT para la cabecera {@code Authorization: Bearer}
 * @param expiresIn segundos de vigencia del token (1800)
 * @param usuario usuario autenticado con roles y permisos
 */
public record RespuestaInicioSesion(String accessToken, long expiresIn, UsuarioSesion usuario) {
}
