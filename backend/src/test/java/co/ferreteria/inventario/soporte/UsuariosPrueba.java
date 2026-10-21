/**
 * Nombre: UsuariosPrueba.java
 * Descripción: Utilitario de prueba que asigna una contraseña conocida con BCrypt a los usuarios de
 *              V100__datos_prueba.sql (que traen un hash inválido a propósito) y obtiene tokens.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

/**
 * Prepara los usuarios del escenario de prueba (admin, gerente, jefe, comprador, vendedor, auditor).
 *
 * <p>La contraseña se genera al azar en cada ejecución, así que no hay ninguna contraseña escrita en el
 * código; cumple la política de RF-SEG-001 (letras y números). El hash BCrypt se calcula una sola vez.</p>
 */
@TestComponent
public class UsuariosPrueba {

	/** Contraseña conocida de todos los usuarios del escenario durante esta ejecución. */
	public static final String CONTRASENA = "Prueba1-" + UUID.randomUUID();

	/** Usuarios humanos de V100 (ids 2 a 7). */
	public static final String[] USUARIOS = {"admin", "gerente", "jefe", "comprador", "vendedor", "auditor"};

	private static String hash;

	private final JdbcTemplate jdbc;
	private final PasswordEncoder passwordEncoder;
	private final MockMvc mvc;

	/**
	 * Crea el utilitario.
	 *
	 * @param jdbc acceso JDBC
	 * @param passwordEncoder BCrypt de la aplicación (fuerza 12)
	 * @param mvc cliente HTTP simulado
	 */
	public UsuariosPrueba(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, MockMvc mvc) {
		this.jdbc = jdbc;
		this.passwordEncoder = passwordEncoder;
		this.mvc = mvc;
	}

	/**
	 * Deja los usuarios del escenario activos, sin bloqueo, sin intentos fallidos y con
	 * {@link #CONTRASENA}.
	 */
	public void restablecer() {
		jdbc.update("""
				UPDATE usuario SET password_hash = ?, intentos_fallidos = 0, bloqueado_hasta = NULL, activo = true
				 WHERE usuario = ANY (?)
				""", hashConocido(), USUARIOS);
	}

	/**
	 * Inicia sesión por la API y devuelve el token.
	 *
	 * @param usuario nombre de usuario del escenario
	 * @return token de acceso
	 * @throws Exception si la solicitud falla
	 */
	public String token(String usuario) throws Exception {
		MvcResult resultado = mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(credenciales(usuario, CONTRASENA)))
				.andReturn();
		if (resultado.getResponse().getStatus() != 200) {
			throw new IllegalStateException("Inicio de sesión de prueba rechazado para " + usuario + ": "
					+ resultado.getResponse().getContentAsString());
		}
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
	}

	/**
	 * Cabecera {@code Authorization} con el token de un usuario.
	 *
	 * @param usuario nombre de usuario del escenario
	 * @return valor {@code Bearer ...}
	 * @throws Exception si la solicitud falla
	 */
	public String bearer(String usuario) throws Exception {
		return "Bearer " + token(usuario);
	}

	/**
	 * Cuerpo JSON de inicio de sesión.
	 *
	 * @param usuario nombre de usuario
	 * @param contrasena contraseña
	 * @return JSON
	 */
	public static String credenciales(String usuario, String contrasena) {
		return "{\"usuario\":\"" + usuario + "\",\"contrasena\":\"" + contrasena + "\"}";
	}

	private synchronized String hashConocido() {
		if (hash == null) {
			hash = passwordEncoder.encode(CONTRASENA);
		}
		return hash;
	}

}
