/**
 * Nombre: TestcontainersConfiguration.java
 * Descripción: Contenedor PostgreSQL 18 compartido por las pruebas de integración.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Levanta PostgreSQL 18 (misma versión mayor que la instalación local) y conecta Spring a él
 * con {@link ServiceConnection}. Flyway aplica V1, V2 y, en el perfil test, V100 con los datos
 * de prueba de {@code docs/plan-pruebas-bd.md}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/** Imagen de PostgreSQL usada en las pruebas. */
	public static final String IMAGEN_POSTGRES = "postgres:18";

	/**
	 * Contenedor de PostgreSQL con zona horaria de Colombia.
	 *
	 * @return contenedor listo para registrar como conexión de servicio
	 */
	@Bean
	@ServiceConnection
	public PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse(IMAGEN_POSTGRES))
				.withEnv("TZ", InventarioApplication.ZONA_HORARIA)
				.withEnv("PGTZ", InventarioApplication.ZONA_HORARIA);
	}

}
