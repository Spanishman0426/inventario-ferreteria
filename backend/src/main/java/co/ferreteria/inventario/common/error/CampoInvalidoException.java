/**
 * Nombre: CampoInvalidoException.java
 * Descripción: Excepción para un dato de entrada que solo puede validarse en el servicio; se traduce a 400
 *              con el error asociado a su campo.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import java.io.Serial;

/**
 * Indica que el valor de un campo no es válido por una razón que Bean Validation no puede comprobar,
 * por ejemplo una referencia a una marca que no existe o está inactiva (RF-PRO-001), o un campo de
 * orden desconocido. El manejador global responde 400 {@code VALIDACION} con un {@link ErrorCampo},
 * igual que los errores de anotaciones, para que el frontend lo muestre junto al campo (RNF-INT-002).
 */
public class CampoInvalidoException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/** Nombre del campo en la petición. */
	private final String campo;

	/** Clave del mensaje en los archivos de mensajes. */
	private final String claveMensaje;

	/** Valores para los marcadores del mensaje. */
	private final transient Object[] argumentos;

	/**
	 * Crea la excepción.
	 *
	 * @param campo nombre del campo en la petición
	 * @param claveMensaje clave del mensaje con la causa
	 * @param argumentos valores para los marcadores del mensaje
	 */
	public CampoInvalidoException(String campo, String claveMensaje, Object... argumentos) {
		super(campo + ": " + claveMensaje);
		this.campo = campo;
		this.claveMensaje = claveMensaje;
		this.argumentos = argumentos.clone();
	}

	/**
	 * Nombre del campo.
	 *
	 * @return campo de la petición
	 */
	public String getCampo() {
		return campo;
	}

	/**
	 * Clave del mensaje.
	 *
	 * @return clave en los archivos de mensajes
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
