/**
 * Nombre: ConfiguracionJwt.java
 * Descripción: Codificador y decodificador de JWT HS256, conversión de permisos a autoridades y BCrypt.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Componentes criptográficos de la autenticación (RF-SEG-001, RNF-SEG-001).
 *
 * <p>El propio backend emite y valida los tokens con una clave simétrica HS256 tomada de
 * {@code JWT_SECRET}; el soporte de resource server de Spring Security los valida en cada solicitud
 * (firma y vencimiento).</p>
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionJwt {

	/** Claim con los códigos de permiso del usuario. */
	public static final String CLAIM_PERMISOS = "permisos";
	/** Claim con el nombre de usuario. */
	public static final String CLAIM_USUARIO = "usuario";
	/** Fuerza de BCrypt pedida para las contraseñas. */
	public static final int FUERZA_BCRYPT = 12;

	/**
	 * Constructor usado por Spring.
	 */
	public ConfiguracionJwt() {
		// Sin estado.
	}

	/**
	 * Clave HMAC-SHA256 derivada de {@code JWT_SECRET}.
	 *
	 * @param propiedades propiedades de seguridad
	 * @return clave secreta
	 */
	@Bean
	public SecretKey claveJwt(PropiedadesSeguridad propiedades) {
		return new SecretKeySpec(propiedades.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	/**
	 * Codificador que firma los tokens emitidos al iniciar sesión.
	 *
	 * @param clave clave secreta
	 * @return codificador HS256
	 */
	@Bean
	public JwtEncoder jwtEncoder(SecretKey clave) {
		return NimbusJwtEncoder.withSecretKey(clave).algorithm(MacAlgorithm.HS256).build();
	}

	/**
	 * Decodificador que valida firma y vencimiento de cada token recibido.
	 *
	 * @param clave clave secreta
	 * @return decodificador HS256
	 */
	@Bean
	public JwtDecoder jwtDecoder(SecretKey clave) {
		return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/**
	 * Convierte la claim {@code permisos} en autoridades sin prefijo, de modo que
	 * {@code @PreAuthorize("hasAuthority('CATALOGO_EDITAR')")} use los códigos de la tabla permiso.
	 * El nombre de la autenticación es la claim {@code sub} (id del usuario).
	 *
	 * @return convertidor de token a autenticación
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
		autoridades.setAuthoritiesClaimName(CLAIM_PERMISOS);
		autoridades.setAuthorityPrefix("");
		JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
		convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
		return convertidor;
	}

	/**
	 * Hash de contraseñas con BCrypt de fuerza 12 (RNF-SEG-001).
	 *
	 * @return codificador de contraseñas
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(FUERZA_BCRYPT);
	}

}
