/**
 * Nombre: TerceroEspecificaciones.java
 * Descripción: Filtros de búsqueda de terceros (texto, rol, tipo de persona y estado) como especificaciones JPA.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros combinables de la búsqueda de terceros (RF-TER-001). Un filtro nulo o vacío no restringe.
 */
public final class TerceroEspecificaciones {

	/** Carácter de escape para los comodines de LIKE. */
	private static final char ESCAPE = '\\';

	private TerceroEspecificaciones() {
		// Clase de utilidades.
	}

	/**
	 * Combina los cuatro filtros.
	 *
	 * @param q texto en el nombre o la razón social, o inicio del número de identificación
	 * @param rol cliente o proveedor
	 * @param tipoPersona natural o jurídica
	 * @param estado activo o inactivo
	 * @return especificación con los filtros presentes
	 */
	public static Specification<Tercero> filtrar(String q, RolTercero rol, TipoPersona tipoPersona,
			EstadoTercero estado) {
		return Specification.allOf(texto(q), conRol(rol), deTipo(tipoPersona), conEstado(estado));
	}

	/**
	 * Nombre o razón social que contiene el texto, sin distinguir mayúsculas (usa la expresión del
	 * índice trigram), o número de identificación que empieza por el texto. Los comodines {@code %} y
	 * {@code _} se buscan literalmente.
	 *
	 * @param q texto a buscar
	 * @return especificación, o una que no filtra si {@code q} está vacío
	 */
	public static Specification<Tercero> texto(String q) {
		if (q == null || q.isBlank()) {
			return Specification.unrestricted();
		}
		String limpio = escaparComodines(q.trim().toLowerCase(Locale.ROOT));
		return (raiz, consulta, cb) -> cb.or(
				cb.like(cb.lower(raiz.get("nombreBusqueda")), "%" + limpio + "%", ESCAPE),
				cb.like(raiz.get("numeroIdentificacion"), limpio + "%", ESCAPE));
	}

	/**
	 * Terceros con un rol.
	 *
	 * @param rol rol buscado
	 * @return especificación, o una que no filtra si es nulo
	 */
	public static Specification<Tercero> conRol(RolTercero rol) {
		if (rol == null) {
			return Specification.unrestricted();
		}
		String campo = rol == RolTercero.CLIENTE ? "esCliente" : "esProveedor";
		return (raiz, consulta, cb) -> cb.isTrue(raiz.get(campo));
	}

	/**
	 * Terceros de un tipo de persona.
	 *
	 * @param tipoPersona tipo buscado
	 * @return especificación, o una que no filtra si es nulo
	 */
	public static Specification<Tercero> deTipo(TipoPersona tipoPersona) {
		if (tipoPersona == null) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, cb) -> cb.equal(raiz.get("tipoPersona"), tipoPersona);
	}

	/**
	 * Terceros en un estado.
	 *
	 * @param estado estado buscado
	 * @return especificación, o una que no filtra si es nulo
	 */
	public static Specification<Tercero> conEstado(EstadoTercero estado) {
		if (estado == null) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, cb) -> cb.equal(raiz.get("activo"), estado.comoActivo());
	}

	private static String escaparComodines(String texto) {
		return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
