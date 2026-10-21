/**
 * Nombre: ProductoFormDialog.test.jsx
 * Descripción: Pruebas del formulario de producto: campo de garantía condicional, mensajes por
 *              campo, envío al crear y editar, y errores del servidor (409 bajo el nombre, 400 por
 *              campo, otros como notificación) (RF-PRO-001, RNF-INT-002).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ProveedoresApp from '../../ProveedoresApp.jsx';
import ApiError from '../../services/ApiError.js';
import {
  listarCategorias,
  listarMarcas,
  listarTarifasIva,
  listarUnidadesMedida,
} from '../../services/catalogosService.js';
import { actualizarProducto, crearProducto } from '../../services/productosService.js';
import ProductoFormDialog from './ProductoFormDialog.jsx';

vi.mock('../../services/catalogosService.js', () => ({
  listarCategorias: vi.fn(),
  listarMarcas: vi.fn(),
  listarUnidadesMedida: vi.fn(),
  listarTarifasIva: vi.fn(),
}));
vi.mock('../../services/productosService.js', () => ({
  crearProducto: vi.fn(),
  actualizarProducto: vi.fn(),
}));

/** Producto existente devuelto por la API, para editar. */
const BOMBILLO = {
  id: 9,
  nombre: 'Bombillo LED 9 W',
  descripcion: 'Luz fría',
  estado: 'ACTIVO',
  categoria: { id: 2, nombre: 'Eléctricos' },
  marca: { id: 20, nombre: 'Philips' },
  unidadBase: { codigo: 'UND', nombre: 'Unidad', esDiscreta: true },
  tarifaIva: { id: 3, nombre: 'IVA 19 %', porcentaje: 19 },
  porcentajeIncremento: 40,
  control: 'GARANTIA',
  plazoGarantiaMeses: 12,
};

/**
 * Monta el diálogo con los proveedores de tema y notificaciones.
 *
 * @param {object} [props] propiedades del diálogo
 * @returns {{usuario: ReturnType<typeof userEvent.setup>, onGuardado: import('vitest').Mock, onCerrar: import('vitest').Mock}} usuario simulado y callbacks
 */
function montar(props = {}) {
  // Sin espera entre teclas: los selectores de MUI ya hacen lentas estas pruebas.
  const usuario = userEvent.setup({ delay: null });
  const onGuardado = vi.fn();
  const onCerrar = vi.fn();
  render(
    <ProveedoresApp>
      <ProductoFormDialog abierto onGuardado={onGuardado} onCerrar={onCerrar} {...props} />
    </ProveedoresApp>,
  );
  return { usuario, onGuardado, onCerrar };
}

/**
 * Abre un selector por su data-testid y elige la opción con el texto indicado.
 *
 * @param {ReturnType<typeof userEvent.setup>} usuario usuario simulado
 * @param {string} campo sufijo del data-testid (marca, categoria, …)
 * @param {string|RegExp} opcion texto de la opción
 */
async function elegir(usuario, campo, opcion) {
  await usuario.click(screen.getByTestId(`producto-campo-${campo}`));
  const lista = await screen.findByRole('listbox');
  await usuario.click(within(lista).getByRole('option', { name: opcion }));
}

/**
 * Llena el formulario con un producto válido sin garantía.
 *
 * @param {ReturnType<typeof userEvent.setup>} usuario usuario simulado
 */
async function llenarValido(usuario) {
  await usuario.type(screen.getByTestId('producto-campo-nombre'), 'Martillo de uña 16 oz');
  await elegir(usuario, 'categoria', 'Herramientas');
  await elegir(usuario, 'marca', 'Stanley');
  await elegir(usuario, 'unidad', /^UND/);
  await elegir(usuario, 'iva', 'IVA 19 %');
  await usuario.type(screen.getByTestId('producto-campo-porcentaje'), '30');
}

describe('ProductoFormDialog', () => {
  beforeEach(() => {
    vi.mocked(listarCategorias).mockResolvedValue([
      { id: 1, nombre: 'Herramientas' },
      { id: 2, nombre: 'Eléctricos' },
    ]);
    vi.mocked(listarMarcas).mockResolvedValue([{ id: 10, nombre: 'Stanley' }]);
    vi.mocked(listarUnidadesMedida).mockResolvedValue([
      { codigo: 'UND', nombre: 'Unidad', esDiscreta: true },
      { codigo: 'KG', nombre: 'Kilogramo', esDiscreta: false },
    ]);
    vi.mocked(listarTarifasIva).mockResolvedValue([
      { id: 1, nombre: 'Exento', porcentaje: 0 },
      { id: 3, nombre: 'IVA 19 %', porcentaje: 19 },
    ]);
    vi.mocked(crearProducto).mockReset();
    vi.mocked(actualizarProducto).mockReset();
  });

  it('llena los selectores con los endpoints de catálogo', async () => {
    const { usuario } = montar();

    await usuario.click(screen.getByTestId('producto-campo-categoria'));
    const lista = await screen.findByRole('listbox');

    expect(
      within(lista)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Herramientas', 'Eléctricos']);
    expect(listarMarcas).toHaveBeenCalled();
    expect(listarUnidadesMedida).toHaveBeenCalled();
    expect(listarTarifasIva).toHaveBeenCalled();
  });

  it('[CA-RF-PRO-001-2] el plazo de garantía aparece solo con control GARANTIA', async () => {
    const { usuario } = montar();

    expect(screen.queryByTestId('producto-campo-plazo')).not.toBeInTheDocument();

    await elegir(usuario, 'control', 'Garantía');
    expect(screen.getByTestId('producto-campo-plazo')).toBeInTheDocument();

    await elegir(usuario, 'control', 'Vencimiento');
    expect(screen.queryByTestId('producto-campo-plazo')).not.toBeInTheDocument();
  });

  it('[CA-RF-PRO-001-2] rechaza garantía sin plazo y no envía', async () => {
    const { usuario } = montar();
    await llenarValido(usuario);
    await elegir(usuario, 'control', 'Garantía');

    await usuario.click(screen.getByTestId('producto-guardar'));

    expect(await screen.findByTestId('producto-error-plazo')).toHaveTextContent(
      'Este campo es obligatorio.',
    );
    expect(crearProducto).not.toHaveBeenCalled();
  });

  it('[CA-RNF-INT-002-1] enviar vacío muestra la causa en cada campo obligatorio', async () => {
    const { usuario } = montar();

    await usuario.click(screen.getByTestId('producto-guardar'));

    for (const campo of ['nombre', 'categoria', 'marca', 'unidad', 'iva', 'porcentaje']) {
      expect(await screen.findByTestId(`producto-error-${campo}`)).toHaveTextContent(
        'Este campo es obligatorio.',
      );
    }
    expect(screen.getByTestId('producto-campo-nombre')).toHaveAttribute('aria-invalid', 'true');
    expect(crearProducto).not.toHaveBeenCalled();
  });

  it('[CA-RNF-INT-002-1] muestra el límite incumplido del nombre y del porcentaje', async () => {
    const { usuario } = montar();

    await usuario.type(screen.getByTestId('producto-campo-nombre'), 'ab');
    await usuario.type(screen.getByTestId('producto-campo-porcentaje'), '600');
    await usuario.click(screen.getByTestId('producto-guardar'));

    expect(await screen.findByTestId('producto-error-nombre')).toHaveTextContent(
      'Mínimo 3 caracteres.',
    );
    expect(screen.getByTestId('producto-error-porcentaje')).toHaveTextContent(
      'Debe estar entre 0 y 500.',
    );
  });

  it('[CA-RF-PRO-001-1] un producto válido se envía con la petición exacta', async () => {
    const creado = { id: 30, nombre: 'Martillo de uña 16 oz' };
    vi.mocked(crearProducto).mockResolvedValue(creado);
    const { usuario, onGuardado } = montar();
    await llenarValido(usuario);

    await usuario.click(screen.getByTestId('producto-guardar'));

    await waitFor(() => expect(onGuardado).toHaveBeenCalledWith(creado));
    expect(crearProducto).toHaveBeenCalledWith({
      nombre: 'Martillo de uña 16 oz',
      descripcion: null,
      categoriaId: 1,
      marcaId: 10,
      unidadBase: 'UND',
      tarifaIvaId: 3,
      porcentajeIncremento: 30,
      control: 'NINGUNO',
      plazoGarantiaMeses: null,
    });
    expect(await screen.findByText('Producto «Martillo de uña 16 oz» registrado.')).toBeVisible();
  });

  it('[CA-RF-PRO-001-1] al editar carga los datos y llama a actualizar', async () => {
    vi.mocked(actualizarProducto).mockResolvedValue({ ...BOMBILLO, plazoGarantiaMeses: 24 });
    const { usuario, onGuardado } = montar({ producto: BOMBILLO });

    expect(screen.getByRole('heading', { name: 'Editar producto' })).toBeInTheDocument();
    expect(screen.getByTestId('producto-campo-nombre')).toHaveValue('Bombillo LED 9 W');
    // La marca Philips no está entre las activas, pero el selector la muestra.
    expect(await screen.findByTestId('producto-campo-marca')).toHaveTextContent('Philips');
    const plazo = screen.getByTestId('producto-campo-plazo');
    await usuario.clear(plazo);
    await usuario.type(plazo, '24');

    await usuario.click(screen.getByTestId('producto-guardar'));

    await waitFor(() => expect(onGuardado).toHaveBeenCalled());
    expect(actualizarProducto).toHaveBeenCalledWith(9, {
      nombre: 'Bombillo LED 9 W',
      descripcion: 'Luz fría',
      categoriaId: 2,
      marcaId: 20,
      unidadBase: 'UND',
      tarifaIvaId: 3,
      porcentajeIncremento: 40,
      control: 'GARANTIA',
      plazoGarantiaMeses: 24,
      estado: 'ACTIVO',
    });
    expect(crearProducto).not.toHaveBeenCalled();
  });

  it('[CA-RF-PRO-001-3] un 409 por duplicado muestra la causa bajo el nombre', async () => {
    const detalle = 'Ya existe el producto «Martillo de uña 16 oz» para la marca Stanley.';
    vi.mocked(crearProducto).mockRejectedValue(
      new ApiError({ status: 409, codigo: 'CONFLICTO_UNICIDAD', detalle }),
    );
    const { usuario, onGuardado } = montar();
    await llenarValido(usuario);

    await usuario.click(screen.getByTestId('producto-guardar'));

    expect(await screen.findByTestId('producto-error-nombre')).toHaveTextContent(detalle);
    expect(screen.getByTestId('producto-campo-nombre')).toHaveAttribute('aria-invalid', 'true');
    // La notificación queda detrás del diálogo abierto (aria-hidden), por eso hidden: true.
    expect(screen.queryByRole('alert', { hidden: true })).not.toBeInTheDocument();
    expect(onGuardado).not.toHaveBeenCalled();
  });

  it('[CA-RNF-INT-002-1] un 400 del servidor muestra cada error en su campo', async () => {
    vi.mocked(crearProducto).mockRejectedValue(
      new ApiError({
        status: 400,
        codigo: 'VALIDACION',
        errores: [{ campo: 'marcaId', mensaje: 'La marca no existe o está inactiva.' }],
      }),
    );
    const { usuario } = montar();
    await llenarValido(usuario);

    await usuario.click(screen.getByTestId('producto-guardar'));

    expect(await screen.findByTestId('producto-error-marca')).toHaveTextContent(
      'La marca no existe o está inactiva.',
    );
  });

  it('otros errores del servidor se muestran como notificación', async () => {
    vi.mocked(crearProducto).mockRejectedValue(
      new ApiError({ status: 500, codigo: 'ERROR_INTERNO' }),
    );
    const { usuario, onGuardado } = montar();
    await llenarValido(usuario);

    await usuario.click(screen.getByTestId('producto-guardar'));

    expect(await screen.findByRole('alert', { hidden: true })).toHaveTextContent(
      'Ocurrió un error inesperado en el servidor.',
    );
    expect(screen.queryByTestId('producto-error-nombre')).not.toBeInTheDocument();
    expect(onGuardado).not.toHaveBeenCalled();
  });

  it('cancelar cierra sin enviar', async () => {
    const { usuario, onCerrar } = montar();

    await usuario.click(screen.getByTestId('producto-cancelar'));

    expect(onCerrar).toHaveBeenCalled();
    expect(crearProducto).not.toHaveBeenCalled();
  });
});
