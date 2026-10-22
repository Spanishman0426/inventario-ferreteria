/**
 * Nombre: elementoCatalogoSchema.test.js
 * Descripción: Pruebas del esquema Yup de marca o categoría: obligatorio, solo espacios, límites de 2 y
 *              100 caracteres (iguales a ElementoCatalogoPeticion) y petición con el nombre recortado.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { describe, expect, it } from 'vitest';

import elementoCatalogoSchema, {
  LIMITES_CATALOGO,
  aPeticionCatalogo,
} from './elementoCatalogoSchema.js';

/**
 * Valida un nombre y devuelve el error, o null si es válido.
 *
 * @param {string} nombre nombre a validar
 * @returns {Promise<unknown>} mensaje de error de Yup o null
 */
async function errorDe(nombre) {
  try {
    await elementoCatalogoSchema.validate({ nombre });
    return null;
  } catch (e) {
    return e.errors[0];
  }
}

describe('elementoCatalogoSchema', () => {
  it('usa los mismos límites que el backend (2 a 100)', () => {
    expect(LIMITES_CATALOGO).toEqual({ nombreMin: 2, nombreMax: 100 });
  });

  it('[CA-RNF-INT-002-1] exige el nombre y rechaza uno de solo espacios', async () => {
    expect(await errorDe('')).toBe('validacion:requerido');
    expect(await errorDe('   ')).toBe('validacion:requerido');
  });

  it('[CA-RNF-INT-002-1] rechaza 1 y 101 caracteres con el límite incumplido', async () => {
    expect(await errorDe('B')).toEqual({
      clave: 'validacion:minimo',
      valores: expect.objectContaining({ min: 2 }),
    });
    expect(await errorDe('x'.repeat(101))).toEqual({
      clave: 'validacion:maximo',
      valores: expect.objectContaining({ max: 100 }),
    });
  });

  it('acepta 2 y 100 caracteres', async () => {
    expect(await errorDe('Ab')).toBeNull();
    expect(await errorDe('x'.repeat(100))).toBeNull();
  });

  it('la petición lleva el nombre recortado', () => {
    expect(aPeticionCatalogo({ nombre: '  Bosch  ' })).toEqual({ nombre: 'Bosch' });
  });
});
