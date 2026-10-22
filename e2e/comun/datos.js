/**
 * Nombre: datos.js
 * Descripción: Datos de las pruebas E2E: catálogos y terceros de database/02_datos_prueba.sql, textos
 *              esperados en cada idioma, nombres e identificaciones únicas (productos, marcas, cédulas y
 *              NIT con su dígito de verificación) para que las pruebas puedan repetirse sobre la misma base.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */

/** Categorías y marcas que cargan los datos de prueba (database/02_datos_prueba.sql). */
export const CATALOGO = {
  categorias: { herramientas: 'Herramientas', plomeria: 'Plomería', electricos: 'Eléctricos' },
  marcas: { stanley: 'Stanley', truper: 'Truper', pavco: 'Pavco' },
  /** Texto con que empieza la opción de la unidad base en el selector ("UND — Unidad"). */
  unidadUnd: 'UND',
  unidadMtr: 'MTR',
  /** Nombre de la tarifa de IVA del 19 %. */
  ivaGeneral: 'Tarifa general',
};

/**
 * Petición válida de producto con ids fijos de los datos de prueba (categoría 1, marca 1, IVA 19 %).
 * Debe ser válida para que el rechazo por permisos (403) no se confunda con uno de validación (400).
 *
 * @param {string} nombre nombre del producto
 * @returns {object} cuerpo de POST /api/v1/productos
 */
export function peticionProducto(nombre) {
  return {
    nombre,
    descripcion: null,
    categoriaId: 1,
    marcaId: 1,
    unidadBase: 'UND',
    tarifaIvaId: 3,
    porcentajeIncremento: 30,
    control: 'NINGUNO',
    plazoGarantiaMeses: null,
  };
}

/** Producto de los datos de prueba con costo conocido: martillo con último costo 18.000 (RNF-INT-003). */
export const MARTILLO = { nombre: 'Martillo de uña 16 oz', costoFormateado: /^\$\s18\.000$/ };

/** Textos visibles que verifican las pruebas, tomados de frontend/src/i18n/locales. */
export const TEXTOS = {
  es: {
    loginTitulo: 'Iniciar sesión',
    productosTitulo: 'Productos',
    menuProductos: 'Productos',
    credencialesInvalidas:
      'El usuario o la contraseña no son correctos, o la cuenta está inactiva.',
    requerido: 'Este campo es obligatorio.',
    minimo3: 'Mínimo 3 caracteres.',
    rangoPorcentaje: 'Debe estar entre 0 y 500.',
    estadoActivo: 'Activo',
    estadoInactivo: 'Inactivo',
    nuevoProducto: 'Nuevo producto',
    /** Etiqueta visible de cada campo del formulario, por sufijo de su id (producto-<campo>). */
    etiquetas: {
      nombre: 'Nombre',
      categoria: 'Categoría',
      marca: 'Marca',
      unidad: 'Unidad base',
      iva: 'Tarifa de IVA',
      porcentaje: 'Porcentaje de incremento',
    },
    /**
     * Causa del rechazo por duplicado (backend: producto.duplicado).
     *
     * @param {string} nombre nombre del producto
     * @param {string} marca nombre de la marca
     * @returns {string} mensaje esperado
     */
    duplicado: (nombre, marca) => `Ya existe el producto «${nombre}» para la marca ${marca}.`,
    /**
     * Notificación tras crear un producto.
     *
     * @param {string} nombre nombre del producto
     * @returns {string} mensaje esperado
     */
    creado: (nombre) => `Producto «${nombre}» registrado.`,
    /**
     * Notificación tras editar un producto.
     *
     * @param {string} nombre nombre del producto
     * @returns {string} mensaje esperado
     */
    actualizado: (nombre) => `Producto «${nombre}» actualizado.`,
    /**
     * Notificación tras desactivar un producto.
     *
     * @param {string} nombre nombre del producto
     * @returns {string} mensaje esperado
     */
    desactivado: (nombre) => `Producto «${nombre}» desactivado.`,
    /** Textos de la administración de catálogos (frontend/src/i18n/locales/es/catalogos.json). */
    catalogos: {
      titulo: 'Catálogos',
      menu: 'Catálogos',
      desactivarTitulo: '¿Desactivar la marca?',
      /**
       * Notificación tras registrar una marca.
       *
       * @param {string} nombre nombre de la marca
       * @returns {string} mensaje esperado
       */
      marcaCreada: (nombre) => `Marca «${nombre}» registrada.`,
      /**
       * Notificación tras desactivar una marca.
       *
       * @param {string} nombre nombre de la marca
       * @returns {string} mensaje esperado
       */
      marcaDesactivada: (nombre) => `Marca «${nombre}» desactivada.`,
    },
    /** Textos de clientes y proveedores (frontend/src/i18n/locales/es/terceros.json y validacion.json). */
    terceros: {
      titulo: 'Clientes y proveedores',
      personaJuridica: 'Persona jurídica',
      predefinido: 'Predefinido',
      cliente: 'Cliente',
      proveedor: 'Proveedor',
      sinPermisoProveedor: 'Su rol no puede asignar el rol Proveedor.',
      soloLecturaProveedor:
        'Solo puede consultar este tercero: es proveedor y su rol no tiene permiso para editar terceros de ese tipo.',
      /**
       * Notificación tras registrar un tercero.
       *
       * @param {string} nombre razón social o nombre y apellido
       * @returns {string} mensaje esperado
       */
      creado: (nombre) => `Tercero «${nombre}» registrado.`,
      /**
       * Error del formulario con un dígito de verificación incorrecto.
       *
       * @param {number} esperado dígito correcto
       * @returns {string} mensaje esperado
       */
      digitoIncorrecto: (esperado) => `No corresponde al NIT; el dígito correcto es ${esperado}.`,
      /**
       * Causa del 409 por identificación duplicada (backend: tercero.identificacion_duplicada).
       *
       * @param {'CC'|'NIT'} tipo tipo de identificación
       * @param {string} numero número
       * @returns {string} mensaje esperado
       */
      duplicado: (tipo, numero) => `Ya existe un tercero con identificación ${tipo} ${numero}.`,
    },
  },
  en: {
    loginTitulo: 'Sign in',
    productosTitulo: 'Products',
    menuProductos: 'Products',
    nuevoProducto: 'New product',
    estadoActivo: 'Active',
  },
};

/** Límites del formulario de producto (ProductoPeticion / productoSchema). */
export const LIMITES = { nombreMax: 150, descripcionMax: 500 };

/**
 * Genera un nombre de producto único por ejecución, para no chocar con ejecuciones anteriores ni con
 * la otra herramienta (los productos no se borran: solo se desactivan).
 *
 * @param {string} herramienta prefijo de la herramienta: PW (Playwright) o SE (Selenium)
 * @param {string} descripcion parte legible del nombre
 * @returns {string} nombre de hasta 150 caracteres
 */
export function nombreUnico(herramienta, descripcion) {
  const sello = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
  return `E2E ${herramienta} ${descripcion} ${sello}`.slice(0, LIMITES.nombreMax);
}

/** Longitud máxima del nombre de una marca o categoría (ElementoCatalogoPeticion). */
export const NOMBRE_CATALOGO_MAX = 100;

/**
 * Genera un nombre de marca o categoría único por ejecución (no se borran: solo se desactivan).
 *
 * @param {string} herramienta prefijo de la herramienta: PW (Playwright) o SE (Selenium)
 * @returns {string} nombre de hasta 100 caracteres
 */
export function nombreMarcaUnico(herramienta) {
  return nombreUnico(herramienta, 'Marca').slice(0, NOMBRE_CATALOGO_MAX);
}

/**
 * Prefijo común para un grupo de productos de una misma prueba (búsqueda y filtros).
 *
 * @param {string} herramienta PW o SE
 * @returns {string} prefijo único
 */
export function prefijoUnico(herramienta) {
  return `E2E ${herramienta} Filtro ${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
}

/** Terceros de los datos de prueba (database/02_datos_prueba.sql) y el Consumidor final del esquema. */
export const TERCEROS = {
  consumidorFinal: { id: 1, cc: '222222222222' },
  pavco: { id: 2, nit: '900123456', dv: 8, razonSocial: 'Distribuidora Pavco Neiva S.A.S.' },
  juanPerez: { id: 5, cc: '1075234567' },
};

/** Pesos de la DIAN para el dígito de verificación del NIT (igual que fn_dv_nit). */
const PESOS_NIT = [3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71];

/**
 * Dígito de verificación de un NIT, calculado aparte de la aplicación para que la prueba no dependa
 * del código que verifica (mismo algoritmo de fn_dv_nit).
 *
 * @param {string} nit número sin dígito
 * @returns {number} dígito de 0 a 9
 */
export function digitoVerificacionNit(nit) {
  let suma = 0;
  for (let i = 0; i < nit.length; i += 1) {
    suma += Number(nit[nit.length - 1 - i]) * PESOS_NIT[i];
  }
  const residuo = suma % 11;
  return residuo <= 1 ? residuo : 11 - residuo;
}

/**
 * Número aleatorio que empieza por un dígito dado (los terceros no se borran entre ejecuciones).
 *
 * @param {string} inicio primer dígito
 * @param {number} digitos cantidad total de dígitos
 * @returns {string} número
 */
function numeroAleatorio(inicio, digitos) {
  let numero = inicio;
  while (numero.length < digitos) {
    numero += Math.floor(Math.random() * 10);
  }
  return numero;
}

/**
 * Cédula única de 10 dígitos que empieza por 3 (Playwright) o 4 (Selenium).
 *
 * @param {'PW'|'SE'} herramienta herramienta
 * @returns {string} cédula
 */
export function cedulaUnica(herramienta) {
  return numeroAleatorio(herramienta === 'PW' ? '3' : '4', 10);
}

/**
 * NIT único de 9 dígitos que empieza por 6 (Playwright) o 7 (Selenium), con su dígito.
 *
 * @param {'PW'|'SE'} herramienta herramienta
 * @returns {{nit: string, dv: number}} NIT y dígito de verificación
 */
export function nitUnico(herramienta) {
  const nit = numeroAleatorio(herramienta === 'PW' ? '6' : '7', 9);
  return { nit, dv: digitoVerificacionNit(nit) };
}

/**
 * Cuerpo válido de POST /api/v1/terceros para una persona jurídica.
 *
 * @param {{nit: string, dv: number, razonSocial: string, cliente?: boolean, proveedor?: boolean}} datos datos
 * @returns {object} TerceroPeticion
 */
export function peticionJuridica({ nit, dv, razonSocial, cliente = false, proveedor = true }) {
  return {
    tipoPersona: 'JURIDICA',
    tipoIdentificacion: 'NIT',
    numeroIdentificacion: nit,
    digitoVerificacion: dv,
    razonSocial,
    direccion: 'Calle 8 # 4-15, Neiva',
    telefono: '6088712345',
    esCliente: cliente,
    esProveedor: proveedor,
  };
}
