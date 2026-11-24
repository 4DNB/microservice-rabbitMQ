package com.example.products.inventory.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservationEntity {

    @Id
    private UUID id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "inventory_reservation_items", joinColumns = @JoinColumn(name = "reservation_id"))
    private List<InventoryReservationItem> items = new ArrayList<>();

    protected InventoryReservationEntity() {
    }

    public InventoryReservationEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public List<InventoryReservationItem> getItems() {
        return items;
    }

    public void setItems(List<InventoryReservationItem> items) {
        this.items = new ArrayList<>(items);
    }
}
