/**
 * Nombre: TerceroController.java
 * Descripción: API REST de clientes y proveedores (/api/v1/terceros).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import java.net.URI;

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

import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.config.OpenApiConfig;
import co.ferreteria.inventario.tercero.dto.CambioEstadoTercero;
import co.ferreteria.inventario.tercero.dto.TerceroPeticion;
import co.ferreteria.inventario.tercero.dto.TerceroRespuesta;
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
 * Clientes y proveedores (RF-TER-001).
 *
 * <p>Todas las operaciones exigen {@code TERCERO_CLIENTE_EDITAR} o {@code TERCERO_PROVEEDOR_EDITAR}
 * (administrador, gerente, vendedor y comprador). Además, el servicio exige el permiso de cada rol del
 * tercero: un vendedor solo registra clientes y un comprador solo proveedores (matriz 2.2). No hay
 * borrado: un tercero se desactiva.</p>
 */
@RestController
@RequestMapping(path = "/api/v1/terceros", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Terceros", description = "Clientes y proveedores (RF-TER-001)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@ApiResponse(responseCode = "401", ref = OpenApiConfig.REF_401)
@ApiResponse(responseCode = "403", ref = OpenApiConfig.REF_403)
public class TerceroController {

	/** Expresión de autorización: cualquiera de los dos permisos de terceros. */
	private static final String PERMISO_TERCEROS =
			"hasAnyAuthority('TERCERO_CLIENTE_EDITAR', 'TERCERO_PROVEEDOR_EDITAR')";

	private final TerceroService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de terceros
	 */
	public TerceroController(TerceroService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Busca terceros paginados con filtros.
	 *
	 * @param q texto en el nombre o razón social, o inicio del número de identificación
	 * @param rol cliente o proveedor
	 * @param tipoPersona natural o jurídica
	 * @param estado activo o inactivo
	 * @param pageable página ({@code page}), tamaño ({@code size}) y orden ({@code sort})
	 * @return página de terceros
	 */
	@GetMapping
	@PreAuthorize(PERMISO_TERCEROS)
	@Operation(operationId = "listarTerceros", summary = "Buscar terceros",
			description = "Lista paginada que incluye al Consumidor final. `q` busca en el nombre o la razón social "
					+ "(sin distinguir mayúsculas) y al inicio del número de identificación. `page` empieza en 0; "
					+ "`size` va de 1 a 100 (20 por defecto). `sort` admite `nombre`, `numeroIdentificacion`, "
					+ "`creadoEn` e `id`, con `,asc` o `,desc`; por defecto `nombre,asc`.")
	@ApiResponse(responseCode = "200", description = "Página de terceros")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	public Pagina<TerceroRespuesta> listar(
			@Parameter(description = "Nombre, razón social o inicio de la identificación.", example = "andina")
			@RequestParam(required = false) @Size(max = 150) String q,
			@Parameter(description = "Rol del tercero.")
			@RequestParam(required = false) RolTercero rol,
			@Parameter(description = "Tipo de persona.")
			@RequestParam(required = false) TipoPersona tipoPersona,
			@Parameter(description = "Estado del tercero; sin valor devuelve todos.")
			@RequestParam(required = false) EstadoTercero estado,
			@ParameterObject @PageableDefault(size = Pagina.TAMANO_POR_DEFECTO, sort = "nombre",
					direction = Sort.Direction.ASC) Pageable pageable) {
		return servicio.listar(q, rol, tipoPersona, estado, pageable);
	}

	/**
	 * Consulta un tercero.
	 *
	 * @param id id del tercero
	 * @return tercero
	 */
	@GetMapping("/{id}")
	@PreAuthorize(PERMISO_TERCEROS)
	@Operation(operationId = "obtenerTercero", summary = "Consultar un tercero",
			description = "Devuelve también los terceros INACTIVOS y el Consumidor final.")
	@ApiResponse(responseCode = "200", description = "Tercero")
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	public TerceroRespuesta obtener(@Parameter(description = "Id del tercero.", example = "4") @PathVariable long id) {
		return servicio.obtener(id);
	}

	/**
	 * Registra un tercero.
	 *
	 * @param datos datos del tercero
	 * @return 201 con el tercero y la cabecera {@code Location}
	 */
	@PostMapping
	@PreAuthorize(PERMISO_TERCEROS)
	@Operation(operationId = "crearTercero", summary = "Registrar tercero",
			description = "Persona natural: CC o NIT, nombre y apellido. Persona jurídica: solo NIT y razón social. "
					+ "El NIT lleva dígito de verificación correcto. Dirección y teléfono (7 a 10 dígitos) son "
					+ "obligatorios; el correo es opcional. Al menos un rol: `esCliente` exige "
					+ "TERCERO_CLIENTE_EDITAR y `esProveedor` TERCERO_PROVEEDOR_EDITAR (403 si falta). La "
					+ "identificación es única por tipo (409). El tercero queda ACTIVO.")
	@ApiResponse(responseCode = "201", description = "Creado",
			headers = @Header(name = "Location", description = "URL del tercero creado",
					schema = @Schema(type = "string", format = "uri", example = "/api/v1/terceros/10")))
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "409", ref = OpenApiConfig.REF_409)
	public ResponseEntity<TerceroRespuesta> crear(@Valid @RequestBody TerceroPeticion datos) {
		TerceroRespuesta creado = servicio.crear(datos);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}")
				.buildAndExpand(creado.id()).toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	/**
	 * Reemplaza los datos de un tercero.
	 *
	 * @param id id del tercero
	 * @param datos datos completos del tercero
	 * @return tercero actualizado
	 */
	@PutMapping("/{id}")
	@PreAuthorize(PERMISO_TERCEROS)
	@Operation(operationId = "actualizarTercero", summary = "Editar tercero",
			description = "Reemplaza todos los campos con las mismas reglas del alta. Exige el permiso de cada rol "
					+ "que el tercero tiene y de cada rol que tendrá. El Consumidor final no se edita (422 "
					+ "REGLA_NEGOCIO). El estado se cambia con PATCH /api/v1/terceros/{id}/estado.")
	@ApiResponse(responseCode = "200", description = "Actualizado")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	@ApiResponse(responseCode = "409", ref = OpenApiConfig.REF_409)
	@ApiResponse(responseCode = "422", ref = OpenApiConfig.REF_422)
	public TerceroRespuesta actualizar(@Parameter(description = "Id del tercero.", example = "4") @PathVariable long id,
			@Valid @RequestBody TerceroPeticion datos) {
		return servicio.actualizar(id, datos);
	}

	/**
	 * Activa o desactiva un tercero.
	 *
	 * @param id id del tercero
	 * @param datos estado nuevo
	 * @return tercero con su estado
	 */
	@PatchMapping("/{id}/estado")
	@PreAuthorize(PERMISO_TERCEROS)
	@Operation(operationId = "cambiarEstadoTercero", summary = "Activar o desactivar tercero",
			description = "Los terceros no se eliminan, solo se desactivan. Exige el permiso de cada rol que el "
					+ "tercero tiene. El Consumidor final no se desactiva (422 REGLA_NEGOCIO). Es idempotente.")
	@ApiResponse(responseCode = "200", description = "Estado aplicado")
	@ApiResponse(responseCode = "400", ref = OpenApiConfig.REF_400)
	@ApiResponse(responseCode = "404", ref = OpenApiConfig.REF_404)
	@ApiResponse(responseCode = "422", ref = OpenApiConfig.REF_422)
	public TerceroRespuesta cambiarEstado(@Parameter(description = "Id del tercero.", example = "4") @PathVariable long id,
			@Valid @RequestBody CambioEstadoTercero datos) {
		return servicio.cambiarEstado(id, datos.estado());
	}

}
