/**
 * Nombre: Permiso.java
 * Descripción: Entidad JPA de solo lectura para la tabla permiso (26 permisos de la matriz 2.2).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Permiso de la matriz 2.2 (RN-27). Su {@code codigo} (CATALOGO_EDITAR, COSTOS_VER, ...) es la
 * autoridad de Spring Security que exigen los {@code @PreAuthorize}. Los carga el esquema V1; la
 * aplicación no los modifica.
 */
@Entity
@Table(name = "permiso")
@Immutable
@Getter
public class Permiso {

	/** Identificador. */
	@Id
	private Short id;

	/** Código único usado como autoridad. */
	@Column(nullable = false, unique = true)
	private String codigo;

	/** Descripción legible. */
	@Column(nullable = false)
	private String descripcion;

	/**
	 * Constructor requerido por JPA; la aplicación no crea permisos.
	 */
	protected Permiso() {
		// Lo usa Hibernate al leer la tabla.
	}

}
