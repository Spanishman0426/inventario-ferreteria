/**
 * Nombre: RegistroAuditoria.java
 * Descripción: Escribe en la tabla auditoria los eventos que no nacen de un trigger: inicios de sesión
 *              correctos y fallidos.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra los accesos {@code INICIAR_SESION} y {@code SESION_FALLIDA} con la IP del cliente
 * (RF-SEG-002, RNF-SEG-003).
 *
 * <p>Debe llamarse dentro de la transacción del inicio de sesión, para que el registro y la
 * actualización del usuario se guarden juntos (RN-28).</p>
 */
@Component
public class RegistroAuditoria {

	/** Acción de inicio de sesión correcto. */
	public static final String INICIAR_SESION = "INICIAR_SESION";
	/** Acción de inicio de sesión rechazado. */
	public static final String SESION_FALLIDA = "SESION_FALLIDA";

	private static final String SQL = """
			INSERT INTO auditoria (usuario_id, ip, accion, entidad, entidad_id, valores_despues)
			VALUES (?, ?::inet, ?, 'usuario', ?, jsonb_build_object('usuario', ?::text, 'motivo', ?::text))
			""";

	private final JdbcTemplate jdbc;
	private final ContextoAuditoria contexto;

	/**
	 * Crea el componente.
	 *
	 * @param jdbc acceso JDBC
	 * @param contexto origen de la IP del cliente
	 */
	public RegistroAuditoria(JdbcTemplate jdbc, ContextoAuditoria contexto) {
		this.jdbc = jdbc;
		this.contexto = contexto;
	}

	/**
	 * Registra un inicio de sesión correcto.
	 *
	 * @param usuarioId id del usuario que inició sesión
	 * @param usuario nombre de usuario
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void inicioSesion(long usuarioId, String usuario) {
		insertar(usuarioId, INICIAR_SESION, usuario, null);
	}

	/**
	 * Registra un inicio de sesión rechazado.
	 *
	 * @param usuarioId id del usuario si existe; {@code null} si el nombre no corresponde a nadie
	 * @param usuario nombre de usuario intentado
	 * @param motivo causa interna del rechazo (CREDENCIALES, BLOQUEADA, INACTIVA, INEXISTENTE)
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void sesionFallida(Long usuarioId, String usuario, String motivo) {
		insertar(usuarioId, SESION_FALLIDA, usuario, motivo);
	}

	private void insertar(Long usuarioId, String accion, String usuario, String motivo) {
		jdbc.update(SQL, usuarioId, contexto.ip().orElse(null), accion, usuarioId, usuario, motivo);
	}

}
