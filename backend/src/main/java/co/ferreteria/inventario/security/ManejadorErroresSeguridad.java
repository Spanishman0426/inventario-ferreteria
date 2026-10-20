/**
 * Nombre: ManejadorErroresSeguridad.java
 * Descripción: Envía los errores 401 y 403 de Spring Security al manejador global de errores.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Punto de entrada de autenticación y manejador de acceso denegado.
 *
 * <p>Los errores de los filtros de seguridad ocurren antes de llegar a un controlador; se delegan al
 * {@link HandlerExceptionResolver} de Spring MVC para que el manejador global responda con el mismo
 * formato Problem Details y en el idioma de la solicitud (RNF-SEG-001).</p>
 */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;

	/**
	 * Crea el manejador.
	 *
	 * @param resolver resolutor de excepciones de Spring MVC
	 */
	public ManejadorErroresSeguridad(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Responde 401 cuando falta autenticación.
	 *
	 * @param request solicitud
	 * @param response respuesta
	 * @param authException causa del rechazo
	 */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		resolver.resolveException(request, response, null, authException);
	}

	/**
	 * Responde 403 cuando el usuario autenticado no tiene permiso.
	 *
	 * @param request solicitud
	 * @param response respuesta
	 * @param accessDeniedException causa del rechazo
	 */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) {
		resolver.resolveException(request, response, null, accessDeniedException);
	}

}
