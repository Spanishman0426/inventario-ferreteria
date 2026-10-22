/**
 * Nombre: ElementoCatalogo.java
 * Descripción: Contrato común de las entidades de catálogo simple (marca y categoría): id, nombre y activo.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

/**
 * Elemento de un catálogo administrable que solo tiene nombre y estado (RF-CFG-001). Permite que
 * {@link ElementoCatalogoService} sirva a marcas y categorías con el mismo código.
 */
public interface ElementoCatalogo {

	/**
	 * Identificador.
	 *
	 * @return id, o {@code null} si aún no se guardó
	 */
	Long getId();

	/**
	 * Nombre único sin distinguir mayúsculas.
	 *
	 * @return nombre
	 */
	String getNombre();

	/**
	 * Cambia el nombre.
	 *
	 * @param nombre nombre nuevo, ya recortado
	 */
	void setNombre(String nombre);

	/**
	 * Si se ofrece en los selectores.
	 *
	 * @return {@code true} si está activo
	 */
	boolean isActivo();

	/**
	 * Activa o desactiva el elemento.
	 *
	 * @param activo estado nuevo
	 */
	void setActivo(boolean activo);

}
