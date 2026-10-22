/**
 * Nombre: ElementoCatalogoFormDialog.jsx
 * Descripción: Diálogo con el formulario para crear o renombrar una marca o categoría (RF-CFG-001).
 *              Valida con elementoCatalogoSchema (RNF-INT-002) y muestra bajo el nombre la causa de un
 *              duplicado o de un error de validación del servidor.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import TextField from '@mui/material/TextField';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';

import useNotificacion from '../../hooks/useNotificacion.js';
import {
  actualizarElementoCatalogo,
  crearElementoCatalogo,
} from '../../services/catalogosService.js';
import elementoCatalogoSchema, {
  LIMITES_CATALOGO,
  aPeticionCatalogo,
} from '../../validation/elementoCatalogoSchema.js';
import { literal, traducirError } from '../../validation/traducirError.js';

/**
 * Formulario de marca o categoría en un diálogo. Si recibe `elemento` edita; si no, crea.
 *
 * @param {object} props propiedades del componente
 * @param {import('../../services/catalogosService.js').TipoCatalogo} props.tipo catálogo
 * @param {boolean} props.abierto si el diálogo está visible
 * @param {import('../../services/catalogosService.js').ElementoCatalogo|null} [props.elemento]
 *   elemento a editar, o null para crear
 * @param {() => void} props.onCerrar se ejecuta al cancelar o cerrar
 * @param {(elemento: object) => void} props.onGuardado se ejecuta con el elemento guardado
 * @returns {import('react').JSX.Element} diálogo del formulario
 */
export default function ElementoCatalogoFormDialog({
  tipo,
  abierto,
  elemento = null,
  onCerrar,
  onGuardado,
}) {
  const { t } = useTranslation(['catalogos', 'comun', 'validacion']);
  const { exito, error: notificarError } = useNotificacion();
  const editando = elemento !== null;

  const formik = useFormik({
    initialValues: { nombre: elemento?.nombre ?? '' },
    validationSchema: elementoCatalogoSchema,
    onSubmit: async (valores, { setFieldError }) => {
      try {
        const peticion = aPeticionCatalogo(valores);
        const guardado = editando
          ? await actualizarElementoCatalogo(tipo, elemento.id, peticion)
          : await crearElementoCatalogo(tipo, peticion);
        exito(t(`${tipo}.${editando ? 'actualizado' : 'creado'}`, { nombre: guardado.nombre }));
        onGuardado(guardado);
      } catch (e) {
        if (e?.status === 409 && e.codigo === 'CONFLICTO_UNICIDAD') {
          setFieldError('nombre', literal(e.message));
        } else if (e?.status === 400 && e.errores?.length > 0) {
          const porCampo = e.erroresPorCampo();
          setFieldError('nombre', literal(porCampo.nombre ?? e.message));
        } else {
          notificarError(e.message);
        }
      }
    },
  });

  const tocado = formik.touched.nombre || formik.submitCount > 0;
  const mensajeError = tocado ? traducirError(t, formik.errors.nombre) : undefined;

  return (
    <Dialog
      open={abierto}
      onClose={formik.isSubmitting ? undefined : onCerrar}
      aria-labelledby="catalogo-form-titulo"
      fullWidth
      maxWidth="xs"
    >
      <form noValidate onSubmit={formik.handleSubmit} data-testid="catalogo-form">
        <DialogTitle id="catalogo-form-titulo">
          {t(`${tipo}.${editando ? 'tituloEditar' : 'tituloCrear'}`)}
        </DialogTitle>
        <DialogContent>
          <TextField
            id="catalogo-nombre"
            name="nombre"
            label={t('formulario.nombre')}
            value={formik.values.nombre}
            onChange={formik.handleChange}
            onBlur={formik.handleBlur}
            error={Boolean(mensajeError)}
            helperText={mensajeError}
            required
            autoFocus
            fullWidth
            margin="dense"
            slotProps={{
              htmlInput: {
                maxLength: LIMITES_CATALOGO.nombreMax,
                'data-testid': 'catalogo-campo-nombre',
              },
              formHelperText: { 'data-testid': 'catalogo-error-nombre' },
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={onCerrar} disabled={formik.isSubmitting} data-testid="catalogo-cancelar">
            {t('comun:acciones.cancelar')}
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={formik.isSubmitting}
            data-testid="catalogo-guardar"
          >
            {t('acciones.guardar')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
