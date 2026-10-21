/**
 * Nombre: datos.js
 * Descripción: Datos de las pruebas E2E: catálogos de database/02_datos_prueba.sql, textos esperados en
 *              cada idioma y nombres únicos para que las pruebas puedan repetirse sobre la misma base.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
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

/**
 * Prefijo común para un grupo de productos de una misma prueba (búsqueda y filtros).
 *
 * @param {string} herramienta PW o SE
 * @returns {string} prefijo único
 */
export function prefijoUnico(herramienta) {
  return `E2E ${herramienta} Filtro ${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
}
