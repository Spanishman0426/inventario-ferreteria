/**
 * Nombre: TerceroFormDialog.jsx
 * Descripción: Diálogo con el formulario para crear, editar o consultar un tercero (RF-TER-001). Los
 *              campos cambian según el tipo de persona; con NIT el dígito de verificación se calcula en
 *              vivo y se puede corregir; las casillas Cliente y Proveedor dependen de los permisos del
 *              usuario y, si no puede editar el tercero, se abre en solo lectura con el motivo.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControl from '@mui/material/FormControl';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormGroup from '@mui/material/FormGroup';
import FormHelperText from '@mui/material/FormHelperText';
import FormLabel from '@mui/material/FormLabel';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';

import useAuth from '../../hooks/useAuth.js';
import useNotificacion from '../../hooks/useNotificacion.js';
import {
  PERMISO_CLIENTES,
  PERMISO_PROVEEDORES,
  actualizarTercero,
  crearTercero,
} from '../../services/tercerosService.js';
import { calcularDigitoVerificacion } from '../../utils/nit.js';
import terceroSchema, {
  LIMITES_TERCERO,
  TIPOS_IDENTIFICACION,
  TIPOS_PERSONA,
  aPeticionTercero,
  valoresInicialesTercero,
} from '../../validation/terceroSchema.js';
import { literal, traducirError } from '../../validation/traducirError.js';

/**
 * Dígito de verificación que corresponde al número, como texto para el campo ('' si no se puede
 * calcular).
 *
 * @param {string} numero número de identificación
 * @returns {string} dígito o texto vacío
 */
function digitoPara(numero) {
  const digito = calcularDigitoVerificacion(numero.trim());
  return digito === null ? '' : String(digito);
}

/**
 * Formulario de tercero en un diálogo. Si recibe `tercero` edita (o solo muestra, si el usuario no
 * puede editarlo); si no, crea.
 *
 * @param {object} props propiedades del componente
 * @param {boolean} props.abierto si el diálogo está visible
 * @param {import('../../services/tercerosService.js').Tercero|null} [props.tercero] tercero a editar o
 *        null para crear
 * @param {() => void} props.onCerrar se ejecuta al cancelar o cerrar
 * @param {(tercero: object) => void} props.onGuardado se ejecuta con el tercero guardado
 * @returns {import('react').JSX.Element} diálogo del formulario
 */
export default function TerceroFormDialog({ abierto, tercero = null, onCerrar, onGuardado }) {
  const { t } = useTranslation(['terceros', 'comun', 'validacion']);
  const { tienePermiso } = useAuth();
  const { exito, error: notificarError } = useNotificacion();
  const editando = tercero !== null;
  const permisos = {
    cliente: tienePermiso(PERMISO_CLIENTES),
    proveedor: tienePermiso(PERMISO_PROVEEDORES),
  };

  // Roles actuales del tercero que el usuario no puede editar (matriz 2.2).
  const rolesSinPermiso = editando
    ? [
        tercero.esCliente && !permisos.cliente ? 'CLIENTE' : null,
        tercero.esProveedor && !permisos.proveedor ? 'PROVEEDOR' : null,
      ].filter(Boolean)
    : [];
  let motivoSoloLectura = null;
  if (tercero?.esConsumidorFinal) {
    motivoSoloLectura = t('formulario.soloLecturaConsumidorFinal');
  } else if (rolesSinPermiso.length > 0) {
    motivoSoloLectura = t('formulario.soloLecturaPermisos', {
      roles: rolesSinPermiso.map((r) => t(`rol.${r}`).toLowerCase()).join(' / '),
    });
  }
  const soloLectura = motivoSoloLectura !== null;

  const formik = useFormik({
    initialValues: valoresInicialesTercero(tercero, permisos),
    validationSchema: terceroSchema,
    onSubmit: async (valores, { setFieldError, setErrors }) => {
      try {
        const peticion = aPeticionTercero(valores);
        const guardado = editando
          ? await actualizarTercero(tercero.id, peticion)
          : await crearTercero(peticion);
        exito(
          t(editando ? 'notificaciones.actualizado' : 'notificaciones.creado', {
            nombre: guardado.nombreMostrado,
          }),
        );
        onGuardado(guardado);
      } catch (e) {
        if (e?.status === 409 && e.codigo === 'CONFLICTO_UNICIDAD') {
          // Identificación duplicada (CA-RF-TER-001-3): la causa va bajo el número.
          setFieldError('numeroIdentificacion', literal(e.message));
        } else if (e?.status === 400 && e.errores?.length > 0) {
          const porCampo = e.erroresPorCampo();
          setErrors(Object.fromEntries(Object.entries(porCampo).map(([c, m]) => [c, literal(m)])));
        } else {
          notificarError(e.message);
        }
      }
    },
  });

  const { values } = formik;
  const natural = values.tipoPersona === 'NATURAL';
  const conNit = values.tipoIdentificacion === 'NIT';

  /**
   * Mensaje de error traducido de un campo, si ya se tocó o se intentó guardar.
   *
   * @param {string} nombre campo
   * @returns {string|undefined} mensaje
   */
  const errorDe = (nombre) =>
    formik.touched[nombre] || formik.submitCount > 0
      ? traducirError(t, formik.errors[nombre])
      : undefined;

  /**
   * Propiedades comunes de un campo de texto conectado a Formik, con data-testid para E2E.
   *
   * @param {keyof import('../../validation/terceroSchema.js').ValoresTercero} nombre campo
   * @param {string} testId sufijo del data-testid
   * @param {object} [htmlInput] propiedades adicionales del input
   * @returns {object} propiedades para TextField
   */
  const campo = (nombre, testId, htmlInput = {}) => {
    const mensaje = errorDe(nombre);
    return {
      id: `tercero-${testId}`,
      name: nombre,
      value: values[nombre],
      onChange: formik.handleChange,
      onBlur: formik.handleBlur,
      error: Boolean(mensaje),
      helperText: mensaje,
      fullWidth: true,
      disabled: soloLectura || formik.isSubmitting,
      label: t(`formulario.${nombre}`),
      slotProps: {
        htmlInput: { 'data-testid': `tercero-campo-${testId}`, ...htmlInput },
        formHelperText: { 'data-testid': `tercero-error-${testId}` },
      },
    };
  };

  /**
   * Cambia el tipo de persona: una jurídica solo se identifica con NIT, así que lo fija y calcula
   * el dígito.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} evento cambio del selector
   */
  const cambiarTipoPersona = (evento) => {
    formik.handleChange(evento);
    if (evento.target.value === 'JURIDICA' && values.tipoIdentificacion !== 'NIT') {
      formik.setFieldValue('tipoIdentificacion', 'NIT');
      formik.setFieldValue('digitoVerificacion', digitoPara(values.numeroIdentificacion));
    }
  };

  /**
   * Cambia el tipo de identificación: con NIT calcula el dígito; con CC lo borra.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} evento cambio del selector
   */
  const cambiarTipoIdentificacion = (evento) => {
    formik.handleChange(evento);
    formik.setFieldValue(
      'digitoVerificacion',
      evento.target.value === 'NIT' ? digitoPara(values.numeroIdentificacion) : '',
    );
  };

  /**
   * Cambia el número y, con NIT, recalcula el dígito en vivo (el usuario puede corregirlo después).
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} evento cambio del campo
   */
  const cambiarNumero = (evento) => {
    formik.handleChange(evento);
    if (conNit) {
      formik.setFieldValue('digitoVerificacion', digitoPara(evento.target.value));
    }
  };

  const errorRol = errorDe('esCliente');
  const avisosRol = [
    !permisos.cliente && !soloLectura
      ? t('formulario.sinPermisoRol', { rol: t('rol.CLIENTE') })
      : null,
    !permisos.proveedor && !soloLectura
      ? t('formulario.sinPermisoRol', { rol: t('rol.PROVEEDOR') })
      : null,
  ].filter(Boolean);

  let titulo = 'formulario.tituloCrear';
  if (soloLectura) titulo = 'formulario.tituloVer';
  else if (editando) titulo = 'formulario.tituloEditar';

  return (
    <Dialog
      open={abierto}
      onClose={formik.isSubmitting ? undefined : onCerrar}
      aria-labelledby="tercero-form-titulo"
      fullWidth
      maxWidth="sm"
    >
      <form noValidate onSubmit={formik.handleSubmit} data-testid="tercero-form">
        <DialogTitle id="tercero-form-titulo">{t(titulo)}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {soloLectura && (
              <Alert severity="info" data-testid="tercero-solo-lectura">
                {motivoSoloLectura}
              </Alert>
            )}
            <TextField
              {...campo('tipoPersona', 'tipo-persona')}
              onChange={cambiarTipoPersona}
              select
              required
              slotProps={{
                select: { SelectDisplayProps: { 'data-testid': 'tercero-campo-tipo-persona' } },
                formHelperText: { 'data-testid': 'tercero-error-tipo-persona' },
              }}
            >
              {TIPOS_PERSONA.map((tipo) => (
                <MenuItem key={tipo} value={tipo}>
                  {t(`tipoPersona.${tipo}`)}
                </MenuItem>
              ))}
            </TextField>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                {...campo('tipoIdentificacion', 'tipo-identificacion')}
                onChange={cambiarTipoIdentificacion}
                select
                required
                sx={{ flex: '1 1 0' }}
                slotProps={{
                  select: {
                    SelectDisplayProps: { 'data-testid': 'tercero-campo-tipo-identificacion' },
                  },
                  formHelperText: { 'data-testid': 'tercero-error-tipo-identificacion' },
                }}
              >
                {TIPOS_IDENTIFICACION.filter((tipo) => natural || tipo === 'NIT').map((tipo) => (
                  <MenuItem key={tipo} value={tipo}>
                    {t(`tipoIdentificacion.${tipo}`)}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                {...campo('numeroIdentificacion', 'numero', {
                  maxLength: LIMITES_TERCERO.identificacionMax,
                  inputMode: 'numeric',
                })}
                onChange={cambiarNumero}
                required
                autoFocus={!editando}
                sx={{ flex: '2 1 0' }}
              />
              {conNit && (
                <TextField
                  {...campo('digitoVerificacion', 'dv', { maxLength: 1, inputMode: 'numeric' })}
                  required
                  helperText={errorDe('digitoVerificacion') ?? t('formulario.ayudaDigito')}
                  sx={{ flex: '1 1 0', minWidth: 110 }}
                />
              )}
            </Stack>
            {natural ? (
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  {...campo('nombre', 'nombre', { maxLength: LIMITES_TERCERO.nombreMax })}
                  required
                />
                <TextField
                  {...campo('apellido', 'apellido', { maxLength: LIMITES_TERCERO.nombreMax })}
                  required
                />
              </Stack>
            ) : (
              <TextField
                {...campo('razonSocial', 'razon-social', {
                  maxLength: LIMITES_TERCERO.razonSocialMax,
                })}
                required
              />
            )}
            <TextField
              {...campo('direccion', 'direccion', { maxLength: LIMITES_TERCERO.direccionMax })}
              required
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                {...campo('telefono', 'telefono', {
                  maxLength: LIMITES_TERCERO.telefonoMax,
                  inputMode: 'tel',
                })}
                required
              />
              <TextField
                {...campo('correo', 'correo', { maxLength: LIMITES_TERCERO.correoMax })}
                type="email"
              />
            </Stack>
            <FormControl
              component="fieldset"
              error={Boolean(errorRol)}
              disabled={soloLectura || formik.isSubmitting}
            >
              <FormLabel component="legend">{t('formulario.roles')}</FormLabel>
              <FormGroup row>
                {[
                  ['esCliente', 'es-cliente', permisos.cliente],
                  ['esProveedor', 'es-proveedor', permisos.proveedor],
                ].map(([nombre, testId, permitido]) => (
                  <FormControlLabel
                    key={nombre}
                    label={t(`formulario.${nombre}`)}
                    data-testid={`tercero-opcion-${testId}`}
                    control={
                      <Checkbox
                        name={nombre}
                        checked={values[nombre]}
                        onChange={(evento) => {
                          formik.setFieldValue(nombre, evento.target.checked);
                          formik.setFieldTouched('esCliente', true, false);
                        }}
                        disabled={!permitido || soloLectura || formik.isSubmitting}
                        slotProps={{ input: { 'data-testid': `tercero-campo-${testId}` } }}
                      />
                    }
                  />
                ))}
              </FormGroup>
              {errorRol && (
                <FormHelperText data-testid="tercero-error-roles">{errorRol}</FormHelperText>
              )}
              {avisosRol.map((aviso) => (
                <FormHelperText key={aviso} error={false} data-testid="tercero-aviso-rol">
                  {aviso}
                </FormHelperText>
              ))}
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onCerrar} disabled={formik.isSubmitting} data-testid="tercero-cancelar">
            {soloLectura ? t('acciones.cerrar') : t('comun:acciones.cancelar')}
          </Button>
          {!soloLectura && (
            <Button
              type="submit"
              variant="contained"
              disabled={formik.isSubmitting}
              data-testid="tercero-guardar"
            >
              {t('acciones.guardar')}
            </Button>
          )}
        </DialogActions>
      </form>
    </Dialog>
  );
}
