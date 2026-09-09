package com.example.backendcarrito.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI carritoOpenApi() {
        return new OpenAPI().info(new Info().title("MS3 - Carrito de compras")
                .version("1.0.0").description("CRUD local de carritos con MongoDB"));
    }
}
