/**
 * Nombre: InicializadorUsuariosE2e.java
 * Descripción: Perfil e2e: asigna a los usuarios admin, jefe y vendedor de los datos de prueba las
 *              contraseñas tomadas de variables de entorno y les quita cualquier bloqueo.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import co.ferreteria.inventario.common.auditoria.ContextoAuditoria;

/**
 * Prepara las cuentas que usan las pruebas end-to-end.
 *
 * <p>Los datos de prueba ({@code db/e2edata}, copia de {@code database/02_datos_prueba.sql}) crean un
 * usuario por rol con un hash que no sirve para iniciar sesión. Al arrancar con el perfil {@code e2e},
 * este componente guarda el hash BCrypt de {@code E2E_ADMIN_PASSWORD}, {@code E2E_JEFE_PASSWORD} y
 * {@code E2E_VENDEDOR_PASSWORD} en los usuarios admin, jefe y vendedor, y reinicia sus intentos
 * fallidos y su bloqueo, para que cada ejecución de las pruebas empiece igual. Ninguna contraseña está
 * en el código ni se escribe en el registro; si falta alguna variable, la aplicación no arranca.</p>
 *
 * <p>El cambio se audita a nombre del usuario Sistema (RN-28), como el de cualquier contraseña.</p>
 */
@Component
@Profile("e2e")
public class InicializadorUsuariosE2e implements ApplicationRunner {

	/** Usuarios de los datos de prueba que reciben contraseña, en orden. */
	public static final List<String> USUARIOS = List.of("admin", "jefe", "vendedor");

	private static final Logger LOG = LoggerFactory.getLogger(InicializadorUsuariosE2e.class);

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final ContextoAuditoria contexto;
	private final TransactionTemplate transaccion;
	private final Map<String, String> contrasenas = new LinkedHashMap<>();

	/**
	 * Crea el inicializador.
	 *
	 * @param usuarios repositorio de usuarios
	 * @param passwordEncoder BCrypt
	 * @param contexto firma de la auditoría
	 * @param transactionManager gestor de transacciones
	 * @param contrasenaAdmin variable E2E_ADMIN_PASSWORD
	 * @param contrasenaJefe variable E2E_JEFE_PASSWORD
	 * @param contrasenaVendedor variable E2E_VENDEDOR_PASSWORD
	 */
	public InicializadorUsuariosE2e(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
			ContextoAuditoria contexto, PlatformTransactionManager transactionManager,
			@Value("${app.e2e.password.admin:}") String contrasenaAdmin,
			@Value("${app.e2e.password.jefe:}") String contrasenaJefe,
			@Value("${app.e2e.password.vendedor:}") String contrasenaVendedor) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.contexto = contexto;
		this.transaccion = new TransactionTemplate(transactionManager);
		contrasenas.put("admin", contrasenaAdmin);
		contrasenas.put("jefe", contrasenaJefe);
		contrasenas.put("vendedor", contrasenaVendedor);
	}

	/**
	 * Asigna las contraseñas y desbloquea las cuentas.
	 *
	 * @param args argumentos de arranque (no se usan)
	 * @throws IllegalStateException si falta una variable o un usuario de los datos de prueba
	 */
	@Override
	public void run(ApplicationArguments args) {
		List<String> faltantes = contrasenas.entrySet().stream()
				.filter(e -> !StringUtils.hasText(e.getValue()))
				.map(e -> "E2E_" + e.getKey().toUpperCase() + "_PASSWORD")
				.toList();
		if (!faltantes.isEmpty()) {
			throw new IllegalStateException("Perfil e2e: faltan las variables " + faltantes);
		}
		Long sistema = usuarios.buscarIdSistema().orElse(null);
		contexto.ejecutarComo(sistema, () -> transaccion.execute(estado -> {
			contrasenas.forEach(this::preparar);
			return null;
		}));
		LOG.info("Perfil e2e: contraseñas asignadas a {}", USUARIOS);
	}

	private void preparar(String nombreUsuario, String contrasena) {
		Long id = usuarios.buscarIdPorUsuario(nombreUsuario).orElseThrow(() -> new IllegalStateException(
				"Perfil e2e: no existe el usuario " + nombreUsuario + "; ¿se cargó db/e2edata?"));
		Usuario usuario = usuarios.bloquearPorId(id).orElseThrow();
		usuario.setPasswordHash(passwordEncoder.encode(contrasena));
		usuario.setIntentosFallidos((short) 0);
		usuario.setBloqueadoHasta(null);
	}

}
