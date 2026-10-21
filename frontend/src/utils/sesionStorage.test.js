/**
 * Nombre: sesionStorage.test.js
 * Descripción: Pruebas del almacenamiento del token en sessionStorage y de su vencimiento.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { describe, expect, it } from 'vitest';

import { borrarSesion, guardarSesion, leerSesion, obtenerToken } from './sesionStorage.js';

describe('sesionStorage', () => {
  it('guarda el token en sessionStorage y nunca en localStorage', () => {
    guardarSesion('abc', 1800, 1_000);

    expect(leerSesion(2_000)).toEqual({ token: 'abc', expiraEn: 1_801_000 });
    expect(sessionStorage.getItem('ferreteria.token')).toBe('abc');
    expect(localStorage.length).toBe(0);
  });

  it('descarta y borra un token vencido', () => {
    guardarSesion('abc', 60, 0);

    expect(leerSesion(60_000)).toBeNull();
    expect(sessionStorage.getItem('ferreteria.token')).toBeNull();
  });

  it('borrarSesion deja la sesión vacía', () => {
    guardarSesion('abc', 1800);
    borrarSesion();

    expect(obtenerToken()).toBeNull();
  });
});
