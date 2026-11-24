package com.example.products.product.presentation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void productTableSupportsCrudOperations() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Keyboard","description":"Mechanical","price":79.99,"stock":8}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Keyboard")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = created.get("id").asLong();

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", is(79.99)));

        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Keyboard Pro","description":"Updated","price":99.50,"stock":4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Keyboard Pro")))
                .andExpect(jsonPath("$.stock", is(4)));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsNegativePriceAndStock() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Invalid product","price":-1.00,"stock":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservesInventoryIdempotentlyAndRollsBackInsufficientMultiProductReservations() throws Exception {
        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Notebook","price":5.00,"stock":3}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asLong();

        UUID reservationId = UUID.randomUUID();
        String reservationRequest = """
                {"reservationId":"%s","items":[{"productId":%d,"quantity":2}]}
                """.formatted(reservationId, productId);

        mockMvc.perform(post("/internal/v1/inventory/reservations")
                        .header("X-Inventory-Service-Key", "test-inventory-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId", is(reservationId.toString())))
                .andExpect(jsonPath("$.items[0].unitPrice", is(5.0)));

        mockMvc.perform(post("/internal/v1/inventory/reservations")
                        .header("X-Inventory-Service-Key", "test-inventory-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequest))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(1)));

        MvcResult secondProductResult = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pen","price":2.00,"stock":3}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long secondProductId = objectMapper.readTree(secondProductResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/internal/v1/inventory/reservations")
                        .header("X-Inventory-Service-Key", "test-inventory-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservationId":"%s","items":[{"productId":%d,"quantity":1},{"productId":%d,"quantity":4}]}
                                """.formatted(UUID.randomUUID(), productId, secondProductId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(1)));
        mockMvc.perform(get("/api/v1/products/{id}", secondProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(3)));

        mockMvc.perform(delete("/api/v1/products/{id}", productId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/products/{id}", secondProductId))
                .andExpect(status().isNoContent());
    }

}
