/**
 * Nombre: CatalogoReferenciaIntegracionTests.java
 * Descripción: Pruebas de las listas de marcas, categorías, unidades de medida y tarifas de IVA para selectores.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;

/**
 * Catálogos de solo lectura que llenan el formulario de producto (RF-PRO-001, RN-05, RN-08).
 */
class CatalogoReferenciaIntegracionTests extends PruebaIntegracionApi {

	@Test
	@DisplayName("[RN-05] Las 13 unidades base se listan ordenadas por código, con su marca de discreta")
	void unidadesMedida() throws Exception {
		mvc.perform(get("/api/v1/unidades-medida").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("vendedor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(13)))
				.andExpect(jsonPath("$[0].codigo").value("CM"))
				.andExpect(jsonPath("$[?(@.esDiscreta == true)].codigo", contains("JGO", "PAR", "UND")));
	}

	@Test
	@DisplayName("[RN-08] Las tarifas de IVA 0, 5 y 19 % se listan ordenadas por porcentaje")
	void tarifasIva() throws Exception {
		mvc.perform(get("/api/v1/tarifas-iva").header(HttpHeaders.AUTHORIZATION, usuarios.bearer("auditor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].porcentaje", contains(0.0, 5.0, 19.0)));
	}

	@Test
	@DisplayName("[CA-RF-CFG-001-D1] Marcas y categorías activas (/activas) se listan ordenadas por nombre")
	void marcasYCategorias() throws Exception {
		String token = usuarios.bearer("comprador");
		mvc.perform(get("/api/v1/marcas/activas").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombre").value("Argos"))
				.andExpect(jsonPath("$[*].nombre", hasItems("Stanley", "Pavco", "Sika")));
		mvc.perform(get("/api/v1/categorias/activas").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombre").value("Adhesivos y selladores"))
				.andExpect(jsonPath("$[*].nombre", hasItems("Herramientas", "Plomería")));
	}

	@Test
	@DisplayName("[CA-RNF-SEG-001-1] Sin token, las listas responden 401")
	void sinToken() throws Exception {
		mvc.perform(get("/api/v1/marcas/activas")).andExpect(status().isUnauthorized());
	}

}
