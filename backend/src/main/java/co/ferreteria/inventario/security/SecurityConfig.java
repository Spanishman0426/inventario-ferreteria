/**
 * Nombre: SecurityConfig.java
 * Descripción: Cadena de filtros de Spring Security: API sin estado con JWT, permisos por método,
 *              CORS restringido al frontend y cabeceras de seguridad.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de seguridad de la API (RF-SEG-001, RNF-SEG-001, RNF-SEG-002, RNF-SEG-005, RNF-SEG-006).
 *
 * <ul>
 *   <li>Sin sesión de servidor: cada solicitud trae un JWT en {@code Authorization: Bearer ...}, que
 *       el soporte de resource server valida (firma HS256 y vencimiento de 30 minutos).</li>
 *   <li><b>CSRF deshabilitado</b>: un ataque CSRF aprovecha que el navegador adjunta solo las
 *       cookies a una solicitud que inicia otro sitio. Aquí el token viaja en la cabecera
 *       {@code Authorization}, que el navegador nunca agrega por su cuenta, y la API no usa cookies
 *       de sesión; un sitio externo no puede enviar una solicitud autenticada.</li>
 *   <li>Autorización: rutas públicas mínimas (login, salud, OpenAPI); el resto exige autenticación y
 *       cada endpoint declara su permiso con {@code @PreAuthorize} sobre los códigos de la tabla
 *       {@code permiso} (matriz 2.2).</li>
 *   <li>CORS: solo el origen de {@code FRONTEND_URL}.</li>
 *   <li>Cabeceras: HSTS de un año con subdominios (solo en solicitudes HTTPS, en producción las que
 *       llegan por el proxy TLS), {@code X-Content-Type-Options: nosniff} y
 *       {@code X-Frame-Options: DENY}.</li>
 *   <li>401 y 403 se responden en formato Problem Details con {@link ManejadorErroresSeguridad}.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class SecurityConfig {

	/** Duración de la cabecera HSTS: un año. */
	public static final long HSTS_SEGUNDOS = 31_536_000L;

	/** Ruta del inicio de sesión. */
	public static final String RUTA_LOGIN = "/api/v1/auth/login";

	/** Rutas accesibles sin autenticación. */
	private static final String[] RUTAS_PUBLICAS = {
		"/actuator/health", "/actuator/health/**", "/actuator/info",
		"/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
		"/error"
	};

	/** Validez en caché de la respuesta a la consulta previa de CORS: una hora. */
	private static final long CORS_MAX_AGE_SEGUNDOS = 3600L;

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
	 * @param convertidorJwt convierte la claim de permisos en autoridades
	 * @return cadena de filtros configurada
	 * @throws Exception si la configuración falla
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ManejadorErroresSeguridad manejadorErrores,
			JwtAuthenticationConverter convertidorJwt) throws Exception {
		http
			// Motivo en el Javadoc de la clase: el token no viaja en cookies.
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(cabeceras -> cabeceras
					.httpStrictTransportSecurity(hsts -> hsts
							.includeSubDomains(true)
							.maxAgeInSeconds(HSTS_SEGUNDOS))
					.contentTypeOptions(Customizer.withDefaults())
					.frameOptions(marcos -> marcos.deny()))
			.authorizeHttpRequests(rutas -> rutas
					.requestMatchers(HttpMethod.POST, RUTA_LOGIN).permitAll()
					.requestMatchers(RUTAS_PUBLICAS).permitAll()
					.anyRequest().authenticated())
			.oauth2ResourceServer(recurso -> recurso
					.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt))
					.authenticationEntryPoint(manejadorErrores)
					.accessDeniedHandler(manejadorErrores))
			.exceptionHandling(errores -> errores
					.authenticationEntryPoint(manejadorErrores)
					.accessDeniedHandler(manejadorErrores));
		return http.build();
	}

	/**
	 * CORS: admite solo el origen del frontend (variable {@code FRONTEND_URL}).
	 *
	 * @param propiedades propiedades de seguridad
	 * @return configuración CORS para todas las rutas
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(PropiedadesSeguridad propiedades) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(List.of(propiedades.frontendUrl()));
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
				HttpHeaders.ACCEPT, HttpHeaders.ACCEPT_LANGUAGE));
		cors.setAllowCredentials(false);
		cors.setMaxAge(CORS_MAX_AGE_SEGUNDOS);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", cors);
		return fuente;
	}

}
