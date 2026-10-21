/**
 * Nombre: TarifaIvaRepository.java
 * Descripción: Repositorio de solo lectura de la tabla tarifa_iva.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consulta de la tabla {@code tarifa_iva} (RN-08).
 */
public interface TarifaIvaRepository extends JpaRepository<TarifaIva, Short> {

	/**
	 * Tarifas activas ordenadas por porcentaje.
	 *
	 * @return tarifas activas
	 */
	List<TarifaIva> findByActivoTrueOrderByPorcentajeAsc();

}
