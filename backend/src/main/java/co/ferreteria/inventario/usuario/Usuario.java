/**
 * Nombre: Usuario.java
 * Descripción: Entidad JPA para la tabla usuario y su relación usuario_rol.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.usuario;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Generated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuenta de usuario (RF-SEG-001). Los usuarios no se eliminan, solo se desactivan.
 *
 * <p>{@code passwordHash} guarda el hash BCrypt (RNF-SEG-001) y nunca sale de la capa de servicio.
 * {@code intentosFallidos}, {@code bloqueadoHasta} y {@code ultimoAcceso} son el control de acceso;
 * según la migración V3, actualizarlos solos no genera auditoría.</p>
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

	/** Identificador generado por la base. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	/** Nombre de usuario único para iniciar sesión. */
	@Column(nullable = false, unique = true, updatable = false)
	@Setter(AccessLevel.NONE)
	private String usuario;

	/** Nombre de la persona. */
	@Column(name = "nombre_completo", nullable = false)
	private String nombreCompleto;

	/** Correo único. */
	@Column(nullable = false, unique = true)
	private String correo;

	/** Hash BCrypt de la contraseña. */
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	/** Si la cuenta puede iniciar sesión. */
	@Column(nullable = false)
	private boolean activo = true;

	/** Usuario técnico de tareas programadas; no inicia sesión ni se edita. */
	@Column(name = "es_sistema", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private boolean esSistema;

	/** Intentos fallidos consecutivos. */
	@Column(name = "intentos_fallidos", nullable = false)
	private short intentosFallidos;

	/** Fin del bloqueo por intentos fallidos. */
	@Column(name = "bloqueado_hasta")
	private Instant bloqueadoHasta;

	/** Último inicio de sesión correcto. */
	@Column(name = "ultimo_acceso")
	private Instant ultimoAcceso;

	/** Fecha de creación, asignada por la base. */
	@Generated
	@Column(name = "creado_en", insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant creadoEn;

	/** Roles asignados (tabla usuario_rol). */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "usuario_rol",
			joinColumns = @JoinColumn(name = "usuario_id"),
			inverseJoinColumns = @JoinColumn(name = "rol_id"))
	@BatchSize(size = 50)
	@Setter(AccessLevel.NONE)
	private Set<Rol> roles = new HashSet<>();

	/**
	 * Crea un usuario nuevo, activo y sin intentos fallidos.
	 *
	 * @param usuario nombre de usuario
	 * @param nombreCompleto nombre de la persona
	 * @param correo correo
	 * @param passwordHash hash BCrypt de la contraseña
	 */
	public Usuario(String usuario, String nombreCompleto, String correo, String passwordHash) {
		this.usuario = usuario;
		this.nombreCompleto = nombreCompleto;
		this.correo = correo;
		this.passwordHash = passwordHash;
	}

	/**
	 * Reemplaza los roles del usuario.
	 *
	 * @param nuevos roles a asignar
	 */
	public void asignarRoles(Collection<Rol> nuevos) {
		roles.retainAll(nuevos);
		roles.addAll(nuevos);
	}

	/**
	 * Indica si el usuario tiene un rol.
	 *
	 * @param codigo código del rol
	 * @return {@code true} si lo tiene
	 */
	public boolean tieneRol(String codigo) {
		return roles.stream().anyMatch(r -> r.getCodigo().equals(codigo));
	}

	/**
	 * Códigos de los roles, ordenados.
	 *
	 * @return códigos de rol
	 */
	public Set<String> codigosRoles() {
		Set<String> codigos = new TreeSet<>();
		roles.forEach(r -> codigos.add(r.getCodigo()));
		return codigos;
	}

	/**
	 * Códigos de los permisos de todos sus roles, ordenados; son sus autoridades.
	 *
	 * @return códigos de permiso
	 */
	public Set<String> codigosPermisos() {
		Set<String> codigos = new TreeSet<>();
		roles.forEach(r -> r.getPermisos().forEach(p -> codigos.add(p.getCodigo())));
		return codigos;
	}

	/**
	 * Indica si la cuenta está bloqueada en un instante.
	 *
	 * @param ahora instante de referencia
	 * @return {@code true} si el bloqueo sigue vigente
	 */
	public boolean estaBloqueado(Instant ahora) {
		return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
	}

}
