package com.example.products.inventory.application;

import com.example.products.inventory.persistence.InventoryReservationEntity;
import com.example.products.inventory.persistence.InventoryReservationItem;
import com.example.products.inventory.persistence.InventoryReservationRepository;
import com.example.products.inventory.presentation.ReserveInventoryRequest;
import com.example.products.product.persistence.ProductEntity;
import com.example.products.product.persistence.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InventoryReservationService {

    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryReservationService(
            ProductRepository productRepository,
            InventoryReservationRepository reservationRepository
    ) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public InventoryReservationEntity reserve(ReserveInventoryRequest request) {
        var existing = reservationRepository.findById(request.reservationId());
        if (existing.isPresent()) {
            if (!matchesRequest(existing.get(), request)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "La reserva ya se creó con otros productos o cantidades"
                );
            }
            return existing.get();
        }

        InventoryReservationEntity reservation = new InventoryReservationEntity(request.reservationId());
        reservationRepository.saveAndFlush(reservation);

        Set<Long> productIds = new HashSet<>();
        List<InventoryReservationItem> items = new ArrayList<>();
        var requestedItems = request.items().stream()
                .sorted(Comparator.comparing(item -> item.productId()))
                .toList();

        for (var requestedItem : requestedItems) {
            if (!productIds.add(requestedItem.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto está repetido en la reserva");
            }

            ProductEntity product = productRepository.findByIdForUpdate(requestedItem.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
            if (product.getStock() < requestedItem.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "No hay suficiente inventario para " + product.getName()
                );
            }

            product.setStock(product.getStock() - requestedItem.quantity());
            items.add(new InventoryReservationItem(
                    product.getId(), product.getName(), requestedItem.quantity(), product.getPrice()
            ));
        }

        reservation.setItems(items);
        return reservationRepository.save(reservation);
    }

    private boolean matchesRequest(InventoryReservationEntity reservation, ReserveInventoryRequest request) {
        var storedItems = reservation.getItems().stream()
                .sorted(Comparator.comparing(InventoryReservationItem::getProductId))
                .toList();
        var requestedItems = request.items().stream()
                .sorted(Comparator.comparing(item -> item.productId()))
                .toList();
        if (storedItems.size() != requestedItems.size()) {
            return false;
        }
        for (int index = 0; index < storedItems.size(); index++) {
            if (!storedItems.get(index).getProductId().equals(requestedItems.get(index).productId())
                    || !storedItems.get(index).getQuantity().equals(requestedItems.get(index).quantity())) {
                return false;
            }
        }
        return true;
    }
}
