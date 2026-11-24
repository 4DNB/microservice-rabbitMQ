package com.example.orders.order.presentation;

import com.example.orders.inventory.InventoryClient;
import com.example.orders.inventory.InventoryClientException;
import com.example.orders.inventory.InventoryReservationResponse;
import com.example.orders.order.domain.OrderStatus;
import com.example.orders.order.persistence.OrderEntity;
import com.example.orders.order.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private InventoryClient inventoryClient;

    @BeforeEach
    void clearOrders() {
        orderRepository.deleteAll();
    }

    @Test
    void createsOrderWithUserReferenceAndReturnsExistingOrderForSameIdempotencyKey() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        String request = orderRequest();
        when(inventoryClient.reserve(any(UUID.class), anyList())).thenAnswer(invocation ->
                inventoryResponse(invocation.getArgument(0))
        );

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(42)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.total", is(10.0)));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(42)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        verify(inventoryClient, times(1)).reserve(any(UUID.class), anyList());
        List<OrderEntity> orders = orderRepository.findAll();
        org.junit.jupiter.api.Assertions.assertEquals(1, orders.size());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CONFIRMED, orders.getFirst().getStatus());
    }

    @Test
    void retriesPendingOrderWithSameKeyAfterInventoryServiceRecovers() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        String request = orderRequest();
        when(inventoryClient.reserve(any(UUID.class), anyList()))
                .thenThrow(new InventoryClientException(HttpStatus.SERVICE_UNAVAILABLE, "unavailable", null))
                .thenAnswer(invocation -> inventoryResponse(invocation.getArgument(0)));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isServiceUnavailable());

        OrderEntity pendingOrder = orderRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.PENDING, pendingOrder.getStatus());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        verify(inventoryClient, times(2)).reserve(any(UUID.class), anyList());
        org.junit.jupiter.api.Assertions.assertEquals(1, orderRepository.count());
    }

    @Test
    void rejectsReuseOfIdempotencyKeyWithDifferentRequest() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        when(inventoryClient.reserve(any(UUID.class), anyList())).thenAnswer(invocation ->
                inventoryResponse(invocation.getArgument(0))
        );

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":42,"customerName":"Taylor Customer","customerEmail":"taylor@example.com",
                                 "items":[{"productId":7,"quantity":3}]}
                                """))
                .andExpect(status().isConflict());
    }

    private String orderRequest() {
        return """
                {"userId":42,"customerName":"Taylor Customer","customerEmail":"taylor@example.com",
                 "items":[{"productId":7,"quantity":2}]}
                """;
    }

    private InventoryReservationResponse inventoryResponse(UUID reservationId) {
        return new InventoryReservationResponse(
                reservationId,
                List.of(new InventoryReservationResponse.Item(
                        7L, "Notebook", 2, new BigDecimal("5.00")
                ))
        );
    }
}
