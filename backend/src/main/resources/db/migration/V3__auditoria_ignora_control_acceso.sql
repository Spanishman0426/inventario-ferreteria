-- Nombre: V3__auditoria_ignora_control_acceso.sql
-- Descripción: Reemplaza fn_auditar para que el control de acceso de la tabla usuario no duplique
--              la auditoría del inicio de sesión, y para que un cambio de contraseña sí se audite.
-- Fecha de creación: 2026-10-20
-- Última actualización: 2026-10-20
-- Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
--
-- Justificación (RF-SEG-002, RN-28):
--   * RF-SEG-002 exige que cada operación auditable genere EXACTAMENTE un registro. El inicio de
--     sesión ya se audita como INICIAR_SESION o SESION_FALLIDA; la actualización de
--     intentos_fallidos, bloqueado_hasta y ultimo_acceso que lo acompaña generaba además un
--     MODIFICAR. Ahora un UPDATE de usuario que cambia SOLO esas tres columnas no se audita.
--     Si cambia cualquier otra columna (activo, correo, nombre...), se audita como antes.
--   * El esquema original excluía password_hash del JSON; por eso un cambio solo de contraseña
--     dejaba antes = después y no se auditaba. Ahora se audita como MODIFICAR con la marca
--     "contrasena_cambiada": true, sin guardar el hash.
-- Los 21 triggers trg_auditar_* siguen apuntando a esta función por nombre; no se recrean.

CREATE OR REPLACE FUNCTION fn_auditar()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  v_antes          jsonb;
  v_despues        jsonb;
  v_accion         text;
  v_clave_cambiada boolean := false;
  -- Columnas de control de acceso de usuario (RF-SEG-001) que no se auditan por sí solas
  c_control        text[]  := ARRAY['intentos_fallidos', 'bloqueado_hasta', 'ultimo_acceso'];
BEGIN
  IF TG_OP IN ('UPDATE','DELETE') THEN v_antes   := to_jsonb(OLD) - 'password_hash'; END IF;
  IF TG_OP IN ('INSERT','UPDATE') THEN v_despues := to_jsonb(NEW) - 'password_hash'; END IF;

  IF TG_OP = 'UPDATE' AND TG_TABLE_NAME = 'usuario' THEN
    -- Se compara vía jsonb para no depender del tipo de fila de cada tabla
    v_clave_cambiada := (to_jsonb(OLD) ->> 'password_hash') IS DISTINCT FROM (to_jsonb(NEW) ->> 'password_hash');
    IF NOT v_clave_cambiada AND (v_antes - c_control) = (v_despues - c_control) THEN
      RETURN NULL;
    END IF;
    IF v_clave_cambiada THEN
      v_despues := v_despues || jsonb_build_object('contrasena_cambiada', true);
    END IF;
  END IF;

  IF TG_OP = 'UPDATE' AND v_antes = v_despues THEN
    RETURN NULL;
  END IF;

  v_accion := CASE TG_OP WHEN 'INSERT' THEN 'CREAR' WHEN 'DELETE' THEN 'ELIMINAR' ELSE 'MODIFICAR' END;
  IF TG_OP = 'UPDATE' AND (v_antes ->> 'estado') IS DISTINCT FROM (v_despues ->> 'estado') THEN
    v_accion := CASE v_despues ->> 'estado'
                  WHEN 'ANULADO'  THEN 'ANULAR'
                  WHEN 'APROBADO' THEN 'APROBAR'
                  ELSE 'CAMBIAR_ESTADO' END;
  END IF;

  INSERT INTO auditoria (usuario_id, ip, accion, entidad, entidad_id, valores_antes, valores_despues)
  VALUES (NULLIF(current_setting('app.usuario_id', true), '')::bigint,
          NULLIF(current_setting('app.ip', true), '')::inet,
          v_accion, TG_TABLE_NAME,
          COALESCE(v_despues ->> 'id', v_antes ->> 'id')::bigint,
          v_antes, v_despues);
  RETURN NULL;
END $$;
