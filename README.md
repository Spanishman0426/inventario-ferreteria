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
│   ├── playwright/      Pruebas de extremo a extremo con Playwright
│   └── selenium/        Pruebas de extremo a extremo con Selenium WebDriver
├── frontend/            Aplicación web en React + Vite
├── load-tests/          Pruebas de carga con k6
├── scripts/             Scripts de apoyo (Windows y Linux)
├── CLAUDE.md            Reglas de trabajo para la IA
└── README.md
```

## Documentación

- [Especificación de requisitos](docs/especificacion-requisitos.md)
- [Modelo de datos](docs/modelo-datos.md)
- [Plan y casos de prueba de la base de datos](docs/plan-pruebas-bd.md)

## Cómo ejecutar

Esta sección se completa en cada fase del proyecto.

### Requisitos previos

- PostgreSQL 16 o superior (recomendado 18) con `psql` y `createdb` en el PATH.
- Las demás herramientas (JDK 21, Node.js, k6) se indicarán en la fase correspondiente.

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

Pendiente: se completa en la fase de backend.

### Frontend

Pendiente: se completa en la fase de frontend.

### Pruebas E2E

Pendiente: se completa en la fase de pruebas de extremo a extremo.

### Pruebas de carga

Pendiente: se completa en la fase de pruebas de carga.

### CI/CD

Pendiente: se completa en la fase de integración continua.

## Convenciones

- Commits en español con formato `tipo(alcance): descripción` (feat, fix, test, docs, ci, refactor).
- El esquema `01` no se modifica; los cambios van en migraciones Flyway nuevas, justificadas con un requisito.
- Los secretos van en variables de entorno o en `.env` (ignorado por Git); se versiona solo `.env.example`.
