/**
 * Nombre: RolRepository.java
 * Descripción: Repositorio JPA de roles (solo lectura).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a la tabla {@code rol} con sus permisos (matriz 2.2).
 */
public interface RolRepository extends JpaRepository<Rol, Short> {

	/**
	 * Roles por código.
	 *
	 * @param codigos códigos buscados
	 * @return roles encontrados
	 */
	List<Rol> findByCodigoIn(Collection<String> codigos);

	/**
	 * Todos los roles con sus permisos, ordenados por id.
	 *
	 * @return roles con permisos
	 */
	@Query("select distinct r from Rol r left join fetch r.permisos order by r.id")
	List<Rol> listarConPermisos();

	/**
	 * Rol por código.
	 *
	 * @param codigo código del rol
	 * @return rol, si existe
	 */
	@Query("select r from Rol r where r.codigo = :codigo")
	java.util.Optional<Rol> buscarPorCodigo(@Param("codigo") String codigo);

}
