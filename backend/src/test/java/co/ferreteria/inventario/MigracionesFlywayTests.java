/**
 * Nombre: MigracionesFlywayTests.java
 * Descripción: Prueba de contexto que verifica que Flyway aplicó V1 (esquema), V2 (reportes), V3 (auditoría),
 *              V4 (secuencias de marca y categoría) y V100 (datos).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arranca el contexto completo contra PostgreSQL 18 en Testcontainers y revisa el resultado de las
 * migraciones con las mismas comprobaciones de {@code docs/modelo-datos.md}, sección 6.3.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MigracionesFlywayTests {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@DisplayName("Flyway aplicó con éxito V1, V2, V3, V4 y los datos de prueba V100, en ese orden")
	void flywayAplicoLasMigraciones() {
		List<String> versiones = jdbc.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank",
				String.class);
		assertThat(versiones).containsExactly("1", "2", "3", "4", "100");

		Integer fallidas = jdbc.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);
		assertThat(fallidas).isZero();
	}

	@Test
	@DisplayName("V1 creó las 33 tablas del modelo y los datos iniciales")
	void v1CreoElEsquema() {
		Integer tablas = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				 WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
				   AND table_name NOT LIKE 'auditoria\\_%'
				   AND table_name <> 'flyway_schema_history'
				""", Integer.class);
		assertThat(tablas).isEqualTo(33);

		assertThat(jdbc.queryForObject("SELECT fn_dv_nit('890903938')", Integer.class)).isEqualTo(8);
		assertThat(jdbc.queryForObject("SELECT fn_codigo_interno(1)", String.class)).isEqualTo("2000000000015");
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM rol_permiso rp JOIN rol r ON r.id = rp.rol_id WHERE r.codigo = 'ADMIN'",
				Integer.class)).isEqualTo(26);
	}

	@Test
	@DisplayName("V2 creó las 13 funciones de reportes R-01 a R-13")
	void v2CreoLosReportes() {
		List<String> funciones = jdbc.queryForList(
				"SELECT routine_name FROM information_schema.routines WHERE routine_schema = 'reportes'",
				String.class);
		assertThat(funciones).contains(
				"existencias_valorizadas", "kardex", "ventas_resumen", "ventas_por_producto",
				"ventas_por_vendedor", "compras_por_proveedor", "proximos_a_vencer", "danados_y_bajas",
				"diferencias_conteo", "reservas_activas", "historial_precios", "iva_por_tarifa",
				"saldos_cierre");
	}

	@Test
	@DisplayName("V100 cargó el escenario de prueba (15 documentos, 11 lotes, 29 movimientos)")
	void v100CargoLosDatosDePrueba() {
		assertThat(jdbc.queryForObject("SELECT count(*) FROM documento", Integer.class)).isEqualTo(15);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM lote", Integer.class)).isEqualTo(11);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_inventario", Integer.class)).isEqualTo(29);
		// PR-08: inventario valorizado al cierre de agosto
		assertThat(jdbc.queryForObject(
				"SELECT sum(valor) FROM reportes.existencias_valorizadas('2026-08-31')", Long.class))
				.isEqualTo(2_774_650L);
	}

	@Test
	@DisplayName("V4 deja la secuencia de marca y categoría en el mayor id: el siguiente alta recibe max(id) + 1")
	void v4ResincronizaConFilas() throws IOException {
		jdbc.execute("SELECT setval(pg_get_serial_sequence('marca', 'id'), 1)");
		jdbc.execute(scriptV4());

		for (String tabla : List.of("marca", "categoria")) {
			Long maximo = jdbc.queryForObject("SELECT max(id) FROM " + tabla, Long.class);
			Map<String, Object> secuencia = jdbc.queryForMap("SELECT last_value, is_called FROM "
					+ jdbc.queryForObject("SELECT pg_get_serial_sequence(?, 'id')", String.class, tabla));
			assertThat(((Number) secuencia.get("last_value")).longValue()).as(tabla).isEqualTo(maximo);
			assertThat(secuencia.get("is_called")).as(tabla).isEqualTo(true);
		}
	}

	@Test
	@Transactional
	@DisplayName("V4 con la tabla vacía deja la secuencia lista para empezar en 1")
	void v4ConTablaVaciaEmpiezaEnUno() throws IOException {
		jdbc.execute("CREATE TEMP TABLE v4_vacia (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, nombre text)");
		jdbc.execute("SELECT setval(pg_get_serial_sequence('v4_vacia', 'id'), 50)");
		String script = scriptV4().replace("'marca'", "'v4_vacia'").replace("FROM marca", "FROM v4_vacia")
				.replace("'categoria'", "'v4_vacia'").replace("FROM categoria", "FROM v4_vacia");
		jdbc.execute(script);

		assertThat(jdbc.queryForObject("INSERT INTO v4_vacia (nombre) VALUES ('a') RETURNING id", Long.class))
				.isEqualTo(1L);
	}

	private static String scriptV4() throws IOException {
		return new ClassPathResource("db/migration/V4__resincronizar_identidad_marca_categoria.sql")
				.getContentAsString(StandardCharsets.UTF_8);
	}

}
