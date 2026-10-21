/**
 * Nombre: NotFoundPage.jsx
 * Descripción: Página para rutas inexistentes, con enlace de vuelta al inicio.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';

/**
 * Aviso de página no encontrada.
 *
 * @returns {import('react').JSX.Element} página 404
 */
export default function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <Stack spacing={2} sx={{ alignItems: 'center', mt: 10, px: 2, textAlign: 'center' }}>
      <Typography variant="h3" component="p" color="text.secondary">
        404
      </Typography>
      <Typography variant="h5" component="h1">
        {t('noEncontrado.titulo')}
      </Typography>
      <Typography color="text.secondary">{t('noEncontrado.mensaje')}</Typography>
      <Button variant="contained" component={Link} to="/">
        {t('acciones.volverInicio')}
      </Button>
    </Stack>
  );
}
