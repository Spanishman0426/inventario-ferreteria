/**
 * Nombre: LanguageSelector.jsx
 * Descripción: Selector de idioma (español/inglés); i18next guarda la elección en localStorage.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useState } from 'react';
import Button from '@mui/material/Button';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import TranslateIcon from '@mui/icons-material/Translate';
import { useTranslation } from 'react-i18next';

import { IDIOMAS } from '../i18n/index.js';

/**
 * Botón con menú para cambiar el idioma de la interfaz.
 *
 * @param {object} props propiedades del componente
 * @param {'inherit'|'primary'} [props.color] color del botón; 'inherit' en la barra superior
 * @returns {import('react').JSX.Element} selector de idioma
 */
export default function LanguageSelector({ color = 'inherit' }) {
  const { t, i18n } = useTranslation();
  const [ancla, setAncla] = useState(null);
  const actual = i18n.resolvedLanguage ?? i18n.language;

  /**
   * Cambia el idioma y cierra el menú.
   *
   * @param {string} idioma código del idioma elegido
   */
  const elegir = (idioma) => {
    setAncla(null);
    i18n.changeLanguage(idioma);
  };

  return (
    <>
      <Button
        color={color}
        startIcon={<TranslateIcon />}
        onClick={(evento) => setAncla(evento.currentTarget)}
        aria-label={t('idioma.seleccionar')}
        aria-haspopup="menu"
        aria-controls={ancla ? 'menu-idioma' : undefined}
        aria-expanded={ancla ? 'true' : undefined}
        data-testid="idioma-selector"
      >
        {actual?.toUpperCase()}
      </Button>
      <Menu id="menu-idioma" anchorEl={ancla} open={Boolean(ancla)} onClose={() => setAncla(null)}>
        {IDIOMAS.map((idioma) => (
          <MenuItem
            key={idioma}
            selected={idioma === actual}
            lang={idioma}
            onClick={() => elegir(idioma)}
            data-testid={`idioma-opcion-${idioma}`}
          >
            {t(`idioma.${idioma}`)}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}
