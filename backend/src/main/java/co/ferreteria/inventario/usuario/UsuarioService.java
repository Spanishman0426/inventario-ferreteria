/**
 * Nombre: UsuarioService.java
 * Descripción: Administración de usuarios: alta, edición, roles, activación y contraseña.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ferreteria.inventario.common.auditoria.ContextoAuditoria;
import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.OperacionRechazadaException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.usuario.dto.RolRespuesta;
import co.ferreteria.inventario.usuario.dto.UsuarioActualizacion;
import co.ferreteria.inventario.usuario.dto.UsuarioCreacion;
import co.ferreteria.inventario.usuario.dto.UsuarioRespuesta;

/**
 * Reglas de la gestión de usuarios (RF-SEG-001, RN-27, RN-28).
 *
 * <ul>
 *   <li>Los usuarios no se eliminan, solo se desactivan.</li>
 *   <li>Las contraseñas se guardan con BCrypt; ninguna respuesta incluye el hash.</li>
 *   <li>Un administrador no puede desactivarse a sí mismo.</li>
 *   <li>Ninguna operación puede dejar el sistema sin administradores activos (desactivar o quitar el
 *       rol ADMIN al último). Se bloquean las filas de los administradores activos para que dos
 *       operaciones simultáneas no lo logren entre ambas.</li>
 *   <li>El usuario Sistema no se edita.</li>
 * </ul>
 * <p>Cada cambio queda auditado por el trigger de la base, firmado por el administrador que lo hizo
 * ({@link co.ferreteria.inventario.common.auditoria.TransaccionAuditadaManager}).</p>
 */
@Service
public class UsuarioService {

	private final UsuarioRepository usuarios;
	private final RolRepository roles;
	private final PasswordEncoder passwordEncoder;
	private final ContextoAuditoria contexto;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarios repositorio de usuarios
	 * @param roles repositorio de roles
	 * @param passwordEncoder BCrypt
	 * @param contexto usuario autenticado
	 */
	public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder passwordEncoder,
			ContextoAuditoria contexto) {
		this.usuarios = usuarios;
		this.roles = roles;
		this.passwordEncoder = passwordEncoder;
		this.contexto = contexto;
	}

	/**
	 * Lista los usuarios (sin el usuario Sistema), ordenados por nombre de usuario.
	 *
	 * @param pagina número de página desde 0
	 * @param tamano tamaño de página
	 * @return página de usuarios
	 */
	@Transactional(readOnly = true)
	public Pagina<UsuarioRespuesta> listar(int pagina, int tamano) {
		return Pagina.desde(usuarios.findByEsSistemaFalse(PageRequest.of(pagina, tamano, Sort.by("usuario"))),
				UsuarioRespuesta::desde);
	}

	/**
	 * Consulta un usuario.
	 *
	 * @param id id del usuario
	 * @return usuario
	 */
	@Transactional(readOnly = true)
	public UsuarioRespuesta obtener(long id) {
		return UsuarioRespuesta.desde(buscarEditable(id));
	}

	/**
	 * Crea un usuario activo con sus roles.
	 *
	 * @param datos datos validados
	 * @return usuario creado
	 */
	@Transactional
	public UsuarioRespuesta crear(UsuarioCreacion datos) {
		if (usuarios.existsByUsuario(datos.usuario())) {
			throw new ConflictoException("usuario.duplicado.usuario", datos.usuario());
		}
		if (usuarios.existsByCorreoIgnoreCase(datos.correo())) {
			throw new ConflictoException("usuario.duplicado.correo", datos.correo());
		}
		Usuario usuario = new Usuario(datos.usuario(), datos.nombreCompleto(), datos.correo(),
				passwordEncoder.encode(datos.contrasena()));
		usuario.asignarRoles(buscarRoles(datos.roles()));
		return UsuarioRespuesta.desde(usuarios.saveAndFlush(usuario));
	}

	/**
	 * Cambia nombre y correo.
	 *
	 * @param id id del usuario
	 * @param datos datos validados
	 * @return usuario actualizado
	 */
	@Transactional
	public UsuarioRespuesta actualizar(long id, UsuarioActualizacion datos) {
		Usuario usuario = buscarEditable(id);
		if (usuarios.existsByCorreoIgnoreCaseAndIdNot(datos.correo(), id)) {
			throw new ConflictoException("usuario.duplicado.correo", datos.correo());
		}
		usuario.setNombreCompleto(datos.nombreCompleto());
		usuario.setCorreo(datos.correo());
		return UsuarioRespuesta.desde(usuarios.saveAndFlush(usuario));
	}

	/**
	 * Reemplaza los roles de un usuario.
	 *
	 * @param id id del usuario
	 * @param codigos códigos de rol
	 * @return usuario actualizado
	 */
	@Transactional
	public UsuarioRespuesta asignarRoles(long id, Set<String> codigos) {
		Usuario usuario = buscarEditable(id);
		List<Rol> nuevos = buscarRoles(codigos);
		boolean pierdeAdmin = usuario.tieneRol(Rol.ADMIN) && nuevos.stream().noneMatch(r -> Rol.ADMIN.equals(r.getCodigo()));
		if (pierdeAdmin && usuario.isActivo()) {
			exigirOtroAdministrador(id);
		}
		usuario.asignarRoles(nuevos);
		return UsuarioRespuesta.desde(usuarios.saveAndFlush(usuario));
	}

	/**
	 * Activa o desactiva una cuenta.
	 *
	 * @param id id del usuario
	 * @param activo nuevo estado
	 * @return usuario actualizado
	 */
	@Transactional
	public UsuarioRespuesta cambiarEstado(long id, boolean activo) {
		Usuario usuario = buscarEditable(id);
		if (!activo && usuario.isActivo()) {
			if (contexto.usuarioId().filter(actual -> actual == id).isPresent()) {
				throw new OperacionRechazadaException(CodigoError.OPERACION_NO_PERMITIDA, "usuario.desactivar_propio");
			}
			if (usuario.tieneRol(Rol.ADMIN)) {
				exigirOtroAdministrador(id);
			}
		}
		usuario.setActivo(activo);
		return UsuarioRespuesta.desde(usuarios.saveAndFlush(usuario));
	}

	/**
	 * Asigna una contraseña nueva y levanta un bloqueo vigente.
	 *
	 * @param id id del usuario
	 * @param contrasena contraseña validada
	 */
	@Transactional
	public void cambiarContrasena(long id, String contrasena) {
		Usuario usuario = buscarEditable(id);
		usuario.setPasswordHash(passwordEncoder.encode(contrasena));
		usuario.setIntentosFallidos((short) 0);
		usuario.setBloqueadoHasta(null);
		usuarios.saveAndFlush(usuario);
	}

	/**
	 * Roles de la matriz 2.2 con sus permisos.
	 *
	 * @return roles ordenados por id
	 */
	@Transactional(readOnly = true)
	public List<RolRespuesta> listarRoles() {
		return roles.listarConPermisos().stream().map(RolRespuesta::desde).toList();
	}

	private Usuario buscarEditable(long id) {
		Usuario usuario = usuarios.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("entidad.usuario", id));
		if (usuario.isEsSistema()) {
			throw new OperacionRechazadaException(CodigoError.OPERACION_NO_PERMITIDA, "usuario.sistema_no_editable");
		}
		return usuario;
	}

	private List<Rol> buscarRoles(Set<String> codigos) {
		List<Rol> encontrados = roles.findByCodigoIn(codigos);
		Set<String> faltantes = new HashSet<>(codigos);
		encontrados.forEach(r -> faltantes.remove(r.getCodigo()));
		if (!faltantes.isEmpty()) {
			throw new RecursoNoEncontradoException("entidad.rol", String.join(", ", faltantes));
		}
		return encontrados;
	}

	/**
	 * Exige que quede al menos un administrador activo distinto de {@code id}.
	 */
	private void exigirOtroAdministrador(long id) {
		List<Long> administradores = usuarios.bloquearAdministradoresActivos();
		if (administradores.stream().noneMatch(otro -> otro != id)) {
			throw new OperacionRechazadaException(CodigoError.OPERACION_NO_PERMITIDA, "usuario.ultimo_administrador");
		}
	}

}
