/**
 * Nombre: index.js
 * Descripción: Tema de MUI de la aplicación, combinado con la localización de MUI del idioma activo.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { createTheme } from '@mui/material/styles';
import { enUS, esES } from '@mui/material/locale';

/** Localizaciones de MUI por código de idioma (textos de paginación, diálogos, etc.). */
const LOCALIZACIONES_MUI = { es: esES, en: enUS };

/** Opciones visuales comunes a todos los idiomas. */
const OPCIONES_BASE = {
  palette: {
    primary: { main: '#1f4e79' },
    secondary: { main: '#d9822b' },
    background: { default: '#f4f6f8' },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiTextField: { defaultProps: { size: 'small', fullWidth: true } },
  },
};

/**
 * Crea el tema de MUI para un idioma.
 *
 * @param {string} idioma código del idioma activo ('es' o 'en'); otro valor usa español
 * @returns {import('@mui/material/styles').Theme} tema con la localización de MUI correspondiente
 */
export function crearTema(idioma) {
  const base = idioma?.split('-')[0];
  return createTheme(OPCIONES_BASE, LOCALIZACIONES_MUI[base] ?? esES);
}
