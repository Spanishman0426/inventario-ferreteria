/**
 * Nombre: AuditoriaController.java
 * Descripción: API de consulta de auditoría (solo lectura).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.common.auditoria;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Consulta de auditoría (RF-SEG-002). Exige {@code AUDITORIA_VER} (matriz 2.2: administrador, gerente y
 * auditor). Solo expone GET: nadie modifica ni borra la auditoría (RNF-SEG-004).
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@Validated
@PreAuthorize("hasAuthority('AUDITORIA_VER')")
@Tag(name = "Auditoría", description = "Consulta de la auditoría de operaciones e inicios de sesión (RF-SEG-002)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@ApiResponse(responseCode = "401", description = "Sin token válido")
@ApiResponse(responseCode = "403", description = "Sin el permiso AUDITORIA_VER")
public class AuditoriaController {

	private final AuditoriaService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de consulta
	 */
	public AuditoriaController(AuditoriaService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Lista la auditoría con filtros, del registro más reciente al más antiguo.
	 *
	 * @param usuarioId autor
	 * @param entidad tabla afectada
	 * @param accion acción
	 * @param desde primer día (AAAA-MM-DD)
	 * @param hasta último día, incluido (AAAA-MM-DD)
	 * @param pagina número de página desde 0
	 * @param tamano tamaño de página (máximo 100)
	 * @return página de registros
	 */
	@GetMapping
	@Operation(summary = "Listar auditoría")
	@ApiResponse(responseCode = "200", description = "Página de registros")
	@ApiResponse(responseCode = "400", description = "Filtro no válido")
	public Pagina<RegistroAuditoriaResumen> listar(
			@RequestParam(required = false) @Min(1) Long usuarioId,
			@RequestParam(required = false) @Size(max = 60) @Pattern(regexp = "^[a-z_]+$") String entidad,
			@Parameter(description = "CREAR, MODIFICAR, ELIMINAR, ANULAR, APROBAR, CAMBIAR_ESTADO, INICIAR_SESION, SESION_FALLIDA o DEPURAR")
			@RequestParam(required = false)
			@Pattern(regexp = "^(CREAR|MODIFICAR|ELIMINAR|ANULAR|APROBAR|CAMBIAR_ESTADO|INICIAR_SESION|SESION_FALLIDA|DEPURAR)$")
			String accion,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "" + Pagina.TAMANO_POR_DEFECTO) @Min(1) @Max(Pagina.TAMANO_MAXIMO) int tamano) {
		return servicio.listar(new FiltroAuditoria(usuarioId, entidad, accion, desde, hasta), pagina, tamano);
	}

	/**
	 * Consulta un registro con los valores antes y después, la IP y la fecha.
	 *
	 * @param id id del registro
	 * @return registro completo
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Consultar un registro de auditoría")
	@ApiResponse(responseCode = "200", description = "Registro")
	@ApiResponse(responseCode = "404", description = "No existe")
	public RegistroAuditoriaDetalle obtener(@PathVariable long id) {
		return servicio.obtener(id);
	}

}
