/**
 * Nombre: ProveedoresApp.jsx
 * Descripción: Proveedores globales que no dependen del enrutador: tema de MUI localizado y
 *              notificaciones. Lo usan la aplicación y las pruebas.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import Notificaciones from './components/Notificaciones.jsx';
import ProveedorTema from './theme/ProveedorTema.jsx';

/**
 * Envuelve el contenido con el tema y las notificaciones.
 *
 * @param {object} props propiedades del componente
 * @param {import('react').ReactNode} props.children contenido
 * @returns {import('react').JSX.Element} contenido con los proveedores
 */
export default function ProveedoresApp({ children }) {
  return (
    <ProveedorTema>
      <Notificaciones>{children}</Notificaciones>
    </ProveedorTema>
  );
}
