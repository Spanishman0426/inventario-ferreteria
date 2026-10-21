/**
 * Nombre: Rol.java
 * Descripción: Entidad JPA de solo lectura para la tabla rol y su relación rol_permiso.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Rol de la matriz 2.2 (ADMIN, GERENTE, JEFE_INVENTARIO, COMPRADOR, VENDEDOR, AUDITOR) con sus
 * permisos, tomados de la tabla {@code rol_permiso}. Los carga el esquema V1.
 */
@Entity
@Table(name = "rol")
@Immutable
@Getter
public class Rol {

	/** Código del rol de administrador. */
	public static final String ADMIN = "ADMIN";

	/** Identificador. */
	@Id
	private Short id;

	/** Código único del rol. */
	@Column(nullable = false, unique = true)
	private String codigo;

	/** Nombre legible. */
	@Column(nullable = false)
	private String nombre;

	/** Permisos concedidos por el rol (tabla rol_permiso). */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "rol_permiso",
			joinColumns = @JoinColumn(name = "rol_id"),
			inverseJoinColumns = @JoinColumn(name = "permiso_id"))
	@BatchSize(size = 10)
	private Set<Permiso> permisos = new HashSet<>();

	/**
	 * Constructor requerido por JPA; la aplicación no crea roles.
	 */
	protected Rol() {
		// Lo usa Hibernate al leer la tabla.
	}

}
