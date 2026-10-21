<!--
  Nombre: README.md
  Descripción: Guía corta de la estructura de carpetas del código fuente del frontend.
  Fecha de creación: 2026-10-21
  Última actualización: 2026-10-21
  Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
-->

# Código fuente del frontend

React + Vite en JavaScript, documentado con JSDoc. Cada archivo lleva su encabezado y todo lo
exportado tiene JSDoc (lo exige `npm run lint`).

| Carpeta       | Contenido                                                                                                                                                                                                                                                                           |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `components/` | Componentes reutilizables: `AppLayout` (barra superior y contenido), `ProtectedRoute` (exige sesión y, opcionalmente, un permiso), `ConfirmDialog`, `LanguageSelector` y `Notificaciones` (proveedor del Snackbar).                                                                 |
| `pages/`      | Una página por ruta: `LoginPage`, `ProductosPage` y `NotFoundPage`.                                                                                                                                                                                                                 |
| `services/`   | `apiClient` (Axios con token, `Accept-Language`, cierre de sesión ante 401 y errores Problem Details convertidos en `ApiError`) y un servicio por recurso de la API (`authService`, `productosService`, `catalogosService`, `usuariosService`, `rolesService`, `auditoriaService`). |
| `context/`    | Contextos de React: `AuthContext` con su `AuthProvider` (usuario, permisos, login y logout) y `NotificacionContext`.                                                                                                                                                                |
| `hooks/`      | Hooks propios: `useAuth` y `useNotificacion`.                                                                                                                                                                                                                                       |
| `i18n/`       | Configuración de i18next y mensajes en `locales/es/*.json` (predeterminado) y `locales/en/*.json`, un archivo por área.                                                                                                                                                             |
| `validation/` | Esquemas Yup. Los mensajes son claves de i18n que `traducirError` traduce al mostrarlos.                                                                                                                                                                                            |
| `theme/`      | Tema de MUI y `ProveedorTema`, que aplica la localización de MUI del idioma activo.                                                                                                                                                                                                 |
| `utils/`      | Utilidades: formato de moneda COP y fechas dd/mm/aaaa en hora de Bogotá (`formato.js`) y almacenamiento del token (`sesionStorage.js`).                                                                                                                                             |
| `test/`       | Preparación de Vitest (`setup.js`) y la utilidad `renderizarApp`.                                                                                                                                                                                                                   |

Los archivos de la raíz son `main.jsx` (punto de entrada), `App.jsx` (componente raíz),
`ProveedoresApp.jsx` (tema y notificaciones) y `RutasApp.jsx` (rutas y permisos de cada una).
Las pruebas van junto al archivo que prueban (`*.test.js` o `*.test.jsx`).

## Reglas

- **Textos:** ningún texto visible va escrito en un componente; todos salen de `i18n/locales`.
- **Sin encabezado:** los archivos JSON (`package.json`, `package-lock.json` y `locales/*.json`)
  no llevan encabezado porque JSON no admite comentarios.
- **Errores de la API:** los componentes deciden según `ApiError.codigo`, que no cambia con el
  idioma, y muestran `ApiError.message`, que el backend ya entrega traducido.
