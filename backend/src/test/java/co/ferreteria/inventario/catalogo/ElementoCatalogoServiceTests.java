/**
 * Nombre: ElementoCatalogoServiceTests.java
 * Descripción: Pruebas unitarias con Mockito de la administración de marcas y categorías: alta con nombre
 *              recortado, duplicados sin distinguir mayúsculas, edición, activación idempotente, 404 y orden.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import org.springframework.test.util.ReflectionTestUtils;

import co.ferreteria.inventario.catalogo.dto.ElementoCatalogoPeticion;
import co.ferreteria.inventario.catalogo.dto.ElementoCatalogoRespuesta;
import co.ferreteria.inventario.catalogo.dto.ReferenciaRespuesta;
import co.ferreteria.inventario.common.error.CampoInvalidoException;
import co.ferreteria.inventario.common.error.ConflictoException;
import co.ferreteria.inventario.common.error.RecursoNoEncontradoException;
import co.ferreteria.inventario.common.web.Pagina;

/**
 * Reglas de {@link ElementoCatalogoService} aisladas de la base, a través de {@link MarcaService} y
 * {@link CategoriaService} con los repositorios simulados (RF-CFG-001).
 */
@ExtendWith(MockitoExtension.class)
class ElementoCatalogoServiceTests {

	@Mock
	private MarcaRepository marcas;
	@Mock
	private CategoriaRepository categorias;

	// ------------------------------------------------------------------ crear

	@Test
	@DisplayName("Crear recorta el nombre, verifica duplicados sin distinguir mayúsculas y deja la marca ACTIVA")
	void crearRecortaYQuedaActiva() {
		MarcaService servicio = new MarcaService(marcas);
		when(marcas.existsByNombreIgnoreCaseAndIdNot("Bosch", -1L)).thenReturn(false);
		when(marcas.saveAndFlush(any(Marca.class))).thenAnswer(i -> conId(i.getArgument(0), 20L));

		ElementoCatalogoRespuesta creada = servicio.crear(new ElementoCatalogoPeticion("  Bosch  "));

		ArgumentCaptor<Marca> guardada = ArgumentCaptor.forClass(Marca.class);
		verify(marcas).saveAndFlush(guardada.capture());
		assertThat(guardada.getValue().getNombre()).isEqualTo("Bosch");
		assertThat(guardada.getValue().isActivo()).isTrue();
		assertThat(creada).isEqualTo(new ElementoCatalogoRespuesta(20L, "Bosch", EstadoCatalogo.ACTIVO));
	}

	@Test
	@DisplayName("Crear con un nombre que ya existe (otras mayúsculas) lanza ConflictoException con la clave de la entidad")
	void crearDuplicado() {
		CategoriaService servicio = new CategoriaService(categorias);
		when(categorias.existsByNombreIgnoreCaseAndIdNot("PLOMERÍA", -1L)).thenReturn(true);

		assertThatThrownBy(() -> servicio.crear(new ElementoCatalogoPeticion("PLOMERÍA")))
				.isInstanceOfSatisfying(ConflictoException.class, e -> {
					assertThat(e.getClaveMensaje()).isEqualTo("categoria.duplicada");
					assertThat(e.getArgumentos()).containsExactly("PLOMERÍA");
				});
		verify(categorias, never()).saveAndFlush(any());
	}

	// ------------------------------------------------------------------ editar

	@Test
	@DisplayName("Editar cambia el nombre excluyendo el propio id de la verificación de duplicados")
	void editarNombre() {
		MarcaService servicio = new MarcaService(marcas);
		Marca stanley = marca(1L, "Stanley", true);
		when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
		when(marcas.existsByNombreIgnoreCaseAndIdNot("Stanley Black", 1L)).thenReturn(false);
		when(marcas.saveAndFlush(stanley)).thenReturn(stanley);

		ElementoCatalogoRespuesta editada = servicio.actualizar(1L, new ElementoCatalogoPeticion(" Stanley Black "));

		assertThat(editada.nombre()).isEqualTo("Stanley Black");
		assertThat(editada.estado()).isEqualTo(EstadoCatalogo.ACTIVO);
	}

	@Test
	@DisplayName("Editar con el mismo nombre no escribe ni verifica duplicados")
	void editarSinCambios() {
		MarcaService servicio = new MarcaService(marcas);
		when(marcas.findById(1L)).thenReturn(Optional.of(marca(1L, "Stanley", false)));

		ElementoCatalogoRespuesta editada = servicio.actualizar(1L, new ElementoCatalogoPeticion("Stanley "));

		assertThat(editada.estado()).isEqualTo(EstadoCatalogo.INACTIVO);
		verify(marcas, never()).existsByNombreIgnoreCaseAndIdNot(anyString(), anyLong());
		verify(marcas, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Editar con el nombre de otra marca lanza ConflictoException")
	void editarDuplicado() {
		MarcaService servicio = new MarcaService(marcas);
		when(marcas.findById(1L)).thenReturn(Optional.of(marca(1L, "Stanley", true)));
		when(marcas.existsByNombreIgnoreCaseAndIdNot("truper", 1L)).thenReturn(true);

		assertThatThrownBy(() -> servicio.actualizar(1L, new ElementoCatalogoPeticion("truper")))
				.isInstanceOf(ConflictoException.class)
				.hasMessage("marca.duplicada");
	}

	@Test
	@DisplayName("Editar o cambiar el estado de un id inexistente lanza RecursoNoEncontradoException")
	void noEncontrado() {
		CategoriaService servicio = new CategoriaService(categorias);
		when(categorias.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.actualizar(99L, new ElementoCatalogoPeticion("Nueva")))
				.isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> servicio.cambiarEstado(99L, EstadoCatalogo.INACTIVO))
				.isInstanceOf(RecursoNoEncontradoException.class);
	}

	// ------------------------------------------------------------------ estado

	@Test
	@DisplayName("[CA-RF-CFG-001-3] Desactivar deja la marca INACTIVA sin eliminarla")
	void desactivar() {
		MarcaService servicio = new MarcaService(marcas);
		Marca stanley = marca(1L, "Stanley", true);
		when(marcas.findById(1L)).thenReturn(Optional.of(stanley));
		when(marcas.saveAndFlush(stanley)).thenReturn(stanley);

		ElementoCatalogoRespuesta resultado = servicio.cambiarEstado(1L, EstadoCatalogo.INACTIVO);

		assertThat(resultado.estado()).isEqualTo(EstadoCatalogo.INACTIVO);
		assertThat(stanley.isActivo()).isFalse();
		verify(marcas, never()).delete(any(Marca.class));
		verify(marcas, never()).deleteById(anyLong());
	}

	@Test
	@DisplayName("[CA-RF-CFG-001-3] Desactivar una categoría ya inactiva no escribe nada (idempotente)")
	void desactivarIdempotente() {
		CategoriaService servicio = new CategoriaService(categorias);
		when(categorias.findById(3L)).thenReturn(Optional.of(categoria(3L, "Plomería", false)));

		ElementoCatalogoRespuesta resultado = servicio.cambiarEstado(3L, EstadoCatalogo.INACTIVO);

		assertThat(resultado.estado()).isEqualTo(EstadoCatalogo.INACTIVO);
		verify(categorias, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Reactivar una categoría inactiva la deja ACTIVA")
	void reactivar() {
		CategoriaService servicio = new CategoriaService(categorias);
		Categoria plomeria = categoria(3L, "Plomería", false);
		when(categorias.findById(3L)).thenReturn(Optional.of(plomeria));
		when(categorias.saveAndFlush(plomeria)).thenReturn(plomeria);

		assertThat(servicio.cambiarEstado(3L, EstadoCatalogo.ACTIVO).estado()).isEqualTo(EstadoCatalogo.ACTIVO);
	}

	// ------------------------------------------------------------------ consultas

	@Test
	@DisplayName("[CA-RF-CFG-001-D1] Los activos para selectores salen del repositorio ordenados, como id y nombre")
	void listarActivos() {
		MarcaService servicio = new MarcaService(marcas);
		when(marcas.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(marca(2L, "Argos", true),
				marca(1L, "Stanley", true)));

		assertThat(servicio.listarActivos()).containsExactly(new ReferenciaRespuesta(2L, "Argos"),
				new ReferenciaRespuesta(1L, "Stanley"));
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName("La búsqueda ordena por nombre y agrega id como desempate")
	void listarConOrden() {
		MarcaService servicio = new MarcaService(marcas);
		when(marcas.findAll(any(Specification.class), any(Pageable.class)))
				.thenAnswer(i -> new PageImpl<>(List.of(marca(1L, "Stanley", true)), i.getArgument(1), 1));

		Pagina<ElementoCatalogoRespuesta> pagina = servicio.listar("sta", EstadoCatalogo.ACTIVO,
				PageRequest.of(0, 20, Sort.by(Sort.Order.desc("nombre"))));

		ArgumentCaptor<Pageable> pedida = ArgumentCaptor.forClass(Pageable.class);
		verify(marcas).findAll(any(Specification.class), pedida.capture());
		assertThat(pedida.getValue().getSort()).containsExactly(Sort.Order.desc("nombre"), Sort.Order.asc("id"));
		assertThat(pagina.contenido()).extracting(ElementoCatalogoRespuesta::nombre).containsExactly("Stanley");
		assertThat(pagina.totalElementos()).isEqualTo(1);
	}

	@Test
	@DisplayName("Ordenar por un campo no permitido lanza CampoInvalidoException en sort")
	void ordenNoValido() {
		MarcaService servicio = new MarcaService(marcas);

		assertThatThrownBy(() -> servicio.listar(null, null, PageRequest.of(0, 20, Sort.by("activo"))))
				.isInstanceOfSatisfying(CampoInvalidoException.class,
						e -> assertThat(e.getCampo()).isEqualTo("sort"));
		verify(marcas, never()).findAll(any(Specification.class), eq(PageRequest.of(0, 20)));
	}

	// ------------------------------------------------------------------ utilidades

	private static Marca conId(Marca marca, long id) {
		ReflectionTestUtils.setField(marca, "id", id);
		return marca;
	}

	private static Marca marca(long id, String nombre, boolean activo) {
		Marca marca = new Marca(nombre);
		ReflectionTestUtils.setField(marca, "id", id);
		marca.setActivo(activo);
		return marca;
	}

	private static Categoria categoria(long id, String nombre, boolean activo) {
		Categoria categoria = new Categoria(nombre);
		ReflectionTestUtils.setField(categoria, "id", id);
		categoria.setActivo(activo);
		return categoria;
	}

}
