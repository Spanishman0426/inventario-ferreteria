/**
 * Nombre: UsuarioRepository.java
 * Descripción: Repositorio JPA de usuarios, con consultas de inicio de sesión y de administradores activos.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * Acceso a la tabla {@code usuario} (RF-SEG-001).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	/**
	 * Id de un usuario por su nombre, sin cargar la entidad.
	 *
	 * @param usuario nombre de usuario
	 * @return id si existe
	 */
	@Query("select u.id from Usuario u where u.usuario = :usuario")
	Optional<Long> buscarIdPorUsuario(@Param("usuario") String usuario);

	/**
	 * Usuario por id con bloqueo de fila ({@code SELECT ... FOR UPDATE}), para que dos inicios de
	 * sesión simultáneos no pierdan intentos fallidos.
	 *
	 * @param id id del usuario
	 * @return usuario bloqueado hasta el fin de la transacción
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.id = :id")
	Optional<Usuario> bloquearPorId(@Param("id") Long id);

	/**
	 * Usuario con sus roles y permisos cargados.
	 *
	 * @param id id del usuario
	 * @return usuario con roles y permisos
	 */
	@Query("select distinct u from Usuario u left join fetch u.roles r left join fetch r.permisos where u.id = :id")
	Optional<Usuario> buscarConPermisos(@Param("id") Long id);

	/**
	 * Usuarios humanos (sin el usuario Sistema), paginados.
	 *
	 * @param pageable página y orden
	 * @return página de usuarios
	 */
	Page<Usuario> findByEsSistemaFalse(Pageable pageable);

	/**
	 * Indica si existe un usuario con ese nombre.
	 *
	 * @param usuario nombre de usuario
	 * @return {@code true} si existe
	 */
	boolean existsByUsuario(String usuario);

	/**
	 * Indica si otro usuario ya usa el correo (sin distinguir mayúsculas).
	 *
	 * @param correo correo
	 * @param id id del usuario que se excluye de la búsqueda
	 * @return {@code true} si otro usuario lo usa
	 */
	boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

	/**
	 * Indica si algún usuario usa el correo (sin distinguir mayúsculas).
	 *
	 * @param correo correo
	 * @return {@code true} si existe
	 */
	boolean existsByCorreoIgnoreCase(String correo);

	/**
	 * Ids de los administradores activos, bloqueando sus filas hasta el fin de la transacción para que
	 * dos operaciones simultáneas no dejen el sistema sin administrador.
	 *
	 * @return ids de los usuarios activos con rol ADMIN
	 */
	@Query(value = """
			SELECT u.id FROM usuario u
			  JOIN usuario_rol ur ON ur.usuario_id = u.id
			  JOIN rol r ON r.id = ur.rol_id
			 WHERE u.activo AND r.codigo = 'ADMIN'
			 ORDER BY u.id
			   FOR UPDATE OF u
			""", nativeQuery = true)
	List<Long> bloquearAdministradoresActivos();

	/**
	 * Id del usuario Sistema (tareas programadas).
	 *
	 * @return id del usuario técnico
	 */
	@Query("select u.id from Usuario u where u.esSistema = true")
	Optional<Long> buscarIdSistema();

}
