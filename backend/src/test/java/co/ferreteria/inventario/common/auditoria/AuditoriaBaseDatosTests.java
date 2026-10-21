/**
 * Nombre: AuditoriaBaseDatosTests.java
 * Descripción: Pruebas de la migración V3 (fn_auditar) y del contexto de auditoría que fija
 *              TransaccionAuditadaManager en cada transacción.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;

/**
 * Comportamiento de la auditoría en la base (RN-28, RF-SEG-002). Cada prueba corre en una transacción
 * que se revierte al final, así que no deja cambios.
 */
class AuditoriaBaseDatosTests extends PruebaIntegracionApi {

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ContextoAuditoria contexto;

	@Test
	@DisplayName("[CA-RF-SEG-002-1] V3: actualizar solo intentos_fallidos, bloqueado_hasta y ultimo_acceso no genera auditoría")
	void soloColumnasDeControlNoSeAuditan() {
		List<String> acciones = enTransaccionRevertida("""
				UPDATE usuario SET intentos_fallidos = 3, bloqueado_hasta = now() + interval '15 minutes',
				                   ultimo_acceso = now() WHERE usuario = 'jefe'
				""");
		assertThat(acciones).isEmpty();
	}

	@Test
	@DisplayName("V3: si junto con el control cambia otra columna (activo), sí se audita como MODIFICAR")
	void controlMasOtraColumnaSeAudita() {
		List<String> acciones = enTransaccionRevertida(
				"UPDATE usuario SET intentos_fallidos = 1, activo = false WHERE usuario = 'jefe'");
		assertThat(acciones).containsExactly("MODIFICAR");
	}

	@Test
	@DisplayName("V3: un cambio solo de contraseña se audita como MODIFICAR (antes no quedaba registro)")
	void cambioDeContrasenaSeAudita() {
		List<String> acciones = enTransaccionRevertida(
				"UPDATE usuario SET password_hash = '$2a$12$otro.hash.de.prueba' WHERE usuario = 'jefe'");
		assertThat(acciones).containsExactly("MODIFICAR");
	}

	@Test
	@DisplayName("V3: las demás tablas se auditan igual que antes")
	void otrasTablasSinCambios() {
		List<String> acciones = enTransaccionRevertida("UPDATE marca SET nombre = nombre || ' X' WHERE id = 1");
		assertThat(acciones).containsExactly("MODIFICAR");
	}

	@Test
	@DisplayName("Toda transacción de escritura recibe app.usuario_id sin que el código lo pida; la de solo lectura no")
	void contextoSeFijaEnTransaccionesDeEscritura() {
		TransactionTemplate escritura = new TransactionTemplate(transactionManager);
		String enEscritura = contexto.ejecutarComo(5L, () -> escritura.execute(estado -> {
			estado.setRollbackOnly();
			return jdbc.queryForObject("SELECT current_setting('app.usuario_id', true)", String.class);
		}));
		assertThat(enEscritura).isEqualTo("5");

		TransactionTemplate lectura = new TransactionTemplate(transactionManager);
		lectura.setReadOnly(true);
		String enLectura = contexto.ejecutarComo(5L, () -> lectura.execute(estado ->
				jdbc.queryForObject("SELECT coalesce(current_setting('app.usuario_id', true), '')", String.class)));
		assertThat(enLectura).isNotEqualTo("5");
	}

	@Test
	@DisplayName("El valor es local a la transacción: la siguiente no hereda el usuario anterior")
	void contextoNoSeFiltraEntreTransacciones() {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		contexto.ejecutarComo(7L, () -> tx.execute(estado -> null));
		String despues = tx.execute(estado ->
				jdbc.queryForObject("SELECT coalesce(current_setting('app.usuario_id', true), '')", String.class));
		assertThat(despues).isEmpty();
	}

	/**
	 * Ejecuta una sentencia firmada por el administrador en una transacción que se revierte y devuelve
	 * las acciones que la sentencia dejó en auditoría.
	 */
	private List<String> enTransaccionRevertida(String sql) {
		long antes = ultimoIdAuditoria();
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		return contexto.ejecutarComo(idUsuario("admin"), () -> tx.execute(estado -> {
			estado.setRollbackOnly();
			jdbc.update(sql);
			return jdbc.queryForList("SELECT accion FROM auditoria WHERE id > ? AND usuario_id = ?", String.class,
					antes, idUsuario("admin"));
		}));
	}

}
