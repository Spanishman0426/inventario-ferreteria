/**
 * Nombre: ReglasContrasena.java
 * Descripción: Constantes de la política de contraseñas de RF-SEG-001.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario.dto;

/**
 * Política de contraseñas: mínimo 8 caracteres, al menos una letra y al menos un número
 * (RF-SEG-001). El máximo de 72 es el límite de bytes que BCrypt tiene en cuenta.
 */
public final class ReglasContrasena {

	/** Longitud mínima. */
	public static final int MINIMO = 8;
	/** Longitud máxima que BCrypt procesa. */
	public static final int MAXIMO = 72;
	/** Al menos una letra (incluidas tildes y ñ) y al menos un dígito. */
	public static final String PATRON = "^(?=.*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ])(?=.*\\d).+$";
	/** Clave del mensaje de validación. */
	public static final String MENSAJE = "{usuario.contrasena.politica}";

	private ReglasContrasena() {
		// Solo constantes.
	}

}
