/**
 * Nombre: LanguageSelector.test.jsx
 * Descripción: Pruebas del cambio de idioma: español por defecto, traducción de la interfaz,
 *              de los mensajes de validación y de MUI, y elección recordada (RNF-INT-003).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CLAVE_IDIOMA } from '../i18n/index.js';
import { obtenerUsuarioActual } from '../services/authService.js';
import { listarProductos } from '../services/productosService.js';
import { renderizarApp } from '../test/renderizar.jsx';
import { guardarSesion } from '../utils/sesionStorage.js';

vi.mock('../services/authService.js', () => ({
  iniciarSesion: vi.fn(),
  obtenerUsuarioActual: vi.fn(),
}));
vi.mock('../services/productosService.js', () => ({ listarProductos: vi.fn() }));
// ProductosPage carga las categorías del filtro al montarse.
vi.mock('../services/catalogosService.js', () => ({ listarCategorias: () => Promise.resolve([]) }));

/**
 * Abre el selector y elige un idioma.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario usuario simulado
 * @param {string} nombreBoton nombre accesible del selector en el idioma actual
 * @param {string} idioma nombre del idioma en el menú
 */
async function elegirIdioma(usuario, nombreBoton, idioma) {
  await usuario.click(screen.getByRole('button', { name: nombreBoton }));
  await usuario.click(await screen.findByRole('menuitem', { name: idioma }));
}

describe('LanguageSelector', () => {
  beforeEach(() => {
    vi.mocked(obtenerUsuarioActual).mockReset();
    vi.mocked(listarProductos).mockReset();
  });

  it('[RNF-INT-003] la interfaz arranca en español', () => {
    renderizarApp('/login');

    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(document.documentElement.lang).toBe('es');
  });

  it('[RNF-INT-003] al elegir inglés traduce la interfaz y los mensajes de validación, y recuerda la elección', async () => {
    const { usuario } = renderizarApp('/login');

    await elegirIdioma(usuario, 'Cambiar idioma', 'English');

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(localStorage.getItem(CLAVE_IDIOMA)).toBe('en');
    expect(document.documentElement.lang).toBe('en');

    await usuario.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(screen.getByLabelText('Username')).toHaveAccessibleDescription(
      'This field is required.',
    );

    // Volver a español retraduce también el error ya mostrado.
    await elegirIdioma(usuario, 'Change language', 'Español');
    expect(screen.getByLabelText('Usuario')).toHaveAccessibleDescription(
      'Este campo es obligatorio.',
    );
    expect(localStorage.getItem(CLAVE_IDIOMA)).toBe('es');
  });

  it('[RNF-INT-003] los textos propios de MUI siguen al idioma elegido', async () => {
    guardarSesion('token-vendedor', 1800);
    vi.mocked(obtenerUsuarioActual).mockResolvedValue({
      id: 5,
      nombreCompleto: 'Valeria Vendedora',
      roles: ['VENDEDOR'],
      permisos: ['CATALOGO_CONSULTAR'],
    });
    vi.mocked(listarProductos).mockResolvedValue({ contenido: [], totalElementos: 0 });
    const { usuario } = renderizarApp('/productos');

    expect(await screen.findByText('Filas por página:')).toBeInTheDocument();

    await elegirIdioma(usuario, 'Cambiar idioma', 'English');

    expect(await screen.findByText('Rows per page:')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Products' })).toBeInTheDocument();
  });
});
