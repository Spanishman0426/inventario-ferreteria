/**
 * Nombre: TablaElementosCatalogo.jsx
 * Descripción: Administración de un catálogo simple (marcas o categorías, RF-CFG-001): búsqueda por
 *              nombre, filtro de estado, tabla paginada y acciones crear, editar y activar o desactivar
 *              con confirmación. No hay borrado: un elemento en uso solo se desactiva.
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
import AddIcon from '@mui/icons-material/Add';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import { useTranslation } from 'react-i18next';

import useNotificacion from '../../hooks/useNotificacion.js';
import {
  buscarElementosCatalogo,
  cambiarEstadoElementoCatalogo,
} from '../../services/catalogosService.js';
import { ESTADOS_CATALOGO, LIMITES_CATALOGO } from '../../validation/elementoCatalogoSchema.js';
import ConfirmDialog from '../ConfirmDialog.jsx';
import ElementoCatalogoFormDialog from './ElementoCatalogoFormDialog.jsx';

/** Prefijo singular de los data-testid de cada fila, por catálogo. */
const SINGULAR = { marcas: 'marca', categorias: 'categoria' };

/** Tamaños de página ofrecidos. */
const TAMANOS_PAGINA = [10, 20, 50];

/** Consulta inicial: sin filtros, primera página de 20. */
const CONSULTA_INICIAL = { q: '', estado: '', page: 0, size: 20 };

/**
 * Tabla administrable de un catálogo.
 *
 * @param {object} props propiedades del componente
 * @param {import('../../services/catalogosService.js').TipoCatalogo} props.tipo catálogo
 * @returns {import('react').JSX.Element} buscador, tabla y diálogos
 */
export default function TablaElementosCatalogo({ tipo }) {
  const { t } = useTranslation(['catalogos', 'comun']);
  const { exito, error: notificarError } = useNotificacion();
  const [texto, setTexto] = useState('');
  const [consulta, setConsulta] = useState(CONSULTA_INICIAL);
  const [resultado, setResultado] = useState({ contenido: [], totalElementos: 0 });
  const [cargando, setCargando] = useState(true);
  const [recarga, setRecarga] = useState(0);
  // Diálogo del formulario: cerrado (null), crear ({elemento: null}) o editar ({elemento}).
  const [formulario, setFormulario] = useState(null);
  // Elemento cuyo cambio de estado espera confirmación.
  const [porCambiar, setPorCambiar] = useState(null);
  const singular = SINGULAR[tipo];
  const prefijo = `catalogo-${tipo}`;

  useEffect(() => {
    let cancelado = false;
    buscarElementosCatalogo(tipo, consulta)
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
  }, [tipo, consulta, recarga, notificarError]);

  /**
   * Cambia la consulta y marca la carga.
   *
   * @param {Partial<typeof CONSULTA_INICIAL>} cambios cambios a aplicar
   */
  const consultar = (cambios) => {
    setCargando(true);
    setConsulta((previa) => ({ ...previa, ...cambios }));
  };

  /** Vuelve a pedir la página actual (tras crear, editar o cambiar el estado). */
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

  /** Aplica el cambio de estado confirmado y recarga la lista. */
  const cambiarEstado = async () => {
    const elemento = porCambiar;
    setPorCambiar(null);
    const nuevo = elemento.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    try {
      await cambiarEstadoElementoCatalogo(tipo, elemento.id, nuevo);
      exito(
        t(`${tipo}.${nuevo === 'ACTIVO' ? 'activado' : 'desactivado'}`, {
          nombre: elemento.nombre,
        }),
      );
      recargar();
    } catch (e) {
      notificarError(e.message);
    }
  };

  const desactivando = porCambiar?.estado === 'ACTIVO';

  return (
    <Stack spacing={2}>
      <Stack
        direction="row"
        spacing={1}
        useFlexGap
        sx={{ flexWrap: 'wrap', alignItems: 'stretch' }}
        component="form"
        onSubmit={buscar}
        role="search"
      >
        <TextField
          label={t(`${tipo}.buscar`)}
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          sx={{ flex: '2 1 240px' }}
          slotProps={{
            htmlInput: {
              maxLength: LIMITES_CATALOGO.nombreMax,
              'data-testid': `${prefijo}-buscar`,
            },
          }}
        />
        <Button
          type="submit"
          variant="contained"
          startIcon={<SearchIcon />}
          sx={{ whiteSpace: 'nowrap', flexShrink: 0, minWidth: 'max-content' }}
          data-testid={`${prefijo}-buscar-boton`}
        >
          {t('comun:acciones.buscar')}
        </Button>
        <TextField
          select
          label={t('filtros.estado')}
          value={consulta.estado}
          onChange={(evento) => consultar({ estado: evento.target.value, page: 0 })}
          sx={{ flex: '1 1 160px', minWidth: 160 }}
          slotProps={{
            select: { SelectDisplayProps: { 'data-testid': `${prefijo}-filtro-estado` } },
          }}
        >
          <MenuItem value="">{t('filtros.todos')}</MenuItem>
          {ESTADOS_CATALOGO.map((e) => (
            <MenuItem key={e} value={e}>
              {t(`estado.${e}`)}
            </MenuItem>
          ))}
        </TextField>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setFormulario({ elemento: null })}
          sx={{ whiteSpace: 'nowrap', flexShrink: 0, minWidth: 'max-content' }}
          data-testid={`${prefijo}-nuevo`}
        >
          {t(`${tipo}.nuevo`)}
        </Button>
      </Stack>
      <Paper>
        {cargando && <LinearProgress />}
        <TableContainer>
          <Table size="small" data-testid={`${prefijo}-tabla`}>
            <TableHead>
              <TableRow>
                <TableCell>{t('columnas.nombre')}</TableCell>
                <TableCell>{t('columnas.estado')}</TableCell>
                <TableCell align="right">{t('columnas.acciones')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {resultado.contenido.map((elemento) => {
                const activo = elemento.estado === 'ACTIVO';
                const accion = activo ? 'desactivar' : 'activar';
                return (
                  <TableRow
                    key={elemento.id}
                    hover
                    data-testid={`catalogo-${singular}-fila-${elemento.id}`}
                  >
                    <TableCell data-testid={`catalogo-${singular}-nombre-${elemento.id}`}>
                      {elemento.nombre}
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={t(`estado.${elemento.estado}`)}
                        color={activo ? 'success' : 'default'}
                        data-testid={`catalogo-${singular}-estado-${elemento.id}`}
                      />
                    </TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                      <Tooltip title={t('acciones.editar', { nombre: elemento.nombre })}>
                        <IconButton
                          size="small"
                          aria-label={t('acciones.editar', { nombre: elemento.nombre })}
                          onClick={() => setFormulario({ elemento })}
                          data-testid={`catalogo-${singular}-editar-${elemento.id}`}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title={t(`acciones.${accion}`, { nombre: elemento.nombre })}>
                        <IconButton
                          size="small"
                          aria-label={t(`acciones.${accion}`, { nombre: elemento.nombre })}
                          onClick={() => setPorCambiar(elemento)}
                          data-testid={`catalogo-${singular}-${accion}-${elemento.id}`}
                        >
                          {activo ? (
                            <BlockIcon fontSize="small" />
                          ) : (
                            <CheckCircleIcon fontSize="small" />
                          )}
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                );
              })}
              {!cargando && resultado.contenido.length === 0 && (
                <TableRow>
                  <TableCell colSpan={3} align="center" data-testid={`${prefijo}-sin-resultados`}>
                    {t(`${tipo}.sinResultados`)}
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
          data-testid={`${prefijo}-paginacion`}
        />
      </Paper>
      {formulario && (
        <ElementoCatalogoFormDialog
          key={formulario.elemento?.id ?? 'nuevo'}
          tipo={tipo}
          abierto
          elemento={formulario.elemento}
          onCerrar={() => setFormulario(null)}
          onGuardado={() => {
            setFormulario(null);
            recargar();
          }}
        />
      )}
      <ConfirmDialog
        abierto={porCambiar !== null}
        titulo={t(`${tipo}.${desactivando ? 'desactivarTitulo' : 'activarTitulo'}`)}
        mensaje={t(`${tipo}.${desactivando ? 'desactivarMensaje' : 'activarMensaje'}`, {
          nombre: porCambiar?.nombre ?? '',
        })}
        textoConfirmar={t(
          desactivando ? 'acciones.confirmarDesactivar' : 'acciones.confirmarActivar',
        )}
        onConfirmar={cambiarEstado}
        onCancelar={() => setPorCambiar(null)}
        testId={`catalogo-${singular}-estado-confirmacion`}
      />
    </Stack>
  );
}
