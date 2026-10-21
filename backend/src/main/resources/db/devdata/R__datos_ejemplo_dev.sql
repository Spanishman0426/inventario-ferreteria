-- Nombre: R__datos_ejemplo_dev.sql
-- Descripción: Datos de ejemplo SOLO para el perfil dev: 5 marcas y 5 categorías de ferretería para probar
--              el CRUD de productos desde Swagger UI. Ni test ni prod cargan la carpeta db/devdata.
-- Fecha de creación: 2026-10-20
-- Última actualización: 2026-10-20
-- Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
--
-- Justificación: RF-PRO-001 exige marca y categoría registradas antes de crear un producto, y su
-- administración (RF-CFG-001) aún no está implementada. No es un cambio de esquema.
--
-- Es una migración REPETIBLE (R__) y no versionada: Flyway la ejecuta después de las versionadas y otra
-- vez solo si cambia su contenido. Si fuera V200, al agregar luego una V4 la base dev ya migrada fallaría
-- la validación (migración resuelta sin aplicar por debajo de la versión actual). Por eso es idempotente:
-- ON CONFLICT sobre los índices únicos lower(nombre) no duplica ni toca lo que ya existe.

-- La auditoría (RN-28) queda firmada por el usuario Sistema
SELECT set_config('app.usuario_id', (SELECT id::text FROM usuario WHERE es_sistema), true);

INSERT INTO marca (nombre) VALUES
  ('Stanley'),
  ('Truper'),
  ('Pavco'),
  ('Pintuco'),
  ('Bosch')
ON CONFLICT ((lower(nombre))) DO NOTHING;

INSERT INTO categoria (nombre) VALUES
  ('Herramientas manuales'),
  ('Eléctricos'),
  ('Plomería'),
  ('Pinturas'),
  ('Tornillería')
ON CONFLICT ((lower(nombre))) DO NOTHING;
