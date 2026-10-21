/**
 * Nombre: ProductoRepository.java
 * Descripción: Repositorio JPA de productos con búsqueda paginada y comprobación de duplicados.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a la tabla {@code producto} (RF-PRO-001, RF-PRO-003).
 *
 * <p>Las consultas que devuelven productos a la API cargan marca, categoría, unidad e IVA en la misma
 * consulta para evitar N+1.</p>
 */
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

	/**
	 * Busca productos con filtros, con sus relaciones cargadas.
	 *
	 * @param filtro filtros de búsqueda
	 * @param pageable página y orden
	 * @return página de productos
	 */
	@Override
	@EntityGraph(attributePaths = {"marca", "categoria", "unidadBase", "tarifaIva"})
	Page<Producto> findAll(Specification<Producto> filtro, Pageable pageable);

	/**
	 * Producto por id con sus relaciones cargadas.
	 *
	 * @param id id del producto
	 * @return producto si existe
	 */
	@EntityGraph(attributePaths = {"marca", "categoria", "unidadBase", "tarifaIva"})
	@Query("select p from Producto p where p.id = :id")
	Optional<Producto> buscarCompleto(@Param("id") Long id);

	/**
	 * Indica si otro producto de la misma marca ya usa el nombre, sin distinguir mayúsculas
	 * (equivale al índice {@code ux_producto_marca_nombre}).
	 *
	 * @param marcaId id de la marca
	 * @param nombre nombre normalizado
	 * @param idExcluido id del producto que se edita, o {@code -1} al crear
	 * @return {@code true} si hay duplicado
	 */
	@Query("""
			select count(p) > 0 from Producto p
			 where p.marca.id = :marcaId and lower(p.nombre) = lower(:nombre) and p.id <> :idExcluido
			""")
	boolean existeDuplicado(@Param("marcaId") Long marcaId, @Param("nombre") String nombre,
			@Param("idExcluido") Long idExcluido);

	/**
	 * Indica si el producto tiene presentaciones registradas.
	 *
	 * @param id id del producto
	 * @return {@code true} si tiene al menos una
	 */
	@Query(value = "SELECT EXISTS (SELECT 1 FROM presentacion WHERE producto_id = :id)", nativeQuery = true)
	boolean tienePresentaciones(@Param("id") Long id);

	/**
	 * Indica si el producto tiene lotes (existencias o historial).
	 *
	 * @param id id del producto
	 * @return {@code true} si tiene al menos uno
	 */
	@Query(value = "SELECT EXISTS (SELECT 1 FROM lote WHERE producto_id = :id)", nativeQuery = true)
	boolean tieneLotes(@Param("id") Long id);

}
