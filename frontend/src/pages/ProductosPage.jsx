/**
 * Nombre: ProductosPage.jsx
 * Descripción: Gestión de productos (RF-PRO-001, RF-PRO-003): búsqueda por nombre, filtros por
 *              categoría y estado, paginación del servidor y acciones crear, editar y desactivar,
 *              visibles solo con CATALOGO_EDITAR. El último costo se muestra únicamente con el
 *              permiso COSTOS_VER (RN-27).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useCallback, useEffect, useState } from 'react';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import LinearProgress from '@mui/material/LinearProgress';
import MenuItem from '@mui/material/MenuItem';
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
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import AddIcon from '@mui/icons-material/Add';
import BlockIcon from '@mui/icons-material/Block';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import { useTranslation } from 'react-i18next';

import ConfirmDialog from '../components/ConfirmDialog.jsx';
import ProductoFormDialog from '../components/productos/ProductoFormDialog.jsx';
import useAuth from '../hooks/useAuth.js';
import useNotificacion from '../hooks/useNotificacion.js';
import { listarCategorias } from '../services/catalogosService.js';
import { inactivarProducto, listarProductos } from '../services/productosService.js';
import { formatearMoneda } from '../utils/formato.js';
import { ESTADOS } from '../validation/productoSchema.js';

/** Tamaños de página ofrecidos. */
const TAMANOS_PAGINA = [10, 20, 50];

/** Consulta inicial: sin filtros, primera página de 20. */
const CONSULTA_INICIAL = { q: '', categoriaId: '', estado: '', page: 0, size: 20 };

/**
 * Lista de productos con búsqueda, filtros, paginación y acciones de edición.
 *
 * @returns {import('react').JSX.Element} página de productos
 */
export default function ProductosPage() {
  const { t } = useTranslation(['productos', 'comun']);
  const { tienePermiso } = useAuth();
  const { exito, error: notificarError } = useNotificacion();
  const [texto, setTexto] = useState('');
  const [consulta, setConsulta] = useState(CONSULTA_INICIAL);
  const [resultado, setResultado] = useState({ contenido: [], totalElementos: 0 });
  const [cargando, setCargando] = useState(true);
  const [recarga, setRecarga] = useState(0);
  const [categorias, setCategorias] = useState([]);
  // Diálogo del formulario: cerrado (null), crear ({producto: null}) o editar ({producto}).
  const [formulario, setFormulario] = useState(null);
  const [porDesactivar, setPorDesactivar] = useState(null);
  const verCostos = tienePermiso('COSTOS_VER');
  const puedeEditar = tienePermiso('CATALOGO_EDITAR');

  useEffect(() => {
    let cancelado = false;
    listarCategorias()
      .then((lista) => {
        if (!cancelado) setCategorias(lista);
      })
      .catch((e) => {
        if (!cancelado) notificarError(e.message);
      });
    return () => {
      cancelado = true;
    };
  }, [notificarError]);

  useEffect(() => {
    let cancelado = false;
    listarProductos(consulta)
      .then((pagina) => {
        if (!cancelado) setResultado(pagina);
      })
      .catch((e) => {
        if (!cancelado) notificarError(e.message);
      })
      .finally(() => {
        if (!cancelado) setCargando(false);
      });
    return () => {
      cancelado = true;
    };
  }, [consulta, recarga, notificarError]);

  /**
   * Cambia la consulta y marca la carga.
   *
   * @param {Partial<typeof CONSULTA_INICIAL>} cambios cambios a aplicar
   */
  const consultar = (cambios) => {
    setCargando(true);
    setConsulta((previa) => ({ ...previa, ...cambios }));
  };

  /** Vuelve a pedir la página actual (tras crear, editar o desactivar). */
  const recargar = useCallback(() => {
    setCargando(true);
    setRecarga((n) => n + 1);
  }, []);

  /**
   * Busca por el texto escrito desde la primera página.
   *
   * @param {import('react').FormEvent} evento envío del formulario
   */
  const buscar = (evento) => {
    evento.preventDefault();
    consultar({ q: texto.trim(), page: 0 });
  };

  /**
   * Cierra el formulario y recarga la lista con el producto guardado.
   */
  const alGuardar = () => {
    setFormulario(null);
    recargar();
  };

  /** Desactiva el producto confirmado y recarga la lista. */
  const desactivar = async () => {
    const producto = porDesactivar;
    setPorDesactivar(null);
    try {
      await inactivarProducto(producto.id);
      exito(t('notificaciones.desactivado', { nombre: producto.nombre }));
      recargar();
    } catch (e) {
      notificarError(e.message);
    }
  };

  const columnas = 6 + (verCostos ? 1 : 0) + (puedeEditar ? 1 : 0);

  return (
    <Stack spacing={2}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5" component="h1">
          {t('titulo')}
        </Typography>
        {puedeEditar && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setFormulario({ producto: null })}
            data-testid="productos-nuevo"
          >
            {t('acciones.nuevo')}
          </Button>
        )}
      </Stack>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={1}
        component="form"
        onSubmit={buscar}
        role="search"
      >
        <TextField
          label={t('buscar')}
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          slotProps={{ htmlInput: { maxLength: 150, 'data-testid': 'productos-buscar' } }}
        />
        <Button
          type="submit"
          variant="contained"
          startIcon={<SearchIcon />}
          data-testid="productos-buscar-boton"
        >
          {t('comun:acciones.buscar')}
        </Button>
        <TextField
          select
          label={t('filtros.categoria')}
          value={consulta.categoriaId}
          onChange={(evento) => consultar({ categoriaId: evento.target.value, page: 0 })}
          sx={{ minWidth: 200 }}
          slotProps={{
            select: { SelectDisplayProps: { 'data-testid': 'productos-filtro-categoria' } },
          }}
        >
          <MenuItem value="">{t('filtros.todas')}</MenuItem>
          {categorias.map((c) => (
            <MenuItem key={c.id} value={c.id}>
              {c.nombre}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          label={t('filtros.estado')}
          value={consulta.estado}
          onChange={(evento) => consultar({ estado: evento.target.value, page: 0 })}
          sx={{ minWidth: 160 }}
          slotProps={{
            select: { SelectDisplayProps: { 'data-testid': 'productos-filtro-estado' } },
          }}
        >
          <MenuItem value="">{t('filtros.todos')}</MenuItem>
          {ESTADOS.map((e) => (
            <MenuItem key={e} value={e}>
              {t(`estado.${e}`)}
            </MenuItem>
          ))}
        </TextField>
      </Stack>
      <Paper>
        {cargando && <LinearProgress />}
        <TableContainer>
          <Table size="small" data-testid="productos-tabla">
            <TableHead>
              <TableRow>
                <TableCell>{t('columnas.nombre')}</TableCell>
                <TableCell>{t('columnas.marca')}</TableCell>
                <TableCell>{t('columnas.categoria')}</TableCell>
                <TableCell>{t('columnas.unidad')}</TableCell>
                <TableCell align="right">{t('columnas.iva')}</TableCell>
                {verCostos && <TableCell align="right">{t('columnas.costo')}</TableCell>}
                <TableCell>{t('columnas.estado')}</TableCell>
                {puedeEditar && <TableCell align="right">{t('columnas.acciones')}</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {resultado.contenido.map((producto) => (
                <TableRow key={producto.id} hover data-testid={`producto-fila-${producto.id}`}>
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
                      data-testid={`producto-estado-${producto.id}`}
                    />
                  </TableCell>
                  {puedeEditar && (
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                      <Tooltip title={t('acciones.editar', { nombre: producto.nombre })}>
                        <IconButton
                          size="small"
                          aria-label={t('acciones.editar', { nombre: producto.nombre })}
                          onClick={() => setFormulario({ producto })}
                          data-testid={`producto-editar-${producto.id}`}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {producto.estado === 'ACTIVO' && (
                        <Tooltip title={t('acciones.desactivar', { nombre: producto.nombre })}>
                          <IconButton
                            size="small"
                            aria-label={t('acciones.desactivar', { nombre: producto.nombre })}
                            onClick={() => setPorDesactivar(producto)}
                            data-testid={`producto-desactivar-${producto.id}`}
                          >
                            <BlockIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
              {!cargando && resultado.contenido.length === 0 && (
                <TableRow>
                  <TableCell
                    colSpan={columnas}
                    align="center"
                    data-testid="productos-sin-resultados"
                  >
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
          data-testid="productos-paginacion"
        />
      </Paper>
      {formulario && (
        <ProductoFormDialog
          key={formulario.producto?.id ?? 'nuevo'}
          abierto
          producto={formulario.producto}
          onCerrar={() => setFormulario(null)}
          onGuardado={alGuardar}
        />
      )}
      <ConfirmDialog
        abierto={porDesactivar !== null}
        titulo={t('desactivar.titulo')}
        mensaje={t('desactivar.mensaje', { nombre: porDesactivar?.nombre ?? '' })}
        textoConfirmar={t('desactivar.confirmar')}
        onConfirmar={desactivar}
        onCancelar={() => setPorDesactivar(null)}
        testId="producto-desactivar-confirmacion"
      />
    </Stack>
  );
}
