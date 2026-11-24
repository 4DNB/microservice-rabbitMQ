package com.example.orders.order.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Email @Size(max = 254) String customerEmail,
        @NotEmpty @Size(max = 100) List<@Valid OrderItemRequest> items
) {
}
