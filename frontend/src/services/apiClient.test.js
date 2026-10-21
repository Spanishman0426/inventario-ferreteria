/**
 * Nombre: apiClient.test.js
 * Descripción: Pruebas del cliente Axios: cabeceras Authorization y Accept-Language, conversión de
 *              Problem Details en ApiError y cierre de sesión ante un 401.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { AxiosError } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';

import i18n from '../i18n/index.js';
import { guardarSesion } from '../utils/sesionStorage.js';
import ApiError from './ApiError.js';
import apiClient, { registrarManejadorNoAutorizado } from './apiClient.js';

/**
 * Crea un adaptador de Axios que responde sin red y guarda la configuración enviada.
 *
 * @param {{status: number, data?: unknown}|null} respuesta respuesta simulada; null simula falla de red
 * @returns {{adapter: (config: object) => Promise<object>, enviada: () => object}} adaptador y acceso a la solicitud enviada
 */
function adaptadorSimulado(respuesta) {
  let enviada;
  const adapter = async (config) => {
    enviada = config;
    if (respuesta === null) {
      throw new AxiosError('Network Error', 'ERR_NETWORK', config, {});
    }
    const completa = { ...respuesta, statusText: '', headers: {}, config };
    if (respuesta.status >= 400) {
      throw new AxiosError('fallo', 'ERR_BAD_REQUEST', config, {}, completa);
    }
    return completa;
  };
  return { adapter, enviada: () => enviada };
}

describe('apiClient', () => {
  afterEach(() => {
    registrarManejadorNoAutorizado(null);
  });

  it('envía el token como Bearer y el idioma actual en Accept-Language', async () => {
    guardarSesion('abc123', 60);
    await i18n.changeLanguage('en');
    const { adapter, enviada } = adaptadorSimulado({ status: 200, data: [] });

    await apiClient.get('/api/v1/marcas', { adapter });

    expect(enviada().headers.Authorization).toBe('Bearer abc123');
    expect(enviada().headers['Accept-Language']).toBe('en');
  });

  it('no envía Authorization si no hay sesión', async () => {
    const { adapter, enviada } = adaptadorSimulado({ status: 200, data: [] });

    await apiClient.get('/api/v1/marcas', { adapter });

    expect(enviada().headers.Authorization).toBeUndefined();
    expect(enviada().headers['Accept-Language']).toBe('es');
  });

  it('convierte un Problem Details en un ApiError legible con errores por campo', async () => {
    const { adapter } = adaptadorSimulado({
      status: 400,
      data: {
        codigo: 'VALIDACION',
        title: 'Datos no válidos',
        detail: 'Uno o más campos no cumplen las reglas de validación.',
        errores: [{ campo: 'nombre', mensaje: 'es obligatorio' }],
        status: 400,
      },
    });

    const error = await apiClient.post('/api/v1/productos', {}, { adapter }).catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.codigo).toBe('VALIDACION');
    expect(error.message).toBe('Uno o más campos no cumplen las reglas de validación.');
    expect(error.erroresPorCampo()).toEqual({ nombre: 'es obligatorio' });
  });

  it('sin Problem Details usa el mensaje traducido del código según el estado HTTP', async () => {
    const { adapter } = adaptadorSimulado({ status: 502, data: '<html>Bad Gateway</html>' });

    const error = await apiClient.get('/api/v1/marcas', { adapter }).catch((e) => e);

    expect(error.codigo).toBe('ERROR_INTERNO');
    expect(error.message).toBe('Ocurrió un error inesperado en el servidor.');
  });

  it('una falla de red produce un error de código RED', async () => {
    const { adapter } = adaptadorSimulado(null);

    const error = await apiClient.get('/api/v1/marcas', { adapter }).catch((e) => e);

    expect(error.codigo).toBe('RED');
    expect(error.status).toBe(0);
    expect(error.message).toBe('No fue posible conectarse con el servidor. Revise su conexión.');
  });

  it('un 401 fuera del login ejecuta el cierre de sesión', async () => {
    const manejador = vi.fn();
    registrarManejadorNoAutorizado(manejador);
    const { adapter } = adaptadorSimulado({
      status: 401,
      data: { codigo: 'NO_AUTENTICADO', title: 'No autenticado', status: 401 },
    });

    const error = await apiClient.get('/api/v1/productos', { adapter }).catch((e) => e);

    expect(error.codigo).toBe('NO_AUTENTICADO');
    expect(manejador).toHaveBeenCalledTimes(1);
  });

  it('un 401 del login (credenciales inválidas) no cierra ninguna sesión', async () => {
    const manejador = vi.fn();
    registrarManejadorNoAutorizado(manejador);
    const { adapter } = adaptadorSimulado({
      status: 401,
      data: { codigo: 'CREDENCIALES_INVALIDAS', title: 'Credenciales inválidas', status: 401 },
    });

    const error = await apiClient.post('/api/v1/auth/login', {}, { adapter }).catch((e) => e);

    expect(error.codigo).toBe('CREDENCIALES_INVALIDAS');
    expect(manejador).not.toHaveBeenCalled();
  });
});
