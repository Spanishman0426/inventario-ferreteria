/**
 * Nombre: ProductoController.java
 * Descripción: API REST del CRUD de productos (/api/v1/productos).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.ferreteria.inventario.catalogo.dto.ProductoPeticion;
import co.ferreteria.inventario.catalogo.dto.ProductoRespuesta;
import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * CRUD de productos (RF-PRO-001, RF-PRO-003).
 *
 * <p>Lectura con {@code CATALOGO_CONSULTAR} (todos los roles); escritura con {@code CATALOGO_EDITAR}
 * (administrador, gerente y jefe de inventario). DELETE es un borrado lógico: el producto queda
 * INACTIVO, porque un elemento de catálogo en uso no se elimina (RF-CFG-001).</p>
 */
@RestController
@RequestMapping(path = "/api/v1/productos", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Productos", description = "Catálogo de productos (RF-PRO-001, RF-PRO-003)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@ApiResponse(responseCode = "401", ref = OpenApiConfig.REF_401)
@ApiResponse(responseCode = "403", ref = OpenApiConfig.REF_403)
public class ProductoController {

	private final ProductoService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de productos
	 */
	public ProductoController(ProductoService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Busca productos paginados con filtros.
	 *
	 * @param q texto contenido en el nombre
	 * @param categoriaId id de la categoría
	 * @param estado estado del producto
	 * @param pageable página ({@code page}), tamaño ({@code size}) y orden ({@code sort})
	 * @return página de productos
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('CATALOGO_CONSULTAR')")
	@Operation(operationId = "listarProductos", summary = "Buscar productos",
			description = "Lista paginada. `page` empieza en 0; `size` va de 1 a 100 (20 por defecto; un valor "
					+ "mayor se limita a 100). `sort` admite `id`, `nombre`, `creadoEn`, `marca` y `categoria`, "
					+ "con `,asc` o `,desc`, y puede repetirse; por defecto `nombre,asc`. Sin el permiso "
					+ "COSTOS_VER no se incluyen `porcentajeIncremento` ni `ultimoCostoBase`.")
	@ApiResponse(responseCode = "200", description = "Página de productos")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	public Pagina<ProductoRespuesta> listar(
			@Parameter(description = "Texto contenido en el nombre, sin distinguir mayúsculas.", example = "martillo")
			@RequestParam(required = false) @Size(max = 150) String q,
			@Parameter(description = "Id de la categoría.", example = "1")
			@RequestParam(required = false) Long categoriaId,
			@Parameter(description = "Estado del producto.")
			@RequestParam(required = false) EstadoProducto estado,
			@ParameterObject @PageableDefault(size = Pagina.TAMANO_POR_DEFECTO, sort = "nombre",
					direction = Sort.Direction.ASC) Pageable pageable) {
		return servicio.listar(q, categoriaId, estado, pageable);
	}

	/**
	 * Consulta un producto.
	 *
	 * @param id id del producto
	 * @return producto
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('CATALOGO_CONSULTAR')")
	@Operation(operationId = "obtenerProducto", summary = "Consultar un producto", description = "Devuelve también los productos INACTIVOS.")
	@ApiResponse(responseCode = "200", description = "Producto")
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	public ProductoRespuesta obtener(@Parameter(description = "Id del producto.", example = "1") @PathVariable long id) {
		return servicio.obtener(id);
	}

	/**
	 * Registra un producto.
	 *
	 * @param datos datos del producto
	 * @return 201 con el producto y la cabecera {@code Location}
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('CATALOGO_EDITAR')")
	@Operation(operationId = "crearProducto", summary = "Registrar producto",
			description = "Marca, categoría y tarifa de IVA deben existir y estar activas; la unidad base debe existir. "
					+ "`plazoGarantiaMeses` es obligatorio si y solo si `control` es GARANTIA. El nombre no se "
					+ "repite dentro de la misma marca, sin distinguir mayúsculas.")
	@ApiResponse(responseCode = "201", description = "Creado",
			headers = @Header(name = "Location", description = "URL del producto creado",
					schema = @Schema(type = "string", format = "uri", example = "/api/v1/productos/10")))
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "409", ref = OpenApiConfig.REF_409)
	@ApiResponse(responseCode = "422", ref = OpenApiConfig.REF_422)
	public ResponseEntity<ProductoRespuesta> crear(@Valid @RequestBody ProductoPeticion datos) {
		ProductoRespuesta creado = servicio.crear(datos);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}")
				.buildAndExpand(creado.id()).toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	/**
	 * Reemplaza los datos de un producto.
	 *
	 * @param id id del producto
	 * @param datos datos completos del producto
	 * @return producto actualizado
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('CATALOGO_EDITAR')")
	@Operation(operationId = "actualizarProducto", summary = "Editar producto",
			description = "Reemplaza todos los campos; `estado` permite reactivar un producto. La unidad base no "
					+ "cambia si el producto tiene presentaciones o lotes (RN-05), ni el control si tiene lotes "
					+ "(RN-18): ambos casos responden 409 OPERACION_NO_PERMITIDA.")
	@ApiResponse(responseCode = "200", description = "Actualizado")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	@ApiResponse(responseCode = "409", ref = OpenApiConfig.REF_409)
	@ApiResponse(responseCode = "422", ref = OpenApiConfig.REF_422)
	public ProductoRespuesta actualizar(@Parameter(description = "Id del producto.", example = "1") @PathVariable long id,
			@Valid @RequestBody ProductoPeticion datos) {
		return servicio.actualizar(id, datos);
	}

	/**
	 * Borrado lógico: deja el producto INACTIVO.
	 *
	 * @param id id del producto
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('CATALOGO_EDITAR')")
	@Operation(operationId = "inactivarProducto", summary = "Inactivar producto (borrado lógico)",
			description = "No elimina el registro: lo deja en estado INACTIVO, porque un elemento de catálogo en uso "
					+ "no se elimina, solo se desactiva (RF-CFG-001). El producto sigue disponible en GET y puede "
					+ "reactivarse con PUT y `estado: ACTIVO`. Es idempotente: repetirlo también responde 204.")
	@ApiResponse(responseCode = "204", description = "Producto inactivado")
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	public void inactivar(@Parameter(description = "Id del producto.", example = "1") @PathVariable long id) {
		servicio.inactivar(id);
	}

}
