/**
 * Nombre: CatalogosPage.jsx
 * Descripción: Administración de catálogos (RF-CFG-001) con pestañas Marcas y Categorías. La ruta exige
 *              el permiso CONFIG_GESTIONAR (matriz 2.2: solo el administrador).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useState } from 'react';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';

import TablaElementosCatalogo from '../components/catalogos/TablaElementosCatalogo.jsx';
import { TIPOS_CATALOGO } from '../services/catalogosService.js';

/**
 * Página de catálogos: una pestaña por catálogo administrable.
 *
 * @returns {import('react').JSX.Element} página de catálogos
 */
export default function CatalogosPage() {
  const { t } = useTranslation('catalogos');
  const [tipo, setTipo] = useState(TIPOS_CATALOGO[0]);

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h1" data-testid="catalogos-titulo">
        {t('titulo')}
      </Typography>
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tipo} onChange={(_evento, valor) => setTipo(valor)} aria-label={t('titulo')}>
          {TIPOS_CATALOGO.map((valor) => (
            <Tab
              key={valor}
              value={valor}
              label={t(`pestanas.${valor}`)}
              id={`catalogos-pestana-${valor}`}
              aria-controls={`catalogos-panel-${valor}`}
              data-testid={`catalogos-pestana-${valor}`}
            />
          ))}
        </Tabs>
      </Box>
      <Box
        role="tabpanel"
        id={`catalogos-panel-${tipo}`}
        aria-labelledby={`catalogos-pestana-${tipo}`}
      >
        {/* La clave reinicia búsqueda y paginación al cambiar de pestaña. */}
        <TablaElementosCatalogo key={tipo} tipo={tipo} />
      </Box>
    </Stack>
  );
}
