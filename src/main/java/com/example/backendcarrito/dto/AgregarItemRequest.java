package com.example.backendcarrito.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AgregarItemRequest(
    @NotNull @Positive Long idProducto,
    @NotBlank String nombre,
    @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal precioUnitario,
    @org.hibernate.validator.constraints.URL String urlImagen,
    @org.hibernate.validator.constraints.URL String urlProducto,
    @NotNull @Positive Integer cantidad
) {}
