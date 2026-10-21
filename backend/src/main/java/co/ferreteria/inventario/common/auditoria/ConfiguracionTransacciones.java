/**
 * Nombre: ConfiguracionTransacciones.java
 * Descripción: Registra TransaccionAuditadaManager como gestor de transacciones de la aplicación.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

/**
 * Reemplaza el {@code transactionManager} que Spring Boot crearía por defecto (este bean hace que la
 * configuración automática de JPA se retire) por uno que fija el contexto de auditoría (RN-28).
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionTransacciones {

	/**
	 * Constructor usado por Spring.
	 */
	public ConfiguracionTransacciones() {
		// Sin estado.
	}

	/**
	 * Gestor de transacciones de la aplicación.
	 *
	 * @param emf fábrica de EntityManager
	 * @param contexto origen del usuario y la IP de la auditoría
	 * @param personalizadores ajustes estándar de Spring Boot (tiempo límite, etc.)
	 * @return gestor de transacciones JPA con contexto de auditoría
	 */
	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory emf, ContextoAuditoria contexto,
			ObjectProvider<TransactionManagerCustomizers> personalizadores) {
		TransaccionAuditadaManager gestor = new TransaccionAuditadaManager(emf, contexto);
		personalizadores.ifAvailable(p -> p.customize(gestor));
		return gestor;
	}

}
