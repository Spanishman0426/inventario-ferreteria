/**
 * Nombre: TerceroServiceTests.java
 * Descripción: Pruebas unitarias (Mockito) de TerceroService: limpieza por tipo de persona, duplicados,
 *              permisos por rol, Consumidor final protegido, estado idempotente y orden.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import co.ferreteria.inventario.common.error.CampoInvalidoException;
import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.OperacionRechazadaException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.tercero.dto.TerceroPeticion;
import co.ferreteria.inventario.tercero.dto.TerceroRespuesta;

/**
 * Reglas de {@link TerceroService} con el repositorio simulado (RF-TER-001, RN-26, RN-27).
 */
@ExtendWith(MockitoExtension.class)
class TerceroServiceTests {

	private static final String CLIENTES = "TERCERO_CLIENTE_EDITAR";
	private static final String PROVEEDORES = "TERCERO_PROVEEDOR_EDITAR";

	@Mock
	private TerceroRepository terceros;

	private TerceroService servicio() {
		return new TerceroService(terceros);
	}

	@AfterEach
	void limpiarSesion() {
		SecurityContextHolder.clearContext();
	}

	// ------------------------------------------------------------------ crear

	@Test
	@DisplayName("[CA-RF-TER-001-1] Crear una persona natural recorta textos, descarta la razón social y queda ACTIVA")
	void crearNatural() {
		sesion(CLIENTES);
		guardarDevuelveLoMismo();
		TerceroPeticion peticion = TerceroCoherenteValidatorTests.natural().numeroIdentificacion(" 1075234567 ")
				.nombre("  Juan ").apellido(" Pérez ").razonSocial("No aplica").correo("  ").build();

		TerceroRespuesta creado = servicio().crear(peticion);

		Tercero guardado = guardado();
		assertThat(guardado.getNumeroIdentificacion()).isEqualTo("1075234567");
		assertThat(guardado.getNombre()).isEqualTo("Juan");
		assertThat(guardado.getApellido()).isEqualTo("Pérez");
		assertThat(guardado.getRazonSocial()).isNull();
		assertThat(guardado.getCorreo()).isNull();
		assertThat(guardado.getDigitoVerificacion()).isNull();
		assertThat(creado.estado()).isEqualTo(EstadoTercero.ACTIVO);
		assertThat(creado.nombreMostrado()).isEqualTo("Juan Pérez");
	}

	@Test
	@DisplayName("[CA-RF-TER-001-1] Crear una persona jurídica guarda NIT, dígito y razón social, sin nombre ni apellido")
	void crearJuridica() {
		sesion(PROVEEDORES);
		guardarDevuelveLoMismo();
		TerceroPeticion peticion = TerceroCoherenteValidatorTests.juridica().nombre("Ignorado").apellido("Ignorado")
				.build();

		TerceroRespuesta creado = servicio().crear(peticion);

		Tercero guardado = guardado();
		assertThat(guardado.getDigitoVerificacion()).isEqualTo((short) 8);
		assertThat(guardado.getNombre()).isNull();
		assertThat(guardado.getApellido()).isNull();
		assertThat(creado.nombreMostrado()).isEqualTo("Bancolombia S.A.");
	}

	@Test
	@DisplayName("[CA-RF-TER-001-2] Con los dos permisos se crea un tercero cliente y proveedor")
	void crearClienteYProveedor() {
		sesion(CLIENTES, PROVEEDORES);
		guardarDevuelveLoMismo();

		TerceroRespuesta creado = servicio().crear(TerceroCoherenteValidatorTests.juridica().esCliente(true).build());

		assertThat(creado.esCliente()).isTrue();
		assertThat(creado.esProveedor()).isTrue();
	}

	@Test
	@DisplayName("[CA-RF-TER-001-3] Crear con una identificación que ya existe lanza ConflictoException")
	void crearDuplicado() {
		sesion(CLIENTES);
		when(terceros.existsByTipoIdentificacionAndNumeroIdentificacionAndIdNot(TipoIdentificacion.CC, "1075234567", -1L))
				.thenReturn(true);

		assertThatThrownBy(() -> servicio().crear(TerceroCoherenteValidatorTests.natural().build()))
				.isInstanceOf(ConflictoException.class)
				.extracting("claveMensaje").isEqualTo("tercero.identificacion_duplicada");
		verify(terceros, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Un vendedor (solo clientes) no crea un proveedor ni un tercero con los dos roles")
	void vendedorNoCreaProveedor() {
		sesion(CLIENTES);

		assertThatThrownBy(() -> servicio().crear(TerceroCoherenteValidatorTests.juridica().build()))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> servicio().crear(TerceroCoherenteValidatorTests.juridica().esCliente(true).build()))
				.isInstanceOf(AccessDeniedException.class);
		verify(terceros, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Un comprador (solo proveedores) no crea un cliente")
	void compradorNoCreaCliente() {
		sesion(PROVEEDORES);

		assertThatThrownBy(() -> servicio().crear(TerceroCoherenteValidatorTests.natural().build()))
				.isInstanceOf(AccessDeniedException.class);
	}

	// ------------------------------------------------------------------ editar y estado

	@Test
	@DisplayName("Editar excluye el propio id de la verificación de duplicados y reemplaza los datos")
	void editar() {
		sesion(CLIENTES);
		Tercero actual = existente(7L, TipoPersona.NATURAL, true, false);
		when(terceros.findById(7L)).thenReturn(Optional.of(actual));
		guardarDevuelveLoMismo();

		servicio().actualizar(7L, TerceroCoherenteValidatorTests.natural().nombre("Ana").build());

		verify(terceros).existsByTipoIdentificacionAndNumeroIdentificacionAndIdNot(TipoIdentificacion.CC, "1075234567", 7L);
		assertThat(guardado().getNombre()).isEqualTo("Ana");
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Editar exige el permiso de los roles actuales: un vendedor no edita un proveedor aunque lo deje como cliente")
	void editarExigeRolesActuales() {
		sesion(CLIENTES);
		when(terceros.findById(8L)).thenReturn(Optional.of(existente(8L, TipoPersona.JURIDICA, false, true)));

		assertThatThrownBy(() -> servicio().actualizar(8L,
				TerceroCoherenteValidatorTests.juridica().esCliente(true).esProveedor(false).build()))
				.isInstanceOf(AccessDeniedException.class);
		verify(terceros, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Editar exige el permiso de los roles nuevos: un vendedor no agrega el rol proveedor a un cliente")
	void editarExigeRolesNuevos() {
		sesion(CLIENTES);
		when(terceros.findById(9L)).thenReturn(Optional.of(existente(9L, TipoPersona.NATURAL, true, false)));

		assertThatThrownBy(() -> servicio().actualizar(9L,
				TerceroCoherenteValidatorTests.natural().esProveedor(true).build()))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@DisplayName("[CA-RF-TER-001-4] El Consumidor final no se edita ni se desactiva: REGLA_NEGOCIO, aun con todos los permisos")
	void consumidorFinalProtegido() {
		sesion(CLIENTES, PROVEEDORES);
		Tercero consumidor = existente(1L, TipoPersona.NATURAL, true, false);
		ReflectionTestUtils.setField(consumidor, "esConsumidorFinal", true);
		when(terceros.findById(1L)).thenReturn(Optional.of(consumidor));

		assertThatThrownBy(() -> servicio().actualizar(1L, TerceroCoherenteValidatorTests.natural().build()))
				.isInstanceOfSatisfying(OperacionRechazadaException.class, e -> {
					assertThat(e.getCodigo()).isEqualTo(CodigoError.REGLA_NEGOCIO);
					assertThat(e.getClaveMensaje()).isEqualTo("tercero.consumidor_final_no_editable");
				});
		assertThatThrownBy(() -> servicio().cambiarEstado(1L, EstadoTercero.INACTIVO))
				.isInstanceOf(OperacionRechazadaException.class);
		verify(terceros, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Desactivar deja el tercero INACTIVO; repetirlo no escribe (idempotente)")
	void desactivarIdempotente() {
		sesion(PROVEEDORES);
		Tercero proveedor = existente(5L, TipoPersona.JURIDICA, false, true);
		when(terceros.findById(5L)).thenReturn(Optional.of(proveedor));
		guardarDevuelveLoMismo();

		assertThat(servicio().cambiarEstado(5L, EstadoTercero.INACTIVO).estado()).isEqualTo(EstadoTercero.INACTIVO);
		assertThat(servicio().cambiarEstado(5L, EstadoTercero.INACTIVO).estado()).isEqualTo(EstadoTercero.INACTIVO);
		verify(terceros).saveAndFlush(proveedor);
	}

	@Test
	@DisplayName("[CA-RF-SEG-001-2] Un vendedor no desactiva un proveedor")
	void vendedorNoDesactivaProveedor() {
		sesion(CLIENTES);
		when(terceros.findById(5L)).thenReturn(Optional.of(existente(5L, TipoPersona.JURIDICA, false, true)));

		assertThatThrownBy(() -> servicio().cambiarEstado(5L, EstadoTercero.INACTIVO))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@DisplayName("Editar o consultar un id inexistente lanza RecursoNoEncontradoException")
	void noEncontrado() {
		when(terceros.findById(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio().obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> servicio().cambiarEstado(99L, EstadoTercero.ACTIVO))
				.isInstanceOf(RecursoNoEncontradoException.class);
	}

	// ------------------------------------------------------------------ búsqueda

	@Test
	@DisplayName("Ordenar por nombre usa el nombre mostrado y agrega id como desempate; un campo desconocido es 400")
	@SuppressWarnings("unchecked")
	void orden() {
		when(terceros.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

		servicio().listar(null, RolTercero.CLIENTE, null, null, PageRequest.of(0, 20, Sort.by("nombre")));

		ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
		verify(terceros).findAll(any(Specification.class), pagina.capture());
		assertThat(pagina.getValue().getSort()).containsExactly(Sort.Order.asc("nombreBusqueda"), Sort.Order.asc("id"));
		assertThatThrownBy(() -> servicio().listar(null, null, null, null, PageRequest.of(0, 20, Sort.by("clave"))))
				.isInstanceOf(CampoInvalidoException.class);
	}

	// ------------------------------------------------------------------ apoyo

	private static void sesion(String... permisos) {
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("2", null, permisos));
	}

	private void guardarDevuelveLoMismo() {
		when(terceros.saveAndFlush(any(Tercero.class))).thenAnswer(i -> i.getArgument(0));
	}

	private Tercero guardado() {
		ArgumentCaptor<Tercero> captor = ArgumentCaptor.forClass(Tercero.class);
		verify(terceros).saveAndFlush(captor.capture());
		return captor.getValue();
	}

	private static Tercero existente(long id, TipoPersona tipo, boolean cliente, boolean proveedor) {
		Tercero t = new Tercero(tipo, tipo == TipoPersona.NATURAL ? TipoIdentificacion.CC : TipoIdentificacion.NIT,
				"1075234567");
		ReflectionTestUtils.setField(t, "id", id);
		t.setNombre("Juan");
		t.setApellido("Pérez");
		t.setRazonSocial("Proveedor S.A.S.");
		t.setEsCliente(cliente);
		t.setEsProveedor(proveedor);
		return t;
	}

}
