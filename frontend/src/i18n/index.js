/**
 * Nombre: index.js
 * Descripción: Configuración de i18next: español por defecto, inglés como segundo idioma y
 *              elección del usuario recordada en localStorage (RNF-INT-003).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';

import authEs from './locales/es/auth.json';
import comunEs from './locales/es/comun.json';
import erroresEs from './locales/es/errores.json';
import productosEs from './locales/es/productos.json';
import validacionEs from './locales/es/validacion.json';
import authEn from './locales/en/auth.json';
import comunEn from './locales/en/comun.json';
import erroresEn from './locales/en/errores.json';
import productosEn from './locales/en/productos.json';
import validacionEn from './locales/en/validacion.json';

/** Idiomas que ofrece la interfaz; el primero es el predeterminado. */
export const IDIOMAS = ['es', 'en'];

/** Idioma predeterminado de la interfaz. */
export const IDIOMA_POR_DEFECTO = 'es';

/** Clave de localStorage donde se guarda el idioma elegido. */
export const CLAVE_IDIOMA = 'ferreteria.idioma';

/**
 * Refleja el idioma activo en el atributo lang del documento (accesibilidad y lectores de pantalla).
 *
 * @param {string} idioma código del idioma activo
 * @returns {void}
 */
function actualizarAtributoLang(idioma) {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = idioma;
  }
}

i18n.on('languageChanged', actualizarAtributoLang);

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      es: {
        auth: authEs,
        comun: comunEs,
        errores: erroresEs,
        productos: productosEs,
        validacion: validacionEs,
      },
      en: {
        auth: authEn,
        comun: comunEn,
        errores: erroresEn,
        productos: productosEn,
        validacion: validacionEn,
      },
    },
    ns: ['comun', 'auth', 'productos', 'errores', 'validacion'],
    defaultNS: 'comun',
    supportedLngs: IDIOMAS,
    fallbackLng: IDIOMA_POR_DEFECTO,
    load: 'languageOnly',
    // Solo se usa la elección guardada: sin ella la interfaz arranca en español aunque el
    // navegador esté en otro idioma.
    detection: {
      order: ['localStorage'],
      lookupLocalStorage: CLAVE_IDIOMA,
      caches: ['localStorage'],
    },
    interpolation: { escapeValue: false },
    initAsync: false,
    react: { useSuspense: false },
  });

export default i18n;
