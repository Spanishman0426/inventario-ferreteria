<#
  Nombre: generar-certificado-dev.ps1
  Descripción: Genera el keystore PKCS12 autofirmado para HTTPS del perfil dev en backend/certs/
               (RNF-SEG-006). La contraseña se toma de SSL_KEYSTORE_PASSWORD o de backend/.env.
  Fecha de creación: 2026-10-20
  Última actualización: 2026-10-20
  Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)

  Uso (desde la raíz del repositorio):
    powershell -ExecutionPolicy Bypass -File scripts\generar-certificado-dev.ps1 [-Forzar]
#>
param(
    # Reemplaza el keystore si ya existe.
    [switch]$Forzar
)

$ErrorActionPreference = 'Stop'

$raiz = Split-Path -Parent $PSScriptRoot
$dirCerts = Join-Path $raiz 'backend\certs'
$keystore = Join-Path $dirCerts 'dev-keystore.p12'
$archivoEnv = Join-Path $raiz 'backend\.env'

# La contraseña nunca va en el script: variable de entorno o backend/.env
$clave = $env:SSL_KEYSTORE_PASSWORD
if (-not $clave -and (Test-Path $archivoEnv)) {
    $linea = Get-Content $archivoEnv | Where-Object { $_ -match '^\s*SSL_KEYSTORE_PASSWORD\s*=' } | Select-Object -First 1
    if ($linea) { $clave = ($linea -split '=', 2)[1].Trim() }
}
if (-not $clave) {
    Write-Error 'Defina SSL_KEYSTORE_PASSWORD (variable de entorno o backend/.env) antes de generar el certificado.'
}
if ($clave.Length -lt 6) {
    Write-Error 'SSL_KEYSTORE_PASSWORD debe tener al menos 6 caracteres (exigencia de keytool).'
}

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool -and $env:JAVA_HOME) {
    $keytool = Get-Command (Join-Path $env:JAVA_HOME 'bin\keytool.exe') -ErrorAction SilentlyContinue
}
if (-not $keytool) {
    Write-Error 'No se encontró keytool. Instale el JDK 21 o defina JAVA_HOME.'
}

if (Test-Path $keystore) {
    if (-not $Forzar) {
        Write-Host "Ya existe $keystore. Use -Forzar para reemplazarlo."
        exit 0
    }
    Remove-Item $keystore -Force -Confirm:$false
}

New-Item -ItemType Directory -Force -Path $dirCerts | Out-Null

& $keytool.Source -genkeypair `
    -alias ferreteria-dev `
    -keyalg RSA -keysize 2048 `
    -storetype PKCS12 `
    -keystore $keystore `
    -storepass $clave `
    -validity 365 `
    -dname 'CN=localhost, OU=Desarrollo, O=Ferreteria, C=CO' `
    -ext 'SAN=dns:localhost,ip:127.0.0.1'
if ($LASTEXITCODE -ne 0) {
    Write-Error "keytool terminó con código $LASTEXITCODE."
}

Write-Host "Keystore generado: $keystore (alias ferreteria-dev, válido 365 días)."
