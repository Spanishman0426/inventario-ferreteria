/**
 * Nombre: RecursoNoEncontradoException.java
 * Descripción: Excepción para un recurso inexistente; se traduce a 404.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import java.io.Serial;

/**
 * Indica que no existe el recurso buscado. El manejador global responde 404 {@code NO_ENCONTRADO}.
 */
public class RecursoNoEncontradoException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/** Clave del nombre del recurso en los archivos de mensajes (por ejemplo {@code entidad.producto}). */
	private final String claveRecurso;

	/** Identificador buscado. */
	private final transient Object identificador;

	/**
	 * Crea la excepción.
	 *
	 * @param claveRecurso clave de mensaje con el nombre del recurso
	 * @param identificador identificador que no se encontró
	 */
	public RecursoNoEncontradoException(String claveRecurso, Object identificador) {
		super(claveRecurso + " " + identificador);
		this.claveRecurso = claveRecurso;
		this.identificador = identificador;
	}

	/**
	 * Clave de mensaje del recurso.
	 *
	 * @return clave de mensaje
	 */
	public String getClaveRecurso() {
		return claveRecurso;
	}

	/**
	 * Identificador que no se encontró.
	 *
	 * @return identificador buscado
	 */
	public Object getIdentificador() {
		return identificador;
	}

}
