/**
 * Nombre: nit.test.js
 * Descripción: Pruebas del cálculo del dígito de verificación del NIT con NIT reales.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { describe, expect, it } from 'vitest';

import { calcularDigitoVerificacion, esDigitoValido } from './nit.js';

describe('nit', () => {
  it.each([
    ['890903938', 8],
    ['899999068', 1],
    ['800197268', 4],
    ['900123456', 8],
    ['901234567', 7],
  ])('[CA-RF-TER-001-3] calcula el dígito de verificación de %s (%i)', (nit, esperado) => {
    expect(calcularDigitoVerificacion(nit)).toBe(esperado);
    expect(esDigitoValido(nit, esperado)).toBe(true);
    expect(esDigitoValido(nit, String(esperado))).toBe(true);
  });

  it('[CA-RF-TER-001-3] rechaza un dígito distinto del calculado', () => {
    expect(esDigitoValido('890903938', 7)).toBe(false);
    expect(esDigitoValido('890903938', '')).toBe(false);
    expect(esDigitoValido('890903938', null)).toBe(false);
  });

  it('usa el residuo como dígito cuando vale 0 o 1', () => {
    expect(calcularDigitoVerificacion('0')).toBe(0);
    expect(calcularDigitoVerificacion('4')).toBe(1);
    expect(calcularDigitoVerificacion('1')).toBe(8);
  });

  it.each([undefined, null, '', '89090393A', '890.903.938', '1234567890123456'])(
    'no calcula dígito para «%s»',
    (entrada) => {
      expect(calcularDigitoVerificacion(entrada)).toBeNull();
      expect(esDigitoValido(entrada, 0)).toBe(false);
    },
  );
});
