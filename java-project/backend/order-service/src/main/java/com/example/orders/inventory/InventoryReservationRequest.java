package com.example.orders.inventory;

import java.util.List;
import java.util.UUID;

public record InventoryReservationRequest(UUID reservationId, List<InventoryReservationItemRequest> items) {
}
