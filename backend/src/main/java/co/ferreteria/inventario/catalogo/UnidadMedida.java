/**
 * Nombre: UnidadMedida.java
 * Descripción: Entidad JPA de solo lectura para la tabla unidad_medida (RN-05).
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
 * Unidad base de existencias (RN-05): UND, PAR, JGO, KG, G, LB, LTR, ML, GAL, MTR, CM, M2, M3.
 * Las discretas (UND, PAR, JGO) solo admiten cantidades enteras. Las 13 unidades las carga el esquema.
 */
@Entity
@Table(name = "unidad_medida")
@Immutable
@Getter
public class UnidadMedida {

	/** Código de la unidad (clave primaria). */
	@Id
	private String codigo;

	/** Nombre legible. */
	@Column(nullable = false)
	private String nombre;

	/** Si solo admite cantidades enteras. */
	@Column(name = "es_discreta", nullable = false)
	private boolean esDiscreta;

	/**
	 * Constructor requerido por JPA; la aplicación no crea estos registros.
	 */
	protected UnidadMedida() {
		// Lo usa Hibernate al leer la tabla.
	}

}
