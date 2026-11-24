package com.example.orders.order.presentation;

import com.example.orders.order.persistence.OrderEntity;
import com.example.orders.order.persistence.OrderItemSnapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        Long userId,
        String customerName,
        String customerEmail,
        String status,
        BigDecimal total,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(OrderEntity order) {
        List<OrderItemSnapshot> items = order.getItems();
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus().name(),
                order.getTotal(),
                order.getCreatedAt(),
                items.stream().map(OrderItemResponse::from).toList()
        );
    }
}
