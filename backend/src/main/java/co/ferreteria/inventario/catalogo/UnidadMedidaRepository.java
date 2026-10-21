/**
 * Nombre: UnidadMedidaRepository.java
 * Descripción: Repositorio de solo lectura de la tabla unidad_medida.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consulta de la tabla {@code unidad_medida} (RN-05). La tabla no tiene columna {@code activo}:
 * las 13 unidades siempre están disponibles.
 */
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, String> {
}
