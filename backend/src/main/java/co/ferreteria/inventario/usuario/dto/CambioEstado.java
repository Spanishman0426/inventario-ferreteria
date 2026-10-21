/**
 * Nombre: CambioEstado.java
 * Descripción: Activación o desactivación de un usuario (PATCH /api/v1/usuarios/{id}/estado).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado de la cuenta. Los usuarios no se eliminan, solo se desactivan (RF-SEG-001).
 *
 * @param activo {@code true} para activar, {@code false} para desactivar
 */
public record CambioEstado(@NotNull Boolean activo) {
}
