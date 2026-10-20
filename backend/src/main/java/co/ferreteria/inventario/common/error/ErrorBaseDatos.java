/**
 * Nombre: ErrorBaseDatos.java
 * Descripción: Extrae SQLSTATE, mensaje y restricción de una excepción originada en PostgreSQL.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.error;

import java.sql.SQLException;
import java.util.Optional;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

/**
 * Datos útiles de un error de PostgreSQL, obtenidos recorriendo la cadena de causas.
 *
 * <p>Spring envuelve el error del controlador JDBC en distintas excepciones
 * ({@code DataIntegrityViolationException}, {@code JpaSystemException},
 * {@code UncategorizedSQLException}); el SQLSTATE siempre está en el {@link SQLException} interno.</p>
 *
 * @param sqlState código SQLSTATE (por ejemplo 23514 o P0001)
 * @param mensaje mensaje primario del servidor (en P0001, el texto del {@code RAISE EXCEPTION})
 * @param restriccion nombre de la restricción violada, si PostgreSQL lo informa
 */
public record ErrorBaseDatos(String sqlState, String mensaje, String restriccion) {

	/** Violación de unicidad. */
	public static final String UNICIDAD = "23505";
	/** Violación de clave foránea. */
	public static final String CLAVE_FORANEA = "23503";
	/** Violación de restricción CHECK. */
	public static final String CHECK = "23514";
	/** Excepción lanzada por un trigger o función con {@code RAISE EXCEPTION}. */
	public static final String TRIGGER_NEGOCIO = "P0001";

	/**
	 * Busca en la cadena de causas el primer {@link SQLException} con SQLSTATE.
	 *
	 * @param error excepción recibida
	 * @return datos del error, o vacío si no proviene de la base de datos
	 */
	public static Optional<ErrorBaseDatos> desde(Throwable error) {
		for (Throwable actual = error; actual != null; actual = actual.getCause()) {
			if (actual instanceof PSQLException psql && psql.getServerErrorMessage() != null) {
				ServerErrorMessage servidor = psql.getServerErrorMessage();
				return Optional.of(new ErrorBaseDatos(servidor.getSQLState(), servidor.getMessage(),
						servidor.getConstraint()));
			}
			if (actual instanceof SQLException sql && sql.getSQLState() != null) {
				return Optional.of(new ErrorBaseDatos(sql.getSQLState(), sql.getMessage(), null));
			}
			if (actual.getCause() == actual) {
				break;
			}
		}
		return Optional.empty();
	}

}
