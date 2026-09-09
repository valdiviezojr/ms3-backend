package com.example.backendcarrito.service;

import com.example.backendcarrito.dto.*;
import com.example.backendcarrito.exception.RecursoNoEncontradoException;
import com.example.backendcarrito.model.*;
import com.example.backendcarrito.repository.CarritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CarritoService {
    private final CarritoRepository repository;

    public Carrito crearCarrito(CrearCarritoRequest request) {
        if (repository.findByIdClienteAndEstado(request.idCliente(), EstadoCarrito.ACTIVO).isPresent()) {
            throw new IllegalArgumentException("El cliente ya tiene un carrito activo");
        }
        Carrito carrito = new Carrito();
        carrito.setIdCliente(request.idCliente());
        carrito.setEstado(EstadoCarrito.ACTIVO);
        carrito.setItems(new ArrayList<>());
        carrito.setFechaCreacion(LocalDateTime.now());
        recalcular(carrito);
        carrito.setFechaActualizacion(carrito.getFechaCreacion());
        return repository.save(carrito);
    }

    public Carrito obtenerPorId(String id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Carrito no encontrado: " + id));
    }

    public Carrito obtenerCarritoActivoPorCliente(String clienteId) {
        return repository.findByIdClienteAndEstado(clienteId, EstadoCarrito.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe carrito activo para el cliente: " + clienteId));
    }

    public Carrito agregarItem(String id, AgregarItemRequest request) {
        Carrito carrito = obtenerPorId(id);
        ItemCarrito existente = carrito.getItems().stream()
                .filter(item -> item.getIdProducto().equals(request.idProducto()))
                .findFirst().orElse(null);
        if (existente == null) {
            carrito.getItems().add(crearItem(request));
        } else {
            existente.setCantidad(sumarCantidades(existente.getCantidad(), request.cantidad()));
        }
        return guardarConResumen(carrito);
    }

    // Punto de adaptación futuro: obtener los datos del producto mediante ProductoClient.
    private ItemCarrito crearItem(AgregarItemRequest request) {
        ItemCarrito item = new ItemCarrito();
        item.setIdProducto(request.idProducto());
        item.setNombre(request.nombre());
        item.setPrecioUnitario(request.precioUnitario());
        item.setUrlImagen(request.urlImagen());
        item.setUrlProducto(request.urlProducto());
        item.setCantidad(request.cantidad());
        item.setFechaAgregado(LocalDateTime.now());
        return item;
    }

    public Carrito actualizarCantidadItem(String id, Long productoId, ActualizarCantidadRequest request) {
        Carrito carrito = obtenerPorId(id);
        buscarItem(carrito, productoId).setCantidad(request.cantidad());
        return guardarConResumen(carrito);
    }

    public Carrito eliminarItem(String id, Long productoId) {
        Carrito carrito = obtenerPorId(id);
        carrito.getItems().remove(buscarItem(carrito, productoId));
        return guardarConResumen(carrito);
    }

    public Carrito vaciarCarrito(String id) {
        Carrito carrito = obtenerPorId(id);
        carrito.getItems().clear();
        return guardarConResumen(carrito);
    }

    public Carrito cambiarEstado(String id, CambiarEstadoRequest request) {
        Carrito carrito = obtenerPorId(id);
        if (request.estado() == EstadoCarrito.ACTIVO) {
            repository.findByIdClienteAndEstado(carrito.getIdCliente(), EstadoCarrito.ACTIVO)
                    .filter(activo -> !activo.getId().equals(id))
                    .ifPresent(activo -> { throw new IllegalArgumentException("El cliente ya tiene un carrito activo"); });
        }
        carrito.setEstado(request.estado());
        carrito.setFechaActualizacion(LocalDateTime.now());
        return repository.save(carrito);
    }

    public void eliminarCarrito(String id) {
        repository.delete(obtenerPorId(id));
    }

    private ItemCarrito buscarItem(Carrito carrito, Long productoId) {
        return carrito.getItems().stream().filter(item -> item.getIdProducto().equals(productoId))
                .findFirst().orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto no encontrado en el carrito: " + productoId));
    }

    private Carrito guardarConResumen(Carrito carrito) {
        recalcular(carrito);
        return repository.save(carrito);
    }

    private int sumarCantidades(int primera, int segunda) {
        try {
            return Math.addExact(primera, segunda);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("La cantidad total excede el máximo permitido");
        }
    }

    private void recalcular(Carrito carrito) {
        int cantidad = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrito item : carrito.getItems()) {
            cantidad = sumarCantidades(cantidad, item.getCantidad());
            subtotal = subtotal.add(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())));
        }
        ResumenCarrito resumen = new ResumenCarrito();
        resumen.setTotalArticulos(cantidad);
        resumen.setSubtotal(subtotal);
        resumen.setMoneda("PEN");
        carrito.setResumen(resumen);
        carrito.setFechaActualizacion(LocalDateTime.now());
    }
}
