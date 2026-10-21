/**
 * Nombre: InicializadorAdministrador.java
 * Descripción: Perfil dev: crea el primer administrador desde variables de entorno si no existe ninguno.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import java.util.List;

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
 * Crea el primer administrador en desarrollo (RF-SEG-001, precondición: "existe al menos un
 * administrador creado en la instalación"; modelo de datos, sección 6.1).
 *
 * <p>Si no hay ningún administrador activo, crea uno con {@code ADMIN_USUARIO}, {@code ADMIN_PASSWORD},
 * {@code ADMIN_CORREO} y {@code ADMIN_NOMBRE} y le asigna el rol ADMIN. La creación queda auditada a
 * nombre del usuario Sistema. Si faltan las variables, solo registra una advertencia. La contraseña
 * nunca se escribe en el registro.</p>
 */
@Component
@Profile("dev")
public class InicializadorAdministrador implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(InicializadorAdministrador.class);

	private final UsuarioRepository usuarios;
	private final RolRepository roles;
	private final PasswordEncoder passwordEncoder;
	private final ContextoAuditoria contexto;
	private final TransactionTemplate transaccion;
	private final String usuario;
	private final String contrasena;
	private final String correo;
	private final String nombre;

	/**
	 * Crea el inicializador.
	 *
	 * @param usuarios repositorio de usuarios
	 * @param roles repositorio de roles
	 * @param passwordEncoder BCrypt
	 * @param contexto firma de la auditoría
	 * @param transactionManager gestor de transacciones
	 * @param usuario variable ADMIN_USUARIO
	 * @param contrasena variable ADMIN_PASSWORD
	 * @param correo variable ADMIN_CORREO
	 * @param nombre variable ADMIN_NOMBRE
	 */
	public InicializadorAdministrador(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder passwordEncoder,
			ContextoAuditoria contexto, PlatformTransactionManager transactionManager,
			@Value("${app.admin.usuario:}") String usuario,
			@Value("${app.admin.password:}") String contrasena,
			@Value("${app.admin.correo:}") String correo,
			@Value("${app.admin.nombre:Administrador}") String nombre) {
		this.usuarios = usuarios;
		this.roles = roles;
		this.passwordEncoder = passwordEncoder;
		this.contexto = contexto;
		this.transaccion = new TransactionTemplate(transactionManager);
		this.usuario = usuario;
		this.contrasena = contrasena;
		this.correo = correo;
		this.nombre = nombre;
	}

	/**
	 * Crea el administrador si hace falta.
	 *
	 * @param args argumentos de arranque (no se usan)
	 */
	@Override
	public void run(ApplicationArguments args) {
		Long sistema = usuarios.buscarIdSistema().orElse(null);
		contexto.ejecutarComo(sistema, () -> transaccion.execute(estado -> {
			crearSiFalta();
			return null;
		}));
	}

	private void crearSiFalta() {
		List<Long> administradores = usuarios.bloquearAdministradoresActivos();
		if (!administradores.isEmpty()) {
			return;
		}
		if (!StringUtils.hasText(usuario) || !StringUtils.hasText(contrasena) || !StringUtils.hasText(correo)) {
			LOG.warn("No hay administrador activo y faltan ADMIN_USUARIO, ADMIN_PASSWORD o ADMIN_CORREO; no se crea ninguno");
			return;
		}
		if (usuarios.existsByUsuario(usuario)) {
			LOG.warn("El usuario {} ya existe pero no es administrador activo; no se modifica", usuario);
			return;
		}
		Usuario admin = new Usuario(usuario, nombre, correo, passwordEncoder.encode(contrasena));
		admin.asignarRoles(roles.findByCodigoIn(List.of(Rol.ADMIN)));
		usuarios.saveAndFlush(admin);
		LOG.info("Administrador inicial {} creado", usuario);
	}

}
