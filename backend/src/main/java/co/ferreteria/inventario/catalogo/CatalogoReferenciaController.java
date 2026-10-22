/**
 * Nombre: CatalogoReferenciaController.java
 * Descripción: Listas de unidades de medida y tarifas de IVA para los selectores del frontend.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.ferreteria.inventario.catalogo.dto.TarifaIvaRespuesta;
import co.ferreteria.inventario.catalogo.dto.UnidadMedidaRespuesta;
import co.ferreteria.inventario.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Catálogos de solo lectura que alimentan el formulario de producto (RF-PRO-001): unidades de medida y
 * tarifas de IVA. Exigen {@code CATALOGO_CONSULTAR}. Las marcas y categorías activas las sirven
 * {@link MarcaController} y {@link CategoriaController} en {@code /activas}.
 */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('CATALOGO_CONSULTAR')")
@Tag(name = "Catálogos de referencia", description = "Unidades de medida y tarifas de IVA para selectores")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@ApiResponse(responseCode = "200", description = "Lista")
@ApiResponse(responseCode = "401", ref = OpenApiConfig.REF_401)
@ApiResponse(responseCode = "403", ref = OpenApiConfig.REF_403)
public class CatalogoReferenciaController {

	private final UnidadMedidaRepository unidades;
	private final TarifaIvaRepository tarifas;

	/**
	 * Crea el controlador.
	 *
	 * @param unidades repositorio de unidades de medida
	 * @param tarifas repositorio de tarifas de IVA
	 */
	public CatalogoReferenciaController(UnidadMedidaRepository unidades, TarifaIvaRepository tarifas) {
		this.unidades = unidades;
		this.tarifas = tarifas;
	}

	/**
	 * Unidades base de RN-05.
	 *
	 * @return las 13 unidades ordenadas por código
	 */
	@GetMapping("/unidades-medida")
	@Transactional(readOnly = true)
	@Operation(operationId = "listarUnidadesMedida", summary = "Listar unidades de medida",
			description = "Las 13 unidades de RN-05, ordenadas por código; `esDiscreta` indica si solo admite enteros.")
	public List<UnidadMedidaRespuesta> unidadesMedida() {
		return unidades.findAll(Sort.by("codigo")).stream()
				.map(UnidadMedidaRespuesta::desde).toList();
	}

	/**
	 * Tarifas de IVA activas.
	 *
	 * @return tarifas ordenadas por porcentaje
	 */
	@GetMapping("/tarifas-iva")
	@Transactional(readOnly = true)
	@Operation(operationId = "listarTarifasIva", summary = "Listar tarifas de IVA activas", description = "Ordenadas por porcentaje (RN-08).")
	public List<TarifaIvaRespuesta> tarifasIva() {
		return tarifas.findByActivoTrueOrderByPorcentajeAsc().stream().map(TarifaIvaRespuesta::desde).toList();
	}

}
