Sistema de Inventario de Ferretería
Sistema web de inventario y facturación para una ferretería (una bodega, COP, Colombia).
Actividad académica: todo el código lo genera la IA; el estudiante supervisa y ejecuta.

Documentos fuente (leerlos antes de diseñar)
@docs/especificacion-requisitos.md (27 RF, 29 RN, 17 RNF; criterios de aceptación)
@docs/modelo-datos.md (33 tablas, triggers, vistas)
@docs/plan-pruebas-bd.md (escenario de prueba y valores esperados)
Stack
backend/: Java 21, Spring Boot 4.x, Maven Wrapper, JPA, Flyway, Lombok, Spring Security + JWT,
springdoc-openapi, JUnit 5, Mockito, Testcontainers, JaCoCo.

frontend/: React + Vite en JavaScript con JSDoc, MUI, Formik + Yup, react-i18next,
React Router, Axios, Vitest + Testing Library.

e2e/: Playwright y Selenium WebDriver (JavaScript). load-tests/: k6.
Base de datos: PostgreSQL 18 (el esquema es compatible desde la 16). CI/CD: GitHub Actions.
Sistema operativo del estudiante: Windows 11 (PowerShell); los scripts deben funcionar
en Windows y en los runners Linux de GitHub Actions.

Reglas obligatorias
Base de datos: el esquema es database/01_esquema_inventario_ferreteria.sql y ya está probado.
Nunca lo modifiques ni uses ddl-auto distinto de validate. Todo cambio va en una nueva
migración Flyway (V3__..., V4__...) y debe justificarse con un requisito.

Encabezado en TODOS los archivos de código, prueba, configuración y script:
Nombre, Descripción, Fecha de creación, Última actualización, Autor.
Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code). Fechas AAAA-MM-DD.
Al modificar un archivo, actualiza "Última actualización".
Usa el comentario propio del lenguaje (/** */ en Java y JS, # en YAML y properties,
<!-- --> en HTML y XML).

Documentación: Javadoc en toda clase y método público del backend; JSDoc en todo
componente, hook, servicio y función exportada del frontend.

Idioma: nombres de dominio en español, igual que la base (producto, categoria, tercero);
términos técnicos en inglés (controller, service, repository). Comentarios en español.

Internacionalización: ningún texto visible al usuario va escrito en el código; todo sale
de los archivos de mensajes (español por defecto, inglés como segundo idioma).

Seguridad: nunca escribas contraseñas, tokens o secretos en el código ni en Git;
usa variables de entorno y un archivo .env.example con valores ficticios.

Pruebas: cada prueba que verifique un criterio de aceptación lleva su identificador al
inicio del nombre visible, por ejemplo [CA-RF-PRO-001-1]. Antes de decir que una tarea
está terminada, ejecuta las pruebas del módulo y muestra el resultado.

Commits: mensajes en español con formato "tipo(alcance): descripción"
(feat, fix, test, docs, ci, refactor). No hagas push; el estudiante lo hace.

Si una instrucción contradice la especificación o el modelo, deténte y pregunta.
Comandos
Backend: cd backend && ./mvnw verify (en Windows: mvnw.cmd verify)
Frontend: cd frontend && npm run dev | npm test | npm run build
E2E: cd e2e/playwright && npx playwright test ; cd e2e/selenium && npm test
Carga: k6 run load-tests/productos.js
