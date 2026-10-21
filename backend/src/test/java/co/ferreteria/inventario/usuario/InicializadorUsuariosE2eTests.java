/**
 * Nombre: InicializadorUsuariosE2eTests.java
 * Descripción: Pruebas unitarias con Mockito del inicializador del perfil e2e: asigna el hash de las
 *              contraseñas de entorno, desbloquea las cuentas y no arranca si falta una variable.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import co.ferreteria.inventario.common.auditoria.ContextoAuditoria;

/**
 * Reglas de {@link InicializadorUsuariosE2e} con repositorio, BCrypt y gestor de transacciones simulados.
 */
@ExtendWith(MockitoExtension.class)
class InicializadorUsuariosE2eTests {

	@Mock
	private UsuarioRepository usuarios;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private PlatformTransactionManager transactionManager;

	private final ContextoAuditoria contexto = new ContextoAuditoria();

	private InicializadorUsuariosE2e crear(String admin, String jefe, String vendedor) {
		return new InicializadorUsuariosE2e(usuarios, passwordEncoder, contexto, transactionManager, admin, jefe,
				vendedor);
	}

	@Test
	@DisplayName("Asigna el hash de cada contraseña y quita intentos fallidos y bloqueo, firmado por Sistema")
	void asignaContrasenas() {
		Map<String, Usuario> cuentas = new HashMap<>();
		long id = 2;
		for (String nombre : InicializadorUsuariosE2e.USUARIOS) {
			Usuario usuario = new Usuario(nombre, nombre, nombre + "@ferreteria.test", "hash-invalido");
			ReflectionTestUtils.setField(usuario, "id", id);
			usuario.setIntentosFallidos((short) 4);
			usuario.setBloqueadoHasta(Instant.parse("2026-10-21T15:00:00Z"));
			cuentas.put(nombre, usuario);
			when(usuarios.buscarIdPorUsuario(nombre)).thenReturn(Optional.of(id));
			when(usuarios.bloquearPorId(id)).thenReturn(Optional.of(usuario));
			when(passwordEncoder.encode("clave-" + nombre)).thenReturn("bcrypt-" + nombre);
			id++;
		}
		when(usuarios.buscarIdSistema()).thenReturn(Optional.of(1L));

		crear("clave-admin", "clave-jefe", "clave-vendedor").run(null);

		cuentas.forEach((nombre, usuario) -> {
			assertThat(usuario.getPasswordHash()).as(nombre).isEqualTo("bcrypt-" + nombre);
			assertThat(usuario.getIntentosFallidos()).as(nombre).isZero();
			assertThat(usuario.getBloqueadoHasta()).as(nombre).isNull();
		});
		assertThat(contexto.usuarioId()).as("el usuario Sistema no queda fijado en el hilo").isEmpty();
	}

	@Test
	@DisplayName("Sin una de las contraseñas no arranca y nombra la variable faltante")
	void faltaVariable() {
		InicializadorUsuariosE2e inicializador = crear("clave-admin", " ", "clave-vendedor");

		assertThatThrownBy(() -> inicializador.run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("E2E_JEFE_PASSWORD");
		verify(passwordEncoder, never()).encode(anyString());
	}

	@Test
	@DisplayName("Si los datos de prueba no se cargaron, falla indicando el usuario ausente")
	void faltaUsuario() {
		when(usuarios.buscarIdSistema()).thenReturn(Optional.of(1L));
		when(usuarios.buscarIdPorUsuario("admin")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> crear("a", "b", "c").run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("admin");
	}

}
