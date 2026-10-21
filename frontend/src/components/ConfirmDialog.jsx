/**
 * Nombre: ConfirmDialog.jsx
 * Descripción: Diálogo de confirmación reutilizable con botones Confirmar y Cancelar traducidos y
 *              data-testid para las pruebas E2E.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useTranslation } from 'react-i18next';

/**
 * Pide al usuario confirmar una acción. Los textos llegan ya traducidos.
 *
 * @param {object} props propiedades del componente
 * @param {boolean} props.abierto si el diálogo está visible
 * @param {string} props.titulo título del diálogo
 * @param {string} [props.mensaje] explicación de la acción
 * @param {string} [props.textoConfirmar] texto del botón de confirmar; por defecto "Confirmar"
 * @param {() => void} props.onConfirmar se ejecuta al confirmar
 * @param {() => void} props.onCancelar se ejecuta al cancelar o cerrar
 * @param {string} [props.testId] prefijo de data-testid: `<prefijo>-dialogo`, `-aceptar`, `-cancelar`
 * @returns {import('react').JSX.Element} diálogo
 */
export default function ConfirmDialog({
  abierto,
  titulo,
  mensaje,
  textoConfirmar,
  onConfirmar,
  onCancelar,
  testId = 'confirmar',
}) {
  const { t } = useTranslation();
  return (
    <Dialog
      open={abierto}
      onClose={onCancelar}
      aria-labelledby="confirmar-titulo"
      data-testid={`${testId}-dialogo`}
    >
      <DialogTitle id="confirmar-titulo">{titulo}</DialogTitle>
      {mensaje && (
        <DialogContent>
          <DialogContentText>{mensaje}</DialogContentText>
        </DialogContent>
      )}
      <DialogActions>
        <Button onClick={onCancelar} data-testid={`${testId}-cancelar`}>
          {t('acciones.cancelar')}
        </Button>
        <Button
          onClick={onConfirmar}
          variant="contained"
          autoFocus
          data-testid={`${testId}-aceptar`}
        >
          {textoConfirmar ?? t('acciones.confirmar')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
