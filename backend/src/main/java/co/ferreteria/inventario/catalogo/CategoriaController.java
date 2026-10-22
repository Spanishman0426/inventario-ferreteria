/**
 * Nombre: CategoriaController.java
 * Descripción: API REST de administración de categorías (/api/v1/categorias): búsqueda, activas, alta, edición y estado.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.ferreteria.inventario.catalogo.dto.CambioEstadoCatalogo;
import co.ferreteria.inventario.catalogo.dto.ElementoCatalogoPeticion;
import co.ferreteria.inventario.catalogo.dto.ElementoCatalogoRespuesta;
import co.ferreteria.inventario.catalogo.dto.ReferenciaRespuesta;
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
 * Catálogo de categorías (RF-CFG-001).
 *
 * <p>Lectura con {@code CATALOGO_CONSULTAR} (todos los roles); escritura con {@code CONFIG_GESTIONAR}
 * (matriz 2.2: solo el administrador). No hay DELETE: una categoría en uso no se elimina, solo se
 * desactiva, y los productos que ya la tienen la conservan.</p>
 */
@RestController
@RequestMapping(path = "/api/v1/categorias", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Categorías", description = "Administración del catálogo de categorías (RF-CFG-001)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@ApiResponse(responseCode = "401", ref = OpenApiConfig.REF_401)
@ApiResponse(responseCode = "403", ref = OpenApiConfig.REF_403)
public class CategoriaController {

	private final CategoriaService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de categorías
	 */
	public CategoriaController(CategoriaService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Busca categorías paginadas con filtros.
	 *
	 * @param q texto contenido en el nombre
	 * @param estado estado de la categoría
	 * @param pageable página ({@code page}), tamaño ({@code size}) y orden ({@code sort})
	 * @return página de categorías
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('CATALOGO_CONSULTAR')")
	@Operation(operationId = "listarCategorias", summary = "Buscar categorías",
			description = "Lista paginada con categorías activas e inactivas. `page` empieza en 0; `size` va de 1 a 100 "
					+ "(20 por defecto). `sort` admite `id` y `nombre`, con `,asc` o `,desc`; por defecto "
					+ "`nombre,asc`. Para los selectores use GET /api/v1/categorias/activas.")
	@ApiResponse(responseCode = "200", description = "Página de categorías")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	public Pagina<ElementoCatalogoRespuesta> listar(
			@Parameter(description = "Texto contenido en el nombre, sin distinguir mayúsculas.", example = "herra")
			@RequestParam(required = false) @Size(max = 100) String q,
			@Parameter(description = "Estado de la categoría; sin valor devuelve todas.")
			@RequestParam(required = false) EstadoCatalogo estado,
			@ParameterObject @PageableDefault(size = Pagina.TAMANO_POR_DEFECTO, sort = "nombre",
					direction = Sort.Direction.ASC) Pageable pageable) {
		return servicio.listar(q, estado, pageable);
	}

	/**
	 * Categorías activas para los selectores del formulario de producto.
	 *
	 * @return categorías activas ordenadas por nombre
	 */
	@GetMapping("/activas")
	@PreAuthorize("hasAuthority('CATALOGO_CONSULTAR')")
	@Operation(operationId = "listarCategoriasActivas", summary = "Listar categorías activas",
			description = "Lista completa, sin paginar, ordenada por nombre. Alimenta el selector de categoría de los "
					+ "productos nuevos; al editar, el producto conserva su categoría aunque esté inactiva.")
	@ApiResponse(responseCode = "200", description = "Categorías activas")
	public List<ReferenciaRespuesta> listarActivas() {
		return servicio.listarActivos();
	}

	/**
	 * Registra una categoría activa.
	 *
	 * @param datos nombre de la categoría
	 * @return 201 con la categoría y la cabecera {@code Location}
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('CONFIG_GESTIONAR')")
	@Operation(operationId = "crearCategoria", summary = "Registrar categoría",
			description = "El nombre se recorta y no se repite sin distinguir mayúsculas. La categoría queda ACTIVA.")
	@ApiResponse(responseCode = "201", description = "Creada",
			headers = @Header(name = "Location", description = "URL de la categoría creada",
					schema = @Schema(type = "string", format = "uri", example = "/api/v1/categorias/10")))
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "409", ref = OpenApiConfig.REF_409)
	public ResponseEntity<ElementoCatalogoRespuesta> crear(@Valid @RequestBody ElementoCatalogoPeticion datos) {
		ElementoCatalogoRespuesta creada = servicio.crear(datos);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}")
				.buildAndExpand(creada.id()).toUri();
		return ResponseEntity.created(ubicacion).body(creada);
	}

	/**
	 * Cambia el nombre de una categoría.
	 *
	 * @param id id de la categoría
	 * @param datos nombre nuevo
	 * @return categoría actualizada
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('CONFIG_GESTIONAR')")
	@Operation(operationId = "actualizarCategoria", summary = "Editar categoría",
			description = "Cambia el nombre; el estado se cambia con PATCH /api/v1/categorias/{id}/estado.")
	@ApiResponse(responseCode = "200", description = "Actualizada")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	@ApiResponse(responseCode = "409", ref = OpenApiConfig.REF_409)
	public ElementoCatalogoRespuesta actualizar(
			@Parameter(description = "Id de la categoría.", example = "1") @PathVariable long id,
			@Valid @RequestBody ElementoCatalogoPeticion datos) {
		return servicio.actualizar(id, datos);
	}

	/**
	 * Activa o desactiva una categoría.
	 *
	 * @param id id de la categoría
	 * @param datos estado nuevo
	 * @return categoría con su estado
	 */
	@PatchMapping("/{id}/estado")
	@PreAuthorize("hasAuthority('CONFIG_GESTIONAR')")
	@Operation(operationId = "cambiarEstadoCategoria", summary = "Activar o desactivar categoría",
			description = "Una categoría en uso no se elimina, solo se desactiva (RF-CFG-001): deja de ofrecerse a "
					+ "productos nuevos y los existentes la conservan. Es idempotente.")
	@ApiResponse(responseCode = "200", description = "Estado aplicado")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	public ElementoCatalogoRespuesta cambiarEstado(
			@Parameter(description = "Id de la categoría.", example = "1") @PathVariable long id,
			@Valid @RequestBody CambioEstadoCatalogo datos) {
		return servicio.cambiarEstado(id, datos.estado());
	}

}
