/**
 * Nombre: CodigoError.java
 * Descripción: Códigos de error propios que acompañan cada respuesta Problem Details.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de códigos de error de la API, con su estado HTTP y sus claves de mensaje.
 *
 * <p>El frontend decide qué hacer según {@code codigo}, no según el texto, que depende del idioma.
 * Las claves siguen el patrón {@code error.<codigo en minúsculas>.titulo} y {@code .detalle}.</p>
 */
public enum CodigoError {

	/** Uno o más campos no cumplen las reglas de validación (RNF-INT-002). */
	VALIDACION(HttpStatus.BAD_REQUEST),
	/** La solicitud está mal formada o no se puede leer. */
	SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST),
	/** Falta autenticación o el token no es válido (RNF-SEG-001). */
	NO_AUTENTICADO(HttpStatus.UNAUTHORIZED),
	/** El usuario no tiene el permiso requerido (RN-27, RNF-SEG-002). */
	ACCESO_DENEGADO(HttpStatus.FORBIDDEN),
	/** El recurso solicitado no existe. */
	NO_ENCONTRADO(HttpStatus.NOT_FOUND),
	/** El método HTTP no está permitido en la ruta. */
	METODO_NO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED),
	/** Ya existe un registro con el mismo valor único (SQLSTATE 23505). */
	CONFLICTO_UNICIDAD(HttpStatus.CONFLICT),
	/** Se referencia un registro que no existe o que está en uso (SQLSTATE 23503). */
	CONFLICTO_REFERENCIA(HttpStatus.CONFLICT),
	/** La base rechazó el dato por una restricción CHECK (SQLSTATE 23514). */
	REGLA_INTEGRIDAD(HttpStatus.UNPROCESSABLE_CONTENT),
	/** Un trigger de negocio rechazó la operación (SQLSTATE P0001). */
	REGLA_NEGOCIO(HttpStatus.UNPROCESSABLE_CONTENT),
	/** Error no previsto; el detalle queda solo en el registro del servidor. */
	ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR);

	private final HttpStatus estado;

	CodigoError(HttpStatus estado) {
		this.estado = estado;
	}

	/**
	 * Estado HTTP asociado al código.
	 *
	 * @return estado HTTP de la respuesta
	 */
	public HttpStatus getEstado() {
		return estado;
	}

	/**
	 * Clave del título en los archivos de mensajes.
	 *
	 * @return clave {@code error.<codigo>.titulo}
	 */
	public String claveTitulo() {
		return "error." + name().toLowerCase() + ".titulo";
	}

	/**
	 * Clave del detalle en los archivos de mensajes.
	 *
	 * @return clave {@code error.<codigo>.detalle}
	 */
	public String claveDetalle() {
		return "error." + name().toLowerCase() + ".detalle";
	}

}
