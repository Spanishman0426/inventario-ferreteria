/**
 * Nombre: ControlProducto.java
 * Descripción: Tipo de control de un producto: ninguno, vencimiento o garantía (RN-18).
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

/**
 * Tipo de control (RF-PRO-001, RN-18). Coincide con el CHECK de {@code producto.control}.
 */
public enum ControlProducto {

	/** Sin control especial. */
	NINGUNO,
	/** Cada lote exige fecha de vencimiento; se consume en orden FEFO (RN-19). */
	VENCIMIENTO,
	/** Cada línea de factura registra la fecha fin de garantía; exige plazo en meses. */
	GARANTIA

}
