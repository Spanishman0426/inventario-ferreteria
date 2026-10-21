/**
 * Nombre: ProductoServiceTests.java
 * Descripción: Pruebas unitarias con Mockito del servicio de productos: crear, actualizar, duplicado,
 *              no encontrado, desactivar, regla de garantía, visibilidad de costos y orden de la búsqueda.
 * Fecha de creación: 2026-10-20
 * Última actualización: 2026-10-20
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import co.ferreteria.inventario.catalogo.dto.ProductoPeticion;
import co.ferreteria.inventario.catalogo.dto.ProductoRespuesta;
import co.ferreteria.inventario.common.error.CampoInvalidoException;
import co.ferreteria.inventario.common.error.CodigoError;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.OperacionRechazadaException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.common.web.Pagina;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Reglas de {@link ProductoService} aisladas de la base: los repositorios son simulados con Mockito
 * (RF-PRO-001, RF-PRO-003, RN-05, RN-18).
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTests {

	private static final long ID = 10L;

	@Mock
	private ProductoRepository productos;
	@Mock
	private MarcaRepository marcas;
	@Mock
	private CategoriaRepository categorias;
	@Mock
	private UnidadMedidaRepository unidades;
	@Mock
	private TarifaIvaRepository tarifas;

	@InjectMocks
	private ProductoService servicio;

	private final Marca stanley = marca(1L, "Stanley", true);
	private final Categoria herramientas = categoria(1L, "Herramientas", true);
	private final UnidadMedida und = unidad("UND", true);
	private final TarifaIva iva19 = tarifa((short) 3, "19");

	@AfterEach
	void limpiarSeguridad() {
		SecurityContextHolder.clearContext();
	}

	// ------------------------------------------------------------------ crear

	@Nested
	@DisplayName("crear")
	class Crear {

		@Test
		@DisplayName("[CA-RF-PRO-001-1] Un producto válido se guarda con nombre y descripción recortados y estado ACTIVO")
		void creaProductoValido() {
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			referenciasCompletas();
			when(productos.existeDuplicado(1L, "Martillo", -1L)).thenReturn(false);
			when(productos.saveAndFlush(any(Producto.class))).thenAnswer(i -> conId(i.getArgument(0), ID));

			ProductoRespuesta creado = servicio.crear(peticion("  Martillo  ").descripcion("  Mango de fibra ").build());

			ArgumentCaptor<Producto> guardado = ArgumentCaptor.forClass(Producto.class);
			verify(productos).saveAndFlush(guardado.capture());
			assertThat(guardado.getValue().getNombre()).isEqualTo("Martillo");
			assertThat(guardado.getValue().getDescripcion()).isEqualTo("Mango de fibra");
			assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoProducto.ACTIVO);
			assertThat(guardado.getValue().getMarca()).isSameAs(stanley);
			assertThat(creado.id()).isEqualTo(ID);
			assertThat(creado.marca().nombre()).isEqualTo("Stanley");
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-3] Un duplicado de nombre y marca lanza conflicto con nombre y marca, y no guarda")
		void rechazaDuplicado() {
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			when(productos.existeDuplicado(1L, "Martillo", -1L)).thenReturn(true);

			assertThatThrownBy(() -> servicio.crear(peticion("Martillo").build()))
					.isInstanceOfSatisfying(ConflictoException.class, e -> assertThat(e.getArgumentos())
							.containsExactly("Martillo", "Stanley"));
			verify(productos, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-2] Una marca inactiva se rechaza en el campo marcaId")
		void rechazaMarcaInactiva() {
			when(marcas.findById(2L)).thenReturn(Optional.of(marca(2L, "Vieja", false)));

			assertThatThrownBy(() -> servicio.crear(peticion("Martillo").marcaId(2L).build()))
					.isInstanceOfSatisfying(CampoInvalidoException.class,
							e -> assertThat(e.getCampo()).isEqualTo("marcaId"));
			verify(productos, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-2] Una marca inexistente se rechaza en el campo marcaId")
		void rechazaMarcaInexistente() {
			when(marcas.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> servicio.crear(peticion("Martillo").marcaId(99L).build()))
					.isInstanceOfSatisfying(CampoInvalidoException.class,
							e -> assertThat(e.getCampo()).isEqualTo("marcaId"));
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-2] Categoría, unidad base o tarifa de IVA inexistentes se rechazan en su campo")
		void rechazaReferenciasInexistentes() {
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			when(productos.existeDuplicado(anyLong(), anyString(), anyLong())).thenReturn(false);
			when(categorias.findById(1L)).thenReturn(Optional.of(herramientas));
			when(categorias.findById(9L)).thenReturn(Optional.empty());
			when(unidades.findById("UND")).thenReturn(Optional.of(und));
			when(unidades.findById("XYZ")).thenReturn(Optional.empty());
			when(tarifas.findById((short) 9)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> servicio.crear(peticion("A1x").categoriaId(9L).build()))
					.isInstanceOfSatisfying(CampoInvalidoException.class,
							e -> assertThat(e.getCampo()).isEqualTo("categoriaId"));
			assertThatThrownBy(() -> servicio.crear(peticion("A2x").unidadBase("XYZ").build()))
					.isInstanceOfSatisfying(CampoInvalidoException.class,
							e -> assertThat(e.getCampo()).isEqualTo("unidadBase"));
			assertThatThrownBy(() -> servicio.crear(peticion("A3x").tarifaIvaId((short) 9).build()))
					.isInstanceOfSatisfying(CampoInvalidoException.class,
							e -> assertThat(e.getCampo()).isEqualTo("tarifaIvaId"));
			verify(productos, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-1] Con garantía, el plazo se guarda como meses (short); una descripción en blanco queda nula")
		void guardaPlazoDeGarantia() {
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			referenciasCompletas();
			when(productos.existeDuplicado(1L, "Taladro", -1L)).thenReturn(false);
			when(productos.saveAndFlush(any(Producto.class))).thenAnswer(i -> conId(i.getArgument(0), ID));

			ProductoRespuesta creado = servicio.crear(peticion("Taladro").control(ControlProducto.GARANTIA)
					.plazoGarantiaMeses(12).descripcion("   ").estado(EstadoProducto.INACTIVO).build());

			assertThat(creado.control()).isEqualTo(ControlProducto.GARANTIA);
			assertThat(creado.plazoGarantiaMeses()).isEqualTo((short) 12);
			assertThat(creado.descripcion()).isNull();
			assertThat(creado.estado()).isEqualTo(EstadoProducto.INACTIVO);
		}

	}

	// ------------------------------------------------------------------ actualizar

	@Nested
	@DisplayName("actualizar")
	class Actualizar {

		@Test
		@DisplayName("Reemplaza los datos de un producto existente")
		void actualizaProducto() {
			Producto existente = producto(ID, "Martillo", stanley);
			Categoria pinturas = categoria(4L, "Pinturas", true);
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(existente));
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			when(productos.existeDuplicado(1L, "Martillo grande", ID)).thenReturn(false);
			when(unidades.findById("UND")).thenReturn(Optional.of(und));
			when(categorias.findById(4L)).thenReturn(Optional.of(pinturas));
			when(tarifas.findById((short) 3)).thenReturn(Optional.of(iva19));
			when(productos.saveAndFlush(existente)).thenReturn(existente);

			ProductoRespuesta resultado = servicio.actualizar(ID, peticion("Martillo grande").categoriaId(4L)
					.porcentajeIncremento(new BigDecimal("45.5")).build());

			assertThat(resultado.nombre()).isEqualTo("Martillo grande");
			assertThat(resultado.categoria().nombre()).isEqualTo("Pinturas");
			assertThat(existente.getPorcentajeIncremento()).isEqualByComparingTo("45.5");
			assertThat(existente.getEstado()).as("sin estado en la petición no cambia").isEqualTo(EstadoProducto.ACTIVO);
		}

		@Test
		@DisplayName("Conserva la marca, categoría y tarifa actuales aunque se hayan desactivado después")
		void conservaReferenciasInactivasPropias() {
			Marca inactiva = marca(7L, "Descontinuada", false);
			Categoria categoriaInactiva = categoria(8L, "Antigua", false);
			TarifaIva tarifaInactiva = tarifa((short) 4, "16");
			ReflectionTestUtils.setField(tarifaInactiva, "activo", false);
			Producto existente = new Producto("Martillo", inactiva, categoriaInactiva, und, tarifaInactiva);
			conId(existente, ID);
			existente.setControl(ControlProducto.NINGUNO);
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(existente));
			when(marcas.findById(7L)).thenReturn(Optional.of(inactiva));
			when(productos.existeDuplicado(7L, "Martillo", ID)).thenReturn(false);
			when(unidades.findById("UND")).thenReturn(Optional.of(und));
			when(categorias.findById(8L)).thenReturn(Optional.of(categoriaInactiva));
			when(tarifas.findById((short) 4)).thenReturn(Optional.of(tarifaInactiva));
			when(productos.saveAndFlush(existente)).thenReturn(existente);

			ProductoRespuesta resultado = servicio.actualizar(ID,
					peticion("Martillo").marcaId(7L).categoriaId(8L).tarifaIvaId((short) 4).build());

			assertThat(resultado.marca().nombre()).isEqualTo("Descontinuada");
		}

		@Test
		@DisplayName("Un producto inexistente lanza RecursoNoEncontradoException")
		void noEncontrado() {
			when(productos.buscarCompleto(ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> servicio.actualizar(ID, peticion("Martillo").build()))
					.isInstanceOf(RecursoNoEncontradoException.class);
			verify(productos, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-3] Un nombre que ya usa otro producto de la marca lanza conflicto")
		void duplicadoAlEditar() {
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(producto(ID, "Martillo", stanley)));
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			when(productos.existeDuplicado(1L, "Alicate", ID)).thenReturn(true);

			assertThatThrownBy(() -> servicio.actualizar(ID, peticion("Alicate").build()))
					.isInstanceOf(ConflictoException.class);
			verify(productos, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("[RN-05] La unidad base no cambia si el producto tiene presentaciones")
		void unidadNoEditableConPresentaciones() {
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(producto(ID, "Martillo", stanley)));
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			when(productos.existeDuplicado(1L, "Martillo", ID)).thenReturn(false);
			when(unidades.findById("KG")).thenReturn(Optional.of(unidad("KG", false)));
			when(productos.tienePresentaciones(ID)).thenReturn(true);

			assertThatThrownBy(() -> servicio.actualizar(ID, peticion("Martillo").unidadBase("KG").build()))
					.isInstanceOfSatisfying(OperacionRechazadaException.class, e -> {
						assertThat(e.getCodigo()).isEqualTo(CodigoError.OPERACION_NO_PERMITIDA);
						assertThat(e.getClaveMensaje()).isEqualTo("producto.unidad_no_editable");
					});
			verify(productos, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("[RN-05] La unidad base no cambia si el producto tiene lotes")
		void unidadNoEditableConLotes() {
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(producto(ID, "Martillo", stanley)));
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			when(productos.existeDuplicado(1L, "Martillo", ID)).thenReturn(false);
			when(unidades.findById("KG")).thenReturn(Optional.of(unidad("KG", false)));
			when(productos.tienePresentaciones(ID)).thenReturn(false);
			when(productos.tieneLotes(ID)).thenReturn(true);

			assertThatThrownBy(() -> servicio.actualizar(ID, peticion("Martillo").unidadBase("KG").build()))
					.isInstanceOf(OperacionRechazadaException.class);
		}

		@Test
		@DisplayName("[RN-18] El tipo de control no cambia si el producto tiene lotes")
		void controlNoEditableConLotes() {
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(producto(ID, "Martillo", stanley)));
			when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
			when(productos.existeDuplicado(1L, "Martillo", ID)).thenReturn(false);
			when(unidades.findById("UND")).thenReturn(Optional.of(und));
			when(productos.tieneLotes(ID)).thenReturn(true);

			assertThatThrownBy(() -> servicio.actualizar(ID,
					peticion("Martillo").control(ControlProducto.VENCIMIENTO).build()))
					.isInstanceOfSatisfying(OperacionRechazadaException.class,
							e -> assertThat(e.getClaveMensaje()).isEqualTo("producto.control_no_editable"));
		}

	}

	// ------------------------------------------------------------------ desactivar

	@Nested
	@DisplayName("desactivar")
	class Desactivar {

		@Test
		@DisplayName("Deja el producto INACTIVO y lo guarda")
		void desactiva() {
			Producto existente = producto(ID, "Martillo", stanley);
			when(productos.findById(ID)).thenReturn(Optional.of(existente));

			servicio.inactivar(ID);

			assertThat(existente.getEstado()).isEqualTo(EstadoProducto.INACTIVO);
			verify(productos).saveAndFlush(existente);
		}

		@Test
		@DisplayName("Si ya estaba INACTIVO no escribe nada (ni auditoría)")
		void yaInactivoNoGuarda() {
			Producto existente = producto(ID, "Martillo", stanley);
			existente.setEstado(EstadoProducto.INACTIVO);
			when(productos.findById(ID)).thenReturn(Optional.of(existente));

			servicio.inactivar(ID);

			verify(productos, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("Un producto inexistente lanza RecursoNoEncontradoException")
		void noEncontrado() {
			when(productos.findById(ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> servicio.inactivar(ID)).isInstanceOf(RecursoNoEncontradoException.class);
		}

	}

	// ------------------------------------------------------------------ regla de garantía (validación de la petición)

	@Nested
	@DisplayName("regla de garantía y obligatorios")
	class ReglaGarantia {

		private static ValidatorFactory fabrica;
		private static Validator validador;

		@org.junit.jupiter.api.BeforeAll
		static void crearValidador() {
			fabrica = Validation.buildDefaultValidatorFactory();
			validador = fabrica.getValidator();
		}

		@org.junit.jupiter.api.AfterAll
		static void cerrarValidador() {
			fabrica.close();
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-2] Control GARANTIA sin plazo es inválido en plazoGarantiaMeses")
		void garantiaSinPlazo() {
			assertThat(campos(peticion("Martillo").control(ControlProducto.GARANTIA).build()))
					.containsExactly("plazoGarantiaMeses");
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-2] Un plazo con control distinto de GARANTIA es inválido")
		void plazoSinGarantia() {
			assertThat(campos(peticion("Martillo").control(ControlProducto.VENCIMIENTO).plazoGarantiaMeses(6).build()))
					.containsExactly("plazoGarantiaMeses");
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-2] Sin marca, sin unidad base y sin tarifa de IVA, cada campo reporta su error")
		void sinObligatorios() {
			assertThat(campos(peticion("Martillo").marcaId(null).unidadBase(null).tarifaIvaId(null).build()))
					.containsExactlyInAnyOrder("marcaId", "unidadBase", "tarifaIvaId");
		}

		@Test
		@DisplayName("[CA-RF-PRO-001-1] Una petición completa con garantía y plazo es válida")
		void peticionValida() {
			assertThat(campos(peticion("Martillo").control(ControlProducto.GARANTIA).plazoGarantiaMeses(6).build()))
					.isEmpty();
		}

		private Set<String> campos(ProductoPeticion datos) {
			Set<ConstraintViolation<ProductoPeticion>> errores = validador.validate(datos);
			return errores.stream().map(v -> v.getPropertyPath().toString())
					.collect(java.util.stream.Collectors.toSet());
		}

	}

	// ------------------------------------------------------------------ consultar: costos y orden

	@Nested
	@DisplayName("consultar")
	class Consultar {

		@Test
		@DisplayName("[CA-RF-PRO-003-3] Sin el permiso COSTOS_VER no se devuelven margen ni último costo")
		void sinPermisoNoVeCostos() {
			autenticar("CATALOGO_CONSULTAR");
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(productoConCosto()));

			ProductoRespuesta respuesta = servicio.obtener(ID);

			assertThat(respuesta.porcentajeIncremento()).isNull();
			assertThat(respuesta.ultimoCostoBase()).isNull();
		}

		@Test
		@DisplayName("[CA-RF-PRO-003-3] Con el permiso COSTOS_VER se devuelven margen y último costo")
		void conPermisoVeCostos() {
			autenticar("CATALOGO_CONSULTAR", ProductoService.PERMISO_COSTOS);
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(productoConCosto()));

			ProductoRespuesta respuesta = servicio.obtener(ID);

			assertThat(respuesta.porcentajeIncremento()).isEqualByComparingTo("30");
			assertThat(respuesta.ultimoCostoBase()).isEqualByComparingTo("18000");
		}

		@Test
		@DisplayName("[CA-RF-PRO-003-3] Sin autenticación tampoco se devuelven costos")
		void sinAutenticacionNoVeCostos() {
			when(productos.buscarCompleto(ID)).thenReturn(Optional.of(productoConCosto()));

			assertThat(servicio.obtener(ID).ultimoCostoBase()).isNull();
		}

		@Test
		@DisplayName("Un producto inexistente lanza RecursoNoEncontradoException")
		void obtenerNoEncontrado() {
			when(productos.buscarCompleto(ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> servicio.obtener(ID)).isInstanceOf(RecursoNoEncontradoException.class);
		}

		@Test
		@DisplayName("La búsqueda traduce el orden a propiedades JPA y agrega id como desempate")
		void ordenTraducidoConDesempate() {
			when(productos.findAll(ArgumentMatchers.<Specification<Producto>>any(), any(Pageable.class)))
					.thenAnswer(i -> new PageImpl<>(List.of(producto(ID, "Martillo", stanley)), (Pageable) i.getArgument(1), 1));

			Pagina<ProductoRespuesta> pagina = servicio.listar("mar", null, null,
					PageRequest.of(0, 20, Sort.by(Sort.Order.desc("marca"))));

			ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
			verify(productos).findAll(ArgumentMatchers.<Specification<Producto>>any(), pedido.capture());
			assertThat(pedido.getValue().getSort())
					.containsExactly(Sort.Order.desc("marca.nombre"), Sort.Order.asc("id"));
			assertThat(pagina.contenido()).extracting(ProductoRespuesta::nombre).containsExactly("Martillo");
			assertThat(pagina.totalElementos()).isEqualTo(1);
		}

		@Test
		@DisplayName("Sin orden pedido se ordena por nombre y luego por id")
		void ordenPorDefecto() {
			when(productos.findAll(ArgumentMatchers.<Specification<Producto>>any(), any(Pageable.class)))
					.thenAnswer(i -> new PageImpl<Producto>(List.of(), (Pageable) i.getArgument(1), 0));

			servicio.listar(null, null, null, PageRequest.of(0, 20));

			ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
			verify(productos).findAll(ArgumentMatchers.<Specification<Producto>>any(), pedido.capture());
			assertThat(pedido.getValue().getSort()).containsExactly(Sort.Order.asc("nombre"), Sort.Order.asc("id"));
		}

		@Test
		@DisplayName("[CA-RNF-INT-002-1] Un campo de orden desconocido se rechaza en el campo sort, sin consultar la base")
		void ordenDesconocido() {
			assertThatThrownBy(() -> servicio.listar(null, null, null, PageRequest.of(0, 20, Sort.by("costo"))))
					.isInstanceOfSatisfying(CampoInvalidoException.class,
							e -> assertThat(e.getCampo()).isEqualTo("sort"));
			verify(productos, never()).findAll(ArgumentMatchers.<Specification<Producto>>any(), any(Pageable.class));
		}

		private Producto productoConCosto() {
			Producto producto = producto(ID, "Martillo", stanley);
			ReflectionTestUtils.setField(producto, "ultimoCostoBase", new BigDecimal("18000"));
			return producto;
		}

		private void autenticar(String... permisos) {
			TestingAuthenticationToken autenticacion = new TestingAuthenticationToken("4", null, permisos);
			SecurityContextHolder.getContext().setAuthentication(autenticacion);
		}

	}

	// ------------------------------------------------------------------ utilidades

	private void referenciasCompletas() {
		when(categorias.findById(1L)).thenReturn(Optional.of(herramientas));
		when(unidades.findById("UND")).thenReturn(Optional.of(und));
		when(tarifas.findById((short) 3)).thenReturn(Optional.of(iva19));
	}

	private Producto producto(long id, String nombre, Marca marca) {
		Producto producto = new Producto(nombre, marca, herramientas, und, iva19);
		producto.setPorcentajeIncremento(new BigDecimal("30"));
		producto.setControl(ControlProducto.NINGUNO);
		return conId(producto, id);
	}

	private static ProductoPeticion.ProductoPeticionBuilder peticion(String nombre) {
		return ProductoPeticion.builder()
				.nombre(nombre)
				.categoriaId(1L)
				.marcaId(1L)
				.unidadBase("UND")
				.tarifaIvaId((short) 3)
				.porcentajeIncremento(new BigDecimal("30"))
				.control(ControlProducto.NINGUNO);
	}

	private static Producto conId(Producto producto, long id) {
		ReflectionTestUtils.setField(producto, "id", id);
		return producto;
	}

	private static Marca marca(long id, String nombre, boolean activo) {
		Marca marca = new Marca();
		ReflectionTestUtils.setField(marca, "id", id);
		ReflectionTestUtils.setField(marca, "nombre", nombre);
		ReflectionTestUtils.setField(marca, "activo", activo);
		return marca;
	}

	private static Categoria categoria(long id, String nombre, boolean activo) {
		Categoria categoria = new Categoria();
		ReflectionTestUtils.setField(categoria, "id", id);
		ReflectionTestUtils.setField(categoria, "nombre", nombre);
		ReflectionTestUtils.setField(categoria, "activo", activo);
		return categoria;
	}

	private static UnidadMedida unidad(String codigo, boolean discreta) {
		UnidadMedida unidad = new UnidadMedida();
		ReflectionTestUtils.setField(unidad, "codigo", codigo);
		ReflectionTestUtils.setField(unidad, "nombre", codigo);
		ReflectionTestUtils.setField(unidad, "esDiscreta", discreta);
		return unidad;
	}

	private static TarifaIva tarifa(short id, String porcentaje) {
		TarifaIva tarifa = new TarifaIva();
		ReflectionTestUtils.setField(tarifa, "id", id);
		ReflectionTestUtils.setField(tarifa, "nombre", "IVA " + porcentaje + " %");
		ReflectionTestUtils.setField(tarifa, "porcentaje", new BigDecimal(porcentaje));
		ReflectionTestUtils.setField(tarifa, "activo", true);
		return tarifa;
	}

}
