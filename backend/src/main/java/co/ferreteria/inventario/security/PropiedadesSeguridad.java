/**
 * Nombre: PropiedadesSeguridad.java
 * Descripción: Propiedades de seguridad (clave y vigencia del JWT, origen del frontend y bloqueo).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Valores de {@code app.seguridad.*}. Los secretos llegan solo por variables de entorno
 * ({@code JWT_SECRET}, {@code FRONTEND_URL}); si faltan, la aplicación no arranca.
 *
 * @param jwtSecret clave HMAC-SHA256 para firmar los tokens; al menos 32 caracteres (256 bits)
 * @param jwtVigencia vigencia del token de acceso (RNF-SEG-006: 30 minutos)
 * @param frontendUrl único origen admitido por CORS
 * @param intentosMaximos intentos fallidos que bloquean la cuenta (RF-SEG-001: 5)
 * @param duracionBloqueo duración del bloqueo (RF-SEG-001: 15 minutos)
 */
@Validated
@ConfigurationProperties("app.seguridad")
public record PropiedadesSeguridad(
		@NotBlank @Size(min = 32) String jwtSecret,
		@NotNull @DefaultValue("30m") Duration jwtVigencia,
		@NotBlank String frontendUrl,
		@Min(1) @DefaultValue("5") int intentosMaximos,
		@NotNull @DefaultValue("15m") Duration duracionBloqueo) {

	/**
	 * Representación sin la clave, para que nunca aparezca en registros.
	 *
	 * @return texto con los valores no secretos
	 */
	@Override
	public String toString() {
		return "PropiedadesSeguridad[jwtSecret=***, jwtVigencia=" + jwtVigencia + ", frontendUrl=" + frontendUrl
				+ ", intentosMaximos=" + intentosMaximos + ", duracionBloqueo=" + duracionBloqueo + "]";
	}

}
