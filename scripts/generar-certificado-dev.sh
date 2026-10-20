#!/usr/bin/env sh
# Nombre: generar-certificado-dev.sh
# Descripción: Genera el keystore PKCS12 autofirmado para HTTPS del perfil dev en backend/certs/
#              (RNF-SEG-006). La contraseña se toma de SSL_KEYSTORE_PASSWORD o de backend/.env.
# Fecha de creación: 2026-10-20
# Última actualización: 2026-10-20
# Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
#
# Uso (desde cualquier carpeta):
#   sh scripts/generar-certificado-dev.sh [--forzar]

set -eu

RAIZ=$(cd "$(dirname "$0")/.." && pwd)
DIR_CERTS="$RAIZ/backend/certs"
KEYSTORE="$DIR_CERTS/dev-keystore.p12"
ARCHIVO_ENV="$RAIZ/backend/.env"

FORZAR=0
if [ "${1:-}" = "--forzar" ]; then
  FORZAR=1
fi

# La contraseña nunca va en el script: variable de entorno o backend/.env
CLAVE="${SSL_KEYSTORE_PASSWORD:-}"
if [ -z "$CLAVE" ] && [ -f "$ARCHIVO_ENV" ]; then
  CLAVE=$(sed -n 's/^[[:space:]]*SSL_KEYSTORE_PASSWORD[[:space:]]*=[[:space:]]*//p' "$ARCHIVO_ENV" | head -n 1 | tr -d '\r')
fi
if [ -z "$CLAVE" ]; then
  echo "Defina SSL_KEYSTORE_PASSWORD (variable de entorno o backend/.env) antes de generar el certificado." >&2
  exit 1
fi
if [ "${#CLAVE}" -lt 6 ]; then
  echo "SSL_KEYSTORE_PASSWORD debe tener al menos 6 caracteres (exigencia de keytool)." >&2
  exit 1
fi

KEYTOOL=$(command -v keytool || true)
if [ -z "$KEYTOOL" ] && [ -n "${JAVA_HOME:-}" ]; then
  KEYTOOL="$JAVA_HOME/bin/keytool"
fi
if [ -z "$KEYTOOL" ] || [ ! -x "$KEYTOOL" ]; then
  echo "No se encontró keytool. Instale el JDK 21 o defina JAVA_HOME." >&2
  exit 1
fi

if [ -f "$KEYSTORE" ]; then
  if [ "$FORZAR" -eq 0 ]; then
    echo "Ya existe $KEYSTORE. Use --forzar para reemplazarlo."
    exit 0
  fi
  rm -f "$KEYSTORE"
fi

mkdir -p "$DIR_CERTS"

"$KEYTOOL" -genkeypair \
  -alias ferreteria-dev \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 \
  -keystore "$KEYSTORE" \
  -storepass "$CLAVE" \
  -validity 365 \
  -dname "CN=localhost, OU=Desarrollo, O=Ferreteria, C=CO" \
  -ext "SAN=dns:localhost,ip:127.0.0.1"

echo "Keystore generado: $KEYSTORE (alias ferreteria-dev, válido 365 días)."
