/**
 * Nombre: OpenApiConfig.java
 * Descripción: Metadatos de la especificación OpenAPI publicada por springdoc (RNF-COM-001).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configura título, descripción y esquema de seguridad de la documentación OpenAPI.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	/** Nombre del esquema de seguridad con token JWT (se usa desde RF-SEG-001). */
	public static final String ESQUEMA_JWT = "bearerAuth";

	/**
	 * Constructor usado por Spring.
	 */
	public OpenApiConfig() {
		// Sin estado.
	}

	/**
	 * Documento OpenAPI base, con textos tomados de los archivos de mensajes en español.
	 *
	 * @param messageSource fuente de mensajes
	 * @return definición OpenAPI con información general y esquema Bearer JWT
	 */
	@Bean
	public OpenAPI openApi(MessageSource messageSource) {
		return new OpenAPI()
				.info(new Info()
						.title(messageSource.getMessage("openapi.titulo", null, LocaleConfig.ESPANOL))
						.description(messageSource.getMessage("openapi.descripcion", null, LocaleConfig.ESPANOL))
						.version("v1"))
				.components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}

}
