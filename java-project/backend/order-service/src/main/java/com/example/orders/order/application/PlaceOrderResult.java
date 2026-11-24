package com.example.orders.order.application;

import com.example.orders.order.persistence.OrderEntity;

public record PlaceOrderResult(OrderEntity order, boolean created) {
}
