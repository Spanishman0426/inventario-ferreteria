/**
 * Nombre: TerceroRepository.java
 * Descripción: Repositorio JPA de terceros con búsqueda por especificaciones y control de duplicados.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acceso a la tabla {@code tercero} (RF-TER-001).
 */
public interface TerceroRepository extends JpaRepository<Tercero, Long>, JpaSpecificationExecutor<Tercero> {

	/**
	 * Indica si otro tercero ya tiene la identificación (RN-26: identificación única por tipo).
	 *
	 * @param tipo CC o NIT
	 * @param numero número de identificación
	 * @param id id excluido (el propio tercero al editar; -1 al crear)
	 * @return {@code true} si existe otro con la misma identificación
	 */
	boolean existsByTipoIdentificacionAndNumeroIdentificacionAndIdNot(TipoIdentificacion tipo, String numero, Long id);

}
