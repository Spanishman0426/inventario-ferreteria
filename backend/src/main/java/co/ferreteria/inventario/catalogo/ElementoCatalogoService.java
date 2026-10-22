/**
 * Nombre: ElementoCatalogoService.java
 * Descripción: Reglas comunes de la administración de marcas y categorías: búsqueda, alta, edición del
 *              nombre sin duplicados y activación o desactivación (sin borrado).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import co.ferreteria.inventario.catalogo.dto.ElementoCatalogoPeticion;
import co.ferreteria.inventario.catalogo.dto.ElementoCatalogoRespuesta;
import co.ferreteria.inventario.catalogo.dto.ReferenciaRespuesta;
import co.ferreteria.inventario.common.error.CampoInvalidoException;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.common.web.Pagina;

/**
 * Administración de un catálogo simple (RF-CFG-001). La extienden {@link MarcaService} y
 * {@link CategoriaService}.
 *
 * <ul>
 *   <li>El nombre se recorta y no se repite sin distinguir mayúsculas (409 {@code CONFLICTO_UNICIDAD}).
 *       Si dos altas compiten, el índice único de la base responde 23505 y el manejador global también
 *       responde 409.</li>
 *   <li>No hay borrado: un elemento en uso se desactiva. Los productos que ya lo usan lo conservan
 *       ({@link ProductoService} admite mantener una marca o categoría inactiva).</li>
 *   <li>Cambiar al estado que ya tiene no escribe nada, por lo que tampoco se audita de nuevo.</li>
 * </ul>
 * <p>Cada escritura queda auditada por el trigger de la base en la misma transacción (RN-28).</p>
 *
 * @param <E> entidad del catálogo
 * @param <R> repositorio de la entidad
 */
public abstract class ElementoCatalogoService<E extends ElementoCatalogo,
		R extends ElementoCatalogoRepository<E> & JpaRepository<E, Long> & JpaSpecificationExecutor<E>> {

	/** Campos por los que se puede ordenar y su propiedad JPA. */
	public static final Map<String, String> CAMPOS_ORDEN = Map.of("id", "id", "nombre", "nombre");

	private final R repositorio;
	private final String claveEntidad;
	private final String claveDuplicado;
	private final Function<String, E> fabrica;

	/**
	 * Crea el servicio.
	 *
	 * @param repositorio repositorio de la entidad
	 * @param claveEntidad clave de mensaje del nombre de la entidad (por ejemplo {@code entidad.marca})
	 * @param claveDuplicado clave de mensaje del conflicto por nombre repetido
	 * @param fabrica crea una entidad activa a partir del nombre
	 */
	protected ElementoCatalogoService(R repositorio, String claveEntidad, String claveDuplicado,
			Function<String, E> fabrica) {
		this.repositorio = repositorio;
		this.claveEntidad = claveEntidad;
		this.claveDuplicado = claveDuplicado;
		this.fabrica = fabrica;
	}

	/**
	 * Busca elementos paginados con filtros.
	 *
	 * @param q texto contenido en el nombre, sin distinguir mayúsculas
	 * @param estado estado buscado; nulo para todos
	 * @param pageable página, tamaño y orden pedidos por el cliente
	 * @return página de elementos
	 */
	@Transactional(readOnly = true)
	public Pagina<ElementoCatalogoRespuesta> listar(String q, EstadoCatalogo estado, Pageable pageable) {
		Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				traducirOrden(pageable.getSort()));
		return Pagina.desde(repositorio.findAll(ElementoCatalogoEspecificaciones.<E>filtrar(q, estado), pagina),
				ElementoCatalogoRespuesta::desde);
	}

	/**
	 * Elementos activos ordenados por nombre, para los selectores del formulario de producto.
	 *
	 * @return lista completa de activos
	 */
	@Transactional(readOnly = true)
	public List<ReferenciaRespuesta> listarActivos() {
		return activosOrdenados().stream()
				.map(e -> new ReferenciaRespuesta(e.getId(), e.getNombre()))
				.toList();
	}

	/**
	 * Registra un elemento activo.
	 *
	 * @param datos datos validados
	 * @return elemento creado
	 */
	@Transactional
	public ElementoCatalogoRespuesta crear(ElementoCatalogoPeticion datos) {
		String nombre = datos.nombre().trim();
		exigirNoDuplicado(nombre, -1L);
		return ElementoCatalogoRespuesta.desde(repositorio.saveAndFlush(fabrica.apply(nombre)));
	}

	/**
	 * Cambia el nombre de un elemento. Si el nombre no cambia, no escribe nada.
	 *
	 * @param id id del elemento
	 * @param datos datos validados
	 * @return elemento actualizado
	 */
	@Transactional
	public ElementoCatalogoRespuesta actualizar(long id, ElementoCatalogoPeticion datos) {
		E elemento = buscar(id);
		String nombre = datos.nombre().trim();
		if (!nombre.equals(elemento.getNombre())) {
			exigirNoDuplicado(nombre, id);
			elemento.setNombre(nombre);
			elemento = repositorio.saveAndFlush(elemento);
		}
		return ElementoCatalogoRespuesta.desde(elemento);
	}

	/**
	 * Activa o desactiva un elemento. Si ya estaba en ese estado, no escribe nada.
	 *
	 * @param id id del elemento
	 * @param estado estado nuevo
	 * @return elemento con su estado
	 */
	@Transactional
	public ElementoCatalogoRespuesta cambiarEstado(long id, EstadoCatalogo estado) {
		E elemento = buscar(id);
		if (elemento.isActivo() != estado.comoActivo()) {
			elemento.setActivo(estado.comoActivo());
			elemento = repositorio.saveAndFlush(elemento);
		}
		return ElementoCatalogoRespuesta.desde(elemento);
	}

	/**
	 * Activos ordenados por nombre; cada subclase lo resuelve con su consulta derivada.
	 *
	 * @return elementos activos
	 */
	protected abstract List<E> activosOrdenados();

	// ------------------------------------------------------------------ utilidades

	private E buscar(long id) {
		return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(claveEntidad, id));
	}

	private void exigirNoDuplicado(String nombre, long idExcluido) {
		if (repositorio.existsByNombreIgnoreCaseAndIdNot(nombre, idExcluido)) {
			throw new ConflictoException(claveDuplicado, nombre);
		}
	}

	/**
	 * Traduce el orden pedido a propiedades JPA, rechaza campos desconocidos y agrega {@code id} como
	 * desempate para que la paginación sea estable.
	 */
	private static Sort traducirOrden(Sort pedido) {
		List<Sort.Order> ordenes = new ArrayList<>();
		boolean conId = false;
		for (Sort.Order orden : pedido) {
			String propiedad = CAMPOS_ORDEN.get(orden.getProperty());
			if (propiedad == null) {
				throw new CampoInvalidoException("sort", "paginacion.orden_no_valido", orden.getProperty(),
						String.join(", ", CAMPOS_ORDEN.keySet().stream().sorted().toList()));
			}
			conId |= "id".equals(propiedad);
			ordenes.add(new Sort.Order(orden.getDirection(), propiedad));
		}
		if (ordenes.isEmpty()) {
			ordenes.add(Sort.Order.asc("nombre"));
		}
		if (!conId) {
			ordenes.add(Sort.Order.asc("id"));
		}
		return Sort.by(ordenes);
	}

}
