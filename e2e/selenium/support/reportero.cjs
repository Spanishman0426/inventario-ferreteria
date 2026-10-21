/**
 * Nombre: reportero.cjs
 * Descripción: Reportero de Mocha que combina la salida "spec" en consola con el reporte JUnit XML de
 *              mocha-junit-reporter (reportes/junit/resultados-selenium.xml), con las capturas de fallo
 *              como adjuntos.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
const path = require('node:path');
const Mocha = require('mocha');
const JUnitReporter = require('mocha-junit-reporter');

/** Ruta del XML; puede cambiarse con la variable E2E_JUNIT_SELENIUM. */
const ARCHIVO_JUNIT =
  process.env.E2E_JUNIT_SELENIUM ||
  path.join(__dirname, '..', 'reportes', 'junit', 'resultados-selenium.xml');

/**
 * Reportero compuesto: Spec (consola) + JUnit XML.
 */
class Reportero {
  /**
   * @param {Mocha.Runner} runner ejecución de Mocha
   * @param {Mocha.MochaOptions} options opciones de Mocha
   */
  constructor(runner, options) {
    this.spec = new Mocha.reporters.Spec(runner, options);
    this.junit = new JUnitReporter(runner, {
      reporterOptions: {
        mochaFile: ARCHIVO_JUNIT,
        testsuitesTitle: 'Selenium E2E',
        attachments: true,
        jenkinsMode: false,
      },
    });
  }
}

module.exports = Reportero;
