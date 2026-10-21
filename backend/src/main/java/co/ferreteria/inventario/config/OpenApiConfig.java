/**
 * Nombre: OpenApiConfig.java
 * Descripción: Metadatos de la especificación OpenAPI publicada por springdoc (RNF-COM-001), con el esquema
 *              Problem Details y las respuestas de error reutilizables.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.ManejadorGlobalErrores;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configura título, descripción, servidor, esquema de seguridad y respuestas de error de la
 * documentación OpenAPI.
 *
 * <p>Las respuestas 400, 401, 403, 404, 409 y 422 se declaran una sola vez en
 * {@code components.responses} con el esquema {@code Problema} (RFC 9457 más {@code codigo} y
 * {@code errores}) y un ejemplo; los controladores las referencian con las constantes {@code REF_*}.
 * Los textos de los ejemplos salen de los archivos de mensajes en español, los mismos que usa
 * {@link ManejadorGlobalErrores}. El servidor es la ruta relativa {@code /}, para que el contrato
 * exportado no dependa del host ni del puerto.</p>
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	/** Nombre del esquema de seguridad con token JWT (se usa desde RF-SEG-001). */
	public static final String ESQUEMA_JWT = "bearerAuth";

	/** Nombre del esquema de error Problem Details. */
	public static final String ESQUEMA_PROBLEMA = "Problema";

	/** Referencia a la respuesta 400 (validación). */
	public static final String REF_400 = "#/components/responses/Error400";
	/** Referencia a la respuesta 401 (sin token válido). */
	public static final String REF_401 = "#/components/responses/Error401";
	/** Referencia a la respuesta 403 (sin permiso). */
	public static final String REF_403 = "#/components/responses/Error403";
	/** Referencia a la respuesta 404 (no existe). */
	public static final String REF_404 = "#/components/responses/Error404";
	/** Referencia a la respuesta 409 (duplicado u operación no permitida). */
	public static final String REF_409 = "#/components/responses/Error409";
	/** Referencia a la respuesta 422 (regla de integridad o de negocio de la base). */
	public static final String REF_422 = "#/components/responses/Error422";

	/** Ruta usada en los ejemplos de error. */
	private static final String RUTA_EJEMPLO = "/api/v1/productos";

	/**
	 * Constructor usado por Spring.
	 */
	public OpenApiConfig() {
		// Sin estado.
	}

	/**
	 * Documento OpenAPI base, con textos tomados de los archivos de mensajes en español.
	 *
	 * @param messageSource fuente de mensajes
	 * @return definición OpenAPI con información general, servidor, esquema Bearer JWT y errores comunes
	 */
	@Bean
	public OpenAPI openApi(MessageSource messageSource) {
		Textos t = new Textos(messageSource);
		Components componentes = new Components()
				.addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT"))
				.addSchemas(ESQUEMA_PROBLEMA, esquemaProblema(t));

		componentes.addResponses("Error400", respuesta(t, "400", ejemplo(t, CodigoError.VALIDACION, null,
				List.of(Map.of("campo", "plazoGarantiaMeses", "mensaje", t.get("producto.garantia.plazo_obligatorio"))),
				null)));
		componentes.addResponses("Error401", respuesta(t, "401", ejemplo(t, CodigoError.NO_AUTENTICADO, null, List.of(), null)));
		componentes.addResponses("Error403", respuesta(t, "403", ejemplo(t, CodigoError.ACCESO_DENEGADO, null, List.of(), null)));
		componentes.addResponses("Error404", respuesta(t, "404", ejemplo(t, CodigoError.NO_ENCONTRADO,
				t.get("error.no_encontrado.detalle_recurso", t.get("entidad.producto"), "999"), List.of(), null)));
		componentes.addResponses("Error409", respuesta(t, "409", ejemplo(t, CodigoError.CONFLICTO_UNICIDAD,
				t.get("producto.duplicado", "Martillo de uña 16 oz", "Stanley"), List.of(), null)));
		componentes.addResponses("Error422", respuesta(t, "422", ejemplo(t, CodigoError.REGLA_INTEGRIDAD,
				t.get(CodigoError.REGLA_INTEGRIDAD.claveDetalle(), "ck_producto_garantia"), List.of(),
				"ck_producto_garantia")));

		return new OpenAPI()
				.info(new Info()
						.title(t.get("openapi.titulo"))
						.description(t.get("openapi.descripcion"))
						.version("v1"))
				.servers(List.of(new Server().url("/").description(t.get("openapi.servidor"))))
				.components(componentes);
	}

	/**
	 * Esquema de las respuestas de error.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Schema<?> esquemaProblema(Textos t) {
		Schema errorCampo = new ObjectSchema()
				.addProperty("campo", new StringSchema().description(t.get("openapi.problema.campo")).example("nombre"))
				.addProperty("mensaje", new StringSchema().description(t.get("openapi.problema.mensaje")));
		StringSchema codigo = new StringSchema();
		Arrays.stream(CodigoError.values()).map(Enum::name).forEach(codigo::addEnumItem);
		codigo.description(t.get("openapi.problema.codigo"));
		return new ObjectSchema()
				.description(t.get("openapi.problema.descripcion"))
				.addProperty("type", new StringSchema().format("uri").example("about:blank"))
				.addProperty("title", new StringSchema().description(t.get("openapi.problema.title")))
				.addProperty("status", new IntegerSchema().description(t.get("openapi.problema.status")))
				.addProperty("detail", new StringSchema().description(t.get("openapi.problema.detail")))
				.addProperty("instance", new StringSchema().format("uri").description(t.get("openapi.problema.instance")))
				.addProperty(ManejadorGlobalErrores.PROPIEDAD_CODIGO, codigo)
				.addProperty(ManejadorGlobalErrores.PROPIEDAD_ERRORES, new ArraySchema().items(errorCampo)
						.description(t.get("openapi.problema.errores")))
				.addProperty(ManejadorGlobalErrores.PROPIEDAD_RESTRICCION, new StringSchema()
						.description(t.get("openapi.problema.restriccion")));
	}

	private static ApiResponse respuesta(Textos t, String estado, Map<String, Object> ejemplo) {
		return new ApiResponse()
				.description(t.get("openapi.respuesta." + estado))
				.content(new Content().addMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						new io.swagger.v3.oas.models.media.MediaType()
								.schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_PROBLEMA))
								.addExamples(((String) ejemplo.get("codigo")).toLowerCase(),
										new Example().value(ejemplo))));
	}

	private static Map<String, Object> ejemplo(Textos t, CodigoError codigo, String detalle,
			List<Map<String, String>> errores, String restriccion) {
		Map<String, Object> ejemplo = new LinkedHashMap<>();
		ejemplo.put("title", t.get(codigo.claveTitulo()));
		ejemplo.put("status", codigo.getEstado().value());
		ejemplo.put("detail", detalle != null ? detalle : t.get(codigo.claveDetalle()));
		ejemplo.put("instance", RUTA_EJEMPLO);
		ejemplo.put(ManejadorGlobalErrores.PROPIEDAD_CODIGO, codigo.name());
		ejemplo.put(ManejadorGlobalErrores.PROPIEDAD_ERRORES, errores);
		if (restriccion != null) {
			ejemplo.put(ManejadorGlobalErrores.PROPIEDAD_RESTRICCION, restriccion);
		}
		return ejemplo;
	}

	/**
	 * Acceso abreviado a los mensajes en español.
	 */
	private record Textos(MessageSource fuente) {

		String get(String clave, Object... argumentos) {
			return fuente.getMessage(clave, argumentos, LocaleConfig.ESPANOL);
		}

	}

}
