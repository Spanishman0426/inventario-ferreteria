/**
 * Nombre: ServicioAutenticacionTests.java
 * Descripción: Pruebas unitarias con Mockito del inicio de sesión: acceso correcto, intentos fallidos,
 *              bloqueo de 15 minutos, cuenta inactiva, usuario inexistente y usuario Sistema.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

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
 * Reglas de {@link ServicioAutenticacion} (RF-SEG-001, RNF-SEG-005) con repositorio, BCrypt, auditoría,
 * emisor de tokens y gestor de transacciones simulados, y un reloj fijo en hora de Colombia.
 */
@ExtendWith(MockitoExtension.class)
class ServicioAutenticacionTests {

	/** 2026-10-20 10:00 en Bogotá. */
	private static final Instant AHORA = Instant.parse("2026-10-20T15:00:00Z");
	private static final long ID = 4L;
	private static final String USUARIO = "jefe";
	private static final String CLAVE = "Clave12345";
	private static final String HASH = "$2a$12$hash-simulado";

	@Mock
	private UsuarioRepository usuarios;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private RegistroAuditoria registro;
	@Mock
	private ServicioToken servicioToken;
	@Mock
	private PlatformTransactionManager transactionManager;

	private final ContextoAuditoria contexto = new ContextoAuditoria();
	private ServicioAutenticacion servicio;
	private Usuario usuario;

	@BeforeEach
	void crearServicio() {
		PropiedadesSeguridad propiedades = new PropiedadesSeguridad("x".repeat(32), Duration.ofMinutes(30),
				"http://localhost:5173", 5, Duration.ofMinutes(15));
		Clock reloj = Clock.fixed(AHORA, ZoneId.of("America/Bogota"));
		servicio = new ServicioAutenticacion(usuarios, passwordEncoder, registro, contexto, servicioToken,
				propiedades, reloj, transactionManager);
		usuario = new Usuario(USUARIO, "Juana Jefe", "jefe@ferreteria.test", HASH);
		ReflectionTestUtils.setField(usuario, "id", ID);
	}

	@Test
	@DisplayName("Acceso correcto: reinicia intentos, actualiza el último acceso, audita a nombre del usuario y emite el token")
	void accesoCorrecto() {
		existe();
		usuario.setIntentosFallidos((short) 3);
		when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);
		when(servicioToken.emitir(ID, USUARIO, Set.of())).thenReturn(new ServicioToken.TokenEmitido("token", 1800));
		doAnswer(i -> {
			assertThat(contexto.usuarioId()).as("la auditoría se firma con quien inicia sesión").contains(ID);
			return null;
		}).when(registro).inicioSesion(ID, USUARIO);

		RespuestaInicioSesion respuesta = servicio.iniciarSesion(new SolicitudInicioSesion(USUARIO, CLAVE));

		assertThat(respuesta.accessToken()).isEqualTo("token");
		assertThat(respuesta.expiresIn()).isEqualTo(1800);
		assertThat(respuesta.usuario().id()).isEqualTo(ID);
		assertThat(usuario.getIntentosFallidos()).isZero();
		assertThat(usuario.getUltimoAcceso()).isEqualTo(AHORA);
		verify(registro).inicioSesion(ID, USUARIO);
		assertThat(contexto.usuarioId()).as("el usuario fijado no queda en el hilo").isEmpty();
	}

	@Test
	@DisplayName("Contraseña incorrecta: CREDENCIALES_INVALIDAS, suma un intento y audita SESION_FALLIDA")
	void contrasenaIncorrecta() {
		existe();

		assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion(USUARIO, "otra")))
				.isInstanceOfSatisfying(OperacionRechazadaException.class,
						e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CREDENCIALES_INVALIDAS));

		assertThat(usuario.getIntentosFallidos()).isEqualTo((short) 1);
		assertThat(usuario.getBloqueadoHasta()).isNull();
		verify(registro).sesionFallida(ID, USUARIO, "CREDENCIALES");
		verify(servicioToken, never()).emitir(anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-1][CA-RNF-SEG-005-2] Tras 5 intentos fallidos la cuenta queda bloqueada 15 minutos")
	void cincoIntentosBloquean() {
		existe();
		SolicitudInicioSesion incorrecta = new SolicitudInicioSesion(USUARIO, "otra");

		for (int intento = 1; intento <= 4; intento++) {
			assertThatThrownBy(() -> servicio.iniciarSesion(incorrecta))
					.isInstanceOfSatisfying(OperacionRechazadaException.class,
							e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CREDENCIALES_INVALIDAS));
			assertThat(usuario.getIntentosFallidos()).isEqualTo((short) intento);
		}
		assertThatThrownBy(() -> servicio.iniciarSesion(incorrecta))
				.isInstanceOfSatisfying(OperacionRechazadaException.class,
						e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CUENTA_BLOQUEADA));

		assertThat(usuario.getBloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
		assertThat(usuario.getIntentosFallidos()).as("al bloquear se reinicia el contador").isZero();
		verify(registro, times(4)).sesionFallida(ID, USUARIO, "CREDENCIALES");
		verify(registro).sesionFallida(ID, USUARIO, "BLOQUEO");
	}

	@Test
	@DisplayName("[CA-RNF-INT-003-1] El mensaje de bloqueo indica la hora de desbloqueo en Bogotá con formato dd/MM/yyyy HH:mm")
	void horaDeBloqueoEnBogota() {
		existe();
		usuario.setIntentosFallidos((short) 4);

		assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion(USUARIO, "otra")))
				.isInstanceOfSatisfying(OperacionRechazadaException.class, e -> {
					assertThat(e.getCodigo()).isEqualTo(CodigoError.CUENTA_BLOQUEADA);
					assertThat(e.getArgumentos()).containsExactly("20/10/2026 10:15");
				});
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-1] Una cuenta bloqueada se rechaza aun con la contraseña correcta, sin verificarla")
	void bloqueadaRechazaContrasenaCorrecta() {
		existe();
		usuario.setBloqueadoHasta(AHORA.plus(Duration.ofMinutes(10)));

		assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion(USUARIO, CLAVE)))
				.isInstanceOfSatisfying(OperacionRechazadaException.class,
						e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CUENTA_BLOQUEADA));

		verify(passwordEncoder, never()).matches(CLAVE, HASH);
		verify(registro).sesionFallida(ID, USUARIO, "BLOQUEADA");
		verify(servicioToken, never()).emitir(anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-1] Vencidos los 15 minutos, la contraseña correcta vuelve a dar acceso y limpia el bloqueo")
	void bloqueoVencidoPermiteAcceso() {
		existe();
		usuario.setBloqueadoHasta(AHORA.minusSeconds(1));
		when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);
		when(servicioToken.emitir(ID, USUARIO, Set.of())).thenReturn(new ServicioToken.TokenEmitido("token", 1800));

		assertThat(servicio.iniciarSesion(new SolicitudInicioSesion(USUARIO, CLAVE)).accessToken()).isEqualTo("token");
		assertThat(usuario.getBloqueadoHasta()).isNull();
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-3] Una cuenta inactiva no inicia sesión aunque la contraseña sea correcta")
	void cuentaInactiva() {
		existe();
		usuario.setActivo(false);
		when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);

		assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion(USUARIO, CLAVE)))
				.isInstanceOfSatisfying(OperacionRechazadaException.class,
						e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CREDENCIALES_INVALIDAS));

		verify(registro).sesionFallida(ID, USUARIO, "INACTIVA");
		verify(servicioToken, never()).emitir(anyLong(), anyString(), any());
		assertThat(usuario.getIntentosFallidos()).as("no cuenta como intento fallido").isZero();
	}

	@Test
	@DisplayName("Usuario inexistente: la misma respuesta genérica y SESION_FALLIDA sin usuario")
	void usuarioInexistente() {
		when(usuarios.buscarIdPorUsuario("nadie")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion("nadie", CLAVE)))
				.isInstanceOfSatisfying(OperacionRechazadaException.class,
						e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CREDENCIALES_INVALIDAS));

		verify(registro).sesionFallida(null, "nadie", "INEXISTENTE");
		verify(usuarios, never()).bloquearPorId(any());
	}

	@Test
	@DisplayName("El usuario Sistema no inicia sesión")
	void usuarioSistema() {
		existe();
		ReflectionTestUtils.setField(usuario, "esSistema", true);

		assertThatThrownBy(() -> servicio.iniciarSesion(new SolicitudInicioSesion(USUARIO, CLAVE)))
				.isInstanceOfSatisfying(OperacionRechazadaException.class,
						e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CREDENCIALES_INVALIDAS));

		verify(registro).sesionFallida(ID, USUARIO, "SISTEMA");
	}

	@Test
	@DisplayName("usuarioActual devuelve los datos del usuario o 404 si no existe")
	void usuarioActual() {
		when(usuarios.buscarConPermisos(ID)).thenReturn(Optional.of(usuario));
		when(usuarios.buscarConPermisos(99L)).thenReturn(Optional.empty());

		assertThat(servicio.usuarioActual(ID).nombreCompleto()).isEqualTo("Juana Jefe");
		assertThatThrownBy(() -> servicio.usuarioActual(99L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	private void existe() {
		when(usuarios.buscarIdPorUsuario(USUARIO)).thenReturn(Optional.of(ID));
		when(usuarios.bloquearPorId(ID)).thenReturn(Optional.of(usuario));
	}

}
