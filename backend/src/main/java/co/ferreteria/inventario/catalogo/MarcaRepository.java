/**
 * Nombre: MarcaRepository.java
 * Descripción: Repositorio de solo lectura de la tabla marca.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consulta de la tabla {@code marca} para selectores y validación de productos (RF-PRO-001).
 */
public interface MarcaRepository extends JpaRepository<Marca, Long> {

	/**
	 * Marcas activas ordenadas por nombre.
	 *
	 * @return marcas activas
	 */
	List<Marca> findByActivoTrueOrderByNombreAsc();

}
