/**
 * Nombre: ProtectedRoute.jsx
 * Descripción: Ruta protegida: exige sesión y, opcionalmente, un permiso de la matriz 2.2
 *              (RF-SEG-001, RNF-SEG-002). El servidor vuelve a validar cada operación.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import { useTranslation } from 'react-i18next';
import { Navigate, Outlet, useLocation } from 'react-router';

import useAuth from '../hooks/useAuth.js';

/**
 * Muestra su contenido solo si hay sesión y, si se indica, el usuario tiene el permiso. Sin
 * sesión lleva al login recordando la ruta pedida; sin permiso muestra un aviso de acceso denegado.
 *
 * @param {object} props propiedades del componente
 * @param {string} [props.permiso] código de permiso exigido (por ejemplo CATALOGO_CONSULTAR)
 * @param {import('react').ReactNode} [props.children] contenido; si falta, se usa el Outlet de la ruta
 * @returns {import('react').JSX.Element} contenido protegido, redirección o aviso
 */
export default function ProtectedRoute({ permiso, children }) {
  const { t } = useTranslation();
  const { autenticado, cargando, tienePermiso } = useAuth();
  const ubicacion = useLocation();

  if (cargando) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress aria-label={t('sesion.cargando')} />
      </Box>
    );
  }
  if (!autenticado) {
    return <Navigate to="/login" replace state={{ desde: ubicacion }} />;
  }
  if (permiso && !tienePermiso(permiso)) {
    return (
      <Alert severity="warning" sx={{ m: 3 }}>
        <AlertTitle>{t('accesoDenegado.titulo')}</AlertTitle>
        {t('accesoDenegado.mensaje')}
      </Alert>
    );
  }
  return children ?? <Outlet />;
}
