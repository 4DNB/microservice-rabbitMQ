package com.example.products.inventory.presentation;

import com.example.products.inventory.persistence.InventoryReservationItem;

import java.math.BigDecimal;

public record InventoryReservationItemResponse(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
    public static InventoryReservationItemResponse from(InventoryReservationItem item) {
        return new InventoryReservationItemResponse(
                item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice()
        );
    }
}
