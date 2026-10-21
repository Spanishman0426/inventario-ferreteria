#!/usr/bin/env bash
# Nombre: preparar_env_e2e.sh
# Descripción: Crea el .env de la raíz para docker-compose.e2e.yml, Playwright, Selenium y k6 con valores
#              aleatorios en cada ejecución de CI, y los enmascara en los logs de GitHub Actions.
#              Ningún valor queda en el repositorio (el .env está en .gitignore).
# Fecha de creación: 2026-10-21
# Última actualización: 2026-10-21
# Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
#
# Uso (desde la raíz, en Linux o Git Bash): bash .github/scripts/preparar_env_e2e.sh
set -euo pipefail

aleatorio() { openssl rand -hex "$1"; }

# Las contraseñas de usuario exigen letras y números (RF-SEG-001): prefijo con letras y sufijo con dígito
db_password=$(aleatorio 16)
jwt_secret=$(aleatorio 32)
admin_password="Ad-$(aleatorio 8)-1"
jefe_password="Je-$(aleatorio 8)-2"
vendedor_password="Ve-$(aleatorio 8)-3"

if [ -n "${GITHUB_ACTIONS:-}" ]; then
  for valor in "$db_password" "$jwt_secret" "$admin_password" "$jefe_password" "$vendedor_password"; do
    echo "::add-mask::${valor}"
  done
fi

cat > .env <<EOF
E2E_PUERTO=8088
E2E_DB_PASSWORD=${db_password}
E2E_JWT_SECRET=${jwt_secret}
E2E_ADMIN_PASSWORD=${admin_password}
E2E_JEFE_PASSWORD=${jefe_password}
E2E_VENDEDOR_PASSWORD=${vendedor_password}
EOF

echo "Se creó .env con valores aleatorios para el entorno e2e."
