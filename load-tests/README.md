<!--
  Nombre: README.md
  Descripción: Guía de las pruebas de carga con k6: modos humo y carga, base de volumen ferreteria_carga,
               umbrales de RNF-REN-001 y RNF-REN-003, resumen JSON y su uso en el reporte de criterios.
  Fecha de creación: 2026-10-21
  Última actualización: 2026-10-21
  Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
-->

# Pruebas de carga (k6)

`productos.js` inicia sesión una vez (en `setup`) y reparte las operaciones de cada usuario virtual así:

| Operación | Peso | Solicitud |
| --- | --- | --- |
| Búsqueda por nombre | 70 % | `GET /api/v1/productos?q=<texto>&page=<0-2>&size=20` |
| Consulta por id | 20 % | `GET /api/v1/productos/{id}` |
| Creación | 10 % | `POST /api/v1/productos` |

Entre operaciones cada usuario espera de 0,5 a 1,5 s (tiempo de lectura).

**Umbrales** (si alguno no se cumple, k6 termina con código 99 y la prueba falla):

| Umbral | Criterio |
| --- | --- |
| `http_req_duration: p(95)<1000` | CA-RNF-REN-001-1: percentil 95 menor a 1 s |
| `http_req_failed: rate<0.01` | CA-RNF-REN-003-1: menos de 1 % de errores con 50 usuarios |

## Modos

| Modo | Usuarios | Duración | Para qué |
| --- | --- | --- | --- |
| `carga` (por defecto) | 50 | 5 min | Medir RNF-REN-001 y RNF-REN-003 sobre el volumen de diseño. |
| `humo` | 1 | 30 s | CI y verificación rápida: confirma que el script, la API y los umbrales funcionan. |

Se elige con `K6_MODO`. Las demás variables (`K6_BASE_URL`, `K6_USUARIO`, `K6_CONTRASENA`, `K6_RESUMEN`) están
descritas en la cabecera de `productos.js`; por defecto se usa el usuario `jefe` con `E2E_JEFE_PASSWORD`, leída del
sistema o del `.env` de la raíz.

## Modo humo (contra el entorno E2E)

```powershell
docker compose -f docker-compose.e2e.yml up -d --build --wait
k6 run -e K6_MODO=humo load-tests/productos.js
```

## Modo carga (volumen de diseño en `ferreteria_carga`)

`datos-volumen.sql` genera con `generate_series` 50.000 productos con 2 presentaciones cada uno (100.000
presentaciones, el volumen de RNF-REN), 200 marcas, 40 categorías y un precio vigente por presentación. Solo corre
en una base llamada `ferreteria_carga` que ya tenga el esquema de Flyway, y se niega a cargar dos veces.

El mismo entorno de `docker-compose.e2e.yml` levanta esa base aparte con `E2E_DB_NOMBRE` (el backend aplica el
esquema y el escenario de prueba, que trae los usuarios):

```powershell
docker compose -f docker-compose.e2e.yml down
$env:E2E_DB_NOMBRE = 'ferreteria_carga'
docker compose -f docker-compose.e2e.yml up -d --build --wait
docker compose -f docker-compose.e2e.yml cp load-tests/datos-volumen.sql postgres:/tmp/datos-volumen.sql
docker compose -f docker-compose.e2e.yml exec -T postgres psql -U ferreteria -d ferreteria_carga -f /tmp/datos-volumen.sql
k6 run load-tests/productos.js
docker compose -f docker-compose.e2e.yml down
Remove-Item Env:E2E_DB_NOMBRE
```

En Linux (bash) los comandos son los mismos, con `export E2E_DB_NOMBRE=ferreteria_carga`. El archivo se copia al
contenedor en vez de pasarlo por una tubería porque PowerShell 5.1 la convierte a ASCII y perdería las tildes.
La carga tarda cerca de un minuto (también genera unos 250.000 registros de auditoría a nombre del usuario Sistema).
Con `-v productos=5000` se genera un volumen menor para pruebas rápidas.

La base vive en memoria (`tmpfs`): `down` la borra. El resumen indica cuántos productos tenía la base, así se sabe
si la medición se hizo con el volumen de diseño.

## Resumen JSON y reporte de criterios

Al terminar, k6 escribe `load-tests/results/k6-resumen-<modo>.json` (carpeta ignorada por Git salvo su
`.gitkeep`) con modo, usuarios, volumen, p95 total y por operación, tasa de fallas y estado de cada umbral.
`node scripts/criterios.mjs` lo lee: el umbral de p95 cuenta para CA-RNF-REN-001-1 en cualquier modo, y el de
fallas cuenta para CA-RNF-REN-003-1 solo si la prueba corrió con 50 usuarios o más. Con solo el modo humo,
CA-RNF-REN-003-1 queda SIN PRUEBA.
