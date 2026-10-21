/**
 * Nombre: LoginPage.jsx
 * Descripción: Página de inicio de sesión con Formik y Yup; avisa credenciales inválidas y
 *              cuenta bloqueada tras 5 intentos fallidos (RF-SEG-001).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useState } from 'react';
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import HardwareIcon from '@mui/icons-material/Hardware';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';
import { Navigate, useLocation } from 'react-router';

import LanguageSelector from '../components/LanguageSelector.jsx';
import useAuth from '../hooks/useAuth.js';
import loginSchema from '../validation/loginSchema.js';
import { traducirError } from '../validation/traducirError.js';

/** Ruta a la que se va tras iniciar sesión si no se pidió otra. */
const RUTA_INICIO = '/productos';

/**
 * Aviso del error de inicio de sesión según el código de la API.
 *
 * @param {object} props propiedades del componente
 * @param {import('../services/ApiError.js').default} props.error error de la API
 * @returns {import('react').JSX.Element} alerta traducida
 */
function AvisoError({ error }) {
  const { t } = useTranslation('auth');
  if (error.codigo === 'CUENTA_BLOQUEADA') {
    return (
      <Alert severity="warning" role="alert" data-testid="login-error">
        <AlertTitle>{t('login.cuentaBloqueadaTitulo')}</AlertTitle>
        {/* El detalle del servidor ya viene traducido e incluye la hora de desbloqueo. */}
        {error.detalle || t('login.cuentaBloqueada')}
      </Alert>
    );
  }
  if (error.codigo === 'CREDENCIALES_INVALIDAS') {
    return (
      <Alert severity="error" role="alert" data-testid="login-error">
        {t('login.credencialesInvalidas')}
      </Alert>
    );
  }
  return (
    <Alert severity="error" role="alert" data-testid="login-error">
      {error.message}
    </Alert>
  );
}

/**
 * Formulario de inicio de sesión. Con sesión activa redirige a la ruta pedida o al inicio.
 *
 * @returns {import('react').JSX.Element} página de login
 */
export default function LoginPage() {
  const { t } = useTranslation(['auth', 'comun']);
  const { login, autenticado, sesionExpirada } = useAuth();
  const ubicacion = useLocation();
  const [errorApi, setErrorApi] = useState(null);
  const destino = ubicacion.state?.desde?.pathname ?? RUTA_INICIO;

  const formik = useFormik({
    initialValues: { usuario: '', contrasena: '' },
    validationSchema: loginSchema,
    onSubmit: async (valores) => {
      setErrorApi(null);
      try {
        await login(valores.usuario.trim(), valores.contrasena);
      } catch (error) {
        setErrorApi(error);
      }
    },
  });

  if (autenticado) {
    return <Navigate to={destino} replace />;
  }

  /**
   * Propiedades comunes de un campo del formulario.
   *
   * @param {'usuario'|'contrasena'} campo nombre del campo
   * @returns {object} props para TextField
   */
  const propsCampo = (campo) => {
    const mostrarError = Boolean(formik.touched[campo] && formik.errors[campo]);
    return {
      id: campo,
      name: campo,
      label: t(`login.${campo}`),
      value: formik.values[campo],
      onChange: formik.handleChange,
      onBlur: formik.handleBlur,
      error: mostrarError,
      helperText: mostrarError ? traducirError(t, formik.errors[campo]) : ' ',
      slotProps: {
        htmlInput: { 'data-testid': `login-${campo}` },
        formHelperText: { 'data-testid': `login-error-${campo}` },
      },
    };
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      <Box sx={{ position: 'absolute', top: 8, right: 8 }}>
        <LanguageSelector color="primary" />
      </Box>
      <Card sx={{ width: '100%', maxWidth: 400 }}>
        <CardContent>
          <Stack
            spacing={2}
            component="form"
            noValidate
            onSubmit={formik.handleSubmit}
            data-testid="login-form"
          >
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <HardwareIcon color="primary" aria-hidden />
              <Typography variant="subtitle1" color="text.secondary">
                {t('comun:app.titulo')}
              </Typography>
            </Stack>
            <Typography variant="h5" component="h1" data-testid="login-titulo">
              {t('login.titulo')}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t('login.subtitulo')}
            </Typography>
            {sesionExpirada && !errorApi && (
              <Alert severity="info">{t('login.sesionExpirada')}</Alert>
            )}
            {errorApi && <AvisoError error={errorApi} />}
            <TextField {...propsCampo('usuario')} autoComplete="username" autoFocus />
            <TextField
              {...propsCampo('contrasena')}
              type="password"
              autoComplete="current-password"
            />
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={formik.isSubmitting}
              data-testid="login-entrar"
            >
              {formik.isSubmitting ? t('login.entrando') : t('login.entrar')}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
