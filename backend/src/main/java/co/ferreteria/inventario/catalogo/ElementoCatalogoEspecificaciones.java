/**
 * Nombre: ElementoCatalogoEspecificaciones.java
 * Descripción: Filtros JPA (Specification) de la búsqueda de marcas y categorías: texto en el nombre y estado.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros de {@code GET /api/v1/marcas} y {@code GET /api/v1/categorias} (RF-CFG-001).
 */
public final class ElementoCatalogoEspecificaciones {

	private static final char ESCAPE = '\\';

	private ElementoCatalogoEspecificaciones() {
		// Clase de utilidades.
	}

	/**
	 * Combina los filtros presentes.
	 *
	 * @param q texto contenido en el nombre, sin distinguir mayúsculas
	 * @param estado estado buscado
	 * @param <E> entidad del catálogo
	 * @return especificación con los filtros presentes
	 */
	public static <E extends ElementoCatalogo> Specification<E> filtrar(String q, EstadoCatalogo estado) {
		return Specification.allOf(nombreContiene(q), conEstado(estado));
	}

	/**
	 * Nombre que contiene el texto, sin distinguir mayúsculas. Los comodines {@code %} y {@code _} del
	 * texto se buscan literalmente.
	 *
	 * @param q texto a buscar
	 * @param <E> entidad del catálogo
	 * @return especificación, o una que no filtra si {@code q} está vacío
	 */
	public static <E extends ElementoCatalogo> Specification<E> nombreContiene(String q) {
		if (q == null || q.isBlank()) {
			return Specification.unrestricted();
		}
		String patron = "%" + escaparComodines(q.trim().toLowerCase(Locale.ROOT)) + "%";
		return (raiz, consulta, cb) -> cb.like(cb.lower(raiz.get("nombre")), patron, ESCAPE);
	}

	/**
	 * Elementos en un estado.
	 *
	 * @param estado estado buscado
	 * @param <E> entidad del catálogo
	 * @return especificación, o una que no filtra si es nulo
	 */
	public static <E extends ElementoCatalogo> Specification<E> conEstado(EstadoCatalogo estado) {
		if (estado == null) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, cb) -> cb.equal(raiz.get("activo"), estado.comoActivo());
	}

	private static String escaparComodines(String texto) {
		return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
