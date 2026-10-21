/**
 * Nombre: formato.test.js
 * Descripción: Pruebas de los formatos colombianos de moneda y fecha (RNF-INT-003).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { describe, expect, it } from 'vitest';

import { formatearFecha, formatearFechaHora, formatearMoneda } from './formato.js';

/**
 * Cambia los espacios duros que Intl usa entre símbolo y número (o cualquier otro espacio) por espacios normales.
 *
 * @param {string} texto texto formateado
 * @returns {string} texto con espacios normales
 */
const normalizar = (texto) => texto.replace(/\s/g, ' ');

describe('formatearMoneda', () => {
  it('[RNF-INT-003] muestra pesos sin decimales con punto de miles: $ 12.350', () => {
    expect(normalizar(formatearMoneda(12350))).toBe('$ 12.350');
    expect(normalizar(formatearMoneda(1234567))).toBe('$ 1.234.567');
    expect(normalizar(formatearMoneda('15470'))).toBe('$ 15.470');
  });

  it('redondea al peso un costo unitario con decimales', () => {
    expect(normalizar(formatearMoneda(833.3333))).toBe('$ 833');
  });

  it('devuelve cadena vacía si no hay valor numérico', () => {
    expect(formatearMoneda(null)).toBe('');
    expect(formatearMoneda(undefined)).toBe('');
    expect(formatearMoneda('abc')).toBe('');
  });
});

describe('formatearFecha', () => {
  it('[RNF-INT-003] muestra dd/mm/aaaa en la hora de Bogotá', () => {
    // 03:00 UTC del 20 de octubre son las 22:00 del 19 en Colombia (UTC-5).
    expect(formatearFecha('2026-10-20T03:00:00Z')).toBe('19/10/2026');
    expect(formatearFecha('2026-10-20T15:30:00Z')).toBe('20/10/2026');
  });

  it('una fecha simple AAAA-MM-DD no cambia de día', () => {
    expect(formatearFecha('2026-10-05')).toBe('05/10/2026');
  });

  it('devuelve cadena vacía con una fecha inválida', () => {
    expect(formatearFecha('no es fecha')).toBe('');
    expect(formatearFecha(null)).toBe('');
  });
});

describe('formatearFechaHora', () => {
  it('[RNF-INT-003] muestra dd/mm/aaaa hh:mm en 24 horas, hora de Bogotá', () => {
    expect(formatearFechaHora('2026-10-20T15:30:00Z')).toBe('20/10/2026 10:30');
    expect(formatearFechaHora('2026-10-21T04:05:00Z')).toBe('20/10/2026 23:05');
  });
});
