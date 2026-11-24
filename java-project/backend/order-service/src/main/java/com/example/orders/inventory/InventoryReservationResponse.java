package com.example.orders.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InventoryReservationResponse(UUID reservationId, List<Item> items) {
    public record Item(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
    }
}
