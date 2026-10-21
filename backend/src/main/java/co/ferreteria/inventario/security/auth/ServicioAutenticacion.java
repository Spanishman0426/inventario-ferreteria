/**
 * Nombre: ServicioAutenticacion.java
 * Descripción: Inicio de sesión con BCrypt, bloqueo por intentos fallidos y auditoría de accesos.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import co.ferreteria.inventario.common.auditoria.ContextoAuditoria;
import co.ferreteria.inventario.common.auditoria.RegistroAuditoria;
import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.OperacionRechazadaException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.security.PropiedadesSeguridad;
import co.ferreteria.inventario.security.ServicioToken;
import co.ferreteria.inventario.usuario.Usuario;
import co.ferreteria.inventario.usuario.UsuarioRepository;

/**
 * Autenticación de usuarios (RF-SEG-001, RNF-SEG-001, RNF-SEG-005).
 *
 * <p>Cada intento se procesa en una transacción que se confirma también cuando el acceso se
 * rechaza, para conservar el contador de intentos, el bloqueo y el registro {@code SESION_FALLIDA}.
 * El rechazo se lanza después de confirmar.</p>
 *
 * <p>Reglas: tras {@code intentosMaximos} contraseñas incorrectas consecutivas la cuenta queda
 * bloqueada durante {@code duracionBloqueo}; un acceso correcto reinicia el contador y actualiza
 * {@code ultimo_acceso}; una cuenta inactiva, el usuario Sistema o un usuario inexistente reciben la
 * misma respuesta genérica, para no revelar qué cuentas existen.</p>
 */
@Service
public class ServicioAutenticacion {

	/** Formato de fecha y hora de Colombia para el mensaje de bloqueo (RNF-INT-003). */
	private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final RegistroAuditoria registro;
	private final ContextoAuditoria contexto;
	private final ServicioToken servicioToken;
	private final PropiedadesSeguridad propiedades;
	private final Clock reloj;
	private final TransactionTemplate transaccion;
	/** Hash de una clave aleatoria: iguala el tiempo de respuesta cuando el usuario no existe. */
	private final String hashFicticio;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarios repositorio de usuarios
	 * @param passwordEncoder BCrypt
	 * @param registro auditoría de accesos
	 * @param contexto usuario que firma la auditoría
	 * @param servicioToken emisión del JWT
	 * @param propiedades intentos y duración del bloqueo
	 * @param reloj reloj de la aplicación
	 * @param transactionManager gestor de transacciones
	 */
	public ServicioAutenticacion(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
			RegistroAuditoria registro, ContextoAuditoria contexto, ServicioToken servicioToken,
			PropiedadesSeguridad propiedades, Clock reloj, PlatformTransactionManager transactionManager) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.registro = registro;
		this.contexto = contexto;
		this.servicioToken = servicioToken;
		this.propiedades = propiedades;
		this.reloj = reloj;
		this.transaccion = new TransactionTemplate(transactionManager);
		this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	/**
	 * Valida las credenciales y emite un token.
	 *
	 * @param solicitud usuario y contraseña
	 * @return token y datos del usuario
	 * @throws OperacionRechazadaException {@code CREDENCIALES_INVALIDAS} (401) o {@code CUENTA_BLOQUEADA} (423)
	 */
	public RespuestaInicioSesion iniciarSesion(SolicitudInicioSesion solicitud) {
		Long usuarioId = usuarios.buscarIdPorUsuario(solicitud.usuario()).orElse(null);
		// La auditoría (incluido el trigger sobre usuario) queda firmada por quien intenta entrar.
		Resultado resultado = contexto.ejecutarComo(usuarioId,
				() -> transaccion.execute(estado -> procesar(usuarioId, solicitud)));
		return switch (resultado) {
			case Resultado.Exito exito -> exito.respuesta();
			case Resultado.Bloqueado bloqueado -> throw new OperacionRechazadaException(CodigoError.CUENTA_BLOQUEADA,
					CodigoError.CUENTA_BLOQUEADA.claveDetalle(),
					FORMATO_HORA.format(bloqueado.hasta().atZone(reloj.getZone())));
			case Resultado.Rechazado rechazado -> throw new OperacionRechazadaException(
					CodigoError.CREDENCIALES_INVALIDAS, CodigoError.CREDENCIALES_INVALIDAS.claveDetalle());
		};
	}

	/**
	 * Datos del usuario autenticado, leídos de la base (roles y permisos vigentes).
	 *
	 * @param usuarioId id del usuario del token
	 * @return datos de sesión
	 */
	@Transactional(readOnly = true)
	public UsuarioSesion usuarioActual(long usuarioId) {
		return usuarios.buscarConPermisos(usuarioId)
				.map(UsuarioSesion::desde)
				.orElseThrow(() -> new RecursoNoEncontradoException("entidad.usuario", usuarioId));
	}

	private Resultado procesar(Long usuarioId, SolicitudInicioSesion solicitud) {
		if (usuarioId == null) {
			passwordEncoder.matches(solicitud.contrasena(), hashFicticio);
			registro.sesionFallida(null, solicitud.usuario(), "INEXISTENTE");
			return new Resultado.Rechazado();
		}
		Usuario usuario = usuarios.bloquearPorId(usuarioId).orElseThrow();
		if (usuario.isEsSistema()) {
			passwordEncoder.matches(solicitud.contrasena(), hashFicticio);
			registro.sesionFallida(usuarioId, solicitud.usuario(), "SISTEMA");
			return new Resultado.Rechazado();
		}
		Instant ahora = reloj.instant();
		if (usuario.estaBloqueado(ahora)) {
			registro.sesionFallida(usuarioId, solicitud.usuario(), "BLOQUEADA");
			return new Resultado.Bloqueado(usuario.getBloqueadoHasta());
		}
		boolean contrasenaCorrecta = passwordEncoder.matches(solicitud.contrasena(), usuario.getPasswordHash());
		if (!usuario.isActivo()) {
			registro.sesionFallida(usuarioId, solicitud.usuario(), "INACTIVA");
			return new Resultado.Rechazado();
		}
		if (!contrasenaCorrecta) {
			return registrarContrasenaIncorrecta(usuario, ahora);
		}
		usuario.setIntentosFallidos((short) 0);
		usuario.setBloqueadoHasta(null);
		usuario.setUltimoAcceso(ahora);
		registro.inicioSesion(usuarioId, usuario.getUsuario());
		ServicioToken.TokenEmitido token =
				servicioToken.emitir(usuarioId, usuario.getUsuario(), usuario.codigosPermisos());
		return new Resultado.Exito(
				new RespuestaInicioSesion(token.valor(), token.vigenciaSegundos(), UsuarioSesion.desde(usuario)));
	}

	private Resultado registrarContrasenaIncorrecta(Usuario usuario, Instant ahora) {
		int intentos = usuario.getIntentosFallidos() + 1;
		if (intentos >= propiedades.intentosMaximos()) {
			// Al bloquear se reinicia el contador: al vencer el bloqueo hay otros N intentos.
			Instant hasta = ahora.plus(propiedades.duracionBloqueo());
			usuario.setIntentosFallidos((short) 0);
			usuario.setBloqueadoHasta(hasta);
			registro.sesionFallida(usuario.getId(), usuario.getUsuario(), "BLOQUEO");
			return new Resultado.Bloqueado(hasta);
		}
		usuario.setIntentosFallidos((short) intentos);
		registro.sesionFallida(usuario.getId(), usuario.getUsuario(), "CREDENCIALES");
		return new Resultado.Rechazado();
	}

	/** Resultado de un intento, decidido dentro de la transacción. */
	private sealed interface Resultado {

		/** Acceso concedido. */
		record Exito(RespuestaInicioSesion respuesta) implements Resultado {
		}

		/** Cuenta bloqueada hasta un instante. */
		record Bloqueado(Instant hasta) implements Resultado {
		}

		/** Credenciales inválidas, cuenta inactiva o inexistente. */
		record Rechazado() implements Resultado {
		}

	}

}
