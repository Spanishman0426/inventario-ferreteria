/**
 * Nombre: AppLayout.jsx
 * Descripción: Estructura de las páginas autenticadas: barra superior con navegación, usuario,
 *              selector de idioma y cierre de sesión; el contenido de la ruta va debajo.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useState } from 'react';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import IconButton from '@mui/material/IconButton';
import Toolbar from '@mui/material/Toolbar';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import HardwareIcon from '@mui/icons-material/Hardware';
import LogoutIcon from '@mui/icons-material/Logout';
import { useTranslation } from 'react-i18next';
import { NavLink, Outlet, useNavigate } from 'react-router';

import useAuth from '../hooks/useAuth.js';
import ConfirmDialog from './ConfirmDialog.jsx';
import LanguageSelector from './LanguageSelector.jsx';

/**
 * Plantilla de las páginas con sesión.
 *
 * @returns {import('react').JSX.Element} barra superior y contenido de la ruta
 */
export default function AppLayout() {
  const { t } = useTranslation();
  const { usuario, logout } = useAuth();
  const navegar = useNavigate();
  const [confirmarSalida, setConfirmarSalida] = useState(false);

  /** Cierra la sesión tras la confirmación y vuelve al login. */
  const salir = () => {
    setConfirmarSalida(false);
    logout();
    navegar('/login', { replace: true });
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="sticky">
        <Toolbar sx={{ gap: 1 }}>
          <HardwareIcon aria-hidden />
          <Typography variant="h6" component="span" sx={{ mr: 2, whiteSpace: 'nowrap' }}>
            {t('app.titulo')}
          </Typography>
          <Box component="nav" aria-label={t('menu.navegacion')} sx={{ flexGrow: 1 }}>
            <Button
              color="inherit"
              component={NavLink}
              to="/productos"
              data-testid="menu-productos"
            >
              {t('menu.productos')}
            </Button>
          </Box>
          <Typography
            variant="body2"
            sx={{ display: { xs: 'none', sm: 'block' } }}
            data-testid="usuario-actual"
          >
            {usuario?.nombreCompleto}
          </Typography>
          <LanguageSelector />
          <Tooltip title={t('sesion.salir')}>
            <IconButton
              color="inherit"
              onClick={() => setConfirmarSalida(true)}
              aria-label={t('sesion.salir')}
              data-testid="sesion-salir"
            >
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Container component="main" maxWidth="lg" sx={{ py: 3 }}>
        <Outlet />
      </Container>
      <ConfirmDialog
        abierto={confirmarSalida}
        titulo={t('sesion.confirmarSalidaTitulo')}
        mensaje={t('sesion.confirmarSalidaMensaje')}
        textoConfirmar={t('sesion.salir')}
        onConfirmar={salir}
        onCancelar={() => setConfirmarSalida(false)}
        testId="sesion-salir-confirmacion"
      />
    </Box>
  );
}
