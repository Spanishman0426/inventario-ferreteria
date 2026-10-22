/**
 * Nombre: CategoriaRepository.java
 * Descripción: Repositorio de la tabla categoria: búsqueda paginada, activos para selectores y verificación de duplicados.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acceso a la tabla {@code categoria} (RF-PRO-001, RF-CFG-001).
 */
public interface CategoriaRepository extends ElementoCatalogoRepository<Categoria>, JpaRepository<Categoria, Long>,
		JpaSpecificationExecutor<Categoria> {

	/**
	 * Elementos activos ordenados por nombre, para los selectores.
	 *
	 * @return elementos activos
	 */
	List<Categoria> findByActivoTrueOrderByNombreAsc();

}
