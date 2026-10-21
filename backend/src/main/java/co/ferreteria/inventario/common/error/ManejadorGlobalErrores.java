/**
 * Nombre: ManejadorGlobalErrores.java
 * Descripción: Traduce las excepciones de la API a respuestas Problem Details (RFC 9457) traducidas.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import co.ferreteria.inventario.common.i18n.Mensajes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Manejador global de errores de la API.
 *
 * <p>Toda respuesta de error es un {@link ProblemDetail} con {@code title} y {@code detail} en el
 * idioma de {@code Accept-Language}, más dos propiedades propias:</p>
 * <ul>
 *   <li>{@code codigo}: valor de {@link CodigoError}, estable entre idiomas;</li>
 *   <li>{@code errores}: lista de {@link ErrorCampo} (vacía si el error no es de un campo).</li>
 * </ul>
 * <p>Los errores de PostgreSQL se clasifican por SQLSTATE: 23505 → 409, 23503 → 409,
 * 23514 → 422 y P0001 → 422 con el mensaje del trigger.</p>
 */
@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

	/** Nombre de la propiedad con el código de error propio. */
	public static final String PROPIEDAD_CODIGO = "codigo";
	/** Nombre de la propiedad con la lista de errores por campo. */
	public static final String PROPIEDAD_ERRORES = "errores";
	/** Nombre de la propiedad con la restricción de base de datos violada. */
	public static final String PROPIEDAD_RESTRICCION = "restriccion";

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

	private final Mensajes mensajes;
	private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

	/**
	 * Crea el manejador.
	 *
	 * @param mensajes acceso a los textos traducidos
	 */
	public ManejadorGlobalErrores(Mensajes mensajes) {
		this.mensajes = mensajes;
	}

	// ------------------------------------------------------------------ validación (400)

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorCampo> errores = ex.getBindingResult().getAllErrors().stream()
				.map(error -> new ErrorCampo(
						error instanceof FieldError campo ? campo.getField() : error.getObjectName(),
						error.getDefaultMessage()))
				.toList();
		return responder(CodigoError.VALIDACION, null, errores, request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorCampo> errores = ex.getParameterValidationResults().stream()
				.flatMap(resultado -> resultado.getResolvableErrors().stream()
						.map(error -> new ErrorCampo(resultado.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
		return responder(CodigoError.VALIDACION, null, errores, request);
	}

	/**
	 * Validación de parámetros fuera de un controlador (por ejemplo en servicios con {@code @Validated}).
	 *
	 * @param ex excepción de Bean Validation
	 * @param request solicitud en curso
	 * @return respuesta 400 {@code VALIDACION} con un error por violación
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Object> manejarViolacionRestricciones(ConstraintViolationException ex,
			WebRequest request) {
		List<ErrorCampo> errores = ex.getConstraintViolations().stream()
				.map(v -> new ErrorCampo(ultimoNodo(v.getPropertyPath().toString()), v.getMessage()))
				.toList();
		return responder(CodigoError.VALIDACION, null, errores, request);
	}

	/**
	 * Campo rechazado por una validación del servicio (referencia inexistente, orden desconocido...).
	 *
	 * @param ex excepción con el campo y la clave del mensaje
	 * @param request solicitud en curso
	 * @return respuesta 400 {@code VALIDACION} con un error en el campo
	 */
	@ExceptionHandler(CampoInvalidoException.class)
	public ResponseEntity<Object> manejarCampoInvalido(CampoInvalidoException ex, WebRequest request) {
		ErrorCampo error = new ErrorCampo(ex.getCampo(), mensajes.obtener(ex.getClaveMensaje(), ex.getArgumentos()));
		return responder(CodigoError.VALIDACION, null, List.of(error), request);
	}

	// ------------------------------------------------------------------ no encontrado (404)

	/**
	 * Recurso de dominio inexistente.
	 *
	 * @param ex excepción con el recurso buscado
	 * @param request solicitud en curso
	 * @return respuesta 404 {@code NO_ENCONTRADO}
	 */
	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ResponseEntity<Object> manejarNoEncontrado(RecursoNoEncontradoException ex, WebRequest request) {
		String detalle = mensajes.obtener("error.no_encontrado.detalle_recurso",
				mensajes.obtener(ex.getClaveRecurso()), ex.getIdentificador());
		return responder(CodigoError.NO_ENCONTRADO, detalle, List.of(), request);
	}

	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return responder(CodigoError.NO_ENCONTRADO, null, List.of(), request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return responder(CodigoError.METODO_NO_PERMITIDO, null, List.of(), request);
	}

	// ------------------------------------------------------------------ conflicto (409)

	/**
	 * Conflicto de unicidad detectado por la aplicación.
	 *
	 * @param ex excepción con la clave del detalle
	 * @param request solicitud en curso
	 * @return respuesta 409 {@code CONFLICTO_UNICIDAD}
	 */
	@ExceptionHandler(ConflictoException.class)
	public ResponseEntity<Object> manejarConflicto(ConflictoException ex, WebRequest request) {
		String detalle = mensajes.obtener(ex.getClaveMensaje(), ex.getArgumentos());
		return responder(CodigoError.CONFLICTO_UNICIDAD, detalle, List.of(), request);
	}

	/**
	 * Operación rechazada por una regla de la aplicación (credenciales, bloqueo, último administrador...).
	 *
	 * @param ex excepción con el código y la clave del detalle
	 * @param request solicitud en curso
	 * @return respuesta con el estado HTTP del código
	 */
	@ExceptionHandler(OperacionRechazadaException.class)
	public ResponseEntity<Object> manejarOperacionRechazada(OperacionRechazadaException ex, WebRequest request) {
		String detalle = mensajes.obtener(ex.getClaveMensaje(), ex.getArgumentos());
		return responder(ex.getCodigo(), detalle, List.of(), request);
	}

	// ------------------------------------------------------------------ base de datos

	/**
	 * Errores de acceso a datos, clasificados por el SQLSTATE de PostgreSQL.
	 *
	 * @param ex excepción de Spring Data / JDBC
	 * @param request solicitud en curso
	 * @return 409, 422 o 500 según el SQLSTATE
	 */
	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<Object> manejarAccesoDatos(DataAccessException ex, WebRequest request) {
		ErrorBaseDatos error = ErrorBaseDatos.desde(ex).orElse(null);
		String sqlState = error == null ? "" : error.sqlState();
		return switch (sqlState) {
			case ErrorBaseDatos.UNICIDAD ->
				responderBaseDatos(CodigoError.CONFLICTO_UNICIDAD, null, error, request);
			case ErrorBaseDatos.CLAVE_FORANEA ->
				responderBaseDatos(CodigoError.CONFLICTO_REFERENCIA, null, error, request);
			case ErrorBaseDatos.CHECK ->
				responderBaseDatos(CodigoError.REGLA_INTEGRIDAD,
						mensajes.obtener(CodigoError.REGLA_INTEGRIDAD.claveDetalle(), error.restriccion()),
						error, request);
			case ErrorBaseDatos.TRIGGER_NEGOCIO ->
				// El texto del RAISE EXCEPTION ya describe la regla violada (RN-12, RN-25, ...).
				responderBaseDatos(CodigoError.REGLA_NEGOCIO, error.mensaje(), error, request);
			default -> errorInterno(ex, request);
		};
	}

	// ------------------------------------------------------------------ seguridad (401 / 403)

	/**
	 * Falta de autenticación (también llega desde el filtro de seguridad).
	 *
	 * @param ex excepción de autenticación
	 * @param request solicitud en curso
	 * @return respuesta 401 {@code NO_AUTENTICADO}
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Object> manejarNoAutenticado(AuthenticationException ex, WebRequest request) {
		return responder(CodigoError.NO_AUTENTICADO, null, List.of(), request);
	}

	/**
	 * Acceso denegado; si el usuario es anónimo se responde 401 en lugar de 403.
	 *
	 * @param ex excepción de autorización
	 * @param request solicitud en curso
	 * @return respuesta 401 {@code NO_AUTENTICADO} o 403 {@code ACCESO_DENEGADO}
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Object> manejarAccesoDenegado(AccessDeniedException ex, WebRequest request) {
		var autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || trustResolver.isAnonymous(autenticacion)) {
			return responder(CodigoError.NO_AUTENTICADO, null, List.of(), request);
		}
		return responder(CodigoError.ACCESO_DENEGADO, null, List.of(), request);
	}

	// ------------------------------------------------------------------ resto

	/**
	 * Cualquier otra excepción: 500 sin exponer detalles internos.
	 *
	 * @param ex excepción no prevista
	 * @param request solicitud en curso
	 * @return respuesta 500 {@code ERROR_INTERNO}
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> manejarNoPrevista(Exception ex, WebRequest request) {
		return errorInterno(ex, request);
	}

	/**
	 * Excepciones de Spring MVC no sobrescritas arriba (cuerpo ilegible, tipo de medio, etc.):
	 * conserva su estado HTTP y agrega título traducido, código y lista de errores.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (body instanceof ProblemDetail problema && problema.getProperties() != null
				&& problema.getProperties().containsKey(PROPIEDAD_CODIGO)) {
			return super.handleExceptionInternal(ex, body, headers, statusCode, request);
		}
		CodigoError codigo = statusCode.is5xxServerError() ? CodigoError.ERROR_INTERNO : CodigoError.SOLICITUD_INVALIDA;
		ProblemDetail problema = crearProblema(codigo, null, List.of(), request);
		problema.setStatus(statusCode.value());
		return super.handleExceptionInternal(ex, problema, headers, statusCode, request);
	}

	// ------------------------------------------------------------------ utilidades

	private ResponseEntity<Object> errorInterno(Exception ex, WebRequest request) {
		LOG.error("Error no previsto en {}", descripcion(request), ex);
		return responder(CodigoError.ERROR_INTERNO, null, List.of(), request);
	}

	private ResponseEntity<Object> responderBaseDatos(CodigoError codigo, String detalle, ErrorBaseDatos error,
			WebRequest request) {
		ProblemDetail problema = crearProblema(codigo, detalle, List.of(), request);
		if (error.restriccion() != null) {
			problema.setProperty(PROPIEDAD_RESTRICCION, error.restriccion());
		}
		LOG.debug("Error de base de datos {} en {}: {}", error.sqlState(), descripcion(request), error.mensaje());
		return ResponseEntity.status(codigo.getEstado()).body(problema);
	}

	private ResponseEntity<Object> responder(CodigoError codigo, String detalle, List<ErrorCampo> errores,
			WebRequest request) {
		return ResponseEntity.status(codigo.getEstado()).body(crearProblema(codigo, detalle, errores, request));
	}

	private ProblemDetail crearProblema(CodigoError codigo, String detalle, List<ErrorCampo> errores,
			WebRequest request) {
		ProblemDetail problema = ProblemDetail.forStatus(codigo.getEstado());
		problema.setTitle(mensajes.obtener(codigo.claveTitulo()));
		problema.setDetail(detalle != null ? detalle : mensajes.obtener(codigo.claveDetalle()));
		problema.setProperty(PROPIEDAD_CODIGO, codigo.name());
		problema.setProperty(PROPIEDAD_ERRORES, errores);
		if (request instanceof ServletWebRequest servlet) {
			problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
		}
		return problema;
	}

	private static String descripcion(WebRequest request) {
		if (request instanceof ServletWebRequest servlet) {
			HttpServletRequest http = servlet.getRequest();
			return http.getMethod() + " " + http.getRequestURI();
		}
		return request.getDescription(false);
	}

	private static String ultimoNodo(String ruta) {
		int punto = ruta.lastIndexOf('.');
		return punto >= 0 ? ruta.substring(punto + 1) : ruta;
	}

}
