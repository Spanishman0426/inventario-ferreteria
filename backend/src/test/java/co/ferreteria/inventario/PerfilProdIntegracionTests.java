/**
 * Nombre: PerfilProdIntegracionTests.java
 * Descripción: Prueba de integración del perfil prod (Render + Neon): Flyway solo aplica V1 a V3, se crea el
 *              administrador inicial desde variables de entorno, HSTS detrás del proxy TLS y actuator público.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import co.ferreteria.inventario.usuario.InicializadorAdministrador;

/**
 * Arranca la aplicación con el perfil {@code prod}, tal como corre en Render, contra PostgreSQL en
 * Testcontainers. Las variables de entorno de producción se simulan con valores ficticios; la conexión
 * real la aporta Testcontainers ({@code @ServiceConnection} tiene prioridad sobre {@code DB_URL}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
	"DB_URL=jdbc:postgresql://reemplazada-por-testcontainers/ferreteria",
	"DB_USER=ficticio",
	"DB_PASSWORD=ficticia",
	"JWT_SECRET=clave-ficticia-de-prueba-con-mas-de-32-caracteres",
	"FRONTEND_URL=https://ferreteria-frontend.example",
	"ADMIN_USUARIO=administrador",
	"ADMIN_PASSWORD=ClaveInicial2026",
	"ADMIN_CORREO=admin@ferreteria.example",
	"RENDER_GIT_COMMIT=abc1234def5678"})
class PerfilProdIntegracionTests {

	private static final String LOGIN = "/api/v1/auth/login";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private InicializadorAdministrador inicializador;

	@Test
	@DisplayName("Perfil prod: Flyway aplica solo V1, V2 y V3, sin datos de prueba, e2e ni dev")
	void flywaySoloAplicaLasMigracionesDelEsquema() {
		List<String> versiones = jdbc.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank",
				String.class);
		assertThat(versiones).containsExactly("1", "2", "3");

		// Los datos de dev y e2e son scripts repetibles (R__) sin versión; los de prueba son V100
		Integer repetibles = jdbc.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE version IS NULL AND type <> 'BASELINE'",
				Integer.class);
		assertThat(repetibles).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM marca", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM documento", Integer.class)).isZero();
	}

	@Test
	@DisplayName("Perfil prod: crea el administrador inicial desde ADMIN_USUARIO y ADMIN_PASSWORD, que puede iniciar sesión")
	void creaElAdministradorInicial() throws Exception {
		Integer administradores = jdbc.queryForObject("""
				SELECT count(*) FROM usuario u
				  JOIN usuario_rol ur ON ur.usuario_id = u.id
				  JOIN rol r ON r.id = ur.rol_id
				 WHERE r.codigo = 'ADMIN' AND u.activo AND u.usuario = 'administrador'
				""", Integer.class);
		assertThat(administradores).isOne();

		// La contraseña se guarda con hash, nunca en texto plano
		String hash = jdbc.queryForObject(
				"SELECT password_hash FROM usuario WHERE usuario = 'administrador'", String.class);
		assertThat(hash).startsWith("$2").doesNotContain("ClaveInicial2026");

		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"administrador\",\"contrasena\":\"ClaveInicial2026\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	@DisplayName("Perfil prod: un nuevo arranque no crea un segundo administrador")
	void unNuevoArranqueNoDuplicaElAdministrador() {
		inicializador.run(new DefaultApplicationArguments());

		Integer usuarios = jdbc.queryForObject(
				"SELECT count(*) FROM usuario WHERE usuario = 'administrador'", Integer.class);
		assertThat(usuarios).isOne();
	}

	@Test
	@DisplayName("[CA-RNF-SEG-006-1] Detrás del proxy TLS de Render (X-Forwarded-Proto: https) la respuesta lleva HSTS")
	void hstsDetrasDelProxyTls() throws Exception {
		mvc.perform(get("/actuator/health").header("X-Forwarded-Proto", "https"))
				.andExpect(status().isOk())
				.andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));

		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Strict-Transport-Security"));
	}

	@Test
	@DisplayName("Perfil prod: /actuator/health responde UP y /actuator/info muestra el commit desplegado, sin autenticación")
	void actuatorPublico() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));

		mvc.perform(get("/actuator/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.app.commit").value("abc1234def5678"));
	}

}
