/**
 * Nombre: CategoriaRepository.java
 * Descripción: Repositorio de solo lectura de la tabla categoria.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consulta de la tabla {@code categoria} para selectores y validación de productos (RF-PRO-001).
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

	/**
	 * Categorías activas ordenadas por nombre.
	 *
	 * @return categorías activas
	 */
	List<Categoria> findByActivoTrueOrderByNombreAsc();

}
