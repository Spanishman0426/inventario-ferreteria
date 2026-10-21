/**
 * Nombre: Categoria.java
 * Descripción: Entidad JPA de solo lectura para la tabla categoria.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Categoría del catálogo: herramientas, eléctricos, plomería, pinturas, etc. (RF-PRO-001, RF-CFG-001).
 * En este paso solo se consulta.
 */
@Entity
@Table(name = "categoria")
@Immutable
@Getter
public class Categoria {

	/** Identificador. */
	@Id
	private Long id;

	/** Nombre único sin distinguir mayúsculas. */
	@Column(nullable = false)
	private String nombre;

	/** Si puede usarse en productos nuevos o editados. */
	@Column(nullable = false)
	private boolean activo;

	/**
	 * Constructor requerido por JPA; la aplicación no crea estos registros.
	 */
	protected Categoria() {
		// Lo usa Hibernate al leer la tabla.
	}

}
