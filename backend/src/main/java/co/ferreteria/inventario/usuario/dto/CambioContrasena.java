/**
 * Nombre: CambioContrasena.java
 * Descripción: Nueva contraseña de un usuario (PUT /api/v1/usuarios/{id}/contrasena).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Contraseña nueva, con las mismas reglas que al crear (RF-SEG-001).
 *
 * @param contrasena contraseña nueva; mínimo 8 caracteres con letras y números
 */
public record CambioContrasena(
		@Schema(format = "password")
		@NotBlank @Size(min = ReglasContrasena.MINIMO, max = ReglasContrasena.MAXIMO)
		@Pattern(regexp = ReglasContrasena.PATRON, message = ReglasContrasena.MENSAJE)
		String contrasena) {

	/**
	 * Representación sin la contraseña.
	 *
	 * @return texto sin datos secretos
	 */
	@Override
	public String toString() {
		return "CambioContrasena[contrasena=***]";
	}

}
