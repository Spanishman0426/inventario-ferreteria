/**
 * Nombre: LocaleConfig.java
 * Descripción: Configura el idioma desde la cabecera Accept-Language y la fuente de mensajes traducidos.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Idiomas soportados por la API: español (por defecto) e inglés.
 *
 * <p>El idioma se toma de la cabecera {@code Accept-Language}; si falta o pide un idioma no
 * soportado, se usa español.</p>
 */
@Configuration(proxyBeanMethods = false)
public class LocaleConfig {

	/** Español, idioma por defecto. */
	public static final Locale ESPANOL = Locale.of("es");

	/** Inglés, segundo idioma. */
	public static final Locale INGLES = Locale.of("en");

	/**
	 * Constructor usado por Spring.
	 */
	public LocaleConfig() {
		// Sin estado.
	}

	/**
	 * Resolutor de idioma basado en {@code Accept-Language}.
	 *
	 * @return resolutor con español por defecto y español e inglés como idiomas soportados
	 */
	@Bean
	public LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setSupportedLocales(List.of(ESPANOL, INGLES));
		resolver.setDefaultLocale(ESPANOL);
		return resolver;
	}

	/**
	 * Fuente de mensajes con los textos de la API y de Bean Validation.
	 *
	 * <p>Se declara a mano porque la configuración automática de Spring Boot solo se activa si existe
	 * un {@code messages.properties} sin idioma; aquí el archivo por defecto es
	 * {@code messages_es.properties}. Un idioma sin archivo propio cae en español, nunca en el idioma
	 * del sistema operativo. Bean Validation usa este mismo bean para resolver {@code {clave}}.</p>
	 *
	 * @return fuente de mensajes de {@code messages_*} y {@code ValidationMessages_*} en UTF-8
	 */
	@Bean
	public MessageSource messageSource() {
		ResourceBundleMessageSource fuente = new ResourceBundleMessageSource();
		fuente.setBasenames("messages", "ValidationMessages");
		fuente.setDefaultEncoding(StandardCharsets.UTF_8.name());
		fuente.setFallbackToSystemLocale(false);
		fuente.setDefaultLocale(ESPANOL);
		return fuente;
	}

}
