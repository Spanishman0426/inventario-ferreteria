/**
 * Nombre: Pagina.java
 * Descripción: Respuesta paginada estable de la API, independiente de la serialización de Spring Data.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Página de resultados.
 *
 * @param contenido elementos de la página
 * @param pagina número de página, desde 0
 * @param tamano tamaño de página pedido
 * @param totalElementos total de elementos que cumplen el filtro
 * @param totalPaginas total de páginas
 * @param <T> tipo de los elementos
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

	/** Tamaño de página por defecto. */
	public static final int TAMANO_POR_DEFECTO = 20;
	/** Tamaño de página máximo admitido. */
	public static final int TAMANO_MAXIMO = 100;

	/**
	 * Convierte una página de Spring Data.
	 *
	 * @param pagina página de origen
	 * @param mapeo conversión de cada elemento
	 * @param <E> tipo de origen
	 * @param <T> tipo de destino
	 * @return página convertida
	 */
	public static <E, T> Pagina<T> desde(Page<E> pagina, Function<E, T> mapeo) {
		return new Pagina<>(pagina.getContent().stream().map(mapeo).toList(), pagina.getNumber(), pagina.getSize(),
				pagina.getTotalElements(), pagina.getTotalPages());
	}

	/**
	 * Construye una página a partir de un conteo total.
	 *
	 * @param contenido elementos de la página
	 * @param pagina número de página
	 * @param tamano tamaño de página
	 * @param total total de elementos
	 * @param <T> tipo de los elementos
	 * @return página
	 */
	public static <T> Pagina<T> de(List<T> contenido, int pagina, int tamano, long total) {
		int paginas = tamano == 0 ? 0 : (int) ((total + tamano - 1) / tamano);
		return new Pagina<>(contenido, pagina, tamano, total, paginas);
	}

}
