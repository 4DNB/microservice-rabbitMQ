package com.example.products.inventory.presentation;

import com.example.products.inventory.persistence.InventoryReservationEntity;

import java.util.List;
import java.util.UUID;

public record InventoryReservationResponse(UUID reservationId, List<InventoryReservationItemResponse> items) {
    public static InventoryReservationResponse from(InventoryReservationEntity reservation) {
        return new InventoryReservationResponse(
                reservation.getId(),
                reservation.getItems().stream().map(InventoryReservationItemResponse::from).toList()
        );
    }
}
