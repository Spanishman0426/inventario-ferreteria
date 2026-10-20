/**
 * Nombre: TestInventarioApplication.java
 * Descripción: Arranca la aplicación localmente contra un PostgreSQL de Testcontainers con datos de prueba.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario;

import org.springframework.boot.SpringApplication;

/**
 * Ejecuta la aplicación con el perfil test y una base efímera en Docker; útil para probar la API
 * sin instalar PostgreSQL ({@code ./mvnw spring-boot:test-run}).
 */
public final class TestInventarioApplication {

	private TestInventarioApplication() {
		// Clase de arranque.
	}

	/**
	 * Arranca la aplicación con el contenedor de PostgreSQL.
	 *
	 * @param args argumentos de línea de comandos
	 */
	public static void main(String[] args) {
		SpringApplication.from(InventarioApplication::main)
				.with(TestcontainersConfiguration.class)
				.withAdditionalProfiles("test")
				.run(args);
	}

}
