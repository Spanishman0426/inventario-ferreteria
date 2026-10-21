/**
 * Nombre: SolicitudInicioSesion.java
 * Descripción: Datos de entrada de POST /api/v1/auth/login.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de inicio de sesión (RF-SEG-001).
 *
 * @param usuario nombre de usuario
 * @param contrasena contraseña en texto plano; solo viaja por HTTPS y nunca se guarda ni se registra
 */
public record SolicitudInicioSesion(
		@Schema(example = "admin") @NotBlank @Size(max = 100) String usuario,
		@Schema(format = "password") @NotBlank @Size(max = 72) String contrasena) {

	/**
	 * Representación sin la contraseña, para que nunca aparezca en registros.
	 *
	 * @return texto con el nombre de usuario
	 */
	@Override
	public String toString() {
		return "SolicitudInicioSesion[usuario=" + usuario + ", contrasena=***]";
	}

}
