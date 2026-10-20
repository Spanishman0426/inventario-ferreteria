/**
 * Nombre: ErrorCampo.java
 * Descripción: Error de validación de un campo dentro de una respuesta Problem Details.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

/**
 * Error de un campo concreto, para mostrar el mensaje junto al campo (RNF-INT-002).
 *
 * @param campo nombre del campo o ruta de la propiedad (por ejemplo {@code lineas[0].cantidad})
 * @param mensaje mensaje traducido con la causa
 */
public record ErrorCampo(String campo, String mensaje) {
}
