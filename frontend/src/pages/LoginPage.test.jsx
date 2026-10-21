/**
 * Nombre: LoginPage.test.jsx
 * Descripción: Pruebas del inicio de sesión: validación por campo, credenciales inválidas, cuenta
 *              bloqueada y redirección a la ruta pedida (RF-SEG-001, RNF-INT-002).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ApiError from '../services/ApiError.js';
import { iniciarSesion, obtenerUsuarioActual } from '../services/authService.js';
import { listarProductos } from '../services/productosService.js';
import { renderizarApp } from '../test/renderizar.jsx';

vi.mock('../services/authService.js', () => ({
  iniciarSesion: vi.fn(),
  obtenerUsuarioActual: vi.fn(),
}));
vi.mock('../services/productosService.js', () => ({ listarProductos: vi.fn() }));
// ProductosPage carga las categorías del filtro al montarse.
vi.mock('../services/catalogosService.js', () => ({ listarCategorias: () => Promise.resolve([]) }));

/** Usuario vendedor devuelto por el login simulado. */
const VENDEDOR = {
  id: 5,
  nombreCompleto: 'Valeria Vendedora',
  roles: ['VENDEDOR'],
  permisos: ['CATALOGO_CONSULTAR', 'VENTA_REGISTRAR'],
};

/**
 * Escribe usuario y contraseña y envía el formulario.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario usuario simulado
 * @param {string} nombre nombre de usuario
 * @param {string} contrasena contraseña
 */
async function enviarCredenciales(usuario, nombre, contrasena) {
  await usuario.type(screen.getByLabelText('Usuario'), nombre);
  await usuario.type(screen.getByLabelText('Contraseña'), contrasena);
  await usuario.click(screen.getByRole('button', { name: 'Entrar' }));
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.mocked(iniciarSesion).mockReset();
    vi.mocked(obtenerUsuarioActual).mockReset();
    vi.mocked(listarProductos).mockReset();
    vi.mocked(listarProductos).mockResolvedValue({
      contenido: [],
      pagina: 0,
      tamano: 20,
      totalElementos: 0,
      totalPaginas: 0,
    });
  });

  it('[RNF-INT-002] exige usuario y contraseña con un mensaje en cada campo', async () => {
    const { usuario } = renderizarApp('/login');

    await usuario.click(screen.getByRole('button', { name: 'Entrar' }));

    const usuarioInput = screen.getByLabelText('Usuario');
    const contrasenaInput = screen.getByLabelText('Contraseña');
    expect(usuarioInput).toHaveAccessibleDescription('Este campo es obligatorio.');
    expect(contrasenaInput).toHaveAccessibleDescription('Este campo es obligatorio.');
    expect(usuarioInput).toHaveAttribute('aria-invalid', 'true');
    expect(iniciarSesion).not.toHaveBeenCalled();
  });

  it('[RNF-INT-002] un usuario de solo espacios cuenta como vacío', async () => {
    const { usuario } = renderizarApp('/login');

    await enviarCredenciales(usuario, '   ', 'Clave123');

    expect(screen.getByLabelText('Usuario')).toHaveAccessibleDescription(
      'Este campo es obligatorio.',
    );
    expect(iniciarSesion).not.toHaveBeenCalled();
  });

  it('[CA-RF-SEG-001-3] con credenciales inválidas o cuenta inactiva muestra el aviso y no abre sesión', async () => {
    vi.mocked(iniciarSesion).mockRejectedValue(
      new ApiError({
        status: 401,
        codigo: 'CREDENCIALES_INVALIDAS',
        titulo: 'Credenciales inválidas',
      }),
    );
    const { usuario } = renderizarApp('/login');

    await enviarCredenciales(usuario, 'vendedor', 'Incorrecta1');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El usuario o la contraseña no son correctos, o la cuenta está inactiva.',
    );
    expect(iniciarSesion).toHaveBeenCalledWith('vendedor', 'Incorrecta1');
    expect(sessionStorage.getItem('ferreteria.token')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('[CA-RF-SEG-001-1] tras el bloqueo por 5 intentos fallidos avisa que la cuenta está bloqueada', async () => {
    vi.mocked(iniciarSesion).mockRejectedValue(
      new ApiError({
        status: 423,
        codigo: 'CUENTA_BLOQUEADA',
        titulo: 'Cuenta bloqueada',
        detalle: 'La cuenta está bloqueada por intentos fallidos hasta el 10:45.',
      }),
    );
    const { usuario } = renderizarApp('/login');

    await enviarCredenciales(usuario, 'vendedor', 'Incorrecta1');

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Cuenta bloqueada');
    expect(aviso).toHaveTextContent('hasta el 10:45');
    expect(sessionStorage.getItem('ferreteria.token')).toBeNull();
  });

  it('tras iniciar sesión redirige a la ruta pedida y guarda el token en sessionStorage', async () => {
    vi.mocked(iniciarSesion).mockResolvedValue({
      accessToken: 'token-de-prueba',
      expiresIn: 1800,
      usuario: VENDEDOR,
    });
    const { usuario } = renderizarApp('/productos');

    // Sin sesión, /productos lleva al login.
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    await enviarCredenciales(usuario, ' vendedor ', 'Clave123');

    expect(await screen.findByRole('heading', { name: 'Productos' })).toBeInTheDocument();
    expect(screen.getByTestId('usuario-actual')).toHaveTextContent('Valeria Vendedora');
    expect(iniciarSesion).toHaveBeenCalledWith('vendedor', 'Clave123');
    expect(sessionStorage.getItem('ferreteria.token')).toBe('token-de-prueba');
    expect(localStorage.getItem('ferreteria.token')).toBeNull();
    expect(listarProductos).toHaveBeenCalled();
  });
});
