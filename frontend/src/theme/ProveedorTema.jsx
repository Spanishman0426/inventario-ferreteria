/**
 * Nombre: ProveedorTema.jsx
 * Descripción: Proveedor del tema de MUI que se recrea cuando cambia el idioma, para que los
 *              textos internos de MUI (paginación, etc.) sigan al idioma de la interfaz.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useMemo } from 'react';
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';

import { crearTema } from './index.js';

/**
 * Aplica el tema localizado y la normalización de estilos de MUI.
 *
 * @param {object} props propiedades del componente
 * @param {import('react').ReactNode} props.children contenido de la aplicación
 * @returns {import('react').JSX.Element} árbol envuelto en el tema
 */
export default function ProveedorTema({ children }) {
  const { i18n } = useTranslation();
  const tema = useMemo(() => crearTema(i18n.language), [i18n.language]);
  return (
    <ThemeProvider theme={tema}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
