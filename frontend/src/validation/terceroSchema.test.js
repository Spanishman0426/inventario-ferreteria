/**
 * Nombre: terceroSchema.test.js
 * Descripción: Pruebas del esquema Yup del tercero y de la conversión a la petición de la API.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { describe, expect, it } from 'vitest';

import terceroSchema, { aPeticionTercero, valoresInicialesTercero } from './terceroSchema.js';

/** Persona natural válida. */
const NATURAL = {
  ...valoresInicialesTercero(null, { cliente: true, proveedor: false }),
  numeroIdentificacion: '1075234567',
  nombre: 'Juan',
  apellido: 'Pérez',
  direccion: 'Neiva',
  telefono: '3157654321',
};

/** Persona jurídica válida. */
const JURIDICA = {
  ...NATURAL,
  tipoPersona: 'JURIDICA',
  tipoIdentificacion: 'NIT',
  numeroIdentificacion: '800197268',
  digitoVerificacion: '4',
  nombre: '',
  apellido: '',
  razonSocial: 'DIAN',
};

/**
 * Errores de validación por campo.
 *
 * @param {object} valores valores del formulario
 * @returns {Promise<Record<string, unknown>>} mensaje por campo
 */
async function errores(valores) {
  try {
    await terceroSchema.validate(valores, { abortEarly: false });
    return {};
  } catch (e) {
    return Object.fromEntries(e.inner.map((i) => [i.path, i.message]));
  }
}

describe('terceroSchema', () => {
  it('[CA-RF-TER-001-1] acepta una persona natural con cédula y una jurídica con NIT y razón social', async () => {
    expect(await errores(NATURAL)).toEqual({});
    expect(await errores(JURIDICA)).toEqual({});
  });

  it('[CA-RF-TER-001-1] exige nombre y apellido a la natural y razón social y NIT a la jurídica', async () => {
    expect(await errores({ ...NATURAL, nombre: '  ', apellido: '' })).toMatchObject({
      nombre: 'validacion:requerido',
      apellido: 'validacion:requerido',
    });
    expect(await errores({ ...JURIDICA, razonSocial: '', tipoIdentificacion: 'CC' })).toMatchObject(
      {
        razonSocial: 'validacion:requerido',
        tipoIdentificacion: 'validacion:juridicaSoloNit',
      },
    );
  });

  it('[CA-RF-TER-001-3] rechaza un dígito de verificación incorrecto indicando el correcto', async () => {
    expect(await errores({ ...JURIDICA, digitoVerificacion: '5' })).toEqual({
      digitoVerificacion: { clave: 'validacion:digitoInvalido', valores: { esperado: 4 } },
    });
    expect(await errores({ ...JURIDICA, digitoVerificacion: '' })).toEqual({
      digitoVerificacion: 'validacion:requerido',
    });
  });

  it('[CA-RF-TER-001-2] admite los dos roles y exige al menos uno', async () => {
    expect(await errores({ ...NATURAL, esCliente: true, esProveedor: true })).toEqual({});
    expect(await errores({ ...NATURAL, esCliente: false, esProveedor: false })).toEqual({
      esCliente: 'validacion:rolObligatorio',
    });
  });

  it('valida formato de identificación, teléfono y correo', async () => {
    expect(
      await errores({ ...NATURAL, numeroIdentificacion: '12.34', telefono: '12345', correo: 'x@' }),
    ).toEqual({
      numeroIdentificacion: 'validacion:identificacionFormato',
      telefono: 'validacion:telefonoFormato',
      correo: 'validacion:correo',
    });
  });

  it('la petición recorta textos y descarta lo que no aplica al tipo', () => {
    expect(
      aPeticionTercero({ ...JURIDICA, nombre: 'x', razonSocial: '  DIAN ', correo: ' ' }),
    ).toEqual({
      tipoPersona: 'JURIDICA',
      tipoIdentificacion: 'NIT',
      numeroIdentificacion: '800197268',
      digitoVerificacion: 4,
      nombre: null,
      apellido: null,
      razonSocial: 'DIAN',
      direccion: 'Neiva',
      telefono: '3157654321',
      correo: null,
      esCliente: true,
      esProveedor: false,
    });
  });
});
