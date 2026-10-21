/**
 * Nombre: ProductoService.java
 * Descripción: Reglas del CRUD de productos: validación de referencias, duplicados, borrado lógico y
 *              visibilidad de costos.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ferreteria.inventario.catalogo.dto.ProductoPeticion;
import co.ferreteria.inventario.catalogo.dto.ProductoRespuesta;
import co.ferreteria.inventario.common.error.CampoInvalidoException;
import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.OperacionRechazadaException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.common.web.Pagina;

/**
 * Reglas del catálogo de productos (RF-PRO-001, RF-PRO-003, RN-05, RN-08, RN-18, RN-20).
 *
 * <ul>
 *   <li>Marca, categoría y tarifa de IVA deben existir y estar activas; al editar se admite conservar
 *       una que se haya desactivado después. Una referencia no válida responde 400 en su campo.</li>
 *   <li>No se repite el nombre dentro de la misma marca, sin distinguir mayúsculas (409).</li>
 *   <li>RN-05: la unidad base no cambia si el producto ya tiene presentaciones o lotes, porque
 *       alteraría el significado de los factores y de las existencias. RN-18: el tipo de control no
 *       cambia si ya tiene lotes.</li>
 *   <li>DELETE es borrado lógico: el producto pasa a INACTIVO y se conserva (RF-CFG-001).</li>
 *   <li>Solo quien tiene {@code COSTOS_VER} recibe margen y último costo (matriz 2.2).</li>
 * </ul>
 * <p>Cada escritura queda auditada por el trigger de la base en la misma transacción (RN-28).</p>
 */
@Service
public class ProductoService {

	/** Permiso que habilita ver costos y utilidad (matriz 2.2). */
	public static final String PERMISO_COSTOS = "COSTOS_VER";

	/** Campos por los que se puede ordenar y su propiedad JPA. */
	public static final Map<String, String> CAMPOS_ORDEN = Map.of(
			"id", "id",
			"nombre", "nombre",
			"creadoEn", "creadoEn",
			"marca", "marca.nombre",
			"categoria", "categoria.nombre");

	private final ProductoRepository productos;
	private final MarcaRepository marcas;
	private final CategoriaRepository categorias;
	private final UnidadMedidaRepository unidades;
	private final TarifaIvaRepository tarifas;

	/**
	 * Crea el servicio.
	 *
	 * @param productos repositorio de productos
	 * @param marcas repositorio de marcas
	 * @param categorias repositorio de categorías
	 * @param unidades repositorio de unidades de medida
	 * @param tarifas repositorio de tarifas de IVA
	 */
	public ProductoService(ProductoRepository productos, MarcaRepository marcas, CategoriaRepository categorias,
			UnidadMedidaRepository unidades, TarifaIvaRepository tarifas) {
		this.productos = productos;
		this.marcas = marcas;
		this.categorias = categorias;
		this.unidades = unidades;
		this.tarifas = tarifas;
	}

	/**
	 * Busca productos paginados con filtros (RF-PRO-003).
	 *
	 * @param q texto contenido en el nombre, sin distinguir mayúsculas
	 * @param categoriaId id de la categoría
	 * @param estado estado del producto
	 * @param pageable página, tamaño y orden pedidos por el cliente
	 * @return página de productos
	 */
	@Transactional(readOnly = true)
	public Pagina<ProductoRespuesta> listar(String q, Long categoriaId, EstadoProducto estado, Pageable pageable) {
		Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), traducirOrden(pageable.getSort()));
		boolean verCostos = puedeVerCostos();
		return Pagina.desde(productos.findAll(ProductoEspecificaciones.filtrar(q, categoriaId, estado), pagina),
				p -> ProductoRespuesta.desde(p, verCostos));
	}

	/**
	 * Consulta un producto, activo o inactivo.
	 *
	 * @param id id del producto
	 * @return producto
	 */
	@Transactional(readOnly = true)
	public ProductoRespuesta obtener(long id) {
		return ProductoRespuesta.desde(buscar(id), puedeVerCostos());
	}

	/**
	 * Registra un producto (RF-PRO-001).
	 *
	 * @param datos datos validados
	 * @return producto creado
	 */
	@Transactional
	public ProductoRespuesta crear(ProductoPeticion datos) {
		String nombre = datos.nombre().trim();
		Marca marca = marcaUtilizable(datos.marcaId(), null);
		exigirNoDuplicado(marca, nombre, -1L);
		Producto producto = new Producto(nombre, marca, categoriaUtilizable(datos.categoriaId(), null),
				unidad(datos.unidadBase()), tarifaUtilizable(datos.tarifaIvaId(), null));
		copiarDatosSimples(datos, producto);
		producto.setEstado(datos.estado() == null ? EstadoProducto.ACTIVO : datos.estado());
		return ProductoRespuesta.desde(productos.saveAndFlush(producto), puedeVerCostos());
	}

	/**
	 * Reemplaza los datos de un producto.
	 *
	 * @param id id del producto
	 * @param datos datos validados
	 * @return producto actualizado
	 */
	@Transactional
	public ProductoRespuesta actualizar(long id, ProductoPeticion datos) {
		Producto producto = buscar(id);
		String nombre = datos.nombre().trim();
		Marca marca = marcaUtilizable(datos.marcaId(), producto.getMarca());
		exigirNoDuplicado(marca, nombre, id);

		UnidadMedida unidad = unidad(datos.unidadBase());
		if (!unidad.getCodigo().equals(producto.getUnidadBase().getCodigo())
				&& (productos.tienePresentaciones(id) || productos.tieneLotes(id))) {
			throw new OperacionRechazadaException(CodigoError.OPERACION_NO_PERMITIDA, "producto.unidad_no_editable");
		}
		if (datos.control() != producto.getControl() && productos.tieneLotes(id)) {
			throw new OperacionRechazadaException(CodigoError.OPERACION_NO_PERMITIDA, "producto.control_no_editable");
		}

		producto.setNombre(nombre);
		producto.setMarca(marca);
		producto.setCategoria(categoriaUtilizable(datos.categoriaId(), producto.getCategoria()));
		producto.setUnidadBase(unidad);
		producto.setTarifaIva(tarifaUtilizable(datos.tarifaIvaId(), producto.getTarifaIva()));
		copiarDatosSimples(datos, producto);
		if (datos.estado() != null) {
			producto.setEstado(datos.estado());
		}
		return ProductoRespuesta.desde(productos.saveAndFlush(producto), puedeVerCostos());
	}

	/**
	 * Borrado lógico: deja el producto INACTIVO. Si ya lo estaba, no escribe nada (ni auditoría).
	 *
	 * @param id id del producto
	 */
	@Transactional
	public void inactivar(long id) {
		Producto producto = productos.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("entidad.producto", id));
		if (producto.getEstado() != EstadoProducto.INACTIVO) {
			producto.setEstado(EstadoProducto.INACTIVO);
			productos.saveAndFlush(producto);
		}
	}

	// ------------------------------------------------------------------ utilidades

	private Producto buscar(long id) {
		return productos.buscarCompleto(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("entidad.producto", id));
	}

	private void exigirNoDuplicado(Marca marca, String nombre, long idExcluido) {
		if (productos.existeDuplicado(marca.getId(), nombre, idExcluido)) {
			throw new ConflictoException("producto.duplicado", nombre, marca.getNombre());
		}
	}

	private static void copiarDatosSimples(ProductoPeticion datos, Producto producto) {
		producto.setPorcentajeIncremento(datos.porcentajeIncremento());
		producto.setControl(datos.control());
		producto.setPlazoGarantiaMeses(datos.plazoGarantiaMeses() == null ? null : datos.plazoGarantiaMeses().shortValue());
		String descripcion = datos.descripcion() == null ? null : datos.descripcion().trim();
		producto.setDescripcion(descripcion == null || descripcion.isEmpty() ? null : descripcion);
	}

	/**
	 * Marca activa, o la marca actual del producto aunque se haya desactivado después.
	 */
	private Marca marcaUtilizable(Long id, Marca actual) {
		return marcas.findById(id)
				.filter(m -> m.isActivo() || esLaMisma(actual == null ? null : actual.getId(), m.getId()))
				.orElseThrow(() -> new CampoInvalidoException("marcaId", "catalogo.referencia.no_disponible", String.valueOf(id)));
	}

	private Categoria categoriaUtilizable(Long id, Categoria actual) {
		return categorias.findById(id)
				.filter(c -> c.isActivo() || esLaMisma(actual == null ? null : actual.getId(), c.getId()))
				.orElseThrow(() -> new CampoInvalidoException("categoriaId", "catalogo.referencia.no_disponible", String.valueOf(id)));
	}

	private TarifaIva tarifaUtilizable(Short id, TarifaIva actual) {
		return tarifas.findById(id)
				.filter(t -> t.isActivo() || esLaMisma(actual == null ? null : actual.getId(), t.getId()))
				.orElseThrow(() -> new CampoInvalidoException("tarifaIvaId", "catalogo.referencia.no_disponible", String.valueOf(id)));
	}

	private UnidadMedida unidad(String codigo) {
		return unidades.findById(codigo.trim())
				.orElseThrow(() -> new CampoInvalidoException("unidadBase", "catalogo.referencia.no_disponible", codigo));
	}

	private static boolean esLaMisma(Object actual, Object candidato) {
		return actual != null && Objects.equals(actual, candidato);
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

	private static boolean puedeVerCostos() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		return autenticacion != null && autenticacion.getAuthorities().stream()
				.anyMatch(a -> PERMISO_COSTOS.equals(a.getAuthority()));
	}

}
