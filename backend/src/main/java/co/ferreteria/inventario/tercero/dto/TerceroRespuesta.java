/**
 * Nombre: TerceroRespuesta.java
 * Descripción: Vista de un tercero que devuelve la API (/api/v1/terceros).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero.dto;

import java.time.Instant;

import co.ferreteria.inventario.tercero.EstadoTercero;
import co.ferreteria.inventario.tercero.Tercero;
import co.ferreteria.inventario.tercero.TipoIdentificacion;
import co.ferreteria.inventario.tercero.TipoPersona;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tercero con todos sus datos (RF-TER-001).
 *
 * @param id identificador
 * @param tipoPersona NATURAL o JURIDICA
 * @param tipoIdentificacion CC o NIT
 * @param numeroIdentificacion número sin dígito de verificación
 * @param digitoVerificacion dígito del NIT; nulo para cédula
 * @param nombre nombre de la persona natural
 * @param apellido apellido de la persona natural
 * @param razonSocial razón social de la persona jurídica
 * @param nombreMostrado razón social o nombre y apellido
 * @param direccion dirección de contacto
 * @param telefono teléfono
 * @param correo correo
 * @param esCliente tiene rol de cliente
 * @param esProveedor tiene rol de proveedor
 * @param esConsumidorFinal es el Consumidor final predefinido (no se edita)
 * @param estado ACTIVO o INACTIVO
 * @param creadoEn fecha de creación
 */
@Schema(name = "Tercero", description = "Cliente o proveedor (RF-TER-001).")
public record TerceroRespuesta(
		@Schema(example = "4") Long id,
		@Schema(example = "JURIDICA") TipoPersona tipoPersona,
		@Schema(example = "NIT") TipoIdentificacion tipoIdentificacion,
		@Schema(example = "900987654") String numeroIdentificacion,
		@Schema(example = "7") Short digitoVerificacion,
		@Schema(example = "null") String nombre,
		@Schema(example = "null") String apellido,
		@Schema(example = "Constructora Andina S.A.S.") String razonSocial,
		@Schema(example = "Constructora Andina S.A.S.") String nombreMostrado,
		@Schema(example = "Carrera 5 # 10-30, Neiva") String direccion,
		@Schema(example = "3001234567") String telefono,
		@Schema(example = "compras@andina.test") String correo,
		@Schema(example = "true") boolean esCliente,
		@Schema(example = "false") boolean esProveedor,
		@Schema(example = "false") boolean esConsumidorFinal,
		@Schema(example = "ACTIVO") EstadoTercero estado,
		@Schema(example = "2026-10-22T15:00:00Z") Instant creadoEn) {

	/**
	 * Vista de una entidad.
	 *
	 * @param t tercero
	 * @return respuesta
	 */
	public static TerceroRespuesta desde(Tercero t) {
		return new TerceroRespuesta(t.getId(), t.getTipoPersona(), t.getTipoIdentificacion(),
				t.getNumeroIdentificacion(), t.getDigitoVerificacion(), t.getNombre(), t.getApellido(),
				t.getRazonSocial(), t.nombreMostrado(), t.getDireccion(), t.getTelefono(), t.getCorreo(),
				t.isEsCliente(), t.isEsProveedor(), t.isEsConsumidorFinal(), EstadoTercero.desdeActivo(t.isActivo()),
				t.getCreadoEn());
	}

}
