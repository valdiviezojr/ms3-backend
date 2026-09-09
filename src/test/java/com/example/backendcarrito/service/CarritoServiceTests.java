package com.example.backendcarrito.service;

import com.example.backendcarrito.dto.*;
import com.example.backendcarrito.model.*;
import com.example.backendcarrito.repository.CarritoRepository;
import com.example.backendcarrito.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarritoServiceTests {
    private CarritoRepository repository;
    private CarritoService service;
    private Carrito carrito;

    @BeforeEach
    void preparar() {
        repository = mock(CarritoRepository.class);
        service = new CarritoService(repository);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        carrito = service.crearCarrito(new CrearCarritoRequest("CLI001"));
        carrito.setId("abc");
        when(repository.findById("abc")).thenReturn(Optional.of(carrito));
    }

    @Test
    void creaVacioConValoresIniciales() {
        assertThat(carrito.getEstado()).isEqualTo(EstadoCarrito.ACTIVO);
        assertThat(carrito.getItems()).isEmpty();
        assertThat(carrito.getResumen().getSubtotal()).isEqualByComparingTo("0");
        assertThat(carrito.getResumen().getTotalArticulos()).isZero();
        assertThat(carrito.getResumen().getMoneda()).isEqualTo("PEN");
        assertThat(carrito.getFechaCreacion()).isEqualTo(carrito.getFechaActualizacion());
    }

    @Test
    void recalculaDecimalesEnTodasLasOperaciones() {
        service.agregarItem("abc", item(101L, "0.10", 2));
        service.agregarItem("abc", item(102L, "0.20", 3));
        assertThat(carrito.getResumen().getSubtotal()).isEqualByComparingTo("0.80");
        assertThat(carrito.getResumen().getTotalArticulos()).isEqualTo(5);
        service.actualizarCantidadItem("abc", 101L, new ActualizarCantidadRequest(4));
        assertThat(carrito.getResumen().getSubtotal()).isEqualByComparingTo("1.00");
        service.eliminarItem("abc", 102L);
        assertThat(carrito.getResumen().getSubtotal()).isEqualByComparingTo("0.40");
        assertThat(carrito.getResumen().getTotalArticulos()).isEqualTo(4);
        service.vaciarCarrito("abc");
        assertThat(carrito.getItems()).isEmpty();
        assertThat(carrito.getResumen().getSubtotal()).isEqualByComparingTo("0");
        assertThat(carrito.getResumen().getTotalArticulos()).isZero();
    }

    @Test
    void productoRepetidoSumaCantidadYConservaPrecio() {
        service.agregarItem("abc", item(101L, "50.00", 2));
        service.agregarItem("abc", item(101L, "99.00", 1));
        assertThat(carrito.getItems()).hasSize(1);
        assertThat(carrito.getResumen().getSubtotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void recursosAusentesProducenErrores() {
        assertThatThrownBy(() -> service.obtenerPorId("otro"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.eliminarItem("abc", 999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.actualizarCantidadItem("abc", 999L, new ActualizarCantidadRequest(1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cambiaEstadoYEliminaCarrito() {
        service.cambiarEstado("abc", new CambiarEstadoRequest(EstadoCarrito.COMPLETADO));
        assertThat(carrito.getEstado()).isEqualTo(EstadoCarrito.COMPLETADO);
        service.eliminarCarrito("abc");
        verify(repository).delete(carrito);
    }

    @Test
    void rechazaOtroCarritoActivo() {
        when(repository.findByIdClienteAndEstado("CLI001", EstadoCarrito.ACTIVO)).thenReturn(Optional.of(carrito));
        assertThatThrownBy(() -> service.crearCarrito(new CrearCarritoRequest("CLI001")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AgregarItemRequest item(Long id, String precio, int cantidad) {
        return new AgregarItemRequest(id, "Producto", new BigDecimal(precio), null, null, cantidad);
    }
}
