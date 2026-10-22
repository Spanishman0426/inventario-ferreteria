/**
 * Nombre: Categoria.java
 * Descripción: Entidad JPA de la tabla categoria, administrable por el administrador (RF-CFG-001).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Categoría del catálogo: herramientas, eléctricos, plomería, pinturas, etc. (RF-PRO-001, RF-CFG-001).
 *
 * <p>No se elimina: un elemento en uso solo se desactiva. La unicidad del nombre sin distinguir
 * mayúsculas la garantiza el índice {@code ux_categoria_nombre}; cada cambio lo audita el trigger
 * {@code fn_auditar} (RN-28).</p>
 */
@Entity
@Table(name = "categoria")
@Getter
public class Categoria implements ElementoCatalogo {

	/** Identificador generado por la base (IDENTITY). */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Nombre único sin distinguir mayúsculas. */
	@Setter
	@Column(nullable = false)
	private String nombre;

	/** Si puede usarse en productos nuevos o editados. */
	@Setter
	@Column(nullable = false)
	private boolean activo;

	/**
	 * Constructor requerido por JPA.
	 */
	protected Categoria() {
		// Lo usa Hibernate al leer la tabla.
	}

	/**
	 * Crea un elemento activo.
	 *
	 * @param nombre nombre ya recortado
	 */
	public Categoria(String nombre) {
		this.nombre = nombre;
		this.activo = true;
	}

}
