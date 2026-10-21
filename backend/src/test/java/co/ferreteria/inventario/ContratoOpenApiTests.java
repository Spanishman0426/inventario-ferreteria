/**
 * Nombre: ContratoOpenApiTests.java
 * Descripción: Verifica que el contrato OpenAPI generado por el código coincida con docs/api/openapi.yaml;
 *              con -Dopenapi.actualizar=true reescribe el archivo.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.ferreteria.inventario.soporte.PruebaIntegracionApi;

/**
 * Prueba de contrato (RNF-COM-001): si un cambio en controladores, DTO o anotaciones altera la API,
 * esta prueba falla hasta que se regenere y revise {@code docs/api/openapi.yaml}.
 *
 * <p>Para regenerarlo: {@code mvnw test -Dtest=ContratoOpenApiTests -Dopenapi.actualizar=true}.
 * El archivo empieza con el encabezado obligatorio del proyecto en comentarios {@code #}; la
 * comparación lo ignora, igual que las diferencias de fin de línea (Windows y Linux).</p>
 */
class ContratoOpenApiTests extends PruebaIntegracionApi {

	/** Ubicación del contrato, relativa al módulo backend (directorio de trabajo de Maven). */
	static final Path CONTRATO = Path.of("..", "docs", "api", "openapi.yaml");

	/** Propiedad del sistema que activa la regeneración del archivo. */
	static final String PROPIEDAD_ACTUALIZAR = "openapi.actualizar";

	private static final String ENCABEZADO = """
			# Nombre: openapi.yaml
			# Descripción: Contrato OpenAPI 3 de la API REST, generado por springdoc desde el código.
			#              No editar a mano: regenerar con
			#              cd backend && mvnw test -Dtest=ContratoOpenApiTests -Dopenapi.actualizar=true
			#              La prueba ContratoOpenApiTests falla si el código y este archivo dejan de coincidir.
			# Fecha de creación: 2026-10-20
			# Última actualización: 2026-10-20
			# Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
			""";

	@Test
	@DisplayName("[RNF-COM-001] El contrato OpenAPI generado coincide con docs/api/openapi.yaml")
	void contratoCoincide() throws Exception {
		String generado = normalizar(mvc.perform(get("/v3/api-docs.yaml")).andReturn()
				.getResponse().getContentAsString(StandardCharsets.UTF_8));
		assertThat(generado).as("springdoc no devolvió el contrato").contains("openapi:");

		if (Boolean.getBoolean(PROPIEDAD_ACTUALIZAR)) {
			Files.createDirectories(CONTRATO.getParent());
			Files.writeString(CONTRATO, ENCABEZADO + generado, StandardCharsets.UTF_8);
			return;
		}
		if (!Files.exists(CONTRATO)) {
			fail("No existe " + CONTRATO.toAbsolutePath().normalize() + "; genérelo con -D" + PROPIEDAD_ACTUALIZAR + "=true");
		}
		String guardado = sinEncabezado(normalizar(Files.readString(CONTRATO, StandardCharsets.UTF_8)));
		if (!guardado.equals(generado)) {
			fail(primeraDiferencia(guardado, generado));
		}
	}

	private static String normalizar(String texto) {
		return texto.replace("\r\n", "\n").stripTrailing() + "\n";
	}

	private static String sinEncabezado(String texto) {
		List<String> lineas = texto.lines().toList();
		int inicio = 0;
		while (inicio < lineas.size() && lineas.get(inicio).startsWith("#")) {
			inicio++;
		}
		return String.join("\n", lineas.subList(inicio, lineas.size())) + "\n";
	}

	private static String primeraDiferencia(String guardado, String generado) throws IOException {
		List<String> a = guardado.lines().toList();
		List<String> b = generado.lines().toList();
		int i = 0;
		while (i < a.size() && i < b.size() && a.get(i).equals(b.get(i))) {
			i++;
		}
		return "El contrato generado no coincide con " + CONTRATO + " (línea " + (i + 1) + " sin encabezado).\n"
				+ "  archivo : " + (i < a.size() ? a.get(i) : "<fin>") + "\n"
				+ "  código  : " + (i < b.size() ? b.get(i) : "<fin>") + "\n"
				+ "Si el cambio es intencional, regenere con -D" + PROPIEDAD_ACTUALIZAR + "=true y revise el diff.";
	}

}
