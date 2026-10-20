/**
 * Nombre: InventarioApplication.java
 * Descripción: Punto de entrada del backend del Sistema de Inventario de Ferretería.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación Spring Boot del sistema de inventario.
 *
 * <p>Fija la zona horaria America/Bogota antes de arrancar (RNF-INT-003), de modo que la JVM,
 * Hibernate y la sesión JDBC con PostgreSQL interpreten fechas en hora de Colombia.</p>
 */
@SpringBootApplication
public class InventarioApplication {

	/** Zona horaria de la aplicación (RNF-INT-003). */
	public static final String ZONA_HORARIA = "America/Bogota";

	/**
	 * Constructor usado por Spring.
	 */
	public InventarioApplication() {
		// Sin estado.
	}

	/**
	 * Arranca la aplicación.
	 *
	 * @param args argumentos de línea de comandos
	 */
	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
		SpringApplication.run(InventarioApplication.class, args);
	}

}
