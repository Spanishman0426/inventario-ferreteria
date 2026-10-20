/**
 * Nombre: Mensajes.java
 * Descripción: Acceso a los textos traducidos de messages_*.properties según el idioma de la solicitud.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import co.ferreteria.inventario.config.LocaleConfig;

/**
 * Envoltorio de {@link MessageSource} que resuelve el idioma de la solicitud en curso.
 *
 * <p>Usa el {@link LocaleResolver} de la aplicación también fuera del {@code DispatcherServlet}
 * (por ejemplo, en los filtros de seguridad), para que un 401 respete {@code Accept-Language}.
 * Fuera de una solicitud usa el idioma del hilo si se fijó, o español.</p>
 */
@Component
public class Mensajes {

	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;

	/**
	 * Crea el componente.
	 *
	 * @param messageSource fuente de mensajes de la aplicación
	 * @param localeResolver resolutor de idioma de la aplicación
	 */
	public Mensajes(MessageSource messageSource, LocaleResolver localeResolver) {
		this.messageSource = messageSource;
		this.localeResolver = localeResolver;
	}

	/**
	 * Devuelve el texto de una clave en el idioma actual.
	 *
	 * @param clave clave del archivo de mensajes
	 * @param argumentos valores para los marcadores {0}, {1}, ...
	 * @return texto traducido, o la clave si no existe
	 */
	public String obtener(String clave, Object... argumentos) {
		return messageSource.getMessage(clave, argumentos, clave, idiomaActual());
	}

	/**
	 * Determina el idioma de la operación en curso.
	 *
	 * @return idioma de la solicitud HTTP, del hilo o español por defecto
	 */
	public Locale idiomaActual() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
			return localeResolver.resolveLocale(atributos.getRequest());
		}
		if (LocaleContextHolder.getLocaleContext() != null) {
			return LocaleContextHolder.getLocale();
		}
		return LocaleConfig.ESPANOL;
	}

}
