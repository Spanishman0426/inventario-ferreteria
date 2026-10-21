/**
 * Nombre: CatalogoReferenciaController.java
 * Descripción: Listas de marcas, categorías, unidades de medida y tarifas de IVA para los selectores del frontend.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
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

import co.ferreteria.inventario.catalogo.dto.ReferenciaRespuesta;
import co.ferreteria.inventario.catalogo.dto.TarifaIvaRespuesta;
import co.ferreteria.inventario.catalogo.dto.UnidadMedidaRespuesta;
import co.ferreteria.inventario.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Catálogos de solo lectura que alimentan el formulario de producto (RF-PRO-001). Solo devuelven
 * elementos activos; su administración llega con RF-CFG-001. Exigen {@code CATALOGO_CONSULTAR}.
 */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('CATALOGO_CONSULTAR')")
@Tag(name = "Catálogos de referencia", description = "Marcas, categorías, unidades y tarifas de IVA para selectores")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@ApiResponse(responseCode = "200", description = "Lista")
@ApiResponse(responseCode = "401", ref = OpenApiConfig.REF_401)
@ApiResponse(responseCode = "403", ref = OpenApiConfig.REF_403)
public class CatalogoReferenciaController {

	private final MarcaRepository marcas;
	private final CategoriaRepository categorias;
	private final UnidadMedidaRepository unidades;
	private final TarifaIvaRepository tarifas;

	/**
	 * Crea el controlador.
	 *
	 * @param marcas repositorio de marcas
	 * @param categorias repositorio de categorías
	 * @param unidades repositorio de unidades de medida
	 * @param tarifas repositorio de tarifas de IVA
	 */
	public CatalogoReferenciaController(MarcaRepository marcas, CategoriaRepository categorias,
			UnidadMedidaRepository unidades, TarifaIvaRepository tarifas) {
		this.marcas = marcas;
		this.categorias = categorias;
		this.unidades = unidades;
		this.tarifas = tarifas;
	}

	/**
	 * Marcas activas.
	 *
	 * @return marcas ordenadas por nombre
	 */
	@GetMapping("/marcas")
	@Transactional(readOnly = true)
	@Operation(operationId = "listarMarcas", summary = "Listar marcas activas", description = "Ordenadas por nombre.")
	public List<ReferenciaRespuesta> marcas() {
		return marcas.findByActivoTrueOrderByNombreAsc().stream().map(ReferenciaRespuesta::desde).toList();
	}

	/**
	 * Categorías activas.
	 *
	 * @return categorías ordenadas por nombre
	 */
	@GetMapping("/categorias")
	@Transactional(readOnly = true)
	@Operation(operationId = "listarCategorias", summary = "Listar categorías activas", description = "Ordenadas por nombre.")
	public List<ReferenciaRespuesta> categorias() {
		return categorias.findByActivoTrueOrderByNombreAsc().stream().map(ReferenciaRespuesta::desde).toList();
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
