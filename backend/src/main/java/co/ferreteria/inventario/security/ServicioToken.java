/**
 * Nombre: ServicioToken.java
 * Descripción: Emite el token JWT de acceso con el id, el nombre de usuario y los permisos.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emisión de tokens de acceso (RF-SEG-001, RNF-SEG-006).
 */
@Service
public class ServicioToken {

	/** Emisor de los tokens. */
	public static final String EMISOR = "inventario-ferreteria";

	private final JwtEncoder encoder;
	private final PropiedadesSeguridad propiedades;
	private final Clock reloj;

	/**
	 * Crea el servicio.
	 *
	 * @param encoder codificador JWT
	 * @param propiedades vigencia del token
	 * @param reloj reloj de la aplicación
	 */
	public ServicioToken(JwtEncoder encoder, PropiedadesSeguridad propiedades, Clock reloj) {
		this.encoder = encoder;
		this.propiedades = propiedades;
		this.reloj = reloj;
	}

	/**
	 * Emite un token firmado.
	 *
	 * @param usuarioId id del usuario (claim {@code sub})
	 * @param usuario nombre de usuario
	 * @param permisos códigos de permiso (claim {@code permisos})
	 * @return token firmado y su vigencia
	 */
	public TokenEmitido emitir(long usuarioId, String usuario, Collection<String> permisos) {
		Instant ahora = reloj.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(EMISOR)
				.subject(Long.toString(usuarioId))
				.issuedAt(ahora)
				.expiresAt(ahora.plus(propiedades.jwtVigencia()))
				.claim(ConfiguracionJwt.CLAIM_USUARIO, usuario)
				.claim(ConfiguracionJwt.CLAIM_PERMISOS, List.copyOf(permisos))
				.build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		String valor = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
		return new TokenEmitido(valor, propiedades.jwtVigencia().toSeconds());
	}

	/**
	 * Token emitido.
	 *
	 * @param valor token firmado
	 * @param vigenciaSegundos segundos hasta su vencimiento
	 */
	public record TokenEmitido(String valor, long vigenciaSegundos) {
	}

}
