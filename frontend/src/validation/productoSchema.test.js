/**
 * Nombre: productoSchema.test.js
 * Descripción: Pruebas de cada regla del esquema Yup del producto, que replica ProductoPeticion y
 *              GarantiaConPlazoValidator del backend (RF-PRO-001, RNF-INT-002), y de la conversión
 *              de los valores del formulario en la petición.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { describe, expect, it } from 'vitest';

import productoSchema, { aPeticion, valoresIniciales } from './productoSchema.js';

/** Valores de formulario válidos (como los deja el formulario: números en texto). */
const VALIDO = {
  nombre: 'Martillo de uña 16 oz',
  descripcion: 'Mango de fibra de vidrio',
  categoriaId: 1,
  marcaId: 2,
  unidadBase: 'UND',
  tarifaIvaId: 3,
  porcentajeIncremento: '30',
  control: 'NINGUNO',
  plazoGarantiaMeses: '',
  estado: '',
};

/**
 * Valida los valores con cambios y devuelve los errores por campo (vacío si es válido).
 *
 * @param {object} cambios campos a sobrescribir sobre {@link VALIDO}
 * @returns {Promise<Record<string, unknown>>} primer error de cada campo
 */
async function errores(cambios = {}) {
  try {
    await productoSchema.validate({ ...VALIDO, ...cambios }, { abortEarly: false });
    return {};
  } catch (e) {
    const porCampo = {};
    for (const error of e.inner) {
      if (!(error.path in porCampo)) porCampo[error.path] = error.message;
    }
    return porCampo;
  }
}

describe('productoSchema', () => {
  it('[CA-RF-PRO-001-1] un producto válido no tiene errores', async () => {
    expect(await errores()).toEqual({});
  });

  describe('[CA-RF-PRO-001-2] campos obligatorios y garantía', () => {
    it('[CA-RF-PRO-001-2] rechaza un producto sin marca', async () => {
      expect(await errores({ marcaId: '' })).toEqual({ marcaId: 'validacion:requerido' });
    });

    it('[CA-RF-PRO-001-2] rechaza un producto sin unidad base', async () => {
      expect(await errores({ unidadBase: '' })).toEqual({ unidadBase: 'validacion:requerido' });
    });

    it('[CA-RF-PRO-001-2] rechaza un producto sin tarifa de IVA', async () => {
      expect(await errores({ tarifaIvaId: '' })).toEqual({ tarifaIvaId: 'validacion:requerido' });
    });

    it('[CA-RF-PRO-001-2] rechaza garantía sin plazo', async () => {
      expect(await errores({ control: 'GARANTIA', plazoGarantiaMeses: '' })).toEqual({
        plazoGarantiaMeses: 'validacion:requerido',
      });
    });

    it('[CA-RF-PRO-001-2] acepta garantía con plazo de 12 meses', async () => {
      expect(await errores({ control: 'GARANTIA', plazoGarantiaMeses: '12' })).toEqual({});
    });
  });

  describe('[CA-RNF-INT-002-1] cada regla informa campo y causa', () => {
    it('[CA-RNF-INT-002-1] nombre obligatorio', async () => {
      expect(await errores({ nombre: '' })).toEqual({ nombre: 'validacion:requerido' });
    });

    it('[CA-RNF-INT-002-1] un nombre de solo espacios cuenta como vacío (@NotBlank)', async () => {
      expect(await errores({ nombre: '     ' })).toEqual({ nombre: 'validacion:requerido' });
    });

    it('[CA-RNF-INT-002-1] nombre de menos de 3 caracteres', async () => {
      expect((await errores({ nombre: 'ab' })).nombre).toEqual({
        clave: 'validacion:minimo',
        valores: expect.objectContaining({ min: 3 }),
      });
    });

    it('[CA-RNF-INT-002-1] nombre de 3 y de 150 caracteres son válidos', async () => {
      expect(await errores({ nombre: 'abc' })).toEqual({});
      expect(await errores({ nombre: 'a'.repeat(150) })).toEqual({});
    });

    it('[CA-RNF-INT-002-1] nombre de más de 150 caracteres', async () => {
      expect((await errores({ nombre: 'a'.repeat(151) })).nombre).toEqual({
        clave: 'validacion:maximo',
        valores: expect.objectContaining({ max: 150 }),
      });
    });

    it('[CA-RNF-INT-002-1] descripción opcional de hasta 500 caracteres', async () => {
      expect(await errores({ descripcion: '' })).toEqual({});
      expect(await errores({ descripcion: 'x'.repeat(500) })).toEqual({});
      expect((await errores({ descripcion: 'x'.repeat(501) })).descripcion).toEqual({
        clave: 'validacion:maximo',
        valores: expect.objectContaining({ max: 500 }),
      });
    });

    it('[CA-RNF-INT-002-1] categoría obligatoria', async () => {
      expect(await errores({ categoriaId: '' })).toEqual({ categoriaId: 'validacion:requerido' });
    });

    it('[CA-RNF-INT-002-1] unidad base de más de 4 caracteres', async () => {
      expect((await errores({ unidadBase: 'UNIDA' })).unidadBase).toEqual({
        clave: 'validacion:maximo',
        valores: expect.objectContaining({ max: 4 }),
      });
    });

    it('[CA-RNF-INT-002-1] porcentaje obligatorio', async () => {
      expect(await errores({ porcentajeIncremento: '' })).toEqual({
        porcentajeIncremento: 'validacion:requerido',
      });
    });

    it('[CA-RNF-INT-002-1] porcentaje no numérico', async () => {
      expect(await errores({ porcentajeIncremento: 'abc' })).toEqual({
        porcentajeIncremento: 'validacion:numero',
      });
    });

    it('[CA-RNF-INT-002-1] porcentaje entre 0 y 500, ambos incluidos', async () => {
      const rango = { clave: 'validacion:rango', valores: { min: 0, max: 500 } };
      expect(await errores({ porcentajeIncremento: '0' })).toEqual({});
      expect(await errores({ porcentajeIncremento: '500' })).toEqual({});
      expect(await errores({ porcentajeIncremento: '-0.01' })).toEqual({
        porcentajeIncremento: rango,
      });
      expect(await errores({ porcentajeIncremento: '500.01' })).toEqual({
        porcentajeIncremento: rango,
      });
    });

    it('[CA-RNF-INT-002-1] porcentaje con hasta 2 decimales (@Digits)', async () => {
      expect(await errores({ porcentajeIncremento: '30.25' })).toEqual({});
      expect(await errores({ porcentajeIncremento: '30.255' })).toEqual({
        porcentajeIncremento: { clave: 'validacion:decimales', valores: { max: 2 } },
      });
    });

    it('[CA-RNF-INT-002-1] control obligatorio y de la lista', async () => {
      expect(await errores({ control: '' })).toEqual({ control: 'validacion:requerido' });
      expect(await errores({ control: 'OTRO' })).toEqual({
        control: 'validacion:opcionInvalida',
      });
    });

    it('[CA-RNF-INT-002-1] plazo de garantía entero entre 1 y 32767', async () => {
      const rango = { clave: 'validacion:rango', valores: { min: 1, max: 32767 } };
      const garantia = (plazo) => errores({ control: 'GARANTIA', plazoGarantiaMeses: plazo });
      expect(await garantia('1')).toEqual({});
      expect(await garantia('32767')).toEqual({});
      expect(await garantia('0')).toEqual({ plazoGarantiaMeses: rango });
      expect(await garantia('32768')).toEqual({ plazoGarantiaMeses: rango });
      expect(await garantia('1.5')).toEqual({ plazoGarantiaMeses: 'validacion:entero' });
    });

    it('[CA-RNF-INT-002-1] estado solo admite ACTIVO o INACTIVO', async () => {
      expect(await errores({ estado: 'ACTIVO' })).toEqual({});
      expect(await errores({ estado: 'BORRADO' })).toEqual({
        estado: 'validacion:opcionInvalida',
      });
    });
  });

  it('el plazo se ignora y se descarta si el control no es GARANTIA', async () => {
    expect(await errores({ control: 'VENCIMIENTO', plazoGarantiaMeses: '12' })).toEqual({});
    const casteado = productoSchema.cast({
      ...VALIDO,
      control: 'VENCIMIENTO',
      plazoGarantiaMeses: '12',
    });
    expect(casteado).not.toHaveProperty('plazoGarantiaMeses');
  });
});

describe('aPeticion', () => {
  it('convierte los textos del formulario en la petición de la API', () => {
    expect(aPeticion(VALIDO)).toEqual({
      nombre: 'Martillo de uña 16 oz',
      descripcion: 'Mango de fibra de vidrio',
      categoriaId: 1,
      marcaId: 2,
      unidadBase: 'UND',
      tarifaIvaId: 3,
      porcentajeIncremento: 30,
      control: 'NINGUNO',
      plazoGarantiaMeses: null,
    });
  });

  it('envía el plazo solo con GARANTIA, la descripción vacía como null y el estado si viene', () => {
    const peticion = aPeticion({
      ...VALIDO,
      descripcion: '   ',
      control: 'GARANTIA',
      plazoGarantiaMeses: '12',
      estado: 'INACTIVO',
    });
    expect(peticion).toMatchObject({
      descripcion: null,
      plazoGarantiaMeses: 12,
      estado: 'INACTIVO',
    });
  });
});

describe('valoresIniciales', () => {
  it('sin producto deja los campos vacíos y el control en NINGUNO', () => {
    expect(valoresIniciales()).toMatchObject({ nombre: '', marcaId: '', control: 'NINGUNO' });
  });

  it('toma los ids y códigos del producto de la API', () => {
    expect(
      valoresIniciales({
        nombre: 'Bombillo LED',
        descripcion: null,
        categoria: { id: 4 },
        marca: { id: 5 },
        unidadBase: { codigo: 'UND' },
        tarifaIva: { id: 3 },
        porcentajeIncremento: 40,
        control: 'GARANTIA',
        plazoGarantiaMeses: 12,
        estado: 'ACTIVO',
      }),
    ).toEqual({
      nombre: 'Bombillo LED',
      descripcion: '',
      categoriaId: 4,
      marcaId: 5,
      unidadBase: 'UND',
      tarifaIvaId: 3,
      porcentajeIncremento: 40,
      control: 'GARANTIA',
      plazoGarantiaMeses: 12,
      estado: 'ACTIVO',
    });
  });
});
