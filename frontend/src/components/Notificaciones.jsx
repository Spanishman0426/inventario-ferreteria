/**
 * Nombre: Notificaciones.jsx
 * Descripción: Proveedor de notificaciones: muestra un Snackbar de MUI con el último mensaje.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useCallback, useMemo, useState } from 'react';
import Alert from '@mui/material/Alert';
import Snackbar from '@mui/material/Snackbar';

import NotificacionContext from '../context/NotificacionContext.js';

/** Milisegundos que permanece visible una notificación. */
const DURACION_MS = 5000;

/**
 * Provee el contexto de notificaciones y pinta el Snackbar.
 *
 * @param {object} props propiedades del componente
 * @param {import('react').ReactNode} props.children contenido de la aplicación
 * @returns {import('react').JSX.Element} proveedor con el Snackbar
 */
export default function Notificaciones({ children }) {
  const [actual, setActual] = useState({
    abierta: false,
    mensaje: '',
    severidad: 'info',
    clave: 0,
  });

  const notificar = useCallback((mensaje, severidad = 'info') => {
    setActual((previa) => ({ abierta: true, mensaje, severidad, clave: previa.clave + 1 }));
  }, []);

  const valor = useMemo(
    () => ({
      notificar,
      exito: (mensaje) => notificar(mensaje, 'success'),
      error: (mensaje) => notificar(mensaje, 'error'),
    }),
    [notificar],
  );

  /**
   * Cierra el Snackbar salvo cuando el usuario hace clic fuera de él.
   *
   * @param {unknown} _evento evento de cierre
   * @param {string} [motivo] motivo informado por MUI
   */
  const cerrar = (_evento, motivo) => {
    if (motivo === 'clickaway') return;
    setActual((previa) => ({ ...previa, abierta: false }));
  };

  return (
    <NotificacionContext.Provider value={valor}>
      {children}
      <Snackbar
        key={actual.clave}
        open={actual.abierta}
        autoHideDuration={DURACION_MS}
        onClose={cerrar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={cerrar} severity={actual.severidad} variant="filled" sx={{ width: '100%' }}>
          {actual.mensaje}
        </Alert>
      </Snackbar>
    </NotificacionContext.Provider>
  );
}
