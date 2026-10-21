/**
 * Nombre: RelojConfig.java
 * Descripción: Reloj de la aplicación en hora de Colombia; las pruebas pueden reemplazarlo.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.ferreteria.inventario.InventarioApplication;

/**
 * Expone un {@link Clock} único para que las reglas con tiempo (vencimiento del token, bloqueo de 15
 * minutos) no dependan del reloj del sistema directamente (RNF-INT-003).
 */
@Configuration(proxyBeanMethods = false)
public class RelojConfig {

	/**
	 * Constructor usado por Spring.
	 */
	public RelojConfig() {
		// Sin estado.
	}

	/**
	 * Reloj del sistema en la zona America/Bogota.
	 *
	 * @return reloj de la aplicación
	 */
	@Bean
	public Clock reloj() {
		return Clock.system(ZoneId.of(InventarioApplication.ZONA_HORARIA));
	}

}
