/**
 * Nombre: TerceroPeticion.java
 * Descripción: Datos para crear o editar un tercero (POST y PUT /api/v1/terceros).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero.dto;

import co.ferreteria.inventario.tercero.TipoIdentificacion;
import co.ferreteria.inventario.tercero.TipoPersona;
import co.ferreteria.inventario.tercero.validacion.TerceroCoherente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Tercero que envía el cliente (RF-TER-001, RN-26). Las reglas de cada campo se validan con Bean
 * Validation; {@link TerceroCoherente} valida las que dependen del tipo de persona y de identificación
 * (nombres o razón social, dígito de verificación del NIT, al menos un rol). La unicidad y los permisos
 * por rol los valida el servicio.
 *
 * @param tipoPersona NATURAL o JURIDICA
 * @param tipoIdentificacion CC o NIT; la persona jurídica solo admite NIT
 * @param numeroIdentificacion de 5 a 15 dígitos, sin dígito de verificación ni separadores
 * @param digitoVerificacion obligatorio y correcto con NIT; no se admite con CC
 * @param nombre obligatorio para persona natural
 * @param apellido obligatorio para persona natural
 * @param razonSocial obligatoria para persona jurídica
 * @param direccion dirección de contacto
 * @param telefono de 7 a 10 dígitos
 * @param correo opcional, con formato válido
 * @param esCliente tiene rol de cliente; nulo equivale a {@code false}
 * @param esProveedor tiene rol de proveedor; nulo equivale a {@code false}
 */
@Builder
@TerceroCoherente
@Schema(name = "TerceroPeticion", description = "Datos para crear o reemplazar un tercero (RF-TER-001).")
public record TerceroPeticion(
		@Schema(description = "Tipo de persona.", example = "JURIDICA", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull
		TipoPersona tipoPersona,

		@Schema(description = "Tipo de identificación; la persona jurídica solo admite NIT.", example = "NIT",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull
		TipoIdentificacion tipoIdentificacion,

		@Schema(description = "Número sin dígito de verificación ni separadores; único por tipo.",
				example = "890903938", pattern = "^[0-9]{5,15}$", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Pattern(regexp = "|[0-9]{5,15}", message = "{tercero.identificacion.formato}")
		String numeroIdentificacion,

		@Schema(description = "Dígito de verificación; obligatorio y correcto con NIT, vacío con CC.", example = "8",
				minimum = "0", maximum = "9")
		@Min(0) @Max(9)
		Integer digitoVerificacion,

		@Schema(description = "Nombre; obligatorio para persona natural.", example = "Juan", maxLength = 100)
		@Size(max = 100)
		String nombre,

		@Schema(description = "Apellido; obligatorio para persona natural.", example = "Pérez Gómez", maxLength = 100)
		@Size(max = 100)
		String apellido,

		@Schema(description = "Razón social; obligatoria para persona jurídica.", example = "Bancolombia S.A.",
				maxLength = 150)
		@Size(max = 150)
		String razonSocial,

		@Schema(description = "Dirección de contacto.", example = "Carrera 5 # 10-30, Neiva", maxLength = 200,
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 200)
		String direccion,

		@Schema(description = "Teléfono de 7 a 10 dígitos.", example = "3001234567", pattern = "^[0-9]{7,10}$",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Pattern(regexp = "|[0-9]{7,10}", message = "{tercero.telefono.formato}")
		String telefono,

		@Schema(description = "Correo; necesario para enviar facturas por correo.", example = "compras@andina.co",
				maxLength = 150)
		@Email @Size(max = 150)
		String correo,

		@Schema(description = "Tiene rol de cliente; exige TERCERO_CLIENTE_EDITAR. Si se omite, false.", example = "true")
		Boolean esCliente,

		@Schema(description = "Tiene rol de proveedor; exige TERCERO_PROVEEDOR_EDITAR. Si se omite, false.",
				example = "false")
		Boolean esProveedor) {

	/**
	 * Un rol omitido equivale a {@code false}, así que los accesores nunca devuelven nulo.
	 */
	public TerceroPeticion {
		esCliente = Boolean.TRUE.equals(esCliente);
		esProveedor = Boolean.TRUE.equals(esProveedor);
	}

}
