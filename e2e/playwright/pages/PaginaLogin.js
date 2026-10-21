/**
 * Nombre: PaginaLogin.js
 * Descripción: Page Object de la página de inicio de sesión (/login) para Playwright.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { expect } from '@playwright/test';

import { USUARIOS, contrasenaDe } from '../../comun/entorno.js';

/** Página de inicio de sesión. */
export class PaginaLogin {
  /**
   * @param {import('@playwright/test').Page} page página del navegador
   */
  constructor(page) {
    this.page = page;
    this.titulo = page.getByTestId('login-titulo');
    this.campoUsuario = page.getByTestId('login-usuario');
    this.campoContrasena = page.getByTestId('login-contrasena');
    this.botonEntrar = page.getByTestId('login-entrar');
    this.aviso = page.getByTestId('login-error');
  }

  /**
   * Abre la página de inicio de sesión.
   *
   * @returns {Promise<void>}
   */
  async abrir() {
    await this.page.goto('/login');
    await expect(this.botonEntrar).toBeVisible();
  }

  /**
   * Escribe usuario y contraseña y envía el formulario.
   *
   * @param {string} usuario nombre de usuario
   * @param {string} contrasena contraseña
   * @returns {Promise<void>}
   */
  async iniciarSesion(usuario, contrasena) {
    await this.campoUsuario.fill(usuario);
    await this.campoContrasena.fill(contrasena);
    await this.botonEntrar.click();
  }

  /**
   * Inicia sesión con un usuario de prueba y espera la barra superior con su nombre.
   *
   * @param {keyof typeof USUARIOS} rol admin, jefe o vendedor
   * @returns {Promise<void>}
   */
  async iniciarSesionComo(rol) {
    await this.abrir();
    await this.iniciarSesion(USUARIOS[rol].usuario, contrasenaDe(rol));
    await expect(this.page.getByTestId('usuario-actual')).toHaveText(USUARIOS[rol].nombreCompleto);
  }
}
