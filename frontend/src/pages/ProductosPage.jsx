/**
 * Nombre: ProductosPage.jsx
 * Descripción: Búsqueda paginada de productos por nombre (RF-PRO-003). Solo lectura en esta fase;
 *              el último costo se muestra únicamente con el permiso COSTOS_VER (RN-27).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useEffect, useState } from 'react';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import LinearProgress from '@mui/material/LinearProgress';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import SearchIcon from '@mui/icons-material/Search';
import { useTranslation } from 'react-i18next';

import useAuth from '../hooks/useAuth.js';
import useNotificacion from '../hooks/useNotificacion.js';
import { listarProductos } from '../services/productosService.js';
import { formatearMoneda } from '../utils/formato.js';

/** Tamaños de página ofrecidos. */
const TAMANOS_PAGINA = [10, 20, 50];

/**
 * Lista de productos con búsqueda y paginación.
 *
 * @returns {import('react').JSX.Element} página de productos
 */
export default function ProductosPage() {
  const { t } = useTranslation(['productos', 'comun']);
  const { tienePermiso } = useAuth();
  const { error: notificarError } = useNotificacion();
  const [texto, setTexto] = useState('');
  const [consulta, setConsulta] = useState({ q: '', page: 0, size: 20 });
  const [resultado, setResultado] = useState({ contenido: [], totalElementos: 0 });
  const [cargando, setCargando] = useState(true);
  const verCostos = tienePermiso('COSTOS_VER');

  useEffect(() => {
    let cancelado = false;
    listarProductos(consulta)
      .then((pagina) => {
        if (!cancelado) setResultado(pagina);
      })
      .catch((error) => {
        if (!cancelado) notificarError(error.message);
      })
      .finally(() => {
        if (!cancelado) setCargando(false);
      });
    return () => {
      cancelado = true;
    };
  }, [consulta, notificarError]);

  /**
   * Cambia la consulta y marca la carga.
   *
   * @param {Partial<{q: string, page: number, size: number}>} cambios cambios a aplicar
   */
  const consultar = (cambios) => {
    setCargando(true);
    setConsulta((previa) => ({ ...previa, ...cambios }));
  };

  /**
   * Busca por el texto escrito desde la primera página.
   *
   * @param {import('react').FormEvent} evento envío del formulario
   */
  const buscar = (evento) => {
    evento.preventDefault();
    consultar({ q: texto.trim(), page: 0 });
  };

  const columnas = verCostos ? 7 : 6;

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h1">
        {t('titulo')}
      </Typography>
      <Stack direction="row" spacing={1} component="form" onSubmit={buscar} role="search">
        <TextField
          label={t('buscar')}
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          slotProps={{ htmlInput: { maxLength: 150 } }}
        />
        <Button type="submit" variant="contained" startIcon={<SearchIcon />}>
          {t('comun:acciones.buscar')}
        </Button>
      </Stack>
      <Paper>
        {cargando && <LinearProgress />}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('columnas.nombre')}</TableCell>
                <TableCell>{t('columnas.marca')}</TableCell>
                <TableCell>{t('columnas.categoria')}</TableCell>
                <TableCell>{t('columnas.unidad')}</TableCell>
                <TableCell align="right">{t('columnas.iva')}</TableCell>
                {verCostos && <TableCell align="right">{t('columnas.costo')}</TableCell>}
                <TableCell>{t('columnas.estado')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {resultado.contenido.map((producto) => (
                <TableRow key={producto.id} hover>
                  <TableCell>{producto.nombre}</TableCell>
                  <TableCell>{producto.marca?.nombre}</TableCell>
                  <TableCell>{producto.categoria?.nombre}</TableCell>
                  <TableCell>{producto.unidadBase?.codigo}</TableCell>
                  <TableCell align="right">{producto.tarifaIva?.porcentaje} %</TableCell>
                  {verCostos && (
                    <TableCell align="right">{formatearMoneda(producto.ultimoCostoBase)}</TableCell>
                  )}
                  <TableCell>
                    <Chip
                      size="small"
                      label={t(`estado.${producto.estado}`)}
                      color={producto.estado === 'ACTIVO' ? 'success' : 'default'}
                    />
                  </TableCell>
                </TableRow>
              ))}
              {!cargando && resultado.contenido.length === 0 && (
                <TableRow>
                  <TableCell colSpan={columnas} align="center">
                    {t('sinResultados')}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={resultado.totalElementos}
          page={consulta.page}
          rowsPerPage={consulta.size}
          rowsPerPageOptions={TAMANOS_PAGINA}
          onPageChange={(_evento, pagina) => consultar({ page: pagina })}
          onRowsPerPageChange={(evento) =>
            consultar({ size: Number(evento.target.value), page: 0 })
          }
        />
      </Paper>
    </Stack>
  );
}
