/**
 * Nombre: CambioRoles.java
 * Descripción: Roles que se asignan a un usuario (PUT /api/v1/usuarios/{id}/roles).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Nuevo conjunto de roles; reemplaza los anteriores.
 *
 * @param roles códigos de rol; al menos uno
 */
public record CambioRoles(@Schema(example = "[\"GERENTE\"]") @NotEmpty Set<@NotBlank String> roles) {
}
