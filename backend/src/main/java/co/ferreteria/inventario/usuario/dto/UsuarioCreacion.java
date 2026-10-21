/**
 * Nombre: UsuarioCreacion.java
 * Descripción: Datos para crear un usuario (POST /api/v1/usuarios).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nuevo usuario (RF-SEG-001). La cuenta nace activa.
 *
 * @param usuario nombre de usuario único (letras, números, punto, guion y guion bajo)
 * @param nombreCompleto nombre de la persona
 * @param correo correo único
 * @param contrasena contraseña inicial; mínimo 8 caracteres con letras y números
 * @param roles códigos de rol; al menos uno
 */
public record UsuarioCreacion(
		@Schema(example = "vendedor2")
		@NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "{usuario.usuario.formato}")
		String usuario,
		@NotBlank @Size(max = 150) String nombreCompleto,
		@NotBlank @Email @Size(max = 150) String correo,
		@Schema(format = "password")
		@NotBlank @Size(min = ReglasContrasena.MINIMO, max = ReglasContrasena.MAXIMO)
		@Pattern(regexp = ReglasContrasena.PATRON, message = ReglasContrasena.MENSAJE)
		String contrasena,
		@Schema(example = "[\"VENDEDOR\"]") @NotEmpty Set<@NotBlank String> roles) {

	/**
	 * Representación sin la contraseña.
	 *
	 * @return texto sin datos secretos
	 */
	@Override
	public String toString() {
		return "UsuarioCreacion[usuario=" + usuario + ", correo=" + correo + ", roles=" + roles + ", contrasena=***]";
	}

}
