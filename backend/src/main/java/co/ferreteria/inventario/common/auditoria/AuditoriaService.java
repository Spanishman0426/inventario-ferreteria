/**
 * Nombre: AuditoriaService.java
 * Descripción: Consulta de solo lectura de la tabla auditoria con filtros y paginación.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.common.web.Pagina;

/**
 * Consulta de auditoría (RF-SEG-002). Solo lectura: la tabla es de solo inserción (RN-28) y aquí no
 * existe ninguna operación de escritura.
 *
 * <p>Las fechas del filtro se interpretan en hora de Colombia, desde el inicio de {@code desde} hasta
 * el final de {@code hasta}; filtrar por fecha permite a PostgreSQL leer solo las particiones del rango.</p>
 */
@Service
@Transactional(readOnly = true)
public class AuditoriaService {

	private static final String COLUMNAS = """
			a.id, a.fecha, a.usuario_id, u.usuario, host(a.ip) AS ip, a.accion, a.entidad, a.entidad_id
			""";

	private final NamedParameterJdbcTemplate jdbc;
	private final Clock reloj;

	/**
	 * Crea el servicio.
	 *
	 * @param jdbc acceso JDBC con parámetros con nombre
	 * @param reloj reloj de la aplicación (zona horaria de los filtros)
	 */
	public AuditoriaService(NamedParameterJdbcTemplate jdbc, Clock reloj) {
		this.jdbc = jdbc;
		this.reloj = reloj;
	}

	/**
	 * Lista registros de auditoría, del más reciente al más antiguo.
	 *
	 * @param filtro filtros opcionales
	 * @param pagina número de página desde 0
	 * @param tamano tamaño de página
	 * @return página de registros
	 */
	public Pagina<RegistroAuditoriaResumen> listar(FiltroAuditoria filtro, int pagina, int tamano) {
		MapSqlParameterSource parametros = new MapSqlParameterSource();
		StringBuilder donde = new StringBuilder(" WHERE true");
		if (filtro.usuarioId() != null) {
			donde.append(" AND a.usuario_id = :usuarioId");
			parametros.addValue("usuarioId", filtro.usuarioId());
		}
		if (filtro.entidad() != null) {
			donde.append(" AND a.entidad = :entidad");
			parametros.addValue("entidad", filtro.entidad());
		}
		if (filtro.accion() != null) {
			donde.append(" AND a.accion = :accion");
			parametros.addValue("accion", filtro.accion());
		}
		if (filtro.desde() != null) {
			donde.append(" AND a.fecha >= :desde");
			parametros.addValue("desde", inicioDelDia(filtro.desde()));
		}
		if (filtro.hasta() != null) {
			donde.append(" AND a.fecha < :hasta");
			parametros.addValue("hasta", inicioDelDia(filtro.hasta().plusDays(1)));
		}
		Long total = jdbc.queryForObject("SELECT count(*) FROM auditoria a" + donde, parametros, Long.class);
		parametros.addValue("limite", tamano).addValue("desplazamiento", (long) pagina * tamano);
		List<RegistroAuditoriaResumen> filas = jdbc.query(
				"SELECT " + COLUMNAS + " FROM auditoria a LEFT JOIN usuario u ON u.id = a.usuario_id" + donde
						+ " ORDER BY a.fecha DESC, a.id DESC LIMIT :limite OFFSET :desplazamiento",
				parametros, (rs, n) -> resumen(rs));
		return Pagina.de(filas, pagina, tamano, total == null ? 0 : total);
	}

	/**
	 * Consulta un registro con sus valores antes y después.
	 *
	 * @param id id del registro
	 * @return registro completo
	 */
	public RegistroAuditoriaDetalle obtener(long id) {
		List<RegistroAuditoriaDetalle> filas = jdbc.query(
				"SELECT " + COLUMNAS + ", a.valores_antes::text AS antes, a.valores_despues::text AS despues"
						+ " FROM auditoria a LEFT JOIN usuario u ON u.id = a.usuario_id WHERE a.id = :id",
				new MapSqlParameterSource("id", id),
				(rs, n) -> {
					RegistroAuditoriaResumen r = resumen(rs);
					return new RegistroAuditoriaDetalle(r.id(), r.fecha(), r.usuarioId(), r.usuario(), r.ip(),
							r.accion(), r.entidad(), r.entidadId(), rs.getString("antes"), rs.getString("despues"));
				});
		return filas.stream().findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException("entidad.auditoria", id));
	}

	private OffsetDateTime inicioDelDia(LocalDate dia) {
		return dia.atStartOfDay(reloj.getZone()).toOffsetDateTime();
	}

	private static RegistroAuditoriaResumen resumen(ResultSet rs) throws SQLException {
		return new RegistroAuditoriaResumen(rs.getLong("id"), rs.getObject("fecha", OffsetDateTime.class),
				rs.getObject("usuario_id", Long.class), rs.getString("usuario"), rs.getString("ip"),
				rs.getString("accion"), rs.getString("entidad"), rs.getObject("entidad_id", Long.class));
	}

}
