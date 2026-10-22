/**
 * Nombre: MarcaController.java
 * Descripción: API REST de administración de marcas (/api/v1/marcas): búsqueda, activas, alta, edición y estado.
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
 * Catálogo de marcas (RF-CFG-001).
 *
 * <p>Lectura con {@code CATALOGO_CONSULTAR} (todos los roles); escritura con {@code CONFIG_GESTIONAR}
 * (matriz 2.2: solo el administrador). No hay DELETE: una marca en uso no se elimina, solo se
 * desactiva, y los productos que ya la tienen la conservan.</p>
 */
@RestController
@RequestMapping(path = "/api/v1/marcas", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Marcas", description = "Administración del catálogo de marcas (RF-CFG-001)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@ApiResponse(responseCode = "401", ref = OpenApiConfig.REF_401)
@ApiResponse(responseCode = "403", ref = OpenApiConfig.REF_403)
public class MarcaController {

	private final MarcaService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de marcas
	 */
	public MarcaController(MarcaService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Busca marcas paginadas con filtros.
	 *
	 * @param q texto contenido en el nombre
	 * @param estado estado de la marca
	 * @param pageable página ({@code page}), tamaño ({@code size}) y orden ({@code sort})
	 * @return página de marcas
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('CATALOGO_CONSULTAR')")
	@Operation(operationId = "listarMarcas", summary = "Buscar marcas",
			description = "Lista paginada con marcas activas e inactivas. `page` empieza en 0; `size` va de 1 a 100 "
					+ "(20 por defecto). `sort` admite `id` y `nombre`, con `,asc` o `,desc`; por defecto "
					+ "`nombre,asc`. Para los selectores use GET /api/v1/marcas/activas.")
	@ApiResponse(responseCode = "200", description = "Página de marcas")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	public Pagina<ElementoCatalogoRespuesta> listar(
			@Parameter(description = "Texto contenido en el nombre, sin distinguir mayúsculas.", example = "stan")
			@RequestParam(required = false) @Size(max = 100) String q,
			@Parameter(description = "Estado de la marca; sin valor devuelve todas.")
			@RequestParam(required = false) EstadoCatalogo estado,
			@ParameterObject @PageableDefault(size = Pagina.TAMANO_POR_DEFECTO, sort = "nombre",
					direction = Sort.Direction.ASC) Pageable pageable) {
		return servicio.listar(q, estado, pageable);
	}

	/**
	 * Marcas activas para los selectores del formulario de producto.
	 *
	 * @return marcas activas ordenadas por nombre
	 */
	@GetMapping("/activas")
	@PreAuthorize("hasAuthority('CATALOGO_CONSULTAR')")
	@Operation(operationId = "listarMarcasActivas", summary = "Listar marcas activas",
			description = "Lista completa, sin paginar, ordenada por nombre. Alimenta el selector de marca de los "
					+ "productos nuevos; al editar, el producto conserva su marca aunque esté inactiva.")
	@ApiResponse(responseCode = "200", description = "Marcas activas")
	public List<ReferenciaRespuesta> listarActivas() {
		return servicio.listarActivos();
	}

	/**
	 * Registra una marca activa.
	 *
	 * @param datos nombre de la marca
	 * @return 201 con la marca y la cabecera {@code Location}
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('CONFIG_GESTIONAR')")
	@Operation(operationId = "crearMarca", summary = "Registrar marca",
			description = "El nombre se recorta y no se repite sin distinguir mayúsculas. La marca queda ACTIVA.")
	@ApiResponse(responseCode = "201", description = "Creada",
			headers = @Header(name = "Location", description = "URL de la marca creada",
					schema = @Schema(type = "string", format = "uri", example = "/api/v1/marcas/10")))
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "409", ref = OpenApiConfig.REF_409)
	public ResponseEntity<ElementoCatalogoRespuesta> crear(@Valid @RequestBody ElementoCatalogoPeticion datos) {
		ElementoCatalogoRespuesta creada = servicio.crear(datos);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}")
				.buildAndExpand(creada.id()).toUri();
		return ResponseEntity.created(ubicacion).body(creada);
	}

	/**
	 * Cambia el nombre de una marca.
	 *
	 * @param id id de la marca
	 * @param datos nombre nuevo
	 * @return marca actualizada
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('CONFIG_GESTIONAR')")
	@Operation(operationId = "actualizarMarca", summary = "Editar marca",
			description = "Cambia el nombre; el estado se cambia con PATCH /api/v1/marcas/{id}/estado.")
	@ApiResponse(responseCode = "200", description = "Actualizada")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	@ApiResponse(responseCode = "409", ref = OpenApiConfig.REF_409)
	public ElementoCatalogoRespuesta actualizar(
			@Parameter(description = "Id de la marca.", example = "1") @PathVariable long id,
			@Valid @RequestBody ElementoCatalogoPeticion datos) {
		return servicio.actualizar(id, datos);
	}

	/**
	 * Activa o desactiva una marca.
	 *
	 * @param id id de la marca
	 * @param datos estado nuevo
	 * @return marca con su estado
	 */
	@PatchMapping("/{id}/estado")
	@PreAuthorize("hasAuthority('CONFIG_GESTIONAR')")
	@Operation(operationId = "cambiarEstadoMarca", summary = "Activar o desactivar marca",
			description = "Una marca en uso no se elimina, solo se desactiva (RF-CFG-001): deja de ofrecerse a "
					+ "productos nuevos y los existentes la conservan. Es idempotente.")
	@ApiResponse(responseCode = "200", description = "Estado aplicado")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	public ElementoCatalogoRespuesta cambiarEstado(
			@Parameter(description = "Id de la marca.", example = "1") @PathVariable long id,
			@Valid @RequestBody CambioEstadoCatalogo datos) {
		return servicio.cambiarEstado(id, datos.estado());
	}

}
