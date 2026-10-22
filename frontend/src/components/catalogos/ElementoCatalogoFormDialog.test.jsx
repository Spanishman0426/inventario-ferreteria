/**
 * Nombre: ElementoCatalogoFormDialog.test.jsx
 * Descripción: Pruebas del formulario de marca o categoría: mensajes de validación, petición al crear y
 *              al editar, y errores del servidor (409 y 400 bajo el nombre, otros como notificación)
 *              (RF-CFG-001, RNF-INT-002).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ProveedoresApp from '../../ProveedoresApp.jsx';
import ApiError from '../../services/ApiError.js';
import {
  actualizarElementoCatalogo,
  crearElementoCatalogo,
} from '../../services/catalogosService.js';
import ElementoCatalogoFormDialog from './ElementoCatalogoFormDialog.jsx';

vi.mock('../../services/catalogosService.js', () => ({
  crearElementoCatalogo: vi.fn(),
  actualizarElementoCatalogo: vi.fn(),
}));

/**
 * Monta el diálogo con los proveedores de tema y notificaciones.
 *
 * @param {object} [props] propiedades del diálogo
 * @returns {{usuario: ReturnType<typeof userEvent.setup>, onGuardado: import('vitest').Mock, onCerrar: import('vitest').Mock}} usuario simulado y callbacks
 */
function montar(props = {}) {
  const usuario = userEvent.setup({ delay: null });
  const onGuardado = vi.fn();
  const onCerrar = vi.fn();
  render(
    <ProveedoresApp>
      <ElementoCatalogoFormDialog
        tipo="marcas"
        abierto
        onGuardado={onGuardado}
        onCerrar={onCerrar}
        {...props}
      />
    </ProveedoresApp>,
  );
  return { usuario, onGuardado, onCerrar };
}

describe('ElementoCatalogoFormDialog', () => {
  beforeEach(() => {
    vi.mocked(crearElementoCatalogo).mockReset();
    vi.mocked(actualizarElementoCatalogo).mockReset();
  });

  it('[CA-RNF-INT-002-1] enviar vacío o con 1 carácter muestra la causa bajo el nombre y no envía', async () => {
    const { usuario } = montar();

    await usuario.click(screen.getByTestId('catalogo-guardar'));
    expect(await screen.findByTestId('catalogo-error-nombre')).toHaveTextContent(
      'Este campo es obligatorio.',
    );

    await usuario.type(screen.getByTestId('catalogo-campo-nombre'), 'B');
    await usuario.click(screen.getByTestId('catalogo-guardar'));
    expect(await screen.findByTestId('catalogo-error-nombre')).toHaveTextContent(
      'Mínimo 2 caracteres.',
    );
    expect(crearElementoCatalogo).not.toHaveBeenCalled();
  });

  it('el campo limita la escritura a 100 caracteres', () => {
    montar();
    expect(screen.getByTestId('catalogo-campo-nombre')).toHaveAttribute('maxLength', '100');
  });

  it('crear envía el nombre recortado y notifica', async () => {
    vi.mocked(crearElementoCatalogo).mockResolvedValue({
      id: 30,
      nombre: 'Bosch',
      estado: 'ACTIVO',
    });
    const { usuario, onGuardado } = montar();
    expect(screen.getByRole('heading', { name: 'Nueva marca' })).toBeInTheDocument();

    await usuario.type(screen.getByTestId('catalogo-campo-nombre'), '  Bosch  ');
    await usuario.click(screen.getByTestId('catalogo-guardar'));

    await waitFor(() => expect(onGuardado).toHaveBeenCalled());
    expect(crearElementoCatalogo).toHaveBeenCalledWith('marcas', { nombre: 'Bosch' });
    expect(await screen.findByText('Marca «Bosch» registrada.')).toBeVisible();
  });

  it('editar carga el nombre y llama a actualizar con el id', async () => {
    vi.mocked(actualizarElementoCatalogo).mockResolvedValue({
      id: 4,
      nombre: 'Plomería y gas',
      estado: 'ACTIVO',
    });
    const { usuario, onGuardado } = montar({
      tipo: 'categorias',
      elemento: { id: 4, nombre: 'Plomería', estado: 'ACTIVO' },
    });
    expect(screen.getByRole('heading', { name: 'Editar categoría' })).toBeInTheDocument();
    const campo = screen.getByTestId('catalogo-campo-nombre');
    expect(campo).toHaveValue('Plomería');

    await usuario.type(campo, ' y gas');
    await usuario.click(screen.getByTestId('catalogo-guardar'));

    await waitFor(() => expect(onGuardado).toHaveBeenCalled());
    expect(actualizarElementoCatalogo).toHaveBeenCalledWith('categorias', 4, {
      nombre: 'Plomería y gas',
    });
    expect(crearElementoCatalogo).not.toHaveBeenCalled();
  });

  it('un 409 por duplicado muestra la causa del servidor bajo el nombre', async () => {
    vi.mocked(crearElementoCatalogo).mockRejectedValue(
      new ApiError({
        status: 409,
        codigo: 'CONFLICTO_UNICIDAD',
        detalle: 'Ya existe la marca «Bosch».',
      }),
    );
    const { usuario, onGuardado } = montar();

    await usuario.type(screen.getByTestId('catalogo-campo-nombre'), 'bosch');
    await usuario.click(screen.getByTestId('catalogo-guardar'));

    expect(await screen.findByTestId('catalogo-error-nombre')).toHaveTextContent(
      'Ya existe la marca «Bosch».',
    );
    expect(onGuardado).not.toHaveBeenCalled();
  });

  it('[CA-RNF-INT-002-1] un 400 del servidor muestra la causa bajo el nombre', async () => {
    vi.mocked(crearElementoCatalogo).mockRejectedValue(
      new ApiError({
        status: 400,
        codigo: 'VALIDACION',
        errores: [{ campo: 'nombre', mensaje: 'debe tener entre 2 y 100 caracteres' }],
      }),
    );
    const { usuario } = montar();

    await usuario.type(screen.getByTestId('catalogo-campo-nombre'), 'Bosch');
    await usuario.click(screen.getByTestId('catalogo-guardar'));

    expect(await screen.findByTestId('catalogo-error-nombre')).toHaveTextContent(
      'debe tener entre 2 y 100 caracteres',
    );
  });

  it('cancelar cierra sin enviar', async () => {
    const { usuario, onCerrar } = montar();

    await usuario.click(screen.getByTestId('catalogo-cancelar'));

    expect(onCerrar).toHaveBeenCalled();
    expect(crearElementoCatalogo).not.toHaveBeenCalled();
  });
});
