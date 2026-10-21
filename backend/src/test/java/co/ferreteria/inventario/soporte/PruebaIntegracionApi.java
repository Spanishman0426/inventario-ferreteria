/**
 * Nombre: PruebaIntegracionApi.java
 * Descripción: Base de las pruebas de integración de la API: contexto completo, MockMvc, PostgreSQL en
 *              Testcontainers y usuarios del escenario con contraseña conocida.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.soporte;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import co.ferreteria.inventario.TestcontainersConfiguration;

/**
 * Todas las subclases comparten un mismo contexto (y un mismo contenedor). Antes de cada prueba los
 * usuarios del escenario quedan activos, desbloqueados y con {@link UsuariosPrueba#CONTRASENA}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, UsuariosPrueba.class})
public abstract class PruebaIntegracionApi {

	/** IP que MockMvc asigna a las solicitudes. */
	protected static final String IP_LOCAL = "127.0.0.1";

	/** Cliente HTTP simulado. */
	@Autowired
	protected MockMvc mvc;

	/** Acceso JDBC para verificar la base. */
	@Autowired
	protected JdbcTemplate jdbc;

	/** Usuarios del escenario. */
	@Autowired
	protected UsuariosPrueba usuarios;

	/**
	 * Restablece los usuarios del escenario.
	 */
	@BeforeEach
	void restablecerUsuarios() {
		usuarios.restablecer();
	}

	/**
	 * Último id de la tabla auditoria; sirve para contar lo que registra una operación.
	 *
	 * @return id máximo actual, o 0
	 */
	protected long ultimoIdAuditoria() {
		Long id = jdbc.queryForObject("SELECT coalesce(max(id), 0) FROM auditoria", Long.class);
		return id == null ? 0 : id;
	}

	/**
	 * Id de un usuario por su nombre.
	 *
	 * @param usuario nombre de usuario
	 * @return id
	 */
	protected long idUsuario(String usuario) {
		Long id = jdbc.queryForObject("SELECT id FROM usuario WHERE usuario = ?", Long.class, usuario);
		return id == null ? 0 : id;
	}

}
