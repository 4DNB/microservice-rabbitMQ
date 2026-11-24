package com.example.orders.inventory;

import java.util.List;
import java.util.UUID;

public interface InventoryClient {
    InventoryReservationResponse reserve(UUID reservationId, List<InventoryReservationItemRequest> items);
}
