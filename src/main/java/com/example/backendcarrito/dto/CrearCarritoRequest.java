package com.example.backendcarrito.dto;

import jakarta.validation.constraints.*;

public record CrearCarritoRequest(
    @NotBlank(message = "idCliente es obligatorio")
    String idCliente
) {}
