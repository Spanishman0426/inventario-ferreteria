/**
 * Nombre: TercerosPage.jsx
 * Descripción: Clientes y proveedores (RF-TER-001): búsqueda por nombre, razón social o identificación,
 *              filtros de rol, tipo de persona y estado, tabla paginada y acciones crear, editar o ver y
 *              activar o desactivar con confirmación. La ruta exige TERCERO_CLIENTE_EDITAR o
 *              TERCERO_PROVEEDOR_EDITAR; las acciones de cada fila dependen de los roles del tercero.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
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
import CheckCircleIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { useTranslation } from 'react-i18next';

import ConfirmDialog from '../components/ConfirmDialog.jsx';
import TerceroFormDialog from '../components/terceros/TerceroFormDialog.jsx';
import useAuth from '../hooks/useAuth.js';
import useNotificacion from '../hooks/useNotificacion.js';
import {
  PERMISO_CLIENTES,
  PERMISO_PROVEEDORES,
  buscarTerceros,
  cambiarEstadoTercero,
} from '../services/tercerosService.js';
import { ESTADOS_TERCERO, ROLES_TERCERO, TIPOS_PERSONA } from '../validation/terceroSchema.js';

/** Tamaños de página ofrecidos. */
const TAMANOS_PAGINA = [10, 20, 50];

/** Consulta inicial: sin filtros, primera página de 20. */
const CONSULTA_INICIAL = { q: '', rol: '', tipoPersona: '', estado: '', page: 0, size: 20 };

/**
 * Identificación para mostrar: el NIT con su dígito (900123456-8) o la cédula.
 *
 * @param {import('../services/tercerosService.js').Tercero} tercero tercero
 * @returns {string} tipo y número
 */
function identificacion(tercero) {
  const numero =
    tercero.digitoVerificacion == null
      ? tercero.numeroIdentificacion
      : `${tercero.numeroIdentificacion}-${tercero.digitoVerificacion}`;
  return `${tercero.tipoIdentificacion} ${numero}`;
}

/**
 * Página de clientes y proveedores.
 *
 * @returns {import('react').JSX.Element} buscador, filtros, tabla y diálogos
 */
export default function TercerosPage() {
  const { t } = useTranslation(['terceros', 'comun']);
  const { tienePermiso } = useAuth();
  const { exito, error: notificarError } = useNotificacion();
  const [texto, setTexto] = useState('');
  const [consulta, setConsulta] = useState(CONSULTA_INICIAL);
  const [resultado, setResultado] = useState({ contenido: [], totalElementos: 0 });
  const [cargando, setCargando] = useState(true);
  const [recarga, setRecarga] = useState(0);
  // Diálogo del formulario: cerrado (null), crear ({tercero: null}) o editar/ver ({tercero}).
  const [formulario, setFormulario] = useState(null);
  // Tercero cuyo cambio de estado espera confirmación.
  const [porCambiar, setPorCambiar] = useState(null);

  useEffect(() => {
    let cancelado = false;
    buscarTerceros(consulta)
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
   * Indica si el usuario puede editar el tercero: no es el Consumidor final y tiene el permiso de
   * cada uno de sus roles (matriz 2.2).
   *
   * @param {import('../services/tercerosService.js').Tercero} tercero tercero de la fila
   * @returns {boolean} verdadero si puede editarlo y cambiar su estado
   */
  const puedeEditar = (tercero) =>
    !tercero.esConsumidorFinal &&
    (!tercero.esCliente || tienePermiso(PERMISO_CLIENTES)) &&
    (!tercero.esProveedor || tienePermiso(PERMISO_PROVEEDORES));

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
    const tercero = porCambiar;
    setPorCambiar(null);
    const nuevo = tercero.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    try {
      await cambiarEstadoTercero(tercero.id, nuevo);
      exito(
        t(`notificaciones.${nuevo === 'ACTIVO' ? 'activado' : 'desactivado'}`, {
          nombre: tercero.nombreMostrado,
        }),
      );
      recargar();
    } catch (e) {
      notificarError(e.message);
    }
  };

  /**
   * Selector de filtro con la opción «Todos».
   *
   * @param {'rol'|'tipoPersona'|'estado'} filtro nombre del filtro
   * @param {string[]} opciones valores posibles
   * @param {string} testId sufijo del data-testid
   * @returns {import('react').JSX.Element} selector
   */
  const selectorFiltro = (filtro, opciones, testId) => (
    <TextField
      select
      label={t(`filtros.${filtro}`)}
      value={consulta[filtro]}
      onChange={(evento) => consultar({ [filtro]: evento.target.value, page: 0 })}
      sx={{ flex: '1 1 160px', minWidth: 160 }}
      slotProps={{ select: { SelectDisplayProps: { 'data-testid': `terceros-filtro-${testId}` } } }}
    >
      <MenuItem value="">{t('filtros.todos')}</MenuItem>
      {opciones.map((valor) => (
        <MenuItem key={valor} value={valor}>
          {t(`${filtro}.${valor}`)}
        </MenuItem>
      ))}
    </TextField>
  );

  const desactivando = porCambiar?.estado === 'ACTIVO';

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h1" data-testid="terceros-titulo">
        {t('titulo')}
      </Typography>
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
          label={t('buscar')}
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          sx={{ flex: '2 1 280px' }}
          slotProps={{ htmlInput: { maxLength: 150, 'data-testid': 'terceros-buscar' } }}
        />
        <Button
          type="submit"
          variant="contained"
          startIcon={<SearchIcon />}
          sx={{ whiteSpace: 'nowrap', flexShrink: 0, minWidth: 'max-content' }}
          data-testid="terceros-buscar-boton"
        >
          {t('comun:acciones.buscar')}
        </Button>
        {selectorFiltro('rol', ROLES_TERCERO, 'rol')}
        {selectorFiltro('tipoPersona', TIPOS_PERSONA, 'tipo-persona')}
        {selectorFiltro('estado', ESTADOS_TERCERO, 'estado')}
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setFormulario({ tercero: null })}
          sx={{ whiteSpace: 'nowrap', flexShrink: 0, minWidth: 'max-content' }}
          data-testid="terceros-nuevo"
        >
          {t('nuevo')}
        </Button>
      </Stack>
      <Paper>
        {cargando && <LinearProgress />}
        <TableContainer>
          <Table size="small" data-testid="terceros-tabla">
            <TableHead>
              <TableRow>
                <TableCell>{t('columnas.identificacion')}</TableCell>
                <TableCell>{t('columnas.nombre')}</TableCell>
                <TableCell>{t('columnas.roles')}</TableCell>
                <TableCell>{t('columnas.telefono')}</TableCell>
                <TableCell>{t('columnas.estado')}</TableCell>
                <TableCell align="right">{t('columnas.acciones')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {resultado.contenido.map((tercero) => {
                const activo = tercero.estado === 'ACTIVO';
                const editable = puedeEditar(tercero);
                const accion = activo ? 'desactivar' : 'activar';
                const nombre = tercero.nombreMostrado;
                return (
                  <TableRow key={tercero.id} hover data-testid={`tercero-fila-${tercero.id}`}>
                    <TableCell data-testid={`tercero-identificacion-${tercero.id}`}>
                      {identificacion(tercero)}
                    </TableCell>
                    <TableCell data-testid={`tercero-nombre-${tercero.id}`}>
                      {nombre}
                      {tercero.esConsumidorFinal && (
                        <Chip
                          size="small"
                          variant="outlined"
                          label={t('predefinido')}
                          sx={{ ml: 1 }}
                          data-testid={`tercero-predefinido-${tercero.id}`}
                        />
                      )}
                    </TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={0.5}>
                        {tercero.esCliente && (
                          <Chip
                            size="small"
                            label={t('rol.CLIENTE')}
                            data-testid={`tercero-rol-cliente-${tercero.id}`}
                          />
                        )}
                        {tercero.esProveedor && (
                          <Chip
                            size="small"
                            color="secondary"
                            label={t('rol.PROVEEDOR')}
                            data-testid={`tercero-rol-proveedor-${tercero.id}`}
                          />
                        )}
                      </Stack>
                    </TableCell>
                    <TableCell>{tercero.telefono}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={t(`estado.${tercero.estado}`)}
                        color={activo ? 'success' : 'default'}
                        data-testid={`tercero-estado-${tercero.id}`}
                      />
                    </TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                      <Tooltip title={t(editable ? 'acciones.editar' : 'acciones.ver', { nombre })}>
                        <IconButton
                          size="small"
                          aria-label={t(editable ? 'acciones.editar' : 'acciones.ver', { nombre })}
                          onClick={() => setFormulario({ tercero })}
                          data-testid={`tercero-${editable ? 'editar' : 'ver'}-${tercero.id}`}
                        >
                          {editable ? (
                            <EditIcon fontSize="small" />
                          ) : (
                            <VisibilityIcon fontSize="small" />
                          )}
                        </IconButton>
                      </Tooltip>
                      {editable && (
                        <Tooltip title={t(`acciones.${accion}`, { nombre })}>
                          <IconButton
                            size="small"
                            aria-label={t(`acciones.${accion}`, { nombre })}
                            onClick={() => setPorCambiar(tercero)}
                            data-testid={`tercero-${accion}-${tercero.id}`}
                          >
                            {activo ? (
                              <BlockIcon fontSize="small" />
                            ) : (
                              <CheckCircleIcon fontSize="small" />
                            )}
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
              {!cargando && resultado.contenido.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center" data-testid="terceros-sin-resultados">
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
          data-testid="terceros-paginacion"
        />
      </Paper>
      {formulario && (
        <TerceroFormDialog
          key={formulario.tercero?.id ?? 'nuevo'}
          abierto
          tercero={formulario.tercero}
          onCerrar={() => setFormulario(null)}
          onGuardado={() => {
            setFormulario(null);
            recargar();
          }}
        />
      )}
      <ConfirmDialog
        abierto={porCambiar !== null}
        titulo={t(`confirmacion.${desactivando ? 'desactivarTitulo' : 'activarTitulo'}`)}
        mensaje={t(`confirmacion.${desactivando ? 'desactivarMensaje' : 'activarMensaje'}`, {
          nombre: porCambiar?.nombreMostrado ?? '',
        })}
        textoConfirmar={t(
          desactivando ? 'acciones.confirmarDesactivar' : 'acciones.confirmarActivar',
        )}
        onConfirmar={cambiarEstado}
        onCancelar={() => setPorCambiar(null)}
        testId="tercero-estado-confirmacion"
      />
    </Stack>
  );
}
