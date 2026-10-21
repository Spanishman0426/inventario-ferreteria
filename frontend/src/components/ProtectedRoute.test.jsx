/**
 * Nombre: ProtectedRoute.test.jsx
 * Descripción: Pruebas de la ruta protegida: sin sesión lleva al login y sin permiso no muestra
 *              la página ni consulta la API (RF-SEG-001).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { obtenerUsuarioActual } from '../services/authService.js';
import { listarProductos } from '../services/productosService.js';
import { renderizarApp } from '../test/renderizar.jsx';
import { guardarSesion } from '../utils/sesionStorage.js';

vi.mock('../services/authService.js', () => ({
  iniciarSesion: vi.fn(),
  obtenerUsuarioActual: vi.fn(),
}));
vi.mock('../services/productosService.js', () => ({ listarProductos: vi.fn() }));

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.mocked(obtenerUsuarioActual).mockReset();
    vi.mocked(listarProductos).mockReset();
  });

  it('sin sesión lleva al login', async () => {
    renderizarApp('/productos');

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(listarProductos).not.toHaveBeenCalled();
  });

  it('[CA-RF-SEG-001-2] un usuario sin el permiso no ve la página ni consulta la API', async () => {
    guardarSesion('token-auditor', 1800);
    vi.mocked(obtenerUsuarioActual).mockResolvedValue({
      id: 7,
      nombreCompleto: 'Usuario sin catálogo',
      roles: [],
      permisos: [],
    });

    renderizarApp('/productos');

    expect(await screen.findByText('Acceso denegado')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Productos' })).not.toBeInTheDocument();
    expect(listarProductos).not.toHaveBeenCalled();
  });

  it('restaura la sesión guardada al recargar y muestra la página permitida', async () => {
    guardarSesion('token-vendedor', 1800);
    vi.mocked(obtenerUsuarioActual).mockResolvedValue({
      id: 5,
      nombreCompleto: 'Valeria Vendedora',
      roles: ['VENDEDOR'],
      permisos: ['CATALOGO_CONSULTAR'],
    });
    vi.mocked(listarProductos).mockResolvedValue({ contenido: [], totalElementos: 0 });

    renderizarApp('/productos');

    expect(await screen.findByRole('heading', { name: 'Productos' })).toBeInTheDocument();
    expect(obtenerUsuarioActual).toHaveBeenCalledTimes(1);
  });
});
