/**
 * Nombre: ProductoEspecificaciones.java
 * Descripción: Filtros de búsqueda de productos (nombre, categoría y estado) como especificaciones JPA.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros combinables de RF-PRO-003. Un filtro nulo o vacío no restringe la búsqueda.
 */
public final class ProductoEspecificaciones {

	/** Carácter de escape para los comodines de LIKE. */
	private static final char ESCAPE = '\\';

	private ProductoEspecificaciones() {
		// Clase de utilidades.
	}

	/**
	 * Combina los tres filtros.
	 *
	 * @param q texto contenido en el nombre, sin distinguir mayúsculas
	 * @param categoriaId id de la categoría
	 * @param estado estado del producto
	 * @return especificación con los filtros presentes
	 */
	public static Specification<Producto> filtrar(String q, Long categoriaId, EstadoProducto estado) {
		return Specification.allOf(nombreContiene(q), deCategoria(categoriaId), conEstado(estado));
	}

	/**
	 * Nombre que contiene el texto, sin distinguir mayúsculas. Usa {@code lower(nombre) LIKE ...}, que
	 * aprovecha el índice trigram {@code ix_producto_nombre_trgm}. Los comodines {@code %} y {@code _}
	 * del texto se buscan literalmente.
	 *
	 * @param q texto a buscar
	 * @return especificación, o una que no filtra si {@code q} está vacío
	 */
	public static Specification<Producto> nombreContiene(String q) {
		if (q == null || q.isBlank()) {
			return Specification.unrestricted();
		}
		String patron = "%" + escaparComodines(q.trim().toLowerCase(Locale.ROOT)) + "%";
		return (raiz, consulta, cb) -> cb.like(cb.lower(raiz.get("nombre")), patron, ESCAPE);
	}

	/**
	 * Productos de una categoría.
	 *
	 * @param categoriaId id de la categoría
	 * @return especificación, o una que no filtra si es nulo
	 */
	public static Specification<Producto> deCategoria(Long categoriaId) {
		if (categoriaId == null) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, cb) -> cb.equal(raiz.get("categoria").get("id"), categoriaId);
	}

	/**
	 * Productos en un estado.
	 *
	 * @param estado estado buscado
	 * @return especificación, o una que no filtra si es nulo
	 */
	public static Specification<Producto> conEstado(EstadoProducto estado) {
		if (estado == null) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado);
	}

	private static String escaparComodines(String texto) {
		return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
