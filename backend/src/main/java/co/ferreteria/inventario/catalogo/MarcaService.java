/**
 * Nombre: MarcaService.java
 * Descripción: Administración del catálogo de marcas (RF-CFG-001) sobre las reglas comunes de ElementoCatalogoService.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Búsqueda, alta, edición y activación de marcas (RF-CFG-001). Las reglas están en
 * {@link ElementoCatalogoService}.
 */
@Service
public class MarcaService extends ElementoCatalogoService<Marca, MarcaRepository> {

	private final MarcaRepository repositorio;

	/**
	 * Crea el servicio.
	 *
	 * @param repositorio repositorio de marcas
	 */
	public MarcaService(MarcaRepository repositorio) {
		super(repositorio, "entidad.marca", "marca.duplicada", Marca::new);
		this.repositorio = repositorio;
	}

	/**
	 * Marcas activas ordenadas por nombre.
	 *
	 * @return marcas activas
	 */
	@Override
	protected List<Marca> activosOrdenados() {
		return repositorio.findByActivoTrueOrderByNombreAsc();
	}

}
