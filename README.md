# Sistema de Inventario de Ferretería

Sistema web de inventario y facturación para una ferretería con **una sola bodega**, en pesos
colombianos (COP). Cubre el ciclo completo de la mercancía: compra, recepción, venta, devolución,
cambio de estado, baja, conteo físico, ajuste y cierre mensual.

> Actividad académica: todo el código lo genera la IA (Claude Code); el estudiante supervisa y ejecuta.
> Las reglas de trabajo para la IA están en [CLAUDE.md](CLAUDE.md).

## Propósito

- Controlar existencias por lote y por estado (disponible, reservada, dañada), sin saldos negativos.
- Costear con PEPS y prioridad por vencimiento (FEFO), y calcular precios desde la última compra.
- Emitir documentos numerados: orden de compra (OC), ingreso (DI), factura (FV), nota crédito (NC),
  devolución a proveedor (DP), acta de baja (AB), ajuste (AJ) y apertura (AP).
- Cerrar meses con conteo físico, auditar cada operación y aplicar permisos por rol.

| Dentro del alcance | Fuera del alcance |
| --- | --- |
| Catálogo con marcas, presentaciones, conversiones y códigos de barras | Operación sin internet |
| Clientes, proveedores y datos de la empresa | Múltiples bodegas y traslados |
| Compras, recepciones parciales, ventas de contado con IVA y descuentos | Facturación electrónica DIAN |
| Devoluciones, reservas, dañados, bajas, conteos, ajustes y cierres | Ventas a crédito y cartera |
| Usuarios, roles, auditoría, migración desde Excel, impresión, exportación y API REST | Contabilidad general |

Línea base: 27 requisitos funcionales, 29 reglas de negocio, 17 requisitos no funcionales y
33 tablas en PostgreSQL.

## Stack

| Capa | Tecnologías |
| --- | --- |
| Backend | Java 21, Spring Boot 4.x, Maven Wrapper, JPA, Flyway, Lombok, Spring Security + JWT, springdoc-openapi |
| Pruebas backend | JUnit 5, Mockito, Testcontainers, JaCoCo |
| Frontend | React + Vite (JavaScript con JSDoc), MUI, Formik + Yup, react-i18next, React Router, Axios |
| Pruebas frontend | Vitest + Testing Library |
| Pruebas E2E | Playwright y Selenium WebDriver (JavaScript) |
| Pruebas de carga | k6 |
| Base de datos | PostgreSQL 18 (esquema compatible desde la 16) |
| CI/CD | GitHub Actions |

## Estructura de carpetas

```
inventario-ferreteria/
├── .github/workflows/   Flujos de CI/CD de GitHub Actions
├── backend/             API REST en Spring Boot
├── database/            Esquema SQL probado, datos de prueba, reportes, pruebas y oráculo
├── docs/                Especificación de requisitos, modelo de datos y plan de pruebas de BD
├── e2e/
│   ├── comun/           Entorno, datos y cliente de la API comunes a las dos suites E2E
│   ├── playwright/      Pruebas de extremo a extremo con Playwright
│   └── selenium/        Pruebas de extremo a extremo con Selenium WebDriver
├── frontend/            Aplicación web en React + Vite
├── load-tests/          Pruebas de carga con k6
├── scripts/             Scripts de apoyo (Windows y Linux)
├── docker-compose.e2e.yml  Entorno E2E: PostgreSQL, backend (perfil e2e) y frontend compilado
├── .env.example         Variables del entorno E2E con valores ficticios
├── CLAUDE.md            Reglas de trabajo para la IA
└── README.md
```

## Documentación

- [Especificación de requisitos](docs/especificacion-requisitos.md)
- [Modelo de datos](docs/modelo-datos.md)
- [Plan y casos de prueba de la base de datos](docs/plan-pruebas-bd.md)
- [API REST: contrato OpenAPI y ejemplos](docs/api/README.md)
- [Pruebas end-to-end (Playwright y Selenium)](docs/pruebas-e2e.md)
- [Criterios de aceptación (catálogo)](docs/criterios-aceptacion.yaml) y [casos de prueba](docs/pruebas/casos-de-prueba.md)
- [Pruebas de carga con k6](load-tests/README.md)
- [Integración continua (GitHub Actions)](docs/ci-cd.md)
- [Despliegue a producción (Render + Neon)](docs/despliegue.md)

## Cómo ejecutar

Esta sección se completa en cada fase del proyecto.

### Requisitos previos

- PostgreSQL 16 o superior (recomendado 18) con `psql` y `createdb` en el PATH.
- JDK 21 (`java` y `keytool` en el PATH, o `JAVA_HOME` definido). Maven no hace falta: se usa el Maven Wrapper.
- Docker Desktop en ejecución para las pruebas del backend (Testcontainers levanta `postgres:18`).
- Las demás herramientas (Node.js, k6) se indicarán en la fase correspondiente.

### Base de datos

Crear la base y aplicar el esquema:

```bash
createdb -U postgres ferreteria
psql -U postgres -d ferreteria -v ON_ERROR_STOP=1 -f database/01_esquema_inventario_ferreteria.sql
```

Debe terminar con `COMMIT`. Para probar la base (**solo en una base de pruebas, nunca en producción**):

```bash
psql -U postgres -d ferreteria -v ON_ERROR_STOP=1 -f database/02_datos_prueba.sql
psql -U postgres -d ferreteria -v ON_ERROR_STOP=1 -f database/04_consultas_reportes.sql
psql -U postgres -d ferreteria -f database/03_pruebas_base_datos.sql
```

El resumen final de `03` debe informar 79 pruebas y 0 fallas.

### Backend

API REST en Spring Boot 4 (Java 21). Flyway crea el esquema al arrancar: `V1__esquema_inicial.sql`
(copia de `database/01`) y `V2__reportes.sql` (copia de `database/04`); no hace falta ejecutar esos
scripts a mano en la base del backend.

**Perfil dev** (el que se usa si no se indica otro). Desde la raíz del repositorio:

1. Crear la base vacía de desarrollo:

   ```bash
   createdb -U postgres ferreteria_dev
   ```

2. Copiar las variables de entorno y poner los valores reales (el archivo `backend/.env` no se sube a Git):

   ```powershell
   Copy-Item backend\.env.example backend\.env      # Windows
   ```

   ```bash
   cp backend/.env.example backend/.env             # Linux / macOS
   ```

   Variables: `DB_URL`, `DB_USER`, `DB_PASSWORD` y `SSL_KEYSTORE_PASSWORD` (mínimo 6 caracteres).
   Una variable definida en el sistema tiene prioridad sobre `backend/.env`.

3. Generar el certificado HTTPS autofirmado en `backend/certs/` (ignorado por Git):

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\generar-certificado-dev.ps1   # Windows
   ```

   ```bash
   sh scripts/generar-certificado-dev.sh                                           # Linux / macOS
   ```

   Con `-Forzar` (o `--forzar`) se reemplaza un keystore existente.

4. Arrancar el backend:

   ```powershell
   cd backend; .\mvnw.cmd spring-boot:run     # Windows
   ```

   ```bash
   cd backend && ./mvnw spring-boot:run       # Linux / macOS
   ```

5. Comprobar que responde (el navegador advertirá del certificado autofirmado; es normal en dev):

   - Salud: <https://localhost:8443/actuator/health> → `{"status":"UP"}`
   - Documentación OpenAPI: <https://localhost:8443/swagger-ui.html>

   Cualquier otra ruta responde 401 en formato Problem Details hasta que se implemente el inicio de sesión.

**Pruebas y verificación** (requiere Docker en ejecución):

```powershell
cd backend; .\mvnw.cmd verify     # Windows
```

```bash
cd backend && ./mvnw verify       # Linux / macOS
```

Ejecuta las pruebas contra PostgreSQL 18 en un contenedor con los datos de prueba
(`src/test/resources/db/testdata/V100__datos_prueba.sql`, copia de `database/02`), genera el Javadoc
en `backend/target/reports/apidocs/` y el informe de cobertura en `backend/target/site/jacoco/index.html`.

Para probar la API sin instalar PostgreSQL: `.\mvnw.cmd spring-boot:test-run` arranca la aplicación con
una base efímera en Docker y los datos de prueba (perfil test, HTTP en el puerto 8080).

**Perfil prod**: se activa con `SPRING_PROFILES_ACTIVE=prod`; toma `DB_URL`, `DB_USER`, `DB_PASSWORD` y
`PORT` de las variables de entorno y sirve HTTP detrás del proxy TLS de la plataforma (envía HSTS).

### Frontend

Pendiente: se completa en la fase de frontend.

### Pruebas E2E

Requieren Docker Desktop, Node.js 24 y Google Chrome. Detalle de escenarios, criterios y reportes en
[docs/pruebas-e2e.md](docs/pruebas-e2e.md).

```powershell
Copy-Item .env.example .env        # una sola vez; cambiar los valores
docker compose -f docker-compose.e2e.yml up -d --build --wait
cd e2e/playwright; npm ci; npx playwright install chromium; npm test
cd ../selenium; npm ci; npm test
cd ../..; docker compose -f docker-compose.e2e.yml down
```

### Pruebas de carga

Requieren k6 y el entorno de `docker-compose.e2e.yml`. Modos, base de volumen `ferreteria_carga` y umbrales en
[load-tests/README.md](load-tests/README.md).

```powershell
k6 run -e K6_MODO=humo load-tests/productos.js     # 1 usuario, 30 s (CI)
k6 run load-tests/productos.js                     # 50 usuarios, 5 min, sobre ferreteria_carga
```

### Casos de prueba y cumplimiento de criterios

Requieren Node.js 24. Los casos se generan desde `docs/criterios-aceptacion.yaml`; el cumplimiento se calcula con
los reportes JUnit XML del backend, el frontend, Playwright y Selenium, el resumen de k6 y la cobertura de JaCoCo
que existan en ese momento (conviene ejecutar antes las pruebas que se quieran incluir).

```powershell
cd scripts; npm ci; cd ..                          # una sola vez
node scripts/casos-de-prueba.mjs                   # docs/pruebas/casos-de-prueba.md
node scripts/criterios.mjs                         # reportes/criterios/criterios.md y criterios.json
```

### CI/CD

Pendiente: se completa en la fase de integración continua.

## Convenciones

- Commits en español con formato `tipo(alcance): descripción` (feat, fix, test, docs, ci, refactor).
- El esquema `01` no se modifica; los cambios van en migraciones Flyway nuevas, justificadas con un requisito.
- Los secretos van en variables de entorno o en `.env` (ignorado por Git); se versiona solo `.env.example`.
