/**
 * Nombre: ServicioTokenTests.java
 * Descripción: Pruebas unitarias del emisor de JWT: claims, vigencia de 30 minutos y firma HS256.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Emisión de tokens (RF-SEG-001, RNF-SEG-006) con el codificador real de Nimbus y una clave generada en
 * cada ejecución; no hay secretos escritos en el código.
 */
class ServicioTokenTests {

	private static final Instant AHORA = Instant.parse("2026-10-20T15:00:00Z");

	private SecretKey clave;
	private ServicioToken servicio;

	@BeforeEach
	void crearServicio() {
		clave = claveAleatoria();
		PropiedadesSeguridad propiedades = new PropiedadesSeguridad("x".repeat(32), Duration.ofMinutes(30),
				"http://localhost:5173", 5, Duration.ofMinutes(15));
		servicio = new ServicioToken(NimbusJwtEncoder.withSecretKey(clave).algorithm(MacAlgorithm.HS256).build(),
				propiedades, Clock.fixed(AHORA, ZoneId.of("America/Bogota")));
	}

	@Test
	@DisplayName("El token lleva emisor, id del usuario como sub, nombre de usuario y permisos")
	void claims() {
		ServicioToken.TokenEmitido emitido = servicio.emitir(4L, "jefe", List.of("CATALOGO_CONSULTAR", "CATALOGO_EDITAR"));

		Jwt jwt = decodificador(clave).decode(emitido.valor());
		assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo(ServicioToken.EMISOR);
		assertThat(jwt.getSubject()).isEqualTo("4");
		assertThat(jwt.getClaimAsString(ConfiguracionJwt.CLAIM_USUARIO)).isEqualTo("jefe");
		assertThat(jwt.getClaimAsStringList(ConfiguracionJwt.CLAIM_PERMISOS))
				.containsExactly("CATALOGO_CONSULTAR", "CATALOGO_EDITAR");
	}

	@Test
	@DisplayName("[CA-RNF-SEG-006-2] El token vence 30 minutos después de emitido y se informa la vigencia en segundos")
	void vigenciaDeTreintaMinutos() {
		ServicioToken.TokenEmitido emitido = servicio.emitir(4L, "jefe", List.of());

		assertThat(emitido.vigenciaSegundos()).isEqualTo(1800);
		Jwt jwt = decodificador(clave).decode(emitido.valor());
		assertThat(jwt.getIssuedAt()).isEqualTo(AHORA);
		assertThat(jwt.getExpiresAt()).isEqualTo(AHORA.plus(Duration.ofMinutes(30)));
	}

	@Test
	@DisplayName("Un token firmado con otra clave es rechazado")
	void otraClaveRechaza() {
		String valor = servicio.emitir(4L, "jefe", List.of()).valor();

		assertThatThrownBy(() -> decodificador(claveAleatoria()).decode(valor)).isInstanceOf(JwtException.class);
	}

	private static NimbusJwtDecoder decodificador(SecretKey clave) {
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
		// El token se emitió con un reloj fijo; el vencimiento contra el reloj real se prueba en integración.
		decodificador.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
		return decodificador;
	}

	private static SecretKey claveAleatoria() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

}
