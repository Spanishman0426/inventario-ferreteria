/**
 * Nombre: Tercero.java
 * Descripción: Entidad JPA de la tabla tercero (RF-TER-001).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cliente, proveedor o ambos; persona natural o jurídica (RF-TER-001, RN-26).
 *
 * <p>La base garantiza las reglas de RN-26 con CHECK (nombres según el tipo, NIT solo para jurídica,
 * dígito de verificación con {@code fn_dv_nit}, al menos un rol) y la unicidad de la identificación
 * por tipo. El Consumidor final viene sembrado en el esquema ({@code esConsumidorFinal}) y la
 * aplicación no lo edita. Los terceros no se eliminan, solo se desactivan; cada cambio queda auditado
 * por el trigger {@code trg_auditar_tercero} (RN-28).</p>
 */
@Entity
@Table(name = "tercero")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tercero {

	/** Identificador generado por la base. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	/** Persona natural o jurídica. */
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_persona", nullable = false)
	private TipoPersona tipoPersona;

	/** CC o NIT. */
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_identificacion", nullable = false)
	private TipoIdentificacion tipoIdentificacion;

	/** Número de identificación, solo dígitos; único por tipo. */
	@Column(name = "numero_identificacion", nullable = false, length = 15)
	private String numeroIdentificacion;

	/** Dígito de verificación del NIT; nulo para cédula. */
	@Column(name = "digito_verificacion")
	private Short digitoVerificacion;

	/** Nombre de la persona natural. */
	@Column
	private String nombre;

	/** Apellido de la persona natural. */
	@Column
	private String apellido;

	/** Razón social de la persona jurídica. */
	@Column(name = "razon_social")
	private String razonSocial;

	/** Dirección de contacto. */
	@Column(nullable = false)
	private String direccion;

	/** Teléfono de 7 a 10 dígitos. */
	@Column(nullable = false, length = 10)
	private String telefono;

	/** Correo opcional. */
	@Column
	private String correo;

	/** Tiene rol de cliente. */
	@Column(name = "es_cliente", nullable = false)
	private boolean esCliente;

	/** Tiene rol de proveedor. */
	@Column(name = "es_proveedor", nullable = false)
	private boolean esProveedor;

	/** Es el Consumidor final predefinido (RN-26); lo fija el esquema. */
	@Column(name = "es_consumidor_final", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private boolean esConsumidorFinal;

	/** Estado de la cuenta del tercero. */
	@Column(nullable = false)
	private boolean activo = true;

	/**
	 * Nombre para buscar y ordenar, calculado por la base con la misma expresión del índice trigram
	 * {@code ix_tercero_nombre_trgm} (RF-TER-001).
	 */
	@Formula("coalesce(razon_social, nombre || ' ' || apellido)")
	@Setter(AccessLevel.NONE)
	private String nombreBusqueda;

	/** Fecha de creación, asignada por la base. */
	@Generated
	@Column(name = "creado_en", insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant creadoEn;

	/**
	 * Crea un tercero nuevo, activo, con su identificación.
	 *
	 * @param tipoPersona persona natural o jurídica
	 * @param tipoIdentificacion CC o NIT
	 * @param numeroIdentificacion número sin dígito de verificación
	 */
	public Tercero(TipoPersona tipoPersona, TipoIdentificacion tipoIdentificacion, String numeroIdentificacion) {
		this.tipoPersona = tipoPersona;
		this.tipoIdentificacion = tipoIdentificacion;
		this.numeroIdentificacion = numeroIdentificacion;
	}

	/**
	 * Roles que tiene el tercero.
	 *
	 * @return conjunto con CLIENTE y/o PROVEEDOR
	 */
	public Set<RolTercero> roles() {
		Set<RolTercero> roles = EnumSet.noneOf(RolTercero.class);
		if (esCliente) {
			roles.add(RolTercero.CLIENTE);
		}
		if (esProveedor) {
			roles.add(RolTercero.PROVEEDOR);
		}
		return roles;
	}

	/**
	 * Nombre para mostrar: la razón social o el nombre y apellido.
	 *
	 * @return nombre para listas y documentos
	 */
	public String nombreMostrado() {
		return tipoPersona == TipoPersona.JURIDICA ? razonSocial : nombre + " " + apellido;
	}

}
