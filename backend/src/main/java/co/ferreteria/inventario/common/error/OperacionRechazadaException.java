/**
 * Nombre: OperacionRechazadaException.java
 * Descripción: Excepción genérica para rechazar una operación con un código de error y un mensaje traducido.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import java.io.Serial;

/**
 * Rechaza una operación con un {@link CodigoError} concreto y una clave del archivo de mensajes.
 * El manejador global responde con el estado HTTP del código y el detalle traducido.
 */
public class OperacionRechazadaException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/** Código de error; define el estado HTTP. */
	private final CodigoError codigo;

	/** Clave del detalle en los archivos de mensajes. */
	private final String claveMensaje;

	/** Valores para los marcadores del mensaje. */
	private final transient Object[] argumentos;

	/**
	 * Crea la excepción.
	 *
	 * @param codigo código de error (define el estado HTTP)
	 * @param claveMensaje clave del detalle en los archivos de mensajes
	 * @param argumentos valores para los marcadores del mensaje
	 */
	public OperacionRechazadaException(CodigoError codigo, String claveMensaje, Object... argumentos) {
		super(claveMensaje);
		this.codigo = codigo;
		this.claveMensaje = claveMensaje;
		this.argumentos = argumentos.clone();
	}

	/**
	 * Código de error.
	 *
	 * @return código de error
	 */
	public CodigoError getCodigo() {
		return codigo;
	}

	/**
	 * Clave del detalle.
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
