/**
 * Nombre: AuthController.java
 * Descripción: Endpoints de inicio de sesión y de consulta del usuario autenticado.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.security.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.ferreteria.inventario.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Autenticación (RF-SEG-001).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT (RF-SEG-001)")
public class AuthController {

	private final ServicioAutenticacion servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de autenticación
	 */
	public AuthController(ServicioAutenticacion servicio) {
		this.servicio = servicio;
	}

	/**
	 * Inicia sesión y devuelve un token de acceso de 30 minutos.
	 *
	 * @param solicitud usuario y contraseña
	 * @return token y datos del usuario
	 */
	@PostMapping("/login")
	@Operation(summary = "Iniciar sesión",
			description = "Valida usuario y contraseña. Cinco intentos fallidos bloquean la cuenta 15 minutos.")
	@ApiResponse(responseCode = "200", description = "Token emitido")
	@ApiResponse(responseCode = "400", description = "Datos incompletos")
	@ApiResponse(responseCode = "401", description = "Credenciales inválidas o cuenta inactiva")
	@ApiResponse(responseCode = "423", description = "Cuenta bloqueada por intentos fallidos")
	public RespuestaInicioSesion iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud) {
		return servicio.iniciarSesion(solicitud);
	}

	/**
	 * Devuelve el usuario autenticado con sus roles y permisos vigentes.
	 *
	 * @param jwt token de la solicitud
	 * @return datos del usuario
	 */
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Usuario autenticado", security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
	@ApiResponse(responseCode = "200", description = "Usuario autenticado")
	@ApiResponse(responseCode = "401", description = "Sin token válido")
	public UsuarioSesion usuarioActual(@AuthenticationPrincipal Jwt jwt) {
		return servicio.usuarioActual(Long.parseLong(jwt.getSubject()));
	}

}
