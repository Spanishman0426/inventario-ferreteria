/**
 * Nombre: ConflictoException.java
 * Descripción: Excepción para un conflicto de unicidad detectado por la aplicación; se traduce a 409.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import java.io.Serial;

/**
 * Indica que la operación choca con un dato existente (por ejemplo, un producto con el mismo nombre
 * y marca, RF-PRO-001). El manejador global responde 409 {@code CONFLICTO_UNICIDAD}.
 */
public class ConflictoException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/** Clave de mensaje con el detalle del conflicto. */
	private final String claveMensaje;

	/** Valores para los marcadores del mensaje. */
	private final transient Object[] argumentos;

	/**
	 * Crea la excepción.
	 *
	 * @param claveMensaje clave de mensaje con el detalle
	 * @param argumentos valores para los marcadores del mensaje
	 */
	public ConflictoException(String claveMensaje, Object... argumentos) {
		super(claveMensaje);
		this.claveMensaje = claveMensaje;
		this.argumentos = argumentos.clone();
	}

	/**
	 * Clave de mensaje con el detalle.
	 *
	 * @return clave de mensaje
	 */
	public String getClaveMensaje() {
		return claveMensaje;
	}

	/**
	 * Valores para los marcadores del mensaje.
	 *
	 * @return copia de los argumentos
	 */
	public Object[] getArgumentos() {
		return argumentos.clone();
	}

}
