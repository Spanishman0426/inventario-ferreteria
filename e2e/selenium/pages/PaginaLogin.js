/**
 * Nombre: PaginaLogin.js
 * Descripción: Page Object de la página de inicio de sesión (/login) para Selenium WebDriver.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { BASE_URL, USUARIOS, contrasenaDe } from '../../comun/entorno.js';
import { escribir, esperarTexto, esperarVisible, porTestId } from '../support/esperas.js';

/** Localizadores de la página. */
export const LOGIN = {
  titulo: porTestId('login-titulo'),
  usuario: porTestId('login-usuario'),
  contrasena: porTestId('login-contrasena'),
  entrar: porTestId('login-entrar'),
  aviso: porTestId('login-error'),
};

/** Página de inicio de sesión. */
export class PaginaLogin {
  /**
   * @param {import('selenium-webdriver').WebDriver} driver navegador
   */
  constructor(driver) {
    this.driver = driver;
  }

  /**
   * Abre la página de inicio de sesión.
   *
   * @returns {Promise<void>}
   */
  async abrir() {
    await this.driver.get(`${BASE_URL}/login`);
    await esperarVisible(this.driver, LOGIN.entrar);
  }

  /**
   * Escribe usuario y contraseña y envía el formulario.
   *
   * @param {string} usuario nombre de usuario
   * @param {string} contrasena contraseña
   * @returns {Promise<void>}
   */
  async iniciarSesion(usuario, contrasena) {
    await escribir(await esperarVisible(this.driver, LOGIN.usuario), usuario);
    await escribir(await this.driver.findElement(LOGIN.contrasena), contrasena);
    await this.driver.findElement(LOGIN.entrar).click();
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
    await esperarTexto(this.driver, porTestId('usuario-actual'), USUARIOS[rol].nombreCompleto);
  }
}
