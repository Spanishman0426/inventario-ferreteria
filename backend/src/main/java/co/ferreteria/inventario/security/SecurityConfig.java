/**
 * Nombre: SecurityConfig.java
 * Descripción: Cadena de filtros de Spring Security: API sin estado, rutas públicas mínimas y HSTS.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad base (RNF-SEG-001, RNF-SEG-005, RNF-SEG-006).
 *
 * <ul>
 *   <li>Sin sesión de servidor: la autenticación con JWT llega en RF-SEG-001.</li>
 *   <li>CSRF desactivado: el token viaja en la cabecera {@code Authorization}, no en cookies,
 *       así que un sitio externo no puede reutilizarlo.</li>
 *   <li>Rutas públicas: salud del servicio y documentación OpenAPI; todo lo demás exige
 *       autenticación y responde 401 en formato Problem Details.</li>
 *   <li>HSTS de un año con subdominios; Spring Security lo envía solo en solicitudes HTTPS
 *       (en producción, las que llegan por el proxy TLS).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	/** Duración de la cabecera HSTS: un año. */
	public static final long HSTS_SEGUNDOS = 31_536_000L;

	/** Rutas accesibles sin autenticación. */
	private static final String[] RUTAS_PUBLICAS = {
		"/actuator/health", "/actuator/health/**", "/actuator/info",
		"/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
		"/error"
	};

	/**
	 * Constructor usado por Spring.
	 */
	public SecurityConfig() {
		// Sin estado.
	}

	/**
	 * Cadena de filtros de seguridad de la API.
	 *
	 * @param http constructor de seguridad HTTP
	 * @param manejadorErrores respuesta 401/403 en formato Problem Details
	 * @return cadena de filtros configurada
	 * @throws Exception si la configuración falla
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ManejadorErroresSeguridad manejadorErrores)
			throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(cabeceras -> cabeceras.httpStrictTransportSecurity(hsts -> hsts
					.includeSubDomains(true)
					.maxAgeInSeconds(HSTS_SEGUNDOS)))
			.authorizeHttpRequests(rutas -> rutas
					.requestMatchers(RUTAS_PUBLICAS).permitAll()
					.anyRequest().authenticated())
			.exceptionHandling(errores -> errores
					.authenticationEntryPoint(manejadorErrores)
					.accessDeniedHandler(manejadorErrores));
		return http.build();
	}

}
