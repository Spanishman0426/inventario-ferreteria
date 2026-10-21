/**
 * Nombre: ContextoAuditoria.java
 * Descripción: Determina el usuario y la IP que firman la auditoría de la operación en curso.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Fuente única del usuario y la IP de la auditoría (RF-SEG-002, RNF-SEG-003).
 *
 * <p>El usuario sale del token JWT autenticado (claim {@code sub} = id del usuario) y la IP de la
 * solicitud HTTP en curso; en producción {@code server.forward-headers-strategy=framework} hace que
 * {@code getRemoteAddr()} sea la IP real del cliente detrás del proxy.</p>
 *
 * <p>Cuando todavía no hay token (inicio de sesión) o no hay solicitud (tareas de arranque), el código
 * que lo sabe fija el usuario con {@link #ejecutarComo(Long, Supplier)}.</p>
 */
@Component
public class ContextoAuditoria {

	/** Usuario fijado explícitamente para el hilo actual; tiene prioridad sobre el token. */
	private static final ThreadLocal<Long> USUARIO_FIJADO = new ThreadLocal<>();

	/**
	 * Constructor usado por Spring.
	 */
	public ContextoAuditoria() {
		// Sin estado propio: todo se lee del hilo actual.
	}

	/**
	 * Id del usuario que realiza la operación.
	 *
	 * @return id fijado con {@link #ejecutarComo(Long, Supplier)}, o el del token autenticado
	 */
	public Optional<Long> usuarioId() {
		Long fijado = USUARIO_FIJADO.get();
		if (fijado != null) {
			return Optional.of(fijado);
		}
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || !autenticacion.isAuthenticated()
				|| autenticacion instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.valueOf(autenticacion.getName()));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	/**
	 * IP del cliente de la solicitud HTTP en curso.
	 *
	 * @return IP remota, o vacío fuera de una solicitud
	 */
	public Optional<String> ip() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
			return Optional.ofNullable(atributos.getRequest().getRemoteAddr());
		}
		return Optional.empty();
	}

	/**
	 * Ejecuta una operación firmada por un usuario concreto, aunque no haya token.
	 *
	 * <p>La transacción debe <em>empezar</em> dentro de {@code operacion}, porque el usuario se envía
	 * a PostgreSQL al iniciar cada transacción de escritura.</p>
	 *
	 * @param usuarioId usuario que firma la auditoría; {@code null} deja el comportamiento normal
	 * @param operacion operación a ejecutar
	 * @param <T> tipo del resultado
	 * @return resultado de la operación
	 */
	public <T> T ejecutarComo(Long usuarioId, Supplier<T> operacion) {
		Long anterior = USUARIO_FIJADO.get();
		if (usuarioId != null) {
			USUARIO_FIJADO.set(usuarioId);
		}
		try {
			return operacion.get();
		} finally {
			if (anterior == null) {
				USUARIO_FIJADO.remove();
			} else {
				USUARIO_FIJADO.set(anterior);
			}
		}
	}

}
