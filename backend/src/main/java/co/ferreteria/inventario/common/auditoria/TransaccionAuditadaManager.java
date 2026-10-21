/**
 * Nombre: TransaccionAuditadaManager.java
 * Descripción: Gestor de transacciones JPA que envía a PostgreSQL el usuario y la IP de la auditoría
 *              al iniciar cada transacción de escritura.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import java.io.Serial;
import java.sql.PreparedStatement;

import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Mecanismo único y transparente que conecta la aplicación con la auditoría de la base (RN-28).
 *
 * <p>El trigger {@code fn_auditar} lee {@code current_setting('app.usuario_id')} y
 * {@code current_setting('app.ip')}. Este gestor, registrado como el {@code transactionManager} de la
 * aplicación, ejecuta al comenzar cada transacción que no sea de solo lectura:</p>
 * <pre>SELECT set_config('app.usuario_id', ?, true), set_config('app.ip', ?, true)</pre>
 * <p>con los datos de {@link ContextoAuditoria}. Así lo reciben los servicios con
 * {@code @Transactional}, los repositorios de Spring Data, las transacciones {@code REQUIRES_NEW} y el
 * {@code JdbcTemplate} usado dentro de una transacción, sin que ningún servicio tenga que recordarlo.
 * El tercer argumento {@code true} limita los valores a la transacción, de modo que una conexión
 * devuelta al pool no conserva el usuario anterior.</p>
 */
public class TransaccionAuditadaManager extends JpaTransactionManager {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String SQL_CONTEXTO =
			"SELECT set_config('app.usuario_id', ?, true), set_config('app.ip', ?, true)";

	/** Origen del usuario y la IP; no se serializa con el gestor. */
	private final transient ContextoAuditoria contexto;

	/**
	 * Crea el gestor.
	 *
	 * @param emf fábrica de {@link EntityManager} de la aplicación
	 * @param contexto origen del usuario y la IP de la auditoría
	 */
	public TransaccionAuditadaManager(EntityManagerFactory emf, ContextoAuditoria contexto) {
		super(emf);
		this.contexto = contexto;
	}

	/**
	 * Inicia la transacción JPA y, si es de escritura, fija el contexto de auditoría en la conexión.
	 *
	 * @param transaction objeto de transacción de {@link JpaTransactionManager}
	 * @param definition definición de la transacción
	 */
	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		super.doBegin(transaction, definition);
		if (definition.isReadOnly()) {
			return;
		}
		try {
			fijarContexto();
		} catch (RuntimeException e) {
			deshacer();
			doCleanupAfterCompletion(transaction);
			throw new CannotCreateTransactionException("No se pudo fijar el contexto de auditoría", e);
		}
	}

	private void fijarContexto() {
		String usuarioId = contexto.usuarioId().map(String::valueOf).orElse("");
		String ip = contexto.ip().orElse("");
		entityManagerActual().unwrap(Session.class).doWork(conexion -> {
			try (PreparedStatement sentencia = conexion.prepareStatement(SQL_CONTEXTO)) {
				sentencia.setString(1, usuarioId);
				sentencia.setString(2, ip);
				sentencia.execute();
			}
		});
	}

	private void deshacer() {
		EntityManager em = entityManagerActual();
		if (em.getTransaction().isActive()) {
			em.getTransaction().rollback();
		}
	}

	private EntityManager entityManagerActual() {
		EntityManagerHolder holder =
				(EntityManagerHolder) TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
		if (holder == null) {
			throw new IllegalStateException("No hay EntityManager ligado a la transacción");
		}
		return holder.getEntityManager();
	}

}
