/**
 * Nombre: ProductoFormDialog.jsx
 * Descripción: Diálogo con el formulario para crear o editar un producto (RF-PRO-001). Valida con
 *              productoSchema (RNF-INT-002), llena los selectores desde los endpoints de catálogo y
 *              muestra los errores del servidor en el campo que corresponde.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { useEffect, useState } from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import LinearProgress from '@mui/material/LinearProgress';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';

import useNotificacion from '../../hooks/useNotificacion.js';
import {
  listarCategorias,
  listarMarcas,
  listarTarifasIva,
  listarUnidadesMedida,
} from '../../services/catalogosService.js';
import { actualizarProducto, crearProducto } from '../../services/productosService.js';
import productoSchema, {
  CONTROLES,
  ESTADOS,
  LIMITES,
  aPeticion,
  valoresIniciales,
} from '../../validation/productoSchema.js';
import { literal, traducirError } from '../../validation/traducirError.js';

/** Opciones vacías mientras cargan los catálogos. */
const SIN_CATALOGOS = { categorias: [], marcas: [], unidades: [], tarifas: [] };

/**
 * Agrega a la lista la opción actual del producto si no está (por ejemplo, una marca ya inactiva),
 * para que el selector pueda mostrarla al editar.
 *
 * @template T
 * @param {T[]} opciones opciones activas del catálogo
 * @param {T|undefined|null} actual opción que tiene el producto
 * @param {(opcion: T) => unknown} clave obtiene el identificador de una opción
 * @returns {T[]} opciones con la actual incluida
 */
function incluirActual(opciones, actual, clave) {
  if (!actual || opciones.some((o) => clave(o) === clave(actual))) return opciones;
  return [...opciones, actual];
}

/**
 * Formulario de producto en un diálogo. Si recibe `producto` edita; si no, crea.
 *
 * @param {object} props propiedades del componente
 * @param {boolean} props.abierto si el diálogo está visible
 * @param {object|null} [props.producto] producto a editar (ProductoRespuesta) o null para crear
 * @param {() => void} props.onCerrar se ejecuta al cancelar o cerrar
 * @param {(producto: object) => void} props.onGuardado se ejecuta con el producto guardado
 * @returns {import('react').JSX.Element} diálogo del formulario
 */
export default function ProductoFormDialog({ abierto, producto = null, onCerrar, onGuardado }) {
  const { t } = useTranslation(['productos', 'comun', 'validacion']);
  const { exito, error: notificarError } = useNotificacion();
  const [catalogos, setCatalogos] = useState(SIN_CATALOGOS);
  const [cargando, setCargando] = useState(true);
  const editando = producto !== null;

  useEffect(() => {
    if (!abierto) return undefined;
    let cancelado = false;
    Promise.all([listarCategorias(), listarMarcas(), listarUnidadesMedida(), listarTarifasIva()])
      .then(([categorias, marcas, unidades, tarifas]) => {
        if (cancelado) return;
        setCatalogos({
          categorias: incluirActual(categorias, producto?.categoria, (o) => o.id),
          marcas: incluirActual(marcas, producto?.marca, (o) => o.id),
          unidades: incluirActual(unidades, producto?.unidadBase, (o) => o.codigo),
          tarifas: incluirActual(tarifas, producto?.tarifaIva, (o) => o.id),
        });
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
  }, [abierto, producto, notificarError]);

  const formik = useFormik({
    initialValues: valoresIniciales(producto),
    validationSchema: productoSchema,
    onSubmit: async (valores, { setFieldError, setErrors }) => {
      try {
        const peticion = aPeticion(valores);
        const guardado = editando
          ? await actualizarProducto(producto.id, peticion)
          : await crearProducto(peticion);
        exito(
          t(editando ? 'notificaciones.actualizado' : 'notificaciones.creado', {
            nombre: guardado.nombre,
          }),
        );
        onGuardado(guardado);
      } catch (e) {
        if (e?.status === 409 && e.codigo === 'CONFLICTO_UNICIDAD') {
          // Duplicado de nombre y marca (CA-RF-PRO-001-3): la causa va bajo el nombre.
          setFieldError('nombre', literal(e.message));
        } else if (e?.status === 400 && e.errores?.length > 0) {
          const porCampo = e.erroresPorCampo();
          setErrors(Object.fromEntries(Object.entries(porCampo).map(([c, m]) => [c, literal(m)])));
        } else {
          notificarError(e.message);
        }
      }
    },
  });

  /**
   * Propiedades comunes de un campo de texto conectado a Formik, con data-testid para E2E.
   *
   * @param {keyof import('../../validation/productoSchema.js').ValoresProducto} nombre campo
   * @param {string} testId sufijo del data-testid
   * @returns {object} propiedades para TextField
   */
  const campo = (nombre, testId) => {
    const tocado = formik.touched[nombre] || formik.submitCount > 0;
    const mensaje = tocado ? traducirError(t, formik.errors[nombre]) : undefined;
    return {
      id: `producto-${testId}`,
      name: nombre,
      value: formik.values[nombre],
      onChange: formik.handleChange,
      onBlur: formik.handleBlur,
      error: Boolean(mensaje),
      helperText: mensaje,
      fullWidth: true,
      label: t(`formulario.${nombre}`),
    };
  };

  /**
   * slotProps de un campo con data-testid en el input y en el mensaje de error.
   *
   * @param {string} testId sufijo del data-testid
   * @param {object} [htmlInput] propiedades adicionales del input
   * @returns {object} slotProps para TextField
   */
  const slots = (testId, htmlInput = {}) => ({
    htmlInput: { 'data-testid': `producto-campo-${testId}`, ...htmlInput },
    formHelperText: { 'data-testid': `producto-error-${testId}` },
  });

  /**
   * slotProps de un selector: data-testid en el elemento que se pulsa para abrirlo.
   *
   * @param {string} testId sufijo del data-testid
   * @returns {object} slotProps para TextField select
   */
  const slotsSelector = (testId) => ({
    select: { SelectDisplayProps: { 'data-testid': `producto-campo-${testId}` } },
    formHelperText: { 'data-testid': `producto-error-${testId}` },
  });

  /**
   * Cambia el control y limpia el plazo si deja de ser GARANTIA, porque el campo se oculta.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} evento cambio del selector
   */
  const cambiarControl = (evento) => {
    formik.handleChange(evento);
    if (evento.target.value !== 'GARANTIA') {
      formik.setFieldValue('plazoGarantiaMeses', '', false);
      formik.setFieldTouched('plazoGarantiaMeses', false, false);
    }
  };

  return (
    <Dialog
      open={abierto}
      onClose={formik.isSubmitting ? undefined : onCerrar}
      aria-labelledby="producto-form-titulo"
      fullWidth
      maxWidth="sm"
    >
      <form noValidate onSubmit={formik.handleSubmit} data-testid="producto-form">
        <DialogTitle id="producto-form-titulo">
          {t(editando ? 'formulario.tituloEditar' : 'formulario.tituloCrear')}
        </DialogTitle>
        {cargando && <LinearProgress />}
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField
              {...campo('nombre', 'nombre')}
              required
              autoFocus
              slotProps={slots('nombre', { maxLength: LIMITES.nombreMax })}
            />
            <TextField
              {...campo('descripcion', 'descripcion')}
              multiline
              minRows={2}
              slotProps={slots('descripcion', { maxLength: LIMITES.descripcionMax })}
            />
            <TextField
              {...campo('categoriaId', 'categoria')}
              select
              required
              slotProps={slotsSelector('categoria')}
            >
              {catalogos.categorias.map((c) => (
                <MenuItem key={c.id} value={c.id}>
                  {c.nombre}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              {...campo('marcaId', 'marca')}
              select
              required
              slotProps={slotsSelector('marca')}
            >
              {catalogos.marcas.map((m) => (
                <MenuItem key={m.id} value={m.id}>
                  {m.nombre}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              {...campo('unidadBase', 'unidad')}
              select
              required
              slotProps={slotsSelector('unidad')}
            >
              {catalogos.unidades.map((u) => (
                <MenuItem key={u.codigo} value={u.codigo}>
                  {u.codigo} — {u.nombre}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              {...campo('tarifaIvaId', 'iva')}
              select
              required
              slotProps={slotsSelector('iva')}
            >
              {catalogos.tarifas.map((ti) => (
                <MenuItem key={ti.id} value={ti.id}>
                  {ti.nombre}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              {...campo('porcentajeIncremento', 'porcentaje')}
              type="number"
              required
              slotProps={slots('porcentaje', {
                min: LIMITES.porcentajeMin,
                max: LIMITES.porcentajeMax,
                step: 0.01,
              })}
            />
            <TextField
              {...campo('control', 'control')}
              onChange={cambiarControl}
              select
              required
              slotProps={slotsSelector('control')}
            >
              {CONTROLES.map((c) => (
                <MenuItem key={c} value={c}>
                  {t(`control.${c}`)}
                </MenuItem>
              ))}
            </TextField>
            {formik.values.control === 'GARANTIA' && (
              <TextField
                {...campo('plazoGarantiaMeses', 'plazo')}
                type="number"
                required
                slotProps={slots('plazo', {
                  min: LIMITES.plazoMin,
                  max: LIMITES.plazoMax,
                  step: 1,
                })}
              />
            )}
            {editando && (
              <TextField
                {...campo('estado', 'estado')}
                label={t('columnas.estado')}
                select
                slotProps={slotsSelector('estado')}
              >
                {ESTADOS.map((e) => (
                  <MenuItem key={e} value={e}>
                    {t(`estado.${e}`)}
                  </MenuItem>
                ))}
              </TextField>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onCerrar} disabled={formik.isSubmitting} data-testid="producto-cancelar">
            {t('comun:acciones.cancelar')}
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={formik.isSubmitting}
            data-testid="producto-guardar"
          >
            {t('acciones.guardar')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
