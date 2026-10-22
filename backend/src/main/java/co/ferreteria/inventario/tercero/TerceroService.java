/**
 * Nombre: TerceroService.java
 * Descripción: Reglas del registro de clientes y proveedores: permisos por rol, identificación única,
 *              Consumidor final protegido y activación o desactivación (sin borrado).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ferreteria.inventario.common.error.CampoInvalidoException;
import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.OperacionRechazadaException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.tercero.dto.TerceroPeticion;
import co.ferreteria.inventario.tercero.dto.TerceroRespuesta;

/**
 * Registro de terceros (RF-TER-001, RN-26, RN-27).
 *
 * <ul>
 *   <li>Permisos por rol (matriz 2.2): un cliente exige {@code TERCERO_CLIENTE_EDITAR} y un proveedor
 *       {@code TERCERO_PROVEEDOR_EDITAR}. Al crear se miran los roles pedidos; al editar, los que el
 *       tercero tiene y los que tendrá; al cambiar el estado, los que tiene. Sin el permiso: 403.</li>
 *   <li>La identificación no se repite por tipo (409 {@code CONFLICTO_UNICIDAD}); si dos altas
 *       compiten, el UNIQUE de la base responde 23505 y el manejador global también responde 409.</li>
 *   <li>El Consumidor final se consulta pero no se edita ni se desactiva (422 {@code REGLA_NEGOCIO}).</li>
 *   <li>No hay borrado: un tercero se desactiva. Cambiar al estado que ya tiene no escribe nada.</li>
 *   <li>Se recortan los textos y se descartan los campos que no aplican al tipo de persona.</li>
 * </ul>
 * <p>Cada escritura queda auditada por el trigger de la base en la misma transacción (RN-28).</p>
 */
@Service
public class TerceroService {

	/** Campos por los que se puede ordenar y su propiedad JPA. */
	public static final Map<String, String> CAMPOS_ORDEN = Map.of(
			"id", "id",
			"nombre", "nombreBusqueda",
			"numeroIdentificacion", "numeroIdentificacion",
			"creadoEn", "creadoEn");

	private final TerceroRepository terceros;

	/**
	 * Crea el servicio.
	 *
	 * @param terceros repositorio de terceros
	 */
	public TerceroService(TerceroRepository terceros) {
		this.terceros = terceros;
	}

	/**
	 * Busca terceros paginados con filtros. Incluye al Consumidor final.
	 *
	 * @param q texto en el nombre o razón social, o inicio del número de identificación
	 * @param rol cliente o proveedor
	 * @param tipoPersona natural o jurídica
	 * @param estado activo o inactivo
	 * @param pageable página, tamaño y orden pedidos por el cliente
	 * @return página de terceros
	 */
	@Transactional(readOnly = true)
	public Pagina<TerceroRespuesta> listar(String q, RolTercero rol, TipoPersona tipoPersona, EstadoTercero estado,
			Pageable pageable) {
		Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				traducirOrden(pageable.getSort()));
		return Pagina.desde(terceros.findAll(TerceroEspecificaciones.filtrar(q, rol, tipoPersona, estado), pagina),
				TerceroRespuesta::desde);
	}

	/**
	 * Consulta un tercero, activo o inactivo.
	 *
	 * @param id id del tercero
	 * @return tercero
	 */
	@Transactional(readOnly = true)
	public TerceroRespuesta obtener(long id) {
		return TerceroRespuesta.desde(buscar(id));
	}

	/**
	 * Registra un tercero activo.
	 *
	 * @param datos datos validados
	 * @return tercero creado
	 */
	@Transactional
	public TerceroRespuesta crear(TerceroPeticion datos) {
		exigirPermisos(rolesPedidos(datos));
		String numero = datos.numeroIdentificacion().trim();
		exigirNoDuplicado(datos.tipoIdentificacion(), numero, -1L);
		Tercero tercero = new Tercero(datos.tipoPersona(), datos.tipoIdentificacion(), numero);
		copiar(datos, tercero);
		return TerceroRespuesta.desde(terceros.saveAndFlush(tercero));
	}

	/**
	 * Reemplaza los datos de un tercero.
	 *
	 * @param id id del tercero
	 * @param datos datos validados
	 * @return tercero actualizado
	 */
	@Transactional
	public TerceroRespuesta actualizar(long id, TerceroPeticion datos) {
		Tercero tercero = buscarEditable(id);
		Set<RolTercero> roles = tercero.roles();
		roles.addAll(rolesPedidos(datos));
		exigirPermisos(roles);
		String numero = datos.numeroIdentificacion().trim();
		exigirNoDuplicado(datos.tipoIdentificacion(), numero, id);
		tercero.setTipoPersona(datos.tipoPersona());
		tercero.setTipoIdentificacion(datos.tipoIdentificacion());
		tercero.setNumeroIdentificacion(numero);
		copiar(datos, tercero);
		return TerceroRespuesta.desde(terceros.saveAndFlush(tercero));
	}

	/**
	 * Activa o desactiva un tercero. Si ya estaba en ese estado, no escribe nada (ni auditoría).
	 *
	 * @param id id del tercero
	 * @param estado estado nuevo
	 * @return tercero con su estado
	 */
	@Transactional
	public TerceroRespuesta cambiarEstado(long id, EstadoTercero estado) {
		Tercero tercero = buscarEditable(id);
		exigirPermisos(tercero.roles());
		if (tercero.isActivo() != estado.comoActivo()) {
			tercero.setActivo(estado.comoActivo());
			tercero = terceros.saveAndFlush(tercero);
		}
		return TerceroRespuesta.desde(tercero);
	}

	// ------------------------------------------------------------------ utilidades

	private Tercero buscar(long id) {
		return terceros.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("entidad.tercero", id));
	}

	/**
	 * Tercero que se puede modificar: el Consumidor final no (RN-26). Se revisa antes que los permisos
	 * para que cualquier rol reciba el mismo mensaje.
	 */
	private Tercero buscarEditable(long id) {
		Tercero tercero = buscar(id);
		if (tercero.isEsConsumidorFinal()) {
			throw new OperacionRechazadaException(CodigoError.REGLA_NEGOCIO, "tercero.consumidor_final_no_editable");
		}
		return tercero;
	}

	private void exigirNoDuplicado(TipoIdentificacion tipo, String numero, long idExcluido) {
		if (terceros.existsByTipoIdentificacionAndNumeroIdentificacionAndIdNot(tipo, numero, idExcluido)) {
			throw new ConflictoException("tercero.identificacion_duplicada", tipo.name(), numero);
		}
	}

	private static Set<RolTercero> rolesPedidos(TerceroPeticion datos) {
		Set<RolTercero> roles = EnumSet.noneOf(RolTercero.class);
		if (datos.esCliente()) {
			roles.add(RolTercero.CLIENTE);
		}
		if (datos.esProveedor()) {
			roles.add(RolTercero.PROVEEDOR);
		}
		return roles;
	}

	/**
	 * Exige el permiso de cada rol (RN-27). La anotación del controlador ya garantiza que el usuario
	 * tiene al menos uno de los dos permisos.
	 */
	private static void exigirPermisos(Set<RolTercero> roles) {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		Set<String> permisos = autenticacion == null ? Set.of()
				: autenticacion.getAuthorities().stream().map(GrantedAuthority::getAuthority)
						.collect(Collectors.toSet());
		for (RolTercero rol : roles) {
			if (!permisos.contains(rol.getPermiso())) {
				throw new AccessDeniedException(rol.getPermiso());
			}
		}
	}

	private static void copiar(TerceroPeticion datos, Tercero tercero) {
		boolean natural = datos.tipoPersona() == TipoPersona.NATURAL;
		tercero.setNombre(natural ? recortar(datos.nombre()) : null);
		tercero.setApellido(natural ? recortar(datos.apellido()) : null);
		tercero.setRazonSocial(natural ? null : recortar(datos.razonSocial()));
		tercero.setDigitoVerificacion(datos.tipoIdentificacion() == TipoIdentificacion.NIT
				? datos.digitoVerificacion().shortValue() : null);
		tercero.setDireccion(recortar(datos.direccion()));
		tercero.setTelefono(datos.telefono().trim());
		tercero.setCorreo(recortar(datos.correo()));
		tercero.setEsCliente(datos.esCliente());
		tercero.setEsProveedor(datos.esProveedor());
	}

	private static String recortar(String texto) {
		if (texto == null) {
			return null;
		}
		String limpio = texto.trim();
		return limpio.isEmpty() ? null : limpio;
	}

	/**
	 * Traduce el orden pedido a propiedades JPA, rechaza campos desconocidos y agrega {@code id} como
	 * desempate para que la paginación sea estable.
	 */
	private static Sort traducirOrden(Sort pedido) {
		List<Sort.Order> ordenes = new ArrayList<>();
		boolean conId = false;
		for (Sort.Order orden : pedido) {
			String propiedad = CAMPOS_ORDEN.get(orden.getProperty());
			if (propiedad == null) {
				throw new CampoInvalidoException("sort", "paginacion.orden_no_valido", orden.getProperty(),
						String.join(", ", CAMPOS_ORDEN.keySet().stream().sorted().toList()));
			}
			conId |= "id".equals(propiedad);
			ordenes.add(new Sort.Order(orden.getDirection(), propiedad));
		}
		if (ordenes.isEmpty()) {
			ordenes.add(Sort.Order.asc("nombreBusqueda"));
		}
		if (!conId) {
			ordenes.add(Sort.Order.asc("id"));
		}
		return Sort.by(ordenes);
	}

}
