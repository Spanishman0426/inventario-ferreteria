#!/usr/bin/env bash
# Nombre: verificar_despliegue.sh
# Descripción: Verificación de humo del despliegue en Render por HTTPS. Fase 1: espera (hasta 15 min) a que
#              el backend (/actuator/info) y el frontend (/version.json) publiquen el commit desplegado, porque
#              mientras Render compila sigue respondiendo la versión anterior. Fase 2: reintenta durante
#              3 min (el backend gratuito tarda en despertar) hasta que /actuator/health responda UP y el
#              frontend cargue en / y en /login (regla de reescritura de React Router).
# Fecha de creación: 2026-10-21
# Última actualización: 2026-10-21
# Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
#
# Uso (Linux o Git Bash):
#   bash .github/scripts/verificar_despliegue.sh <url-backend> <url-frontend> [commit]
# Sin commit (o con "-") se omite la fase 1 y solo se verifica la salud.
# Variables opcionales: ESPERA_VERSION_SEG (900), HUMO_SEG (180), INTERVALO_SEG (10).
# En GitHub Actions escribe la causa de una falla en la salida "motivo" del paso.
set -euo pipefail

URL_BACKEND="${1:?Falta la URL del backend}"
URL_FRONTEND="${2:?Falta la URL del frontend}"
COMMIT="${3:--}"
URL_BACKEND="${URL_BACKEND%/}"
URL_FRONTEND="${URL_FRONTEND%/}"
ESPERA_VERSION_SEG="${ESPERA_VERSION_SEG:-900}"
HUMO_SEG="${HUMO_SEG:-180}"
INTERVALO_SEG="${INTERVALO_SEG:-10}"

# Registra la causa de la falla (para el issue deploy-fallo) y termina con error
fallar() {
  echo "::error::$1"
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "motivo=$1" >> "$GITHUB_OUTPUT"
  fi
  exit 1
}

# Descarga una URL; imprime el cuerpo o nada si falla (código distinto de 2xx, tiempo agotado, etc.)
descargar() {
  curl --silent --show-error --fail --location --max-time 30 \
       -H 'Cache-Control: no-cache' "$1" 2>/dev/null || true
}

# Extrae el valor de "commit" de un JSON pequeño sin depender de jq
leer_commit() {
  # "|| true": sin coincidencia grep devuelve 1 y, con pipefail, terminaría el script
  { grep -o '"commit"[[:space:]]*:[[:space:]]*"[^"]*"' || true; } | head -n1 | sed -E 's/.*"([^"]*)"$/\1/'
}

for url in "$URL_BACKEND" "$URL_FRONTEND"; do
  case "$url" in
    https://*) ;;
    *) fallar "La URL $url no usa HTTPS; la verificación de humo solo se hace por HTTPS." ;;
  esac
done

# ------------------------------------------------------------------ fase 1: nueva versión publicada
if [ "$COMMIT" != "-" ]; then
  echo "Fase 1: esperando el commit ${COMMIT:0:7} en backend y frontend (máximo ${ESPERA_VERSION_SEG} s)..."
  limite=$((SECONDS + ESPERA_VERSION_SEG))
  commit_backend=""
  commit_frontend=""
  while :; do
    commit_backend="$(descargar "$URL_BACKEND/actuator/info" | leer_commit)"
    # Parámetro distinto en cada intento para no recibir una copia en caché
    commit_frontend="$(descargar "$URL_FRONTEND/version.json?t=$SECONDS" | leer_commit)"
    if [ "$commit_backend" = "$COMMIT" ] && [ "$commit_frontend" = "$COMMIT" ]; then
      echo "Fase 1 correcta: backend y frontend publican ${COMMIT:0:7}."
      break
    fi
    if [ "$SECONDS" -ge "$limite" ]; then
      fallar "La nueva versión no se publicó en ${ESPERA_VERSION_SEG} s (backend: ${commit_backend:-sin respuesta}, frontend: ${commit_frontend:-sin respuesta}, esperado: ${COMMIT}). Revisa los logs de despliegue en Render."
    fi
    echo "  backend: ${commit_backend:-sin respuesta} · frontend: ${commit_frontend:-sin respuesta}; nuevo intento en ${INTERVALO_SEG} s"
    sleep "$INTERVALO_SEG"
  done
fi

# ------------------------------------------------------------------ fase 2: salud con reintentos
echo "Fase 2: verificando salud por HTTPS (reintentos durante ${HUMO_SEG} s)..."
limite=$((SECONDS + HUMO_SEG))
while :; do
  salud="sin respuesta"; inicio="sin respuesta"; login="sin respuesta"
  if descargar "$URL_BACKEND/actuator/health" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then salud="UP"; fi
  if descargar "$URL_FRONTEND/" | grep -q '<div id="root">'; then inicio="carga"; fi
  # Ruta de React Router: si la reescritura a index.html falla, Render responde 404
  if descargar "$URL_FRONTEND/login" | grep -q '<div id="root">'; then login="carga"; fi
  if [ "$salud" = "UP" ] && [ "$inicio" = "carga" ] && [ "$login" = "carga" ]; then
    echo "Fase 2 correcta: /actuator/health UP, frontend carga en / y en /login."
    exit 0
  fi
  if [ "$SECONDS" -ge "$limite" ]; then
    fallar "Verificación de humo fallida tras ${HUMO_SEG} s: backend /actuator/health ${salud}, frontend / ${inicio}, frontend /login ${login}."
  fi
  echo "  health: ${salud} · /: ${inicio} · /login: ${login}; nuevo intento en ${INTERVALO_SEG} s"
  sleep "$INTERVALO_SEG"
done
