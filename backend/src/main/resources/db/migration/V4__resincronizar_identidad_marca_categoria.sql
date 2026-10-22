-- Nombre: V4__resincronizar_identidad_marca_categoria.sql
-- Descripción: Resincroniza las secuencias IDENTITY de marca y categoria con el mayor id existente.
-- Fecha de creación: 2026-10-21
-- Última actualización: 2026-10-21
-- Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
--
-- Justificación (RF-CFG-001):
--   * Con RF-CFG-001 la aplicación empieza a crear marcas y categorías (POST /api/v1/marcas y
--     /api/v1/categorias). En producción estas tablas se cargaron a mano; si se usaron ids explícitos
--     (OVERRIDING SYSTEM VALUE), la secuencia quedó atrás y el primer alta chocaría con la clave
--     primaria (23505).
--   * Con filas, la secuencia queda en max(id) y el siguiente id es max(id) + 1.
--   * Sin filas, queda en 1 con is_called = false, así que el siguiente id es 1.
--   * No cambia tablas ni datos y puede ejecutarse sobre cualquier base (es idempotente).

SELECT setval(pg_get_serial_sequence('marca', 'id'), COALESCE(max(id), 1), max(id) IS NOT NULL)
  FROM marca;

SELECT setval(pg_get_serial_sequence('categoria', 'id'), COALESCE(max(id), 1), max(id) IS NOT NULL)
  FROM categoria;
