/**
 * Nombre: eslint.config.js
 * Descripción: Reglas de ESLint (configuración plana): JavaScript, hooks de React, Fast Refresh,
 *              JSDoc obligatorio en funciones y componentes exportados, y compatibilidad con Prettier.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import jsdoc from 'eslint-plugin-jsdoc';
import prettier from 'eslint-config-prettier';

export default [
  { ignores: ['dist/', 'coverage/', 'node_modules/'] },
  js.configs.recommended,
  reactHooks.configs.flat['recommended-latest'],
  reactRefresh.configs.vite,
  jsdoc.configs['flat/recommended-typescript-flavor-error'],
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: { ...globals.browser },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    rules: {
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]', argsIgnorePattern: '^_' }],
      // JSDoc obligatorio en todo lo exportado: funciones, componentes (flecha o declarados) y clases.
      'jsdoc/require-jsdoc': [
        'error',
        {
          publicOnly: true,
          require: {
            FunctionDeclaration: true,
            ArrowFunctionExpression: true,
            FunctionExpression: true,
            ClassDeclaration: true,
            MethodDefinition: true,
          },
        },
      ],
      'jsdoc/require-description': 'error',
      'jsdoc/tag-lines': ['error', 'any', { startLines: 1 }],
    },
  },
  {
    // Archivos de configuración y pruebas corren en Node.
    files: ['*.config.js', 'src/**/*.test.{js,jsx}', 'src/test/**'],
    languageOptions: { globals: { ...globals.node } },
  },
  prettier,
];
