package com.example.backendcarrito;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_TESTS", matches = "true")
class CarritoMongoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired MongoTemplate mongo;
    static final String DATABASE = "carrito_test_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://localhost:27017/" + DATABASE);
    }

    @AfterAll
    static void limpiar(@Autowired MongoTemplate template) {
        template.getDb().drop();
    }

    @Test
    void crudValidacionDocumentacionYPersistencia() throws Exception {
        String body = mvc.perform(post("/api/carritos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idCliente\":\"CLI_TEST\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.resumen.subtotal").value(0))
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        String url = "/api/carritos/" + id;
        mvc.perform(post(url + "/items").contentType(MediaType.APPLICATION_JSON).content("""
                {"idProducto":101,"nombre":"Producto","precioUnitario":50.10,"cantidad":2}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.resumen.subtotal").value(100.20));
        Document stored = mongo.getCollection("carritos").find().first();
        assertThat(stored.get("_id")).isInstanceOf(org.bson.types.ObjectId.class);
        assertThat(stored.getString("id_cliente")).isEqualTo("CLI_TEST");
        assertThat(stored.get("fecha_creacion")).isInstanceOf(java.util.Date.class);
        assertThat(stored.get("resumen", Document.class).get("subtotal"))
                .isInstanceOf(org.bson.types.Decimal128.class);
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].cantidad").value(2));
        mvc.perform(get("/api/carritos/cliente/CLI_TEST")).andExpect(status().isOk());
        mvc.perform(patch(url + "/items/101").contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resumen.subtotal").value(150.30));
        mvc.perform(patch(url + "/items/101").contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.cantidad").exists());
        mvc.perform(patch(url + "/estado").contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INVALIDO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/carritos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(url + "/items/999")).andExpect(status().isNotFound());
        mvc.perform(delete(url + "/items/101")).andExpect(status().isOk()).andExpect(jsonPath("$.resumen.totalArticulos").value(0));
        mvc.perform(delete(url + "/items")).andExpect(status().isOk());
        mvc.perform(patch(url + "/estado").contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"COMPLETADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COMPLETADO"));
        mvc.perform(get("/api/carritos/cliente/CLI_TEST")).andExpect(status().isNotFound());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths").exists());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(delete(url)).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }
}
