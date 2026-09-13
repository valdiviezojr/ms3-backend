package com.example.backendcarrito.controller;

import com.example.backendcarrito.config.CorsConfig;
import com.example.backendcarrito.dto.CrearCarritoRequest;
import com.example.backendcarrito.exception.RecursoNoEncontradoException;
import com.example.backendcarrito.model.Carrito;
import com.example.backendcarrito.service.CarritoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarritoController.class)
@Import(CorsConfig.class)
class CarritoCorsTests {
    @Autowired MockMvc mvc;
    @MockitoBean CarritoService service;
    private static final String ORIGIN = "http://localhost:5173";

    @Test
    void permitePreflightDeCreacionDesdeVite() throws Exception {
        for (String origin : new String[] {ORIGIN, "http://127.0.0.1:5173"}) {
            mvc.perform(options("/api/carritos")
                    .header("Origin", origin)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
        verifyNoInteractions(service);
    }

    @Test
    void permiteLeerCarritoCreadoYEncontrado() throws Exception {
        Carrito carrito = new Carrito();
        carrito.setId("test");
        carrito.setIdCliente("CLI001");
        when(service.crearCarrito(any(CrearCarritoRequest.class))).thenReturn(carrito);
        when(service.obtenerCarritoActivoPorCliente("CLI001")).thenReturn(carrito);
        mvc.perform(post("/api/carritos").header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"idCliente\":\"CLI001\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(jsonPath("$.idCliente").value("CLI001"));
        mvc.perform(get("/api/carritos/cliente/CLI001").header("Origin", ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(jsonPath("$.idCliente").value("CLI001"));
    }

    @Test
    void permiteLeer404SinCarrito() throws Exception {
        when(service.obtenerCarritoActivoPorCliente("CLI001"))
                .thenThrow(new RecursoNoEncontradoException("No existe carrito activo para el cliente: CLI001"));
        mvc.perform(get("/api/carritos/cliente/CLI001").header("Origin", ORIGIN))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(jsonPath("$.mensaje").value("No existe carrito activo para el cliente: CLI001"));
    }

    @Test
    void permiteLeer400DeCarritoDuplicado() throws Exception {
        when(service.crearCarrito(any(CrearCarritoRequest.class)))
                .thenThrow(new IllegalArgumentException("El cliente ya tiene un carrito activo"));
        mvc.perform(post("/api/carritos").header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"idCliente\":\"CLI001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(jsonPath("$.mensaje").value("El cliente ya tiene un carrito activo"));
    }

    @Test
    void rechazaOrigenNoConfigurado() throws Exception {
        mvc.perform(options("/api/carritos").header("Origin", "https://otro.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
