/**
 * Nombre: CategoriaService.java
 * Descripción: Administración del catálogo de categorías (RF-CFG-001) sobre las reglas comunes de ElementoCatalogoService.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Búsqueda, alta, edición y activación de categorías (RF-CFG-001). Las reglas están en
 * {@link ElementoCatalogoService}.
 */
@Service
public class CategoriaService extends ElementoCatalogoService<Categoria, CategoriaRepository> {

	private final CategoriaRepository repositorio;

	/**
	 * Crea el servicio.
	 *
	 * @param repositorio repositorio de categorías
	 */
	public CategoriaService(CategoriaRepository repositorio) {
		super(repositorio, "entidad.categoria", "categoria.duplicada", Categoria::new);
		this.repositorio = repositorio;
	}

	/**
	 * Categorías activas ordenadas por nombre.
	 *
	 * @return categorías activas
	 */
	@Override
	protected List<Categoria> activosOrdenados() {
		return repositorio.findByActivoTrueOrderByNombreAsc();
	}

}
