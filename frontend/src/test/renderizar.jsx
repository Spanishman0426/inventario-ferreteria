/**
 * Nombre: renderizar.jsx
 * Descripción: Utilidad de pruebas que monta la aplicación completa (proveedores, sesión y rutas)
 *              sobre un enrutador en memoria, empezando en la ruta indicada.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';

import AuthProvider from '../context/AuthProvider.jsx';
import ProveedoresApp from '../ProveedoresApp.jsx';
import RutasApp from '../RutasApp.jsx';

/**
 * Monta la aplicación en una ruta y prepara un usuario simulado de user-event.
 *
 * @param {string} [ruta] ruta inicial, por ejemplo '/login'
 * @returns {import('@testing-library/react').RenderResult & {usuario: ReturnType<typeof userEvent.setup>}} resultado del render y usuario simulado
 */
export function renderizarApp(ruta = '/') {
  const usuario = userEvent.setup();
  const resultado = render(
    <ProveedoresApp>
      <MemoryRouter initialEntries={[ruta]}>
        <AuthProvider>
          <RutasApp />
        </AuthProvider>
      </MemoryRouter>
    </ProveedoresApp>,
  );
  return { ...resultado, usuario };
}
