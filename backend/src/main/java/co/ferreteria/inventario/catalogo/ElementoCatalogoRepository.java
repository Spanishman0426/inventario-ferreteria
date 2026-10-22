/**
 * Nombre: ElementoCatalogoRepository.java
 * Descripción: Consultas comunes de los repositorios de catálogo simple (marca y categoría).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import org.springframework.data.repository.NoRepositoryBean;

/**
 * Consultas que {@link ElementoCatalogoService} necesita de cualquier catálogo simple (RF-CFG-001).
 *
 * @param <E> entidad del catálogo
 */
@NoRepositoryBean
public interface ElementoCatalogoRepository<E extends ElementoCatalogo> {

	/**
	 * Si existe otro elemento con el mismo nombre sin distinguir mayúsculas.
	 *
	 * @param nombre nombre a comparar
	 * @param id id excluido (el elemento que se edita); {@code -1} al crear
	 * @return {@code true} si hay un duplicado
	 */
	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

}
