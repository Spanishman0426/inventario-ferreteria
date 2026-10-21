/**
 * Nombre: TarifaIva.java
 * Descripción: Entidad JPA de solo lectura para la tabla tarifa_iva (RN-08).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Tarifa de IVA aplicable a un producto (RN-08): 0 %, 5 % o 19 %.
 */
@Entity
@Table(name = "tarifa_iva")
@Immutable
@Getter
public class TarifaIva {

	/** Identificador. */
	@Id
	private Short id;

	/** Nombre legible (por ejemplo, Tarifa general). */
	@Column(nullable = false)
	private String nombre;

	/** Porcentaje entre 0 y 100. */
	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal porcentaje;

	/** Si puede asignarse a productos. */
	@Column(nullable = false)
	private boolean activo;

	/**
	 * Constructor requerido por JPA; la aplicación no crea estos registros.
	 */
	protected TarifaIva() {
		// Lo usa Hibernate al leer la tabla.
	}

}
