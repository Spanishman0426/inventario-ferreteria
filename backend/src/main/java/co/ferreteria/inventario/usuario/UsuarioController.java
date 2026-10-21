/**
 * Nombre: UsuarioController.java
 * Descripción: API de administración de usuarios y consulta de roles.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.ferreteria.inventario.common.web.Pagina;
import co.ferreteria.inventario.config.OpenApiConfig;
import co.ferreteria.inventario.usuario.dto.CambioContrasena;
import co.ferreteria.inventario.usuario.dto.CambioEstado;
import co.ferreteria.inventario.usuario.dto.CambioRoles;
import co.ferreteria.inventario.usuario.dto.RolRespuesta;
import co.ferreteria.inventario.usuario.dto.UsuarioActualizacion;
import co.ferreteria.inventario.usuario.dto.UsuarioCreacion;
import co.ferreteria.inventario.usuario.dto.UsuarioRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Gestión de usuarios y roles (RF-SEG-001). Exige el permiso {@code USUARIOS_GESTIONAR} (matriz 2.2:
 * solo el administrador). No existe DELETE: los usuarios solo se desactivan.
 */
@RestController
@RequestMapping("/api/v1")
@Validated
@PreAuthorize("hasAuthority('USUARIOS_GESTIONAR')")
@Tag(name = "Usuarios", description = "Usuarios, roles y activación de cuentas (RF-SEG-001)")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@ApiResponse(responseCode = "401", description = "Sin token válido")
@ApiResponse(responseCode = "403", description = "Sin el permiso USUARIOS_GESTIONAR")
public class UsuarioController {

	private final UsuarioService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de usuarios
	 */
	public UsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Lista los usuarios paginados.
	 *
	 * @param pagina número de página desde 0
	 * @param tamano tamaño de página (máximo 100)
	 * @return página de usuarios
	 */
	@GetMapping("/usuarios")
	@Operation(summary = "Listar usuarios")
	public Pagina<UsuarioRespuesta> listar(
			@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "" + Pagina.TAMANO_POR_DEFECTO) @Min(1) @Max(Pagina.TAMANO_MAXIMO) int tamano) {
		return servicio.listar(pagina, tamano);
	}

	/**
	 * Consulta un usuario.
	 *
	 * @param id id del usuario
	 * @return usuario
	 */
	@GetMapping("/usuarios/{id}")
	@Operation(summary = "Consultar un usuario")
	@ApiResponse(responseCode = "404", description = "No existe")
	public UsuarioRespuesta obtener(@PathVariable long id) {
		return servicio.obtener(id);
	}

	/**
	 * Crea un usuario activo.
	 *
	 * @param datos datos del usuario
	 * @return usuario creado
	 */
	@PostMapping("/usuarios")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear usuario",
			description = "Contraseña de al menos 8 caracteres con letras y números; se guarda con BCrypt.")
	@ApiResponse(responseCode = "201", description = "Creado")
	@ApiResponse(responseCode = "400", description = "Datos no válidos")
	@ApiResponse(responseCode = "404", description = "Rol inexistente")
	@ApiResponse(responseCode = "409", description = "Usuario o correo duplicado")
	public UsuarioRespuesta crear(@Valid @RequestBody UsuarioCreacion datos) {
		return servicio.crear(datos);
	}

	/**
	 * Cambia nombre y correo.
	 *
	 * @param id id del usuario
	 * @param datos datos nuevos
	 * @return usuario actualizado
	 */
	@PutMapping("/usuarios/{id}")
	@Operation(summary = "Editar nombre y correo")
	@ApiResponse(responseCode = "409", description = "Correo duplicado")
	public UsuarioRespuesta actualizar(@PathVariable long id, @Valid @RequestBody UsuarioActualizacion datos) {
		return servicio.actualizar(id, datos);
	}

	/**
	 * Reemplaza los roles.
	 *
	 * @param id id del usuario
	 * @param datos roles nuevos
	 * @return usuario actualizado
	 */
	@PutMapping("/usuarios/{id}/roles")
	@Operation(summary = "Asignar roles", description = "No se puede quitar el rol ADMIN al último administrador activo.")
	@ApiResponse(responseCode = "409", description = "Dejaría el sistema sin administrador")
	public UsuarioRespuesta asignarRoles(@PathVariable long id, @Valid @RequestBody CambioRoles datos) {
		return servicio.asignarRoles(id, datos.roles());
	}

	/**
	 * Activa o desactiva la cuenta.
	 *
	 * @param id id del usuario
	 * @param datos estado nuevo
	 * @return usuario actualizado
	 */
	@PatchMapping("/usuarios/{id}/estado")
	@Operation(summary = "Activar o desactivar",
			description = "Un administrador no puede desactivarse a sí mismo ni desactivar al último administrador activo.")
	@ApiResponse(responseCode = "409", description = "Autodesactivación o último administrador")
	public UsuarioRespuesta cambiarEstado(@PathVariable long id, @Valid @RequestBody CambioEstado datos) {
		return servicio.cambiarEstado(id, datos.activo());
	}

	/**
	 * Asigna una contraseña nueva.
	 *
	 * @param id id del usuario
	 * @param datos contraseña nueva
	 */
	@PutMapping("/usuarios/{id}/contrasena")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Cambiar contraseña", description = "Mismas reglas que al crear; también levanta un bloqueo vigente.")
	@ApiResponse(responseCode = "204", description = "Contraseña cambiada")
	public void cambiarContrasena(@PathVariable long id, @Valid @RequestBody CambioContrasena datos) {
		servicio.cambiarContrasena(id, datos.contrasena());
	}

	/**
	 * Roles disponibles con sus permisos.
	 *
	 * @return roles de la matriz 2.2
	 */
	@GetMapping("/roles")
	@Operation(summary = "Listar roles con sus permisos")
	public List<RolRespuesta> listarRoles() {
		return servicio.listarRoles();
	}

}
