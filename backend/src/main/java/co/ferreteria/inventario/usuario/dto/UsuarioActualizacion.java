/**
 * Nombre: UsuarioActualizacion.java
 * Descripción: Datos editables de un usuario (PUT /api/v1/usuarios/{id}).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nombre y correo de un usuario. El nombre de usuario no cambia.
 *
 * @param nombreCompleto nombre de la persona
 * @param correo correo único
 */
public record UsuarioActualizacion(
		@NotBlank @Size(max = 150) String nombreCompleto,
		@NotBlank @Email @Size(max = 150) String correo) {
}
