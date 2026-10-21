/**
 * Nombre: Producto.java
 * Descripción: Entidad JPA de la tabla producto (RF-PRO-001).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Generated;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Artículo genérico del catálogo con marca, categoría, unidad base, tarifa de IVA, porcentaje de
 * incremento y tipo de control (RF-PRO-001, RN-05, RN-08, RN-18, RN-20).
 *
 * <p>El estado ACTIVO/INACTIVO se guarda en la columna booleana {@code activo}; los productos no se
 * eliminan, solo se inactivan (RF-CFG-001). {@code ultimoCostoBase} lo actualiza cada recepción
 * (RF-COM-002, RN-20) y no se edita desde esta entidad. Cada cambio queda auditado por el trigger
 * {@code trg_auditar_producto} (RN-28).</p>
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Producto {

	/** Identificador generado por la base. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	/** Denominación; única por marca sin distinguir mayúsculas. */
	@Column(nullable = false)
	private String nombre;

	/** Marca del producto. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "marca_id", nullable = false)
	private Marca marca;

	/** Categoría del producto. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private Categoria categoria;

	/** Unidad en la que se guarda la existencia (RN-05). */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unidad_base", nullable = false)
	private UnidadMedida unidadBase;

	/** Tarifa de IVA (RN-08). */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tarifa_iva_id", nullable = false)
	private TarifaIva tarifaIva;

	/** Margen sobre el costo, entre 0 y 500 % (RN-20). */
	@Column(name = "porcentaje_incremento", nullable = false, precision = 6, scale = 2)
	private BigDecimal porcentajeIncremento;

	/** Tipo de control (RN-18). */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ControlProducto control = ControlProducto.NINGUNO;

	/** Plazo de garantía en meses; solo cuando {@code control} es GARANTIA. */
	@Column(name = "plazo_garantia_meses")
	private Short plazoGarantiaMeses;

	/** Detalle adicional. */
	@Column
	private String descripcion;

	/** Costo unitario en unidad base de la última compra; lo fija la recepción (RN-20). */
	@Column(name = "ultimo_costo_base", precision = 18, scale = 4, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private BigDecimal ultimoCostoBase;

	/** Estado ACTIVO/INACTIVO, guardado en la columna {@code activo}. */
	@Convert(converter = EstadoProductoConverter.class)
	@Column(name = "activo", nullable = false)
	private EstadoProducto estado = EstadoProducto.ACTIVO;

	/** Fecha de creación, asignada por la base. */
	@Generated
	@Column(name = "creado_en", insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant creadoEn;

	/**
	 * Crea un producto nuevo, activo.
	 *
	 * @param nombre denominación
	 * @param marca marca
	 * @param categoria categoría
	 * @param unidadBase unidad base
	 * @param tarifaIva tarifa de IVA
	 */
	public Producto(String nombre, Marca marca, Categoria categoria, UnidadMedida unidadBase, TarifaIva tarifaIva) {
		this.nombre = nombre;
		this.marca = marca;
		this.categoria = categoria;
		this.unidadBase = unidadBase;
		this.tarifaIva = tarifaIva;
	}

}
