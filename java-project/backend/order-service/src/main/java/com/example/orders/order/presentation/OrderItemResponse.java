package com.example.orders.order.presentation;

import com.example.orders.order.persistence.OrderItemSnapshot;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
    public static OrderItemResponse from(OrderItemSnapshot item) {
        return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice());
    }
}
